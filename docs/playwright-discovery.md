# Playwright browser discovery

Scrape → Website Copyright · Playwright uses the existing `browser` method ID, so saved searches remain compatible. Playwright replaces Selenium; the other three providers are unchanged. No search API key is required for this method.

Install Chromium on each backend host after installing the Maven dependencies:

```sh
./mvnw exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

Linux hosts may need `install --with-deps chromium`. See https://playwright.dev/java/docs/browsers.

Each search gets a temporary isolated Chromium context, without personal cookies or profiles. The backend permits one browser search at a time and closes it on completion, cancellation or failure. `heimdall.browser.visible=true` opens a local window; hosted installations normally use headless mode. Browser objects and manual input are accessed only on the owning search thread.

Business/location queries automatically include the selected copyright years (for example `plumbers in Chicago "copyright 2024"`, then 2023 and alternate copyright expressions). Explicit copyright queries are preserved. Queries never fall back to an unqualified business search. Search pages are paginated per query, with duplicates removed across queries. Google snippets are only discovery hints; live website copyright and phone checks still determine eligibility.

The existing target, year filters, request budget, website checking and session-bound manual verification controls remain. Google can still block automated searches or request verification; Playwright does not bypass or guarantee acceptance of CAPTCHA. Verification expires after five minutes. Diagnostics report Google script errors, timeouts and failed verification resources without retaining raw URLs, tokens or cookies.

Validation: run `./mvnw test`. The optional local Chromium iframe click fixture (not a CAPTCHA) is enabled with `HEIMDALL_BROWSER_RELAY_TEST=true ./mvnw -Dtest=PlaywrightClickRelayTest test`.
