# Skip businesses from saved lead files

On either active discovery screen, expand **Skip leads from saved files**, select one or more files, then run a search. No files are selected by default. Selection applies to subsequent searches in the current workspace session and can be cleared.

The UI loads only file names, methods, and counts. Search requests pass repeated `excludeFileIds` parameters. The backend resolves a snapshot from the authenticated account's saved files; missing, deleted, or foreign IDs reject the search before a provider call. Selected files stay unchanged.

Matching uses namespaced Google place IDs for newly saved Maps results, plus normalized business name and address or name and phone for older files and cross-provider matches. Name alone does not match, so separate branches are not excluded merely for sharing a name. Old records with no reliable contact/location details may not be identifiable.

Maps excludes matches before counting toward its target and continues within the existing request budget and cancellation rules. Foursquare reads up to 50 candidates in its existing single request when exclusions are selected, removes matches, and stops at the chosen target. Foursquare results remain session-only under the existing retention behavior; this feature does not enable saving them.

Provider exhaustion can produce fewer or zero fresh results. No extra unbounded searches are started. Existing saved files and history remain backward compatible with the optional `providerId` field.

Validation: account isolation/deleted files, normalized matching and branch distinction, stable IDs, Maps pagination and budget exhaustion, controller persistence, Foursquare filtering and single-request behavior; frontend build and public-page SEO checks. UI multi-select and file filtering checked with sample data in an isolated local preview.
