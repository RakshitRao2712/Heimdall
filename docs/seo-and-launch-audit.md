# Heimdall: SEO and launch-risk audit

Reviewed 2 November 2026. Scope: repository code, generated public HTML, policy copy, and the official provider documents linked below. This is a technical/content audit, not a legal opinion, trademark clearance, penetration test, or guarantee against claims. **Public paid launch is not cleared by this audit.**

## Completed

- How It Works describes 10/25/50-match targets; 10/25/50/100 API-call budgets (default 50); pagination; related/area queries; cross-query deduplication; live progress; stop reasons; backend cancellation; and saved partial results.
- Clarifies no guaranteed lead count, no website-age inference from copyright dates, no marketing consent from public availability, no verified-phone claim, and possible geography/directory/phone errors.
- Pricing defines lifetime as the operating lifetime of the product. Removed an unsupported limited-time scarcity claim. Both Google and Brave usage fees are separate from ₹100.
- Terms and privacy identify the independent operator, separate providers, external assets, source permissions, public-data/outreach limitations, session keys, and limits of deleting private files. Includes Google terms/privacy and Brave privacy links.
- Added Google's official unmodified first-party Maps attribution asset next to Places results, private-file results, and team-file results. **This does not cure storage/export restrictions or prove all third-party attribution requirements are met.**
- Nine generated public routes have unique titles/descriptions, one initial H1, semantic readable fallback content, internal navigation, social metadata, and valid WebPage/Organization/WebSite/Breadcrumb JSON-LD when a production origin is configured. No fabricated ratings, customer reviews, endorsements, or FAQ-rich-result promises.
- Sitemap excludes private routes. Login, dashboard and 404 HTML carry noindex. Without a real `VITE_SITE_URL`, staging remains intentionally non-indexable. Unknown URLs have a deployment configuration for real 404 status.
- Added `npm run seo:check`. Verified both a synthetic HTTPS production-origin build and the default staging build, then restored the staging build.
- Deployment example adds nosniff, referrer/permissions headers and noindex for API responses. These are examples, not proof that a live host serves those headers.

## Blocking findings that remain open

### 1. Google Places content rights — PUBLIC-LAUNCH BLOCKER

Google Maps Platform terms §3.2.3 prohibit specified exporting, bulk downloading, storing/resharing, copying business names/addresses, and certain listings/advertising uses. Places policies restrict caching/storage, with specified exceptions such as place IDs. Full-result storage, exports and team copies are enabled at the owner's explicit request after reverting the place-ID-only architecture. These features remain a public-launch blocker pending written provider permission or redesign. A user-owned key, request limit, source logo or disclaimer does not grant those rights.

**Current owner instruction: restore the earlier full-result behavior.** The reference-only migration already removed details from existing managed Google records; reverting code cannot reconstruct those names and numbers. Those records require a new search. The separate place workflow SQL feature and on-demand details controls have been removed. Existing team status and notes remain available.

Sources: https://cloud.google.com/maps-platform/terms (especially §3.2.3); https://developers.google.com/maps/documentation/places/web-service/policies ; https://cloud.google.com/maps-platform/terms/maps-service-terms . Regional agreements may differ.

### 2. Brave and destination-website permissions — OPEN

Brave says storing search results requires a plan with storage rights. We have not verified the rights attached to each user's plan. The app keeps result-derived titles/URLs and website contact details. Do not assume API access licenses all downstream storage, export, sharing, or outreach. Review Brave's current agreement and the destination site's terms/rights before launch.

Sources: https://brave.com/search/api/ (storage FAQ); https://api-dashboard.search.brave.com/documentation/resources/privacy-notice .

The current website fetcher does not implement a robots.txt access-policy check and sends a Chrome-like user agent. Adopt a transparent crawler identity and a tested access/robots policy; do not treat robots allowance as legal permission. Redirect/public-network validation and network egress protections also require hardening: current URL checks alone do not protect against private destinations/DNS/redirect attacks. This is a security/privacy review blocker, not a claim of an observed exploit.

### 3. Privacy, consumer terms and merchant readiness — OPEN

Have qualified counsel review applicable Indian and recipient-country rules, privacy notices, lawful bases, outreach/do-not-contact requirements, consumer/refund obligations, and required operator/grievance/merchant contact details. Public business information may still be personal data. Do not claim DPDP/GDPR compliance based on this copy. India's DPDP framework has commencement details that must be checked for the launch date and activity: https://www.meity.gov.in/documents/act-and-policies/digital-personal-data-protection-rules-2025-gDOxUjMtQWa?pageTitle=Digital-Personal-Data-Protection-Rules-2025%3B .

Account/privacy deletion is currently a support process; there is no automated whole-account erasure workflow, formal retention schedule or recorded versioned clickwrap acceptance. Private-file deletion leaves separate search/team/export copies. Set an operational policy for deletion requests, backups, security incidents and access restrictions. Confirm processing providers, locations, email-delivery setup and production cookie/security settings.

Razorpay live onboarding/KYC and production webhook/refund reconciliation must be completed before live payment claims. Refund entitlement revocation is not automatically driven by a production refund webhook. The policy's 7-day processing statement requires a real support/refund process; bank credit is not guaranteed within 7 calendar days. Confirm refund eligibility wording with counsel, rather than interpreting this audit as approval.

### 4. Brand and asset rights — OPEN

Local Anton and Yellowtail font license notices exist. Official Google Maps and Brave logo sources are recorded. Those limited checks do not establish rights to every pasted component, stock image/CDN asset, supplied texture/logo, icon, name or animation. Retain each license/provenance and check commercial redistribution rights; obtain trademark clearance for Heimdall in the intended markets. No trademark registry clearance was performed.

## SEO activation / performance still requiring deployment

1. Set `VITE_SITE_URL` to the actual HTTPS domain, rebuild, run `npm run seo:check`, and verify canonicals, sitemap, robots and HTTP status/headers on that domain. Configure host variants/HTTPS redirects.
2. Keep dashboard/API content authenticated and noindex. Never put keys, private leads or account data in public metadata or the sitemap.
3. Verify the deployed rendered pages in Google Search Console, submit the sitemap, and inspect all public routes. Indexing/rankings are not guaranteed.
4. Measure real mobile Core Web Vitals/Lighthouse after hosting: the animated hero/intro, third-party images and JS may affect LCP/INP. The static build checks do not measure field performance, uptime, crawler rendering or server capacity. The current app has semantic initial HTML, not full React SSR.
5. Assess backend concurrency, request queues, persistent storage/backups and abuse/rate controls before scaling. A local successful 50-result test is not a 500-user load test.

## Source/verification record

- Google terms and Places policies, Brave storage/privacy documentation checked on review date.
- Latest non-testimonial live run evidence: Places 50 matches / 15 provider requests; Copyright 50 raw website matches / 24 requests with year range 2000–2026. These are single-run observations, not speed, quality or volume guarantees; not used as guaranteed marketing claims.
- Production dependency advisory check: `npm audit --omit=dev` reported zero known advisories on review date. This is not a source-code security audit or an assurance against future vulnerabilities.
- Re-run: `npm run build && npm run seo:check`. Production-origin check uses a synthetic `.example` domain only during verification; do not deploy that origin.
