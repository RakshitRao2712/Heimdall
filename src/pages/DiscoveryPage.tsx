import pages from '@/seo/pages.json';
import { SiteNav } from '@/components/site-nav';
import { PolicyLinks, SocialLinks } from '@/components/policy-links';
import { ContentReview } from '@/components/content-review';
const resourceLinks = [
  ['/google-places', 'Google Places discovery'], ['/foursquare', 'Foursquare discovery'],
  ['/for-freelancers', 'For freelancers'], ['/for-web-designers', 'For web designers'], ['/for-agencies', 'For agencies'],
  ['/guides', 'Practical guides'], ['/guides/first-search', 'Your first search'],
  ['/google-places-setup', 'Connect Google Places'], ['/foursquare-setup', 'Connect Foursquare'],
  ['/how-it-works', 'How it works'], ['/pricing', 'Pricing'],
];
export function DiscoveryLinks() { return <nav className="discovery-links" aria-label="Discovery guides and use cases">{resourceLinks.map(([href,label])=><a key={href} href={href}>{label} <span aria-hidden="true">↗</span></a>)}</nav>; }
export default function DiscoveryPage({path}:{path:string}) {
  const page=(pages as Record<string,{heading:string;description:string;sections:string[][]}>)[path];
  return <div className="info-page"><SiteNav/><main className="info-main discovery-main"><a href="/" className="info-back">← Back to home</a>
    <p className="info-eyebrow">Heimdall · Business discovery</p><h1>{page.heading}</h1><p className="info-intro">{page.description}</p>
    <div className="info-grid">{page.sections.map(([title,text])=><section className="info-card" key={title}><h2>{title}</h2><p>{text}</p></section>)}</div>
    {path==='/guides/first-search'&&<section className="guide-screenshots" aria-labelledby="guide-screens"><h2 id="guide-screens">See the interface before you search</h2><p>Original screenshots of Heimdall’s public guides. These explain the workflow; they do not show live customer data or promise a result count.</p><figure><img src="/images/guides/google-setup.webp" width="1280" height="613" loading="lazy" decoding="async" alt="Heimdall Google Places setup guide with steps for connecting a provider key"/><figcaption>Follow the setup guide to create and connect your own provider key.</figcaption></figure><figure><img src="/images/guides/workflow.webp" width="1280" height="613" loading="lazy" decoding="async" alt="Heimdall workflow guide showing the steps from a search to reviewing and organizing leads"/><figcaption>Review results and permissions before saving or sharing a folder.</figcaption></figure></section>}
    <section className="info-note"><h2>Continue your research</h2><p>Provider coverage, fields and permissions vary. Read the setup and workflow guides before using either method.</p><DiscoveryLinks/></section>
    <a className="info-cta" href="/login">Open your workspace →</a><ContentReview/>
  </main><footer className="info-footer"><a href="/">Heimdall</a><PolicyLinks/><SocialLinks/></footer></div>;
}
