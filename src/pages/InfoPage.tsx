import { PolicyLinks, SocialLinks } from "@/components/policy-links";
import { LifetimeOffer } from '@/components/lifetime-offer';
import TextLoop from '@/components/ui/text-loop';
import { SiteNav, publicLinks } from '@/components/site-nav';
import { useSession } from '@/auth/session';
import { ArrowRight, Check, KeyRound, MapPin, FolderOpen, Search } from 'lucide-react';

const setupUrl = 'https://developers.google.com/maps/documentation/places/web-service/get-api-key';
const billingUrl = 'https://developers.google.com/maps/documentation/places/web-service/usage-and-billing';

export default function InfoPage({ path }: { path: string }) {
  const { user } = useSession();
  const start = user ? '/dashboard' : '/login';
  const isPricing = path === '/pricing';
  const isBrave = path === '/brave-search-setup';
  const isSetup = path === '/google-places-setup' || isBrave;
  return <div className="info-page">
    <SiteNav />
    <main className="info-main">
      <a href="/" className="info-back">← Back to home</a>
      <p className="info-eyebrow">{isPricing ? 'Pricing' : isSetup ? (isBrave ? 'Brave Search Setup' : 'Google Places Setup') : 'How It Works'}</p>
      <h1>{isSetup ? <TextLoop staticText="Connect" rotatingTexts={[isBrave ? "Brave Search." : "Google Places.", "your API key.", "new opportunities."]} /> : !isPricing ? <TextLoop staticText="Find" rotatingTexts={["new leads.", "the right fit.", "your next customer."]} /> : <TextLoop staticText="One payment." rotatingTexts={["Full access.", "Lifetime value.", "More possibilities."]} />}</h1>
      <p className="info-intro">{isPricing ? '₹100 once for Heimdall Premium lifetime access. Google Places and Brave Search API usage is billed separately by each provider to your own account; API fees are not included in ₹100.' : isSetup ? (isBrave ? 'Use your own Brave Search API key to discover websites and check their copyright notices.' : 'Use a key from your own Google Cloud project to find businesses through Heimdall.') : 'Choose a discovery method, review the results, and keep your leads organized in one workspace.'}</p>
      {isPricing ? <LifetimeOffer /> : isSetup ? isBrave ? <><div className="info-steps">
<section className="info-card"><span className="info-step">01</span><h2>Create a Brave Search API account</h2><p>Register and verify your email at the <a href="https://api-dashboard.search.brave.com/" target="_blank" rel="noreferrer">Brave API dashboard ↗</a>. You can use any browser.</p></section>
<section className="info-card"><span className="info-step">02</span><h2>Activate a Search plan</h2><p>Choose a plan that includes Web Search and complete Brave’s billing setup. Review current allowances and charges in your own account before searching.</p></section>
<section className="info-card"><span className="info-step">03</span><h2>Create your API key</h2><p>Open API Keys in the Brave dashboard, add a key, and copy it. Keep this secret; never share it in a team note or public file.</p><a href="https://api-dashboard.search.brave.com/documentation/quickstart" target="_blank" rel="noreferrer">Brave’s setup guide ↗</a></section>
<section className="info-card"><span className="info-step">04</span><h2>Connect Website Copyright</h2><p>Open Scrape → Website Copyright, connect your Brave Search key, then enter a business type and location. Heimdall discovers websites and checks for 2023/2024 copyright notices and public phone numbers. Results are not guaranteed.</p><p>Your key stays in your backend session until sign-out or session expiry. Reconnect it in a new browser session. Greater search depth may make additional billable requests.</p><a className="info-cta" href={user ? '/dashboard?method=copyright' : '/login'}>Connect Brave Search <ArrowRight size={16}/></a></section>
</div><aside className="info-note"><h2>Your API usage, your billing</h2><p>Brave bills your own account under its current plan. The ₹100 Heimdall purchase excludes Brave Search and Google Places API charges. Check your key, plan, and usage limits if a search fails.</p><a href="/google-places-setup">Set up Google Places →</a></aside></> : <>
        <div className="info-steps">
          <section className="info-card"><span className="info-step">01</span><h2>Create a Google Cloud project</h2><p>Open <a href="https://console.cloud.google.com/" target="_blank" rel="noreferrer">Google Cloud Console ↗</a>, create or select a project, and link an active billing account.</p></section>
          <section className="info-card"><span className="info-step">02</span><h2>Enable Places API (New)</h2><p>In the API Library, find and enable Places API (New) for that project. Heimdall uses this API for its Google Places searches.</p></section>
          <section className="info-card"><span className="info-step">03</span><h2>Create and restrict your key</h2><p>Create an API key in Credentials and restrict its API access to Places API (New). Requests come from the Heimdall server: IP restrictions must allow that server's outbound IP, rather than your browser's website referrer.</p><a href={setupUrl} target="_blank" rel="noreferrer">Follow Google's setup guide ↗</a></section>
          <section className="info-card"><span className="info-step">04</span><h2>Connect inside Heimdall</h2><p>Sign in, open Scrape → Google Places, paste your key into the API key field, and save it. Enter a business type and location, then run a search.</p><p>Your key stays in your server session and clears when you sign out or the session expires. Reconnect it when needed.</p><a className="info-cta" href={user ? '/dashboard?method=maps' : '/login'}>Connect Google Places <ArrowRight size={16} /></a></section>
        </div>
        <aside className="info-note"><a href="/brave-search-setup">Set up Brave Search for Website Copyright →</a><h2>Having trouble connecting?</h2><p>Check that billing is active, Places API (New) is enabled, and your key restrictions allow the request. For quota errors, check your project's usage limits in Google Cloud.</p><a href={billingUrl} target="_blank" rel="noreferrer">Google usage, quotas and billing ↗</a></aside>
      </> : <>
        <div className="info-grid">
          <section className="info-card"><MapPin size={28} /><h2>Google Places</h2><p>Connect your own key, then search by business type and location. Find operational businesses without a listed website regardless of review count.</p><a href="/google-places-setup">Set up Google Places →</a></section>
          <section className="info-card"><Search size={28} /><h2>Website Copyright</h2><p>Search websites for a business type and location. Heimdall checks sites in parallel for older copyright years and public phone numbers, surfacing opportunities for a website refresh.</p><p>Requires your own Brave Search API key. Brave bills your usage separately; the ₹100 purchase excludes API fees. Results depend on available websites and search results.</p><a href="/brave-search-setup">Set up Brave Search →</a></section>
          <section className="info-card"><Check size={28} /><h2>Review your results</h2><p>See business details, available phone numbers, and search duration. Filter results by text or phone availability and revisit saved searches.</p></section>
          <section className="info-card"><FolderOpen size={28} /><h2>Build your lead library</h2><p>Save a completed search to Leads, give the file a useful name, and reopen it later. Export a CSV when you are ready to work with your list.</p><a className="info-cta" href={start}>Start discovering <ArrowRight size={16} /></a></section>
        </div>
      </>}
    </main>
    <footer className="info-footer"><a href="/">Heimdall</a><nav aria-label="Footer">{publicLinks.map(l => <a key={l.href} href={l.href} aria-current={path === l.href ? 'page' : undefined}>{l.label}</a>)}</nav><PolicyLinks/><SocialLinks/></footer>
  </div>;
}
