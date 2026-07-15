# Address search eligibility, cache, and staleness

Documents the address/POI search call decision for SearchStopScreen: when
`onAddressSearchTextChanged` is allowed to call the NSW `stop_finder` API, what gets
cached, and how a late response is prevented from clobbering newer UI state.

## Classes

All in `searchstop/address/` (`:feature:trip-planner:ui`):

| Class | Kind | Responsibility |
|---|---|---|
| `AddressSearchGate` | enum | The four possible eligibility outcomes. |
| `AddressSearchEligibility` | pure object | `evaluate(normalizedQuery, isAddressSearchEnabled, minQueryLength) -> AddressSearchGate`. No I/O, no state. |
| `AddressSearchQueryNormalizer.kt` | pure functions | `normalizeAddressQuery` (trim) and `addressSearchCacheKey` (trim + lowercase). |
| `AddressSearchMinQueryLength.kt` | pure function | `resolveAddressSearchMinQueryLength(flag)` — reads and validates the Remote Config integer. |
| `AddressSearchCache` | stateful class | Per-`SearchStopViewModel` bounded LRU with per-entry TTL. |

Each is independently unit-testable without a ViewModel, Koin, or a fake network layer —
that was the point of splitting them out rather than growing
`SearchStopViewModel.onAddressSearchTextChanged` in place.

## Gate order

`AddressSearchEligibility.evaluate` checks, in order: kill switch off -> blank -> below
threshold -> eligible. `SearchStopViewModel.onAddressSearchTextChanged` calls this
**before** creating a loading state or a coroutine, and again after the 350ms debounce
(a flag flip or further edit mid-debounce must not fire a now-stale request).

## Cache

Keyed by `addressSearchCacheKey` (case-folded, trimmed). Two TTLs: 120s for a non-empty
result, 30s for a genuinely empty one (`AddressSearchCache` companion constants).
Checked after the eligibility gate and before the debounce delay — a cache hit renders
immediately with no loading flicker and no network call.

The cache lives on the ViewModel instance, not behind DI — it must not outlive the
screen or be shared across `SearchStopViewModel` instances: no persistence, no
cross-session sharing.

**A failed request is never cached.** `RealRemoteAddressResultsManager` deliberately
does not catch its own exceptions — it lets them propagate so `SearchStopViewModel` can
tell "the call failed" apart from "the call succeeded with zero results." Only
`Result.success` reaches `addressSearchCache.put(...)`; a failure still resolves to an
empty `addressResults` list for the UI (this is an optional section, so it never shows
an error state), but the *next* identical query is free to retry immediately rather
than being blocked by a 30s empty-result TTL that was never earned. Caching a transient
failure as "no results" was a real bug caught late in review — the fix is
`fetchResult.onSuccess { addressSearchCache.put(cacheKey, it) }` gating the cache write,
not the `getOrElse { emptyList() }` that produces the UI-facing value.

## Staleness guard, not request coalescing

`SearchStopViewModel` keeps a single monotonic `addressSearchRequestToken` (an `Int`
incremented each time a request is scheduled). A response only updates UI state if the
token it captured is still current — this guards against a late response overwriting a
newer query's UI state even if the underlying HTTP call doesn't cancel promptly.

**Deliberately not implemented**: `Deferred`-based duplicate-request coalescing (i.e.
detecting "a request for this exact normalized query is already pending" and reusing
it). `SearchStopViewModel` only ever has one live `addressSearchJob`, and it is
cancelled before every new one is launched — so two concurrent in-flight requests for
the same query cannot occur under the current architecture. Adding coalescing machinery
for a race that can't happen would be speculative complexity. Revisit only if the
ViewModel's single-job-cancel-and-replace invariant changes (e.g. a future multi-source
merge).

## Redaction

`SearchStopViewModel`'s failure log records `normalizedQuery.length`, never the query
text — addresses can be personal data. `RealRemoteAddressResultsManager` no longer logs
at all (see Cache section above: it propagates failures instead of catching them), so
there is a single redacted log site, not two.

## Analytics

Each resolved, non-stale address fetch fires `search_stop_query` with
`resultSource = address`, `queryLength`, `resultsCount` (or `isError` on failure), and
the same `searchSessionId` as the local pipeline's firing for that query. Cache hits do
not fire - the event counts real network calls, which is the number the API cost model
cares about. Raw query text is never sent, with one exception: the zero-result
carve-out in `SearchQueryAnalyticsRedaction` (zero results in both pipelines, no
digits, 25 chars or fewer) - the address completion site owns that decision for
address-eligible queries because only it knows both pipelines' counts. See
`docs/ANALYTICS_REGISTRY_HANDOFF.md` for the param registry status.

## Remote Config

| Key | Type | Fallback | Valid range |
|---|---|---|---|
| `search_stop_address_search_enabled` | Boolean | `false` | - |
| `search_stop_address_min_query_length` | Integer | `6` | `2..12`, else fallback |

`resolveAddressSearchMinQueryLength` range-checks in `Long` space before narrowing to
`Int`, so an oversized remote value can't wrap around into a false in-range result.
