import { useEffect, useState, type FormEvent } from 'react';
import { ArrowLeft, ArrowRight, MailCheck } from 'lucide-react';
import { authRequest } from '@/auth/session';

export type EmailChallenge = { challengeId: string; email: string; expiresIn: number; resendAfter: number };
export function EmailVerification({ challenge, onChallenge, onVerified, onBack }: {
  challenge: EmailChallenge; onChallenge: (next: EmailChallenge) => void;
  onVerified: (user: { email: string }) => void; onBack: () => void;
}) {
  const [code, setCode] = useState(''); const [busy, setBusy] = useState(false);
  const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [started, setStarted] = useState(Date.now()); const [now, setNow] = useState(Date.now());
  useEffect(() => { setStarted(Date.now()); setCode(''); }, [challenge.challengeId]);
  useEffect(() => { const timer = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(timer); }, []);
  const remaining = Math.max(0, challenge.resendAfter - Math.max(0, Math.floor((now - started) / 1000)));
  const expired = now - started >= challenge.expiresIn * 1000;
  async function verify(event: FormEvent) {
    event.preventDefault(); if (busy) return;
    setBusy(true); setError(''); setNotice('');
    try { onVerified(await authRequest('register/verify', { challengeId: challenge.challengeId, code })); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not verify this code. Please retry.'); }
    finally { setBusy(false); }
  }
  async function resend() {
    if (busy || remaining > 0) return;
    setBusy(true); setError(''); setNotice('');
    try { onChallenge(await authRequest('register/resend', { challengeId: challenge.challengeId })); setNotice('A new code has been sent. Use the latest email.'); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not send a new code. Please retry.'); }
    finally { setBusy(false); }
  }
  return <main className="email-verify-page"><section className="onboarding-card email-verify-card" aria-labelledby="verify-title">
    <MailCheck size={32} className="email-verify-icon" />
    <header><h1 id="verify-title">Check your email</h1><p>Enter the six-digit code sent to <strong>{challenge.email}</strong>.</p></header>
    <form onSubmit={verify}>
      <label htmlFor="email-otp">Verification code</label>
      <input id="email-otp" className="email-otp" name="otp" autoFocus required autoComplete="one-time-code" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} value={code} onChange={event => setCode(event.target.value.replace(/\D/g, ''))} disabled={busy} aria-describedby="otp-help" placeholder="000000" />
      <p id="otp-help" className="onboarding-note">{expired ? 'This code has expired. Start registration again to get a new code.' : 'Your code expires in 10 minutes. Check your spam folder too.'}</p>
      {error && <p className="profile-form-error" role="alert">{error}</p>}
      {notice && <p className="profile-form-success" role="status">{notice}</p>}
      <button className="onboarding-continue" type="submit" disabled={busy || code.length !== 6 || expired}>{busy ? 'Please wait…' : 'Verify & create account'}<ArrowRight size={17} /></button>
    </form>
    <div className="email-verify-actions"><button type="button" disabled={busy} onClick={onBack}><ArrowLeft size={14} />Change email</button><button type="button" disabled={busy || remaining > 0 || expired} onClick={() => void resend()}>{remaining > 0 ? `Resend in ${remaining}s` : 'Resend code'}</button></div>
  </section></main>;
}
