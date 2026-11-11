# SEO implementation and evidence — 12 November 2026

## Repository changes
- Added Google Places and Foursquare feature pages, freelancer/web designer/agency use-case pages, a guide hub and an illustrated first-search guide. All are prerendered, linked and listed in the generated sitemap.
- Added original public-interface screenshots in compressed WebP. They are explicitly illustrative and contain no customer data or API keys.
- Added Organization sameAs for the actual LinkedIn company page and Article markup for the guide. No invented reviews or aggregate ratings.
- Replaced invalid Pages absolute redirect syntax with valid legacy setup redirects. The www/HTTPS redirect remains a zone-level configuration and must be live-tested.
- IndexNow: public ownership proof file, guarded submission script, and GitHub workflow that waits for the exact deployed commit before notifying participating search engines. A receipt is not an indexing guarantee. No private routes are submitted.
- Added live route/status/canonical/noindex audit and consent/privacy checks.
- Optimized local WebP artwork and backgrounds, mobile-sized hero images, lightweight mobile/reduced-motion logo intro and hero, fewer desktop curved facets, lazy loading below-fold avatars and closed-menu images, removed blocking external font imports, and added asset cache headers. Original images remain for previously crawled asset URLs.
- GA4 sign_up and premium_activated marked as key events. No synthetic payments or fake customer conversions are sent.

## Repeatable verification
```
VITE_SITE_URL=https://heimdall.life npm run build
npm run seo:check
node scripts/analytics-check.mjs
npm run seo:live
npm run indexnow:submit
```

Live route audit writes `/tmp/heimdall-seo-live.json`. IndexNow must run after the proof file is live. The GitHub action waits for `release.json` to match GITHUB_SHA; failed deployments do not submit new pages.

## Work that cannot be manufactured
- Genuine customer reviews require real customers and their permission. Do not invent testimonials, ratings or outcomes.
- The LinkedIn page/post now provides a real external presence and site link. Additional editorial backlinks require independent publication decisions; no paid-link spam or unsolicited mass outreach was performed.
- Search engines decide when to crawl/index. Requests and sitemap acceptance do not prove inclusion or rankings.
- Field Core Web Vitals require enough real mobile traffic. Lighthouse is a lab observation, not field-performance certification.
- A real end-to-end paid conversion requires an authorized real customer payment. Registration/Premium event code and consent filtering are tested; GA4 reporting populates when real consented events occur.
