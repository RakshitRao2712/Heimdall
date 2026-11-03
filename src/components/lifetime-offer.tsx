import { PurchaseButton } from "./ui/premium-access";
import { BadgeCheck, Box, ArrowUpRight, Sparkles } from 'lucide-react';


const included = [
  ['Discovery methods', '2 methods'],
  ['Google Places', 'Your API key'],
  ['Foursquare', 'Your API key'],
  ['Named lead files', 'Included'],
  ['CSV exports', 'Source rules apply'],
  ['Access', 'Lifetime'],
];

export function LifetimeOffer() {

  return <section className="lifetime-offer" aria-labelledby="lifetime-heading" id="lifetime-access">
    <div className="lifetime-card">
      <div className="lifetime-card-title"><Box aria-hidden="true" size={32} strokeWidth={1.5} /><h3>Lifetime access</h3></div>
      <p className="lifetime-card-subtitle">Full access. Just ₹100. For early users.</p>
      <div className="lifetime-price">₹100<span>one time</span></div>
      <p className="lifetime-card-description">One purchase. A workspace to keep discovering.</p>
      <dl className="lifetime-included">
        {included.map(([label, value]) => <div key={label}><dt><BadgeCheck size={21} strokeWidth={1.6} aria-hidden="true" />{label}</dt><dd>{value}</dd></div>)}
      </dl>
      <PurchaseButton/>
      <p className="lifetime-card-note">One-time payment for the product’s operating lifetime. API usage is separate. Premium activates after payment verification.</p>
    </div>
    <div className="lifetime-copy">
      <span className="lifetime-eyebrow"><Sparkles size={15} aria-hidden="true" />Small price. New possibilities.</span>
      <h2 id="lifetime-heading">Find your next lead.<br /><span>Keep the possibilities.</span></h2>
      <p>Get current Heimdall Premium features for ₹100 once at our early-user price. Lifetime means the operating lifetime of the Heimdall product; it is not a promise that the service will run forever.</p>
      <p>Our ₹100 one-time lifetime offer brings the discovery tools, saved lead files, and exports together in one workspace.</p>
      <div className="lifetime-detail"><h3>Your keys. Your API billing.</h3><p>Google Places uses your own API key. Google charges your Google Cloud account according to your API usage. The ₹100 Heimdall price does not include Google Places or Foursquare API fees. Heimdall does not pay provider API charges. Google’s global allowance is 1,000 free Text Search Enterprise requests per month. Foursquare publishes 500 free Pro calls.</p><div className="lifetime-provider-links"><a href="/google-places-setup">Connect Google Places <ArrowUpRight size={15} aria-hidden="true" /></a><a href="/foursquare-setup">Connect Foursquare <ArrowUpRight size={15} aria-hidden="true" /></a></div></div>
      <a className="lifetime-workflow" href="/how-it-works">Explore how Heimdall works <ArrowUpRight size={16} aria-hidden="true" /></a>
    </div>
  </section>;
}
