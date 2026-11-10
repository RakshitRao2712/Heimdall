import { safeAiReferrer } from './seo/ai-referral';
import { useEffect, useState } from 'react';

// Measurement IDs identify a public stream; they are not API credentials.
const measurementId = 'G-Q5NJTPEFNN';
const consentKey = 'heimdall-analytics-consent';
type Consent = 'accepted' | 'rejected' | null;
type AnalyticsWindow = Window & { dataLayer?: unknown[]; gtag?: (...args: unknown[]) => void; [key: `ga-disable-${string}`]: boolean | undefined };
const analyticsWindow = () => window as unknown as AnalyticsWindow;
const routes: Record<string, string> = {
  '/': 'Heimdall home', '/pricing': 'Heimdall pricing', '/how-it-works': 'How Heimdall works',
  '/google-places-setup': 'Google Places setup', '/foursquare-setup': 'Foursquare setup',
  '/contact': 'Contact Heimdall', '/privacy': 'Heimdall privacy', '/terms': 'Heimdall terms',
  '/about': 'About Heimdall', '/refund': 'Heimdall refunds', '/login': 'Heimdall sign in', '/dashboard': 'Heimdall workspace',
};
function readConsent(): Consent { try { const value = localStorage.getItem(consentKey); return value === 'accepted' || value === 'rejected' ? value : null; } catch { return null; } }
function pageData() {
  const path = window.location.pathname.replace(/\/+$/, '') || '/';
  // Never transmit query strings, hashes, unknown paths, or DOM-derived page titles.
  return { page_location: `https://heimdall.life${routes[path] ? path : '/404'}`, page_title: routes[path] || 'Page not found', page_referrer: safeAiReferrer(document.referrer) };
}
let started = false;
function enableAnalytics() {
  if (window.location.hostname !== 'heimdall.life' || navigator.doNotTrack === '1') return;
  const w = analyticsWindow(); w[`ga-disable-${measurementId}`] = false;
  if (started) return;
  started = true;
  w.dataLayer = w.dataLayer || [];
  // Google's gtag consumes an Arguments object on the data layer.
  w.gtag = function () { w.dataLayer!.push(arguments); };
  w.gtag('consent', 'default', { analytics_storage: 'granted', ad_storage: 'denied', ad_user_data: 'denied', ad_personalization: 'denied' });
  w.gtag('set', pageData());
  w.gtag('js', new Date());
  w.gtag('config', measurementId, { send_page_view: true, allow_google_signals: false, allow_ad_personalization_signals: false, cookie_prefix: 'heimdall', cookie_flags: 'SameSite=Lax;Secure', ...pageData() });
  const script = document.createElement('script'); script.async = true; script.src = `https://www.googletagmanager.com/gtag/js?id=${measurementId}`; document.head.appendChild(script);
}
function disableAnalytics() {
  analyticsWindow()[`ga-disable-${measurementId}`] = true;
  // Disable future collection immediately; remove analytics cookies from this site.
  for (const cookie of document.cookie.split(';')) {
    const name = cookie.split('=')[0].trim();
    if (!name.startsWith('heimdall_ga')) continue;
    for (const domain of ['', ';domain=heimdall.life', ';domain=.heimdall.life']) document.cookie = `${name}=;Max-Age=0;path=/${domain};SameSite=Lax;Secure`;
  }
}
export function trackEvent(name: 'sign_up' | 'premium_activated' | 'search_completed', provider?: 'maps' | 'foursquare') {
  if (readConsent() !== 'accepted' || window.location.hostname !== 'heimdall.life' || navigator.doNotTrack === '1') return;
  enableAnalytics();
  analyticsWindow().gtag?.('event', name, { ...pageData(), ...(provider ? { discovery_provider: provider } : {}) });
}
export function AnalyticsConsent() {
  const [consent, setConsent] = useState<Consent>(readConsent);
  const [settings, setSettings] = useState(false);
  const available = window.location.hostname === 'heimdall.life';
  useEffect(() => { if (consent === 'accepted') enableAnalytics(); else disableAnalytics(); }, [consent]);
  function choose(value: Exclude<Consent, null>) { try { localStorage.setItem(consentKey, value); } catch { /* Consent remains effective for this page. */ } setConsent(value); setSettings(false); }
  if (!available) return null;
  return <>{(!consent || settings) ? <section className="analytics-consent" aria-label="Analytics preferences"><div><strong>A little insight. Your choice.</strong><p>Allow Google Analytics cookies to help us understand visits and improve Heimdall. Optional; rejecting won’t affect your access. <a href="/privacy">Privacy policy</a></p></div><div className="analytics-consent-actions"><button onClick={() => choose('rejected')}>Reject</button><button onClick={() => choose('accepted')}>Accept analytics</button></div></section> : <button className="analytics-preferences" onClick={() => setSettings(true)}>Cookie settings</button>}</>;
}
