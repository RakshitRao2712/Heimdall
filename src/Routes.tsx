import AboutPage from './pages/AboutPage';
import { AnalyticsConsent } from './analytics';
import { WorkspaceConnection } from './components/workspace-connection';
import { SeoMetadata } from "./seo/metadata";
import { SiteIntro } from './components/ui/site-intro';
import { lazy, Suspense, useEffect, useState } from 'react';
const Onboarding = lazy(() => import('./pages/Onboarding'));
const PolicyPage = lazy(() => import('./pages/PolicyPage'));
const InfoPage = lazy(() => import('./pages/InfoPage'));
const DiscoveryPage = lazy(() => import('./pages/DiscoveryPage'));
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
  if (!profile) return <WorkspaceConnection />;
  if (!profile.completed) return <Onboarding onComplete={() => setReveal(true)} />;
  return <SiteIntro play={reveal}><Dashboard /></SiteIntro>;
}
function Route() {
  const { user, loading } = useSession(); const path = window.location.pathname.replace(/\/+$/, '') || '/';
  if (import.meta.env.DEV && path === '/connection-preview') return <WorkspaceConnection immediate />;
  if (['/google-places','/foursquare','/for-freelancers','/for-web-designers','/for-agencies','/guides','/guides/first-search'].includes(path)) return <DiscoveryPage path={path}/>;
  if (path === '/about') return <AboutPage />;
  if (['/contact', '/privacy', '/terms', '/refund'].includes(path)) return <PolicyPage path={path} />;
  if (path === '/brave-search-setup' || path === '/serpapi-setup') return <InfoPage path='/foursquare-setup' />;
  if (['/pricing', '/how-it-works', '/google-places-setup', '/foursquare-setup'].includes(path)) return <InfoPage path={path} />;
  if (path === '/') return <Home />;
  if (path !== '/login' && path !== '/dashboard') return <main className="info-main"><h1>Page not found</h1><p>This page does not exist.</p><a href="/">Back to Heimdall</a></main>;
  if (loading) return <WorkspaceConnection />;
  if (path === '/dashboard' && !user) { window.location.replace('/login'); return null; }
  if (path === '/login' && user) { if (new URLSearchParams(window.location.search).get('google') === 'success') { try { sessionStorage.setItem('heimdall-login-intro', 'pending'); } catch { /* Optional intro. */ } } window.location.replace('/dashboard'); return null; }
  return path === '/login' ? <Login /> : <DashboardEntry />;
}
export default function App() { return <SessionProvider><AnalyticsConsent/><SeoMetadata/><SiteIntro><Suspense fallback={<div className="route-loading" role="status">Loading Heimdall…</div>}><Route /></Suspense></SiteIntro></SessionProvider>; }
