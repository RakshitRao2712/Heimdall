# Heimdall

A React + TypeScript frontend built with Vite, Tailwind CSS, Framer Motion, and GSAP, with a Spring Boot backend in `backend/`.

The black landing page includes:

- An animated Tilted Grid Hero with ten supplied R2 images.
- An immersive fullscreen navigation with a clip-path reveal, keyboard focus trap, and Escape-to-close.
- A wheel-driven Perspective Tunnel Hero with pointer parallax.
- A responsive feature-card section.

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

The dashboard supports both lead endpoints, counts returned leads and phone entries, filtering, and CSV export. Cancelling stops the browser request, not necessarily work already underway on the backend. Google sign-in is a disabled visual placeholder as requested. Self-service password reset and email verification are not implemented. No demo testimonials are presented as verified customer reviews, and credentials are never logged by the frontend.

The local account store is intended for a single backend instance. Before production deployment, use HTTPS with `server.servlet.session.cookie.secure=true`, durable protected account storage, and operational account recovery. Serve `/login` and `/dashboard` with the SPA fallback and proxy `/api` to the backend. The included backend must be deployed together with this frontend.

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
