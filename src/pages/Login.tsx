import { EmailVerification, type EmailChallenge } from '@/components/ui/email-verification';
import { useEffect, useState, type FormEvent } from 'react';
import { SignInPage } from '@/components/ui/sign-in';
import { authRequest, useSession } from '@/auth/session';
export default function Login() {
  const [create, setCreate] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [googleEnabled, setGoogleEnabled] = useState(false);
  const { setUser } = useSession();
  const [challenge, setChallenge] = useState<EmailChallenge | null>(null);
  function signedIn(user: { email: string }) {
    try { sessionStorage.setItem('heimdall-login-intro', 'pending'); } catch { /* Optional transition marker. */ }
    setUser(user); window.location.assign('/dashboard');
  }
  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/auth/google/config', { signal: controller.signal, credentials: 'same-origin' }).then(response => response.ok ? response.json() : null).then(config => setGoogleEnabled(config?.enabled === true)).catch(() => {});
    const result = new URLSearchParams(window.location.search).get('google');
    if (result === 'error') { setError('Google sign-in could not be completed. Try again, or use your email and password.'); window.history.replaceState({}, '', '/login'); }
    if (result === 'success') {
      window.history.replaceState({}, '', '/login');
      fetch('/api/auth/me', { signal: controller.signal, credentials: 'same-origin' }).then(response => { if (!response.ok) throw new Error(); return response.json(); }).then(signedIn).catch(() => { if (!controller.signal.aborted) setError('Your Google session could not be loaded. Please try again.'); });
    }
    return () => controller.abort();
  }, []);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (busy) return;
    const form = event.currentTarget; const data = new FormData(form);
    setBusy(true); setError(''); setNotice('');
    try {
      const result = await authRequest(create ? 'register' : 'login', { email: data.get('email'), password: data.get('password'), rememberMe: data.has('rememberMe') });
      form.reset();
      if (create) setChallenge(result);
      else signedIn(result);
    } catch (e) { setError(e instanceof Error ? e.message : 'Sign-in failed. Please try again.'); }
    finally { setBusy(false); }
  }
  if (challenge) return <div className="auth-page"><a className="auth-brand" href="/" aria-label="Heimdall home"><span className="auth-brand-identity"><img src="/images/heimdall-logo.svg" alt="" width="30" height="34"/><span>HEIMDALL</span></span><span>← Back to home</span></a><EmailVerification challenge={challenge} onChallenge={setChallenge} onVerified={signedIn} onBack={() => { setChallenge(null); setError(''); setNotice(''); }} /></div>;
  return <div className="auth-page bg-background text-foreground"><a className="auth-brand" href="/" aria-label="Heimdall home"><span className="auth-brand-identity"><img src="/images/heimdall-logo.svg" alt="" width="30" height="34"/><span>HEIMDALL</span></span><span>← Back to home</span></a><SignInPage title={create ? 'Start with Heimdall.' : 'Welcome to Heimdall.'} description={create ? 'Start with your email and a password of at least 10 characters. We’ll send a verification code before creating your account.' : 'Sign in to your lead workspace. Google Places discovery and copyright search, together.'} heroImageSrc="https://cdn.21st.dev/assets/mirror/ec/ecff1664e7fc3185d0e947571f984ea5fa3de9580fb0e73a03cd9c9b3461cb09.jpg" busy={busy} error={error} notice={notice} createAccount={create} onGoogleSignIn={googleEnabled ? () => { window.location.assign('/api/auth/google/authorize/google'); } : undefined} onSignIn={submit} onResetPassword={() => setNotice('Self-service password reset is not available yet. Contact the operator of this Heimdall instance for account recovery.')} onCreateAccount={() => { setCreate(!create); setError(''); setNotice(''); }}/></div>;
}
