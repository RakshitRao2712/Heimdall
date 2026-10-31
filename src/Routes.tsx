import { SiteIntro } from './components/ui/site-intro';
import { lazy, Suspense, useEffect, useState } from 'react';
const Onboarding = lazy(() => import('./pages/Onboarding'));
const InfoPage = lazy(() => import('./pages/InfoPage'));
const Home = lazy(() => import('./App'));
const Login = lazy(() => import('./pages/Login'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
import { SessionProvider, useSession } from './auth/session';
function DashboardEntry() {
  const { profile, profileError, retryProfile } = useSession();
  const [reveal, setReveal] = useState(() => {
    try { return sessionStorage.getItem('heimdall-login-intro') === 'pending'; } catch { return false; }
  });
  useEffect(() => { try { sessionStorage.removeItem('heimdall-login-intro'); } catch { /* Optional transition marker. */ } }, []);
  if (profileError) return <main className="onboarding-page"><div className="onboarding-card"><p role="alert">{profileError}</p><button className="onboarding-continue" onClick={retryProfile}>Retry</button></div></main>;
  if (!profile) return <div className="route-loading" role="status">Opening your Heimdall workspace…</div>;
  if (!profile.completed) return <Onboarding onComplete={() => setReveal(true)} />;
  return <SiteIntro play={reveal}><Dashboard /></SiteIntro>;
}
function Route() {
  const { user, loading } = useSession(); const path = window.location.pathname;
  if (['/pricing', '/how-it-works', '/google-places-setup'].includes(path)) return <InfoPage path={path} />;
  if (path !== '/login' && path !== '/dashboard') return <Home />;
  if (loading) return <div className="route-loading" role="status">Opening your Heimdall workspace…</div>;
  if (path === '/dashboard' && !user) { window.location.replace('/login'); return null; }
  if (path === '/login' && user) { if (new URLSearchParams(window.location.search).get('google') === 'success') { try { sessionStorage.setItem('heimdall-login-intro', 'pending'); } catch { /* Optional intro. */ } } window.location.replace('/dashboard'); return null; }
  return path === '/login' ? <Login /> : <DashboardEntry />;
}
export default function App() { return <SessionProvider><SiteIntro><Suspense fallback={<div className="route-loading" role="status">Loading Heimdall…</div>}><Route /></Suspense></SiteIntro></SessionProvider>; }
