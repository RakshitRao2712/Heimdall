import { BadgeCheck, Box, ArrowUpRight, Sparkles } from 'lucide-react';
import { useSession } from '@/auth/session';

const included = [
  ['Discovery methods', '2 methods'],
  ['Google Places', 'Your API key'],
  ['Website Copyright', 'Included'],
  ['Named lead files', 'Included'],
  ['CSV exports', 'Included'],
  ['Access', 'Lifetime'],
];

export function LifetimeOffer() {
  const { user } = useSession();
  return <section className="lifetime-offer" aria-labelledby="lifetime-heading" id="lifetime-access">
    <div className="lifetime-card">
      <div className="lifetime-card-title"><Box aria-hidden="true" size={32} strokeWidth={1.5} /><h3>Lifetime access</h3></div>
      <p className="lifetime-card-subtitle">Full access. Just ₹100. For early users.</p>
      <div className="lifetime-price">₹100<span>one time</span></div>
      <p className="lifetime-card-description">One purchase. A workspace to keep discovering.</p>
      <dl className="lifetime-included">
        {included.map(([label, value]) => <div key={label}><dt><BadgeCheck size={21} strokeWidth={1.6} aria-hidden="true" />{label}</dt><dd>{value}</dd></div>)}
      </dl>
      <a className="lifetime-button" href={user ? '/dashboard' : '/login'}>{user ? 'Open dashboard' : 'Get started free'}<ArrowUpRight size={21} aria-hidden="true" /></a>
      <p className="lifetime-card-note">₹100 lifetime offer planned for launch. No payment collected today.</p>
    </div>
    <div className="lifetime-copy">
      <span className="lifetime-eyebrow"><Sparkles size={15} aria-hidden="true" />Small price. New possibilities.</span>
      <h2 id="lifetime-heading">Find your next lead.<br /><span>Keep the possibilities.</span></h2>
      <p>Get full access to Heimdall for just ₹100, with lifetime access to our product. This limited-time offer is exclusively for early users.</p>
      <p>We’re starting with free access while Heimdall grows. Our planned ₹100 one-time lifetime offer brings the discovery tools, saved lead files, and exports together in one workspace.</p>
      <div className="lifetime-detail"><h3>Your key. Your Google billing.</h3><p>Google Places uses your own API key. Google usage charges, if any, are separate from Heimdall access. Website Copyright search doesn’t require a Google Places key.</p><a href="/google-places-setup">See how to connect Google Places <ArrowUpRight size={15} aria-hidden="true" /></a></div>
      <a className="lifetime-workflow" href="/how-it-works">Explore how Heimdall works <ArrowUpRight size={16} aria-hidden="true" /></a>
    </div>
  </section>;
}
