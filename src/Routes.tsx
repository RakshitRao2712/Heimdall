import { lazy, Suspense } from 'react';
const Home = lazy(() => import('./App'));
const Login = lazy(() => import('./pages/Login'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
import { SessionProvider, useSession } from './auth/session';
function Route() {
  const { user, loading } = useSession(); const path = window.location.pathname;
  if (path !== '/login' && path !== '/dashboard') return <Home />;
  if (loading) return <div className="route-loading" role="status">Opening your Heimdall workspace…</div>;
  if (path === '/dashboard' && !user) { window.location.replace('/login'); return null; }
  if (path === '/login' && user) { window.location.replace('/dashboard'); return null; }
  return path === '/login' ? <Login /> : <Dashboard />;
}
export default function App() { return <SessionProvider><Suspense fallback={<div className="route-loading" role="status">Loading Heimdall…</div>}><Route /></Suspense></SessionProvider>; }
