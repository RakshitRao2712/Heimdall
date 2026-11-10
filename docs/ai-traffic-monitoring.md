# Heimdall AI visibility monitoring

Baseline reviewed 11 November 2026 (India time). No recurring automation is scheduled.

## Three separate measurements

1. **Human AI referrals — GA4**
   Open Traffic acquisition, choose Session source / medium, and review sessions from chatgpt.com, chat.openai.com, perplexity.ai, claude.ai, copilot.microsoft.com, gemini.google.com, you.com, and poe.com.
   https://analytics.google.com/analytics/web/#/a415339260p563089627/reports/intelligenthome
   Compare the last complete 7 days with the previous 7. Record sessions, engaged sessions, sign_up and premium_activated events. Do not treat crawler requests as visitors.
   Baseline Oct14–Nov10: 4 recorded sessions, all Direct. Earlier analytics intentionally erased referrers, so this does NOT prove there were no AI-origin visits.

2. **Citations — Bing AI Performance**
   https://www.bing.com/webmasters/aiperformance?siteUrl=https://heimdall.life/
   Review total citations, cited pages and grounding queries. Baseline Aug10–Nov9: 0 reported citations and 0 cited pages. This covers Microsoft Copilots and partners, not every AI engine.

3. **Crawler access — Cloudflare AI Crawl Control**
   https://dash.cloudflare.com/f1666d0e3aa93ab41de4eeae11925960/heimdall.life/ai/metrics
   Review by individual crawler and status code. Baseline last7days: 547 requests, 389 allowed (218 HTTP200 + 171 redirects), 158 unsuccessful (148 HTTP404, 1 HTTP403, 9 HTTP502). The default includes ordinary search and other bots, not exclusively generative AI.
   Investigate failures on public canonical pages. A missing obsolete asset is different from a blocked homepage. Request counts are not unique visitors or citations.

## Collection change awaiting deployment

src/seo/ai-referral.ts retains only a recognized AI service HTTPS origin. No conversation path, query, fragment, credentials, arbitrary referrer, or private hostname is sent. Existing analytics consent and Do Not Track checks still apply. Privacy copy explains this.
After deployment, check a genuine consented AI referral in GA4; do not manufacture visits to inflate metrics. New attribution is prospective and cannot recover erased historical referrers. Referrer suppression, denied consent, browser protections and app handoffs can still hide AI traffic. Google AI Overviews/AI Mode clicks cannot be reliably separated from normal Google organic traffic by a hostname rule.

## Weekly review checklist

- Use identical completed date windows and record timezone.
- Record human referrals, citations and crawler outcomes separately.
- Keep owner/test visits distinct where known.
- Record which landing pages gain real AI-origin sessions.
- Investigate public-page 403/429/5xx increases without blanket firewall bypasses.
- Keep a dated log; traffic or citations are not guarantees of customer outcomes.
