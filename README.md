# Heimdall

Heimdall is a local business discovery workspace. Search Google Places and Foursquare by business type and location, review available details, and organize eligible leads with your team.

Website: https://heimdall.life · Support: support@heimdall.life

- Google Places: find operational businesses without a website listed in their returned record.
- Foursquare: review available business websites, phone numbers, and social profiles.
- Premium: ₹100 once for the product operating lifetime; provider API charges are separate.
- Use your own provider keys. Results are not guaranteed and source permissions apply.

Built with React, TypeScript, Vite, and a Spring Boot backend in `backend/`.

## Local development

Requires Node.js 22.12+ and npm.

```sh
npm install
npm run dev
```

Open http://127.0.0.1:5174.

```sh
npm run build
npm run preview
```

The production build is written to `dist/`. Preview runs on port 4174.

## Project structure

- `src/App.tsx`: page composition and image configuration.
- `src/components/ui/`: reusable hero, navigation, and card components.
- `src/components/blocks/features-8.tsx`: feature section.
- `src/styles.css`: theme and page styles.

The `@/` alias resolves to `src/`. Images load from the supplied external CDNs. Animation components include reduced-motion handling.

## Optional backend proxy

The current landing page does not perform lead searches. For future integration, Vite forwards `/api` to `http://localhost:8080`. Copy `.env.example` to `.env` to override `API_TARGET`. Keep API credentials on the backend.

For static production hosting, configure a reverse proxy separately if `/api` is needed; Vite's development proxy is not included in the build.

Heimdall navigation links to the page sections. Instagram, X, and LinkedIn icons are present with placeholder destinations until real profile URLs are supplied.

## Sign-in and dashboard

`Get started` opens `/login`; authenticated users see `Dashboard` and open `/dashboard`. The browser checks `/api/auth/me` rather than trusting local storage. Both pages require the included `backend/` Spring Boot service with the new authentication code running on port 8080:

```sh
cd backend
./mvnw spring-boot:run
```

Create an account from the sign-in page. Passwords require at least 10 characters and at most 72 UTF-8 bytes. The backend stores BCrypt hashes in ignored `.data/accounts.properties`, uses HTTP-only session cookies and CSRF protection, and protects all lead endpoints. Session idle time is eight hours, or seven days with the extended-session option; browser cookie persistence is still browser-controlled. Sessions are in memory and are cleared when the backend restarts. Account hashes persist.

The dashboard supports both discovery methods, counts returned matches and phone entries, filtering, saved folders, teams, and CSV export. Continuous searches try related queries up to a user-chosen request budget. Cancel stops further backend requests and retains partial results; an in-flight call can finish. Google sign-in and email OTP registration are implemented when the corresponding provider credentials are configured. Self-service password reset is not available. No demo testimonials are presented as verified customer reviews. Provider content rights remain a launch blocker documented in docs/seo-and-launch-audit.md.

The local account store is intended for a single backend instance. Before production deployment, use HTTPS with `server.servlet.session.cookie.secure=true`, durable protected account storage, and operational account recovery. Serve generated per-route HTML including `/login` and `/dashboard`, return HTTP 404 for unknown routes, and proxy `/api` to the backend. The included backend must be deployed together with this frontend.

## Integrated App1 workspace

The `/dashboard` page uses the supplied App1 layout adapted for Heimdall: a persistent sidebar with Dashboard, Copyright method, and Google Maps method; four summary cards; a lead-discovery activity chart; method cards; and recent search activity. All counts derive from the signed-in account's latest 50 successful searches. Lead matches and phone entries can repeat across searches; they are not deduplicated totals. Empty accounts show zero activity.

Search views are deep-linkable at `/dashboard?method=copyright` and `/dashboard?method=maps`. They include business/location search, copyright result-page selection, elapsed-time status, cancellation, phone-only and text filters, CSV export, and saved results. A cancelled browser request can still complete on the server; refresh to retrieve any saved result. The activity chart uses the browser's local dates.

The updated backend provides authenticated endpoints:

- `GET /api/workspace/history`: the current account's latest 50 search runs, including results.
- `GET /api/workspace/search/maps?query=...`: Google Places search, normalized results, and saved run.
- `GET /api/workspace/search/copyright?query=...&pages=2`: browser copyright search, normalized results, and saved run.

History is saved under the backend's ignored `.data/searches/` directory, with a per-account filename derived from the email hash. Override `heimdall.history.directory` if needed. These files contain public business results and should stay private to the backend operator. Results survive backend restarts; sessions do not. This local file store is designed for a single backend instance. Restart the backend after installing these changes; the previous backend version does not have workspace endpoints.

The Maps service now validates query input, serializes it as JSON safely, applies network timeouts, and reports provider authentication/quota failures rather than treating them as empty results. Google search may still return a browser challenge; the scraper reports that failure and does not solve challenges.

## Bento statistics

The dashboard overview adapts the supplied Finance Bento style for lead discovery: gradient cards, dot-matrix lead totals, phone-coverage gauge, successful-search counts, average search duration, method split, recent results, and SVG activity charts. The 7D, 30D, and All controls filter actual saved runs. All covers retained history (at least a seven-day plotting window); no finance values or synthetic growth are used. The stream shows cumulative matches within the selected period; interval charts show leads and completed searches per date bucket. Contact coverage means the percentage of lead matches with at least one phone number. Empty periods show zero counts and an unavailable coverage/time measure. Chart date ranges follow the browser's local timezone. Recent search rows reopen saved results.

## Bring your own Google Places key

The methods are named **Google Places** and **Website Copyright**. Google Places requires the signed-in user to add a Google Cloud API key on its search screen. Enable Places API (New) and billing for that key; Google validates its permissions and quota when the first search runs. The backend has no shared-key fallback.

Keys are sent in a CSRF-protected POST body, retained only in the user's server-side session, and never returned to the browser or saved in account/history files. Save, replace, and remove controls are available. Sign-out, session expiry, or backend restart clears the key. Website Copyright requires no Places key. The existing saved results remain readable without a key.

Endpoints: `GET /api/workspace/places-key` returns only `{ configured: boolean }`; POST accepts `{ apiKey: string }`; DELETE removes it. Writes require the CSRF header. Both workspace and legacy Google Places search endpoints require the session key and return HTTP 428 if absent.

## Named lead files

The **Leads** sidebar page displays account-specific named files using the supplied Glass Folder Cards component. After a search returns leads, choose **Save to Leads**, select a generated search, and name the file. You can also choose **Save generated leads** inside the library. Open a file to filter its contacts, apply the phone-only filter, or export CSV; the folder menu offers view, rename, and download actions.

Lead files store independent snapshots of generated results, so they remain available after a run leaves the 50-search history window. The backend stores up to 100 named files per account under ignored `.data/lead-files/` (override `heimdall.files.directory`). Names accept 1–100 characters; CSV downloads sanitize filesystem characters. Renaming does not change leads. Files are private to the authenticated account and persist across sessions and restarts. Creation and renaming require CSRF-protected requests. No placeholder/demo lead files are added to user accounts.

## Bottom navigation

The workspace uses the supplied Nimbus Liquid Navbar as a fixed bottom dock instead of a left sidebar. Dashboard, Leads, and Profile open their corresponding pages; Scrape opens an upward menu with Google Places and Website Copyright. The menu supports keyboard navigation, Escape, and outside-click dismissal. Profile shows account information, session API-key status, a key-management shortcut, and sign-out. Navigation stays visible on mobile, respects safe-area insets, and leaves room below page content. Sound is disabled, and the provided component includes a CSS fallback when WebGL is unavailable.

### First-sign-in profile setup

Authenticated dashboard visits load `/api/workspace/profile` before rendering the workspace. Accounts without a completed profile enter a setup form requiring first name and display name, with optional last name and website. Saving the profile plays the logo reveal, then opens the dashboard. Completion is stored per account in ignored `.data/profiles/<sha256-email>.json`, survives sessions and restarts, and can be updated from Profile. Photos remain browser-local. The bottom dock uses the saved display name. Login-triggered reveals happen after setup; refreshing the dashboard does not replay them.

### Email verification for registration

New accounts require a six-digit code delivered through Resend. `/api/auth/register` sends a code and returns an opaque challenge; it does not create or sign in an account. `/api/auth/register/verify` creates the account only after successful verification, then starts the normal setup → reveal → dashboard flow. Existing accounts can still sign in without re-registering.

Configure `RESEND_API_KEY` and `HEIMDALL_MAIL_FROM` in the backend environment, or put them in the backend working directory's ignored `.env.local` as plain `KEY=value` entries (no quotes or `export`). The sender must be verified with Resend. Restart the backend after configuring them. Never use a `VITE_` prefix or put these credentials in frontend code. With no mail configuration, registration returns a clear service-unavailable error and creates no account.

Codes expire after 10 minutes, permit five attempts, and can only be used once. Resend waits 60 seconds and each email address is limited to five sends per hour, alongside the authentication IP limit. Resending replaces the old challenge. Only code digests and BCrypt password hashes are held in server memory until verification; pending registrations clear on restart. Codes are never logged or returned to the browser. Profile text and account data persist separately.

### Teams and shared lead work

The bottom dock includes Teams. Account holders can create a team (becoming its leader) or join with a leader-provided, random join code. Only current team members can list/read that team's shared workspace or modify its data; leaders alone can view/replace join codes and remove members. Members can leave; the leader remains in the team. Replacing a code prevents future joins with the old code but does not remove current members.

Any member can import a non-empty file from their own Leads library. The import is a shared snapshot with independent lead IDs; the private source file is unchanged and isn't synchronized after import. Team leads can be marked done or reopened. Status updates record the actor and time and reject stale versions. Notes are append-only, attributed to the authenticated author with the saved display name and timestamp; clients cannot choose the author. All members see notes and status updates through five-second polling. Membership summaries refresh every ten seconds.

Teams are persisted atomically in ignored `.data/teams/<team-id>.json` files with owner-only file permissions, matching the existing single-backend storage design. Updates are synchronized within that process to preserve concurrent notes. Each account can join up to 20 teams, each team has up to 50 members, 50 imported folders and 2,000 leads, and each lead can have up to 100 notes of 2,000 characters. Join attempts are limited. No demo teams are automatically added. For multiple backend instances, replace the file store with shared transactional storage before scaling.

Display names are unique across accounts, case-insensitively after Unicode normalization and whitespace normalization. Setup and Profile check availability while typing; the server rechecks atomically on save and returns 409 if another account already owns the name. An account can keep its current display name.

### Google sign-in
Create a Web application OAuth client in Google Cloud → Google Auth Platform. Configure branding/audience and add test users while in Testing mode. Add the exact redirect URI `http://127.0.0.1:5174/api/auth/google/callback`.

Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in the backend `.env.local` (never frontend VITE variables). The currently running backend uses `/Users/Krish/Documents/Playground/leadScrapper-fast/.env.local`. Restart the backend after configuring. The login button becomes available when both credentials are present. Use `127.0.0.1:5174` consistently; localhost has separate session cookies.

For deployment set `HEIMDALL_FRONTEND_URL` to the public HTTPS origin and `HEIMDALL_GOOGLE_REDIRECT_URI` to its `/api/auth/google/callback` URL, register that exact callback with Google, and proxy `/api` to the backend. Google scopes are only openid, email, profile; no Places or Gmail access. The backend validates OIDC and verified email, persists Google subject bindings, discards provider tokens after login, and uses the existing workspace session/onboarding. Password accounts keep their password; Google can automatically link authoritative Gmail/Workspace addresses. Existing third-party email accounts require password sign-in rather than automatic linking. Google-only accounts get an unknowable random password hash.

### Premium purchases (Razorpay)
Both discovery APIs (including legacy routes) require a verified Premium purchase. Set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` in the backend `.env.local`; never use VITE variables for the secret. Orders are created server-side at ₹100 / INR 10000 paise and tied to the signed-in account. Checkout responses are checked using HMAC-SHA256, followed by a server API check for captured status, exact amount/currency/order and zero refunds. Access records persist in `.data/billing` with separate test/live stores. Configure automatic capture in Razorpay. Test payments never grant live-mode Premium. The pricing card and locked discovery views open checkout; verified buyers receive Profile and navbar badges. Live API keys require Razorpay merchant activation; live checkout has not yet been verified. Refund/dispute webhook automation is not yet configured; operator-managed entitlement revocation is needed if a purchase is later refunded. Use an HTTPS public deployment before configuring webhooks.

### SEO and production indexing
The production build generates initial semantic HTML and unique head metadata for each of the eight public routes. React replaces that initial content after startup; this is a progressive HTML fallback, not full React server rendering. Login/dashboard HTML is noindex. Unknown routes need real HTTP 404 responses; see `deploy/Caddyfile.example`. Do not rewrite every request to the homepage: serve each generated route's `index.html`.

Set `VITE_SITE_URL=https://YOUR-REAL-DOMAIN` before building to generate self-referencing canonical URLs, Open Graph/Twitter previews, Organization/WebSite/WebPage/Breadcrumb JSON-LD, and the public sitemap. Without it, robots.txt disallows crawling and HTML is noindex for staging safety. The value is public and must not contain secrets. Rebuild after changing the domain. The sitemap excludes authenticated routes and does not invent modification dates. No fabricated reviews or ratings are emitted.

After deployment: verify redirects to one HTTPS hostname, verify real 404 statuses, check `/robots.txt` and `/sitemap.xml`, register the domain with Google Search Console using its DNS verification record, submit the sitemap, and inspect the main public URLs. Search rankings and rich results are not guaranteed. Validate Google rendering, Core Web Vitals and mobile layouts on the public site; review Search Console query data before expanding keyword-focused content. The intro animation still needs real-device performance measurement.

### Search provider update
Website Copyright uses each signed-in user’s own Brave Search API key. Connect it in Scrape → Website Copyright; setup instructions are at `/brave-search-setup`. Keys are kept only in the backend session, never returned to the client or persisted, and cleared on sign-out/session expiry. Both workspace and legacy search routes require the session key and Premium access. No shared environment key is used. Google Places and Brave Search usage are billed separately by each provider and excluded from ₹100. Live Brave testing requires a user-supplied active key.

### Target-based discovery
Both methods offer 10, 25, or 50 matching leads. They paginate each provider query, then automatically try related queries and area modifiers while preserving the original search text and all qualification filters. Matches are deduplicated across queries (place IDs for Google; website domains for Brave). They no longer stop after the first 60 Google candidates or first Brave query, and the expanded run has no fixed three-minute cutoff.

Users choose an explicit API request budget of 10, 25, 50 (default), or 100 requests. Each provider call can incur usage fees. Searches stop at the target, cancellation, provider failure, exhaustion of all query variants, or that budget. Reaching a specific count cannot be guaranteed if there are insufficient qualifying businesses. Live progress includes the active query, request count, candidates checked, matches, and final stop reason. `POST /api/workspace/search/cancel` uses CSRF protection and stops further backend requests; in-flight requests may finish before cancellation takes effect. Partial results are saved to search history. Only one search runs per signed-in session at a time.

### SEO and launch-risk review
See [the audit](docs/seo-and-launch-audit.md) before public launch. Full Google result storage, exports and team copies are enabled at the owner’s request pending provider/legal review. Broader provider-use permissions remain a documented launch-review requirement. No legal compliance or lawsuit immunity is claimed. Run `npm run build` and `npm run seo:check`; configure the real `VITE_SITE_URL` only when ready to deploy. No domain means intentional staging noindex.

### Security hardening (2 November 2026)
See [the security review](docs/security-review.md) for findings, regression evidence, request limits and production settings. Searches now require CSRF-protected POST requests; previous GET search integrations must be updated. Activate `SPRING_PROFILES_ACTIVE=production` only behind the documented same-host HTTPS reverse proxy.
