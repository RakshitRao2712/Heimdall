import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
export type AccountProfile = { firstName: string; lastName: string; username: string; website: string; completed: boolean };
export type User = { email: string };
const Session = createContext<{
  user: User | null; loading: boolean; setUser: (user: User | null) => void;
  profile: AccountProfile | null; profileError: string;
  setProfile: (profile: AccountProfile) => void; retryProfile: () => void;
}>({ user: null, loading: true, setUser: () => {}, profile: null, profileError: '', setProfile: () => {}, retryProfile: () => {} });
export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null); const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<AccountProfile | null>(null);
  const [profileError, setProfileError] = useState('');
  const [profileAttempt, setProfileAttempt] = useState(0);
  useEffect(() => { const controller = new AbortController(); fetch('/api/auth/me', { credentials: 'same-origin', signal: controller.signal }).then(async r => { if (r.ok) setUser(await r.json()); }).catch(() => {}).finally(() => { if (!controller.signal.aborted) setLoading(false); }); return () => controller.abort(); }, []);
  useEffect(() => {
    setProfile(null); setProfileError('');
    if (!user) return;
    const controller = new AbortController();
    fetch('/api/workspace/profile', { credentials: 'same-origin', cache: 'no-store', signal: controller.signal })
      .then(async response => {
        if (!response.ok) throw new Error('Cannot load your profile. Please retry.');
        const value = await response.json();
        if (typeof value.completed !== 'boolean') throw new Error('Restart the updated backend to enable profile setup.');
        if (!controller.signal.aborted) setProfile(value);
      }).catch(error => { if (!controller.signal.aborted) setProfileError(error instanceof Error ? error.message : 'Cannot load your profile.'); });
    return () => controller.abort();
  }, [user?.email, profileAttempt]);
  return <Session.Provider value={{ user, loading, setUser, profile, profileError, setProfile, retryProfile: () => setProfileAttempt(value => value + 1) }}>{children}</Session.Provider>;
}
export const useSession = () => useContext(Session);
export async function authRequest(path: string, data?: unknown) {
  const csrf = await fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' });
  if (!csrf.ok) throw new Error('Cannot connect to Heimdall. Check that the backend is running.');
  const { token, headerName } = await csrf.json();
  const response = await fetch(`/api/auth/${path}`, { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json', [headerName]: token }, body: data ? JSON.stringify(data) : undefined });
  if (!response.ok) {
    const messages: Record<number, string> = { 400: 'Use a valid email and a password of 10–72 bytes.', 401: 'Email or password is incorrect.', 403: 'Your session expired. Please try again.', 409: 'Unable to create this account. Try signing in.', 429: 'Too many attempts. Try again in 15 minutes.' };
    let message = '';
    if (path.startsWith('register')) {
      try { const body = await response.json(); message = body.detail || body.message || ''; } catch { /* Use status fallback. */ }
    }
    throw new Error(message || messages[response.status] || 'Heimdall could not complete this request. Try again shortly.');
  }
  return response.status === 204 ? null : response.json();
}

export async function placesKeyRequest(method: 'POST' | 'DELETE', apiKey?: string) {
  const csrf = await fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' });
  if (!csrf.ok) throw new Error('Cannot reach the backend. Please try again.');
  const { token, headerName } = await csrf.json();
  const response = await fetch('/api/workspace/places-key', { method, credentials: 'same-origin', headers: { 'Content-Type': 'application/json', [headerName]: token }, body: method === 'POST' ? JSON.stringify({ apiKey }) : undefined });
  if (response.status === 401) { window.location.replace('/login'); throw new Error('Please sign in again.'); }
  if (!response.ok) throw new Error(response.status === 400 ? 'Enter a valid Google Places API key.' : response.status === 403 ? 'Session expired. Please retry.' : 'Could not update your API key. Check the backend and retry.');
  return response.json() as Promise<{ configured: boolean }>;
}

export async function workspaceMutation<T>(path: string, method: 'POST' | 'PATCH', data: unknown): Promise<T> {
  const csrf = await fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' });
  if (!csrf.ok) throw new Error('Cannot reach the backend. Please try again.');
  const { token, headerName } = await csrf.json();
  const response = await fetch(`/api/workspace/${path}`, { method, credentials: 'same-origin', headers: { 'Content-Type': 'application/json', [headerName]: token }, body: JSON.stringify(data) });
  if (response.status === 401) { window.location.replace('/login'); throw new Error('Please sign in again.'); }
  if (!response.ok) { let message = ''; try { const body = await response.json(); message = body.detail || body.message || ''; } catch {} throw new Error(message || 'Could not save your changes. Please try again.'); }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}
