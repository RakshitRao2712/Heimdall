> Historical test notes: later live pagination/continuous-search results supersede the limits and outcomes below. See `seo-and-launch-audit.md` and the current README for current behavior and launch blockers.

# Search QA — 1 November 2026

## Live UI checks
- Existing session restored through Google sign-in after backend restart.
- Razorpay sandbox checkout completed; Premium access and navbar badge present. No live charge.
- Missing Places key: submission prevented and API-key field focused.
- Connected user-supplied key: exactly one approved Places request (`cafes in Seattle`), completed successfully in 0.6 seconds with zero filtered leads.
- Genuine zero-result state: completion count, saved history, empty-state explanation; Save/Export disabled.
- Copyright real request: Google challenge detected; actionable error shown; no challenge bypass attempted.
- An earlier browser request held the browser slot; browser command/network and driver-download timeouts are now bounded.

## Fixes
- Escape cancels the frontend request as well as resetting the orb. Backend may still finish work after an HTTP disconnect; this is explicitly stated in the cancellation notice.
- Failed searches no longer resolve the orb through its success animation; input resets and the actionable error remains.
- Search exceptions preserve their safe `detail` message and status in the API response.
- Clear stale live-region searching announcements on error/reset.
- Guard against duplicate in-flight submissions with the request ref.
- Reuse the Places HTTP client and tolerate null entries/blank website fields.
- Handle 400/401/403 provider errors as key/configuration errors, and quota errors distinctly, without returning provider bodies or keys.
- Input length aligned with backend's 500-character maximum.
- Zero-result copy explains actual discovery filters.
- Fixed the old startup test's incorrect application-package reference.

## Automated verification
36 backend tests passed, including application startup, provider filtering, invalid/empty/oversized input, missing keys, malformed/empty provider responses, network failures, provider 400/401/403/429/500 errors, search-history account isolation, genuine empty results, failure-not-saved behavior, key/session checks, lead file persistence, and existing team/auth/billing regressions. Frontend production build passed.
Additional authenticated HTTP checks passed: anonymous API requests return 401; unpaid workspace and legacy search routes return 402; key format validation returns 400; missing CSRF returns 403; key-status responses expose only a boolean.

## Limits / remaining work
No successful live Copyright result was obtained because Google challenged the automated search. A supported search-provider integration may be needed for reliable production discovery; do not promise this method is production-ready based on parsing fixtures.
No non-empty live Places result was obtained in the single authorized request. Provider filtering and non-empty persistence are covered with deterministic fixtures rather than claimed as live evidence.
Client cancellation stops waiting, not guaranteed termination of server work. Production job IDs and server-side cancellation would improve this further.
No load test of hundreds of simultaneous searches was performed. Real-device visual/accessibility checks and production egress/provider behavior still need verification.
