# SerpApi copyright discovery

The third Scrape entry is Website Copyright · SerpApi (`/dashboard?method=serp`). It uses SerpApi Google organic results, then shares Brave’s query variants, copyright-year verification, public phone extraction, domain deduplication, cancellation, and target handling. Targets are 10/25/50, with a default maximum of 50 provider requests. Each SerpApi page is one provider request; Google pagination uses start=0/10/20. Provider result counts differ, so equal request counts do not mean equal candidate counts.

Connect a personal SerpApi key in the dedicated connection panel. Keys remain in the authenticated backend session, are never returned to the browser or persisted, and are cleared on sign-out/session expiry. SerpApi requires its key on the outbound server request URL; application errors do not expose the URL or raw provider errors. Do not enable HTTP wire logging with real credentials.

The new POST endpoint inherits CSRF, Premium gating, per-account throttling and active-search isolation. Search history, saved files and team imports retain `serp` as the source. The dashboard groups both copyright providers in its copyright charts and lists them separately in Search mix and recent search labels.

Provider request/response contract: https://serpapi.com/search-api
Setup instructions are available at `/serpapi-setup`. Provider charges are separate from Heimdall’s purchase.

Validation: frontend production build; 70 backend tests including SerpApi parsing/pagination, safe errors, key isolation, target/deduplication/cancellation and saved-file provenance. Browser verified the real method UI and missing-key guard. No live SerpApi search has been run: the user will supply the key for a later comparison.
