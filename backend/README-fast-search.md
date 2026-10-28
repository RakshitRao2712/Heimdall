# Faster Google website leads

Requires Java 21+, Maven, and installed Google Chrome. The Google Maps implementation is unchanged.

```sh
./mvnw spring-boot:run
```

```sh
curl --get 'http://localhost:8080/api/lead/google-search' \
  --data-urlencode 'query=plumbers in Chicago' \
  --data-urlencode 'pages=2'
```

The response keeps the existing fields: `websiteUrl`, `businessName`, `phoneNumbers`, `copyrightYear`, `snippet`.

- Search runs in headless Chrome. Each results page is parsed from one HTML snapshot, avoiding per-link WebDriver calls and implicit waits. No fixed sleep is used.
- Website requests use a reused HTTP client and eight shared workers. Each domain is checked once per search. Website HTML is fetched without Chrome, images, or JavaScript.
- Results are capped by `google.search.max.results` (default 20). `pages` accepts 1–10; Google controls how many results it serves per page.
- Visible copyright notices must end in 2023 or 2024. `© 2018–2024` qualifies; `© 2023–2026` does not. Footer notices take priority over body text; JavaScript template source does not qualify.
- Public phone numbers come from `tel:` links, WhatsApp links, schema telephone fields, and visible text. If needed, up to two linked contact pages are visited on the same site. Phone numbers have 10–15 digits and punctuation is removed. Numbers may be landlines; mobile status and ownership are not verified.
- Search-result subpages can fall back to the homepage for copyright checks. Website fetch failures skip that candidate.
- Google challenges and browser failures return HTTP 502 rather than silently reporting zero leads. Concurrent browser searches or a full worker queue return HTTP 503; invalid inputs return HTTP 400.

Optional settings in `src/main/resources/application.properties`:

```properties
google.search.max.results=20
google.search.page.timeout=10
google.search.concurrency=8
```

Timeout is per page request, not for the whole search. One candidate can require a result-page request, a homepage request, and two contact-page requests. More workers can increase load and blocking; concurrency accepts 1–32. Google may still require consent or present a challenge. The scraper does not solve challenges. Websites requiring JavaScript to display copyright or phones may be missed. A copyright year is a prospecting signal, not proof of when a website was last updated.

Run deterministic extraction, search-parser, concurrency, and local HTTP integration checks:

```sh
./mvnw -Dtest=GoogleSearchScraperServiceTest test
```

These checks do not contact Google. Actual speed and lead count depend on Google responses and target websites; no production throughput claim is implied.

## Heimdall authentication

The frontend uses session authentication. Create an account via `POST /api/auth/register`, sign in with `POST /api/auth/login`, check `GET /api/auth/me`, and sign out using `POST /api/auth/logout`. Before each POST, fetch `GET /api/auth/csrf` and send the returned token with the returned header name, keeping the same cookie jar. All lead endpoints now require a signed-in session.

Credentials JSON is `{ "email": "you@example.com", "password": "at-least-10-characters", "rememberMe": false }`. Accounts persist as BCrypt hashes in ignored `.data/accounts.properties` (override `heimdall.auth.file`). This store supports one backend instance. Sessions are in memory; restarting requires signing in again. Extended sessions have a seven-day idle limit; ordinary sessions have eight hours. Account creation and login share an in-memory limit of 20 attempts per source IP per 15 minutes. Secure deployment requires HTTPS, `server.servlet.session.cookie.secure=true`, durable account storage, and an account recovery process. Google OAuth and self-service reset are not configured.

Run account persistence tests with `./mvnw -Dtest=AccountStoreTest test`.

## Heimdall workspace endpoints

Authenticated workspace searches use `GET /api/workspace/search/maps?query=...` and `GET /api/workspace/search/copyright?query=...&pages=2`. Both return a saved run with `id`, `method`, `query`, `createdAt`, `elapsedMs`, and normalized `leads`. `GET /api/workspace/history` returns only the current account's latest 50 successful runs. The original `/api/lead` endpoints remain available to signed-in callers.

Runs persist in ignored `.data/searches/` (override `heimdall.history.directory`). Storage is per account and intended for one backend instance. Failed searches are not saved as successes. If a client disconnects, in-flight searches may still complete and save their results. Network/provider failures are returned as errors; Maps authentication and quota errors do not count as zero leads.

Verify persistence, account isolation, endpoint normalization, auth, and scraper behavior:

```sh
./mvnw -Dtest=SearchHistoryTest,WorkspaceControllerTest,AccountStoreTest,GoogleSearchScraperServiceTest test
```

## User-owned Google Places keys

There is no configured/shared Google Places key fallback. Both `/api/lead` and `/api/workspace/search/maps` use only the authenticated session's key and return HTTP 428 when it is missing. `GET /api/workspace/places-key` returns a configured flag; POST with `{ "apiKey": "..." }` stores a key in that session; DELETE removes it. POST/DELETE require CSRF protection. Keys are not written to disk or returned in responses. Sign-out, expiry, and server restart clear them. A saved key's Google API access is checked when a search runs. Enable Places API (New) and billing in the key owner's Google Cloud project.

## Named lead files

`GET /api/workspace/files` lists the current account's named files. POST with `{ "name": "Chicago plumbers", "runId": "..." }` copies a non-empty generated search owned by that account into a new file. `PATCH /api/workspace/files/{id}` with `{ "name": "New name" }` renames an owned file. Writes require CSRF tokens. Files persist under `.data/lead-files/` (override `heimdall.files.directory`), independently of recent-search retention, with a limit of 100 files per account. File names are labels, never filesystem paths. `LeadFilesTest` verifies persistence, ownership checks, renaming, and retention beyond source history.
