import { useState, type FormEvent } from 'react';
import { SignInPage } from '@/components/ui/sign-in';
import { authRequest, useSession } from '@/auth/session';
export default function Login() {
  const [create, setCreate] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const { setUser } = useSession();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (busy) return;
    const form = event.currentTarget; const data = new FormData(form);
    setBusy(true); setError(''); setNotice('');
    try {
      const user = await authRequest(create ? 'register' : 'login', { email: data.get('email'), password: data.get('password'), rememberMe: data.has('rememberMe') });
      try { sessionStorage.setItem('heimdall-login-intro', 'pending'); } catch { /* Sign-in still works without storage. */ }
      form.reset(); setUser(user); window.location.assign('/dashboard');
    } catch (e) { setError(e instanceof Error ? e.message : 'Sign-in failed. Please try again.'); }
    finally { setBusy(false); }
  }
  return <div className="auth-page bg-background text-foreground"><a className="auth-brand" href="/">HEIMDALL <span>← Back to home</span></a><SignInPage title={create ? 'Start with Heimdall.' : 'Welcome to Heimdall.'} description={create ? 'Create your account to discover local businesses and website opportunities. Use a password of at least 10 characters.' : 'Sign in to your lead workspace. Google Places discovery and copyright search, together.'} heroImageSrc="https://cdn.21st.dev/assets/mirror/ec/ecff1664e7fc3185d0e947571f984ea5fa3de9580fb0e73a03cd9c9b3461cb09.jpg" busy={busy} error={error} notice={notice} createAccount={create} onSignIn={submit} onResetPassword={() => setNotice('Self-service password reset is not available yet. Contact the operator of this Heimdall instance for account recovery.')} onCreateAccount={() => { setCreate(!create); setError(''); setNotice(''); }}/></div>;
}
