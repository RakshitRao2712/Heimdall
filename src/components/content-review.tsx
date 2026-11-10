import brand from '@/seo/brand.json';
import review from '@/seo/review.json';
export function ContentReview() {
  return <aside className="content-review"><p>Written and maintained by <a href="/about">{brand.operator}</a>. Content reviewed <time dateTime={review.date}>{review.label}</time></p><p>Provider pricing and access can change. Google’s published global Text Search Enterprise free cap is 1,000 monthly billable events; this is not 1,000 Heimdall searches. India eligibility and other SKUs have different pricing. Foursquare publishes 500 free Pro calls; check your account’s applicable plan and allowance. Your provider bills any charges separately.</p><nav aria-label="Official provider sources">{review.sources.map(source => <a key={source.url} href={source.url} target="_blank" rel="noreferrer">{source.label} ↗</a>)}</nav></aside>;
}
