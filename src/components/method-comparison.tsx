import { useId } from 'react';
import comparison from '@/seo/comparison.json';
export function MethodComparison() {
  const id = useId();
  return <section className="method-comparison" id="compare-methods" aria-labelledby={id}>
    <div className="comparison-copy"><span className="comparison-eyebrow">Two ways to discover</span><h2 id={id}><em>Different searches.</em><span>New possibilities.</span></h2><p>Choose the method that fits your next search. Find businesses without a website listed through Google Places, or explore available business contacts with Foursquare.</p><p className="comparison-note">Both require Premium and your own provider key. API charges are separate. Results and contact fields depend on provider coverage; your target is not a guaranteed count.</p><a href="/how-it-works">Explore how it works <span aria-hidden="true">↗</span></a></div>
    <div className="comparison-table-wrap"><table className="comparison-table"><caption className="sr-only">Compare Google Places and Foursquare in Heimdall</caption><thead><tr><th scope="col"><img src="/images/google-logo.svg" width="30" height="30" alt=""/><span>Google Places</span><small>Find a missing website</small></th><th scope="col"><img src="/images/foursquare-logo.svg" width="30" height="30" alt=""/><span>Foursquare</span><small>Explore business contacts</small></th></tr></thead><tbody>{comparison.rows.map(row => <tr key={row.label}><td><span className="comparison-label">{row.label}</span>{row.google}</td><td><span className="comparison-label">{row.label}</span>{row.foursquare}</td></tr>)}</tbody><tfoot><tr><td><a href="/google-places-setup">Set up Google Places ↗</a></td><td><a href="/foursquare-setup">Set up Foursquare ↗</a></td></tr></tfoot></table></div>
  </section>;
}
