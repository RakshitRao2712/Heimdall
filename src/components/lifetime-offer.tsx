import { PurchaseButton } from "./ui/premium-access";
import { BadgeCheck, Box, ArrowUpRight, Sparkles } from 'lucide-react';


const included = [
  ['Discovery methods', '2 methods'],
  ['Google Places', 'Your API key'],
  ['Foursquare', 'Your API key'],
  ['Named lead files', 'Included'],
  ['CSV exports', 'Source rules apply'],
  ['Access', 'Free'],
];

export function LifetimeOffer() {

  return <section className="lifetime-offer" aria-labelledby="lifetime-heading" id="lifetime-access">
    <div className="lifetime-card">
      <div className="lifetime-card-title"><Box aria-hidden="true" size={32} strokeWidth={1.5} /><h3>Free access</h3></div>
      <p className="lifetime-card-subtitle">Full access to Heimdall. No purchase needed.</p>
      <div className="lifetime-price">Free<span>API usage is separate</span></div>
      <p className="lifetime-card-description">One workspace. Keep discovering.</p>
      <dl className="lifetime-included">
        {included.map(([label, value]) => <div key={label}><dt><BadgeCheck size={21} strokeWidth={1.6} aria-hidden="true" />{label}</dt><dd>{value}</dd></div>)}
      </dl>
      <PurchaseButton/>
      <p className="lifetime-card-note">No Heimdall subscription or access fee. Bring your own provider API keys.</p>
    </div>
    <div className="lifetime-copy">
      <span className="lifetime-eyebrow"><Sparkles size={15} aria-hidden="true" />Free access. New possibilities.</span>
      <h2 id="lifetime-heading">Find your next lead.<br /><span>Keep the possibilities.</span></h2>
      <p>Use Heimdall for free. Access both discovery methods, organize lead files, and collaborate with your team without buying a plan.</p>
      <p>Your discovery tools, saved lead files, and eligible exports are together in one workspace.</p>
      <div className="lifetime-detail"><h3>Your keys. Your API billing.</h3><p>Google Places uses your own API key. Google charges your Google Cloud account according to your API usage. Free Heimdall access does not include Google Places or Foursquare API fees. Heimdall does not pay provider API charges. Google publishes a global free cap of 1,000 monthly Text Search Enterprise events; India eligibility and other SKUs differ. Foursquare publishes 500 free Pro calls. These are provider allowances, not guaranteed free Heimdall searches.</p><p className="provider-source-links"><a href="https://developers.google.com/maps/billing-and-pricing/pricing" target="_blank" rel="noreferrer">Google pricing ↗</a><a href="https://docs.foursquare.com/developer/reference/upcoming-changes" target="_blank" rel="noreferrer">Foursquare pricing ↗</a><a className="lifetime-workflow" href="/how-it-works">Explore how Heimdall works <ArrowUpRight size={16} aria-hidden="true" /></a></p></div>
    </div>
  </section>;
}
