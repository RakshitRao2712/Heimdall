import { ContentReview } from '@/components/content-review';
import { MethodComparison } from '@/components/method-comparison';
import { Troubleshooting } from '@/components/troubleshooting';
import { PageFAQ } from '@/components/page-faq';
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
  const isWebSetup = path === '/foursquare-setup';
  const searchProvider = 'Foursquare';
  const providerDashboard = 'https://foursquare.com/developers/';
  const isSetup = path === '/google-places-setup' || isWebSetup;
  return <div className="info-page">
    <SiteNav />
    <main className="info-main">
      <a href="/" className="info-back">← Back to home</a>
      <p className="info-eyebrow">{isPricing ? 'Pricing' : isSetup ? (isWebSetup ? `${searchProvider} Setup` : 'Google Places Setup') : 'How It Works'}</p>
      <h1>{isSetup ? <TextLoop staticText="Connect" rotatingTexts={[isWebSetup ? `${searchProvider}.` : "Google Places.", "your API key.", "new opportunities."]} /> : !isPricing ? <TextLoop staticText="Find" rotatingTexts={["new leads.", "the right fit.", "your next customer."]} /> : <TextLoop staticText="One payment." rotatingTexts={["Full access.", "Lifetime value.", "More possibilities."]} />}</h1>
      <p className="info-intro">{isPricing ? '₹100 once for Heimdall Premium lifetime access. Google Places and Foursquare API usage is billed separately by each provider to your own account; API fees are not included in ₹100.' : isSetup ? (isWebSetup ? `Use your own ${searchProvider} API key to find businesses and review their contact details.` : 'Use a key from your own Google Cloud project to find businesses through Heimdall.') : 'Search for local businesses with Google Places or Foursquare. Connect your own API key, choose a lead target, and review the results.'}</p>
      {isPricing ? <LifetimeOffer /> : isSetup ? isWebSetup ? <><div className="info-steps">
<section className="info-card"><span className="info-step">01</span><h2>Create a {searchProvider} account</h2><p>Register and verify your email at the <a href={providerDashboard} target="_blank" rel="noreferrer">{searchProvider} dashboard ↗</a>. You can use any browser.</p></section>
<section className="info-card"><span className="info-step">02</span><h2>Create a Places project</h2><p>Create or select a project in the Foursquare developer console and enable Places API access.</p></section>
<section className="info-card"><span className="info-step">03</span><h2>Generate a Service API key</h2><p>Open the project credentials, generate a Service API key and copy it privately.</p><a href="https://docs.foursquare.com/fsq-developers-places/reference/authentication">Foursquare authentication guide ↗</a></section>
<section className="info-card"><span className="info-step">04</span><h2>Connect Foursquare in Heimdall</h2><p>Open Scrape → Foursquare Business Discovery, paste your key and search by business type and city. Filter the returned listings by website, phone, or social profile.</p><a className="info-cta" href={user ? '/dashboard?method=foursquare' : '/login'}>Connect Foursquare <ArrowRight size={16}/></a></section>
</div><aside className="info-note"><h2>500 free Pro calls</h2><p>Foursquare publishes a 500-call free Pro allowance. One Heimdall Foursquare search makes one request. Any usage charges are billed to your own Foursquare account. Heimdall does not pay provider API fees.</p><a href="https://docs.foursquare.com/developer/reference/upcoming-changes">Foursquare pricing ↗</a></aside></> : <>
        <div className="info-steps">
          <section className="info-card"><span className="info-step">01</span><h2>Create a Google Cloud project</h2><p>Open <a href="https://console.cloud.google.com/" target="_blank" rel="noreferrer">Google Cloud Console ↗</a>, create or select a project, and link an active billing account.</p></section>
          <section className="info-card"><span className="info-step">02</span><h2>Enable Places API (New)</h2><p>In the API Library, find and enable Places API (New) for that project. Heimdall uses this API for its Google Places searches.</p></section>
          <section className="info-card"><span className="info-step">03</span><h2>Create and restrict your key</h2><p>Create an API key in Credentials and restrict its API access to Places API (New). Requests come from the Heimdall server: IP restrictions must allow that server's outbound IP, rather than your browser's website referrer.</p><a href={setupUrl} target="_blank" rel="noreferrer">Follow Google's setup guide ↗</a></section>
          <section className="info-card"><span className="info-step">04</span><h2>Connect inside Heimdall</h2><p>Sign in, open Scrape → Google Places, paste your key into the API key field, and save it. Enter a business type and location, then run a search.</p><p>Your key stays in your server session and clears when you sign out or the session expires. Reconnect it when needed.</p><a className="info-cta" href={user ? '/dashboard?method=maps' : '/login'}>Connect Google Places <ArrowRight size={16} /></a></section>
        </div>
        <aside className="info-note"><a href="/foursquare-setup">Set up Foursquare →</a><h2>Google Places free allowance</h2><p>Google’s global pricing includes 1,000 free Text Search Enterprise requests per month. Accounts eligible for India pricing have separate allowances. A Heimdall search can make several requests, and Google bills your own Cloud account for any charges.</p><a href={billingUrl} target="_blank" rel="noreferrer">Google usage, quotas and billing ↗</a></aside>
      </> : <>
        <div className="info-grid">
          <section className="info-card"><MapPin size={28} /><h2>Google Places</h2><p>Connect your own key, then search by business type and location. Find operational businesses without a listed website regardless of review count.</p><a href="/google-places-setup">Set up Google Places →</a></section>
          <section className="info-card"><Search size={28} /><h2>Foursquare Business Discovery</h2><p>Find up to 50 businesses by category and city. Review available phone numbers, websites and social profiles.</p><a href="/foursquare-setup">Set up Foursquare →</a></section>
          <section className="info-card"><Check size={28} /><h2>Choose 10, 25, or 50 matches</h2><p>Your lead target counts unique matches, not the number of pages fetched. Google Places keeps operational businesses with no website listed in the returned record. That does not prove the business has no website elsewhere.</p></section>
          <section className="info-card"><Search size={28}/><h2>Keep searching across related queries</h2><p>Heimdall fetches additional pages, then tries related category and area variations when one query runs out. Google Places results are deduplicated by place ID across the run. Foursquare makes one search request.</p><p>Filters stay in place. Results may extend beyond the exact area or category you intended, so review relevance before outreach. Some businesses may have multiple domains or provider records.</p></section>
          <section className="info-card"><KeyRound size={28}/><h2>Understand API requests and cancellation</h2><p>Google Places may make multiple requests while looking for matches. Foursquare makes one request per search. The provider bills any usage charges to your account.</p><p>A Google Places search may stop early if it runs out of query variations, reaches its internal request limit, or encounters a provider error. You can cancel a search, though an in-flight request may finish.</p></section>
          <section className="info-card"><Check size={28}/><h2>Review before contacting anyone</h2><p>Live counters show candidates checked, provider requests, and matches. The final summary explains why the run stopped. Returned business details can be incomplete, duplicated, or outdated. Review website, phone, and social fields when available.</p><p>These are unverified prospecting matches, not verified contacts or permission to send marketing. Check the business, location, number, and applicable outreach rules yourself.</p></section>
          <section className="info-card"><FolderOpen size={28} /><h2>Build your lead library</h2><p>Save an eligible completed search to Leads, name the file, and revisit it later. Saved files retain the returned business details, and CSV exports contain the saved contact information. Only retain, export, or share information where the source terms and applicable law permit it. Deleting a private lead file does not remove separate team copies or the search history.</p><a className="info-cta" href={start}>Start discovering <ArrowRight size={16} /></a></section>
        </div>
        <aside className="info-note"><h2>Source permissions matter</h2><p>Heimdall is an independent tool, not endorsed by Google, Foursquare, or the businesses listed. Using your own API key does not override provider terms. Google storage, export and sharing restrictions require provider/legal review before public launch. Foursquare results are shown for the current visit.</p><a href="/terms">Read usage terms →</a> · <a href="/privacy">Privacy and data handling →</a></aside>

      </>}
      {path === '/how-it-works' && <MethodComparison />}
      <Troubleshooting path={path} />
      <PageFAQ path={path} />
      <ContentReview />
    </main>
    <footer className="info-footer"><a href="/">Heimdall</a><nav aria-label="Footer">{publicLinks.map(l => <a key={l.href} href={l.href} aria-current={path === l.href ? 'page' : undefined}>{l.label}</a>)}</nav><PolicyLinks/><SocialLinks/></footer>
  </div>;
}
