import { trackEvent } from '@/analytics';
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { authRequest } from '@/auth/session';

export type EmailChallenge = { challengeId: string; email: string; expiresIn: number; resendAfter: number };
export function EmailVerification({ challenge, onChallenge, onVerified, onBack }: {
  challenge: EmailChallenge; onChallenge: (next: EmailChallenge) => void;
  onVerified: (user: { email: string }) => void; onBack: () => void;
}) {
  const [code, setCode] = useState(''); const [busy, setBusy] = useState(false);
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [resending, setResending] = useState(false);
  const [focused, setFocused] = useState(false); const [caret, setCaret] = useState(0);
  const input = useRef<HTMLInputElement>(null);
  const [started, setStarted] = useState(Date.now()); const [now, setNow] = useState(Date.now());
  useEffect(() => { setStarted(Date.now()); setCode(''); setCaret(0); setError(''); }, [challenge.challengeId]);
  useEffect(() => { const timer = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(timer); }, []);
  const remaining = Math.max(0, challenge.resendAfter - Math.max(0, Math.floor((now - started) / 1000)));
  const expired = now - started >= challenge.expiresIn * 1000;
  async function verify(event: FormEvent) {
    event.preventDefault(); if (busy || code.length !== 6 || expired) return;
    setBusy(true); setError(''); setNotice('');
    try { const user = await authRequest('register/verify', { challengeId: challenge.challengeId, code }); trackEvent('sign_up'); onVerified(user); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not verify this code. Please retry.'); input.current?.focus(); }
    finally { setBusy(false); }
  }
  async function resend() {
    if (busy || remaining > 0 || expired) return;
    setBusy(true); setResending(true); setError(''); setNotice('');
    try { onChallenge(await authRequest('register/resend', { challengeId: challenge.challengeId })); setNotice('A new code has been sent. Use the latest email.'); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not send a new code. Please retry.'); }
    finally { setBusy(false); setResending(false); }
  }
  return <main className="email-verify-page"><section className="email-verify-card" aria-labelledby="verify-title" aria-busy={busy}>
    <header><h1 id="verify-title"><span className="otp-heading-base">Enter the</span><span className="otp-heading-script">code</span></h1><p>Enter the six-digit code sent to <strong>{challenge.email}</strong></p></header>
    <form onSubmit={verify}>
      <label className="otp-sr-only" htmlFor="email-otp">Verification code</label>
      <div className={`otp-code-control${error ? ' otp-code-control--error' : ''}${busy ? ' otp-code-control--busy' : ''}`}>
        <div className="otp-digits" aria-hidden="true">{Array.from({length:6}, (_, i) => <span key={i} className={`otp-digit${code[i] ? ' is-filled' : ''}${focused && Math.min(caret,5) === i ? ' is-current' : ''}`}><span key={`${i}-${code[i] || ''}`}>{code[i] || ''}</span>{focused && !code[i] && Math.min(caret,5) === i && <i />}</span>)}</div>
        <input ref={input} id="email-otp" className="email-otp" name="otp" autoFocus required autoComplete="one-time-code" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} value={code} onFocus={() => setFocused(true)} onBlur={() => setFocused(false)} onSelect={e=>setCaret(e.currentTarget.selectionStart ?? code.length)} onChange={event => { const value = event.target.value.replace(/\D/g, '').slice(0,6); setCode(value); setCaret(event.target.selectionStart ?? value.length); setError(''); }} disabled={busy} aria-invalid={!!error} aria-describedby={`otp-feedback`} />
      </div>
      <div className="otp-feedback" id="otp-feedback" aria-live="polite">{error ? <p className="otp-error" role="alert">{error}</p> : expired ? <p className="otp-error">This code has expired. Start registration again to get a new code.</p> : notice ? <p className="otp-success">{notice}</p> : null}</div>
      <div className="otp-button-row">
        <button className="onboarding-continue" type="submit" disabled={busy || code.length !== 6 || expired}>{busy && !resending ? 'Verifying…' : 'Verify'}</button>
        <button className="otp-resend" type="button" disabled={busy || (!expired && remaining > 0)} onClick={() => expired ? onBack() : void resend()}>{expired ? 'Start again' : resending ? 'Sending…' : remaining > 0 ? `Resend code (${remaining}s)` : 'Resend code'}</button>
      </div>
    </form>
  </section></main>;
}
