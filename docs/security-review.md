# Heimdall security review — 2 November 2026

This is a code and local-test review, not a guarantee against compromise or an independent penetration test. No paid API searches, real payment attempts, destructive user-data changes, or credential disclosure were used for security testing.

## Findings addressed

| Finding | Change | Evidence |
|---|---|---|
| High: website requests accepted private destinations and followed automatic redirects | Separate website HTTP transport validates HTTP(S), standard ports and host syntax; rejects private/special IPv4 and IPv6, including mapped/transition addresses; rejects mixed public/private DNS answers at the actual socket resolver; validates every redirect; disables cookies, ambient proxy use and automatic redirects. TLS certificate verification stays enabled. | Private-address, mixed-answer, unsafe URL, and listening-loopback denial tests. Resolver supplies validated addresses to the connection manager, avoiding a separate unchecked DNS resolution. |
| High: searches with provider charges executed on GET | Active and legacy search endpoints now require POST and Spring CSRF protection. Frontend obtains a session-bound token. GET cannot start a search. | Full Spring Security integration test: missing token 403; non-Premium authenticated POST 402; GET 405. |
| Resource exhaustion through large/slow websites | Maximum 1 MiB decompressed HTML; at most five hops; one shared deadline covers the redirect chain and body; bounded connection pool and existing worker queue. Non-HTML bodies rejected. | Code-level limits; hostile local fixtures and existing extraction/concurrency tests. Not a load test. |
| Distributed password guesses could evade IP-only login throttling | Additional normalized-account limit of 10 login attempts per 15 minutes across IPs, alongside existing IP limits. Successful login resets the account counter. | Different-IP login test. Limits are intentionally temporary; sufficiently distributed abuse still requires edge controls. |
| Registration revealed existing accounts before proving email ownership | Removed the initial existing-email response. Registration returns a real emailed challenge for either case; an existing password can never be replaced by registration. | Existing-account verification test preserves the original password. This does not prevent all enumeration through timing or email behavior. |
| Long-lived/session identity state | Password login replaces the old session; OAuth success now replaces it too, clearing old provider keys and workspace state. Added seven-day absolute lifetime in addition to idle expiry. | Email-login integration checks old session invalidation, removal of old key state, old-CSRF rejection, logout and expiry tests. OAuth live sign-in was not repeated in this audit. |
| Abuse across tabs or sessions | One active workspace discovery run per account, not just per session. API filter limits search starts (12/account/hour), team joins (10/account/15 min), writes (120/account/min) and authentication traffic (120/IP/min). | Account-independent-IP limiter test; bounded in-memory maps. Single-instance controls, not distributed rate limiting. |
| Oversized API payloads and detailed error responses | JSON request bodies bounded to 16 KiB including chunked bodies. Form/header/Tomcat limits added. Internal exception messages/stack traces omitted from framework error responses; expected controller validation errors remain descriptive. | Oversized-body and replayable-body tests; build/runtime validation. |
| Browser/proxy production gaps | Added production profile: TLS-only session cookies, loopback backend binding and loopback-only trusted proxy headers. Deployment example adds CSP, HSTS, framing restrictions, content sniffing and referrer protections. API replies are no-store. | Production property test and API header integration assertion. Deployment headers are examples until installed on the real HTTPS host. |
| Known dependency advisories | Removed unused Selenium/WebDriverManager and their transitive dependencies. Patched jsoup to 1.23.2, Jackson BOMs to 3.1.7/2.21.7, Tomcat to 11.0.25. Apache HttpClient is Boot-managed 5.6.4. | OSV runtime scan after patching: 99 dependencies, no returned advisories. npm production audit: no known advisories on review date. This does not prove absence of vulnerabilities or that each former advisory was exploitable in this app. |

Existing protections retained: BCrypt cost 12; real email OTP with expiry, single use, attempt and email-send limits; OIDC verified identity checks; authenticated account-scoped files/history/profile/team access; leader-only team administration; optimistic team versions; server-side payment ownership, HMAC, captured status, INR amount and refund checks; separate test/live entitlements; API keys kept in backend sessions; CSV formula escaping; React text escaping.

## Production activation and remaining work

1. Rotate the Places and Brave API keys previously pasted into chat. Use provider restrictions/quotas and keep all secrets in the host secret manager, never the frontend build. Restrict Google OAuth origins/callbacks to the deployed domain.
2. Build the frontend, serve `dist` through HTTPS Caddy using `deploy/Caddyfile.example`, and set `SPRING_PROFILES_ACTIVE=production`. This example assumes Caddy and Java on the same host. With containers or a separate proxy, use a private network and explicitly trust only that proxy's addresses; never expose port 8080 or trust arbitrary forwarding headers. Keep the real `HEIMDALL_FRONTEND_URL` and callback on HTTPS.
3. Verify the production CSP against Google login, every page, and Razorpay checkout before accepting real payments. The header deliberately avoids unsafe inline scripts but permits inline styles used by the animation components. Do not deploy the Vite development server as public hosting.
4. Put edge rate limits/bot controls in front of login and scraping, deny private/metadata destinations with network egress rules, add operational alerts and monitor resource usage. Application filters are only one defense layer. OS DNS resolution can still be delayed; a constrained DNS resolver and egress proxy are recommended.
5. The current file stores, sessions and limiters assume one Java process. Before horizontal scaling use a transactional database/shared session store and distributed limits; add encrypted backups, retention, restoration tests, least-privilege host accounts, patch automation and secret scanning in CI.
6. App-managed MFA/passkeys, password recovery, a user-facing active-session/revocation screen and payment refund/webhook reconciliation have not been added. Users may protect their Google account with MFA, but Heimdall does not enforce it. Prioritize these before a broader paid launch.
7. No external production penetration test or 500-user load test was performed. Robots/source permissions, provider licensing, privacy obligations and merchant onboarding remain separate launch issues; this security work does not clear them.

## References

- OWASP SSRF prevention: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
- Spring CSRF: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring sessions: https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/session-management.html
- Advisory source: https://osv.dev/ ; version ranges cross-checked through OSV's package-specific affected records and linked upstream advisories.

## Repeatable checks

Run `npm run security:check` for production npm advisories plus the resolved Maven runtime dependency scan against OSV (public package coordinates only). Network/scanner failure exits unsuccessfully, never reports a clean scan. Run `cd backend && ./mvnw test` and the frontend build separately. Weekly Dependabot update configuration is prepared in `.github/dependabot.yml`; it only becomes active after pushing to a supported GitHub repository. No workflow was pushed during this task.

## Final verification

62 backend tests passed with the patched dependencies. Frontend production build, SEO checks and diff whitespace checks passed. The repeatable security audit returned zero npm production advisories and no OSV findings for 99 Maven runtime packages. The restarted local backend returned 401 for anonymous account/history reads, 403 for login without CSRF, and 200 for the public CSRF endpoint, with no-store and frame-denial headers. Session restart requires users to sign in and reconnect session-only provider keys.
