# Address search eligibility and staleness

Documents the address/POI search call decision for SearchStopScreen: when
`onAddressSearchTextChanged` is allowed to call the NSW `stop_finder` API, and how a late
response is prevented from clobbering newer UI state.

## Classes

All in `searchstop/address/` (`:feature:trip-planner:ui`):

| Class | Kind | Responsibility |
|---|---|---|
| `AddressSearchGate` | enum | The four possible eligibility outcomes. |
| `AddressSearchEligibility` | pure object | `evaluate(normalizedQuery, isAddressSearchEnabled, minQueryLength) -> AddressSearchGate`. No I/O, no state. |
| `AddressSearchQueryNormalizer.kt` | pure functions | `normalizeAddressQuery` (trim) and `addressSearchCacheKey` (trim + lowercase). |
| `AddressSearchMinQueryLength.kt` | pure function | `resolveAddressSearchMinQueryLength(flag)` — reads and validates the Remote Config integer. |

Each is independently unit-testable without a ViewModel, Koin, or a fake network layer —
that was the point of splitting them out rather than growing
`SearchStopViewModel.onAddressSearchTextChanged` in place.

## Gate order

`AddressSearchEligibility.evaluate` checks, in order: kill switch off -> blank -> below
threshold -> eligible. Nothing else. Every query of `search_stop_address_min_query_length`
characters or more calls the API.

`SearchStopViewModel.onAddressSearchTextChanged` calls this **before** creating a loading
state or a coroutine, and again after the 350ms debounce (a flag flip mid-debounce must
not fire a now-stale request). The post-debounce check clears the address section on its
way out, so a previous query's addresses can't linger under a newly-suppressed one.

### Reverted: the stop-count gate

A gate that suppressed the call when on-device stop matches exceeded
`search_stop_address_max_local_stops` (with a 12-character escape hatch) shipped and was
reverted. It broke the main case it was supposed to serve: `13 hassall` matches dozens of
Hassall St bus stops, so the address section went blank for exactly the rider who was
typing a street address, and stayed blank until character 12.

The lesson is that a busy local stop list is not evidence the rider is satisfied. Do not
reintroduce any eligibility input derived from the local pipeline's results — the two
pipelines are independent by design, and coupling them is what produced the bug.

### No result cache

`AddressSearchCache` (a bounded per-ViewModel LRU with 120s/30s TTLs) was reverted with
the stop-count gate. On screen a cached "no results" is indistinguishable from a call that
never happened, and either way the rider sees nothing. Every eligible query now reaches
the network after the debounce. If the call volume ever needs reducing, raise
`search_stop_address_min_query_length` or lengthen the debounce — both are visible in the
typing experience, unlike a silent cache.

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
text — addresses can be personal data. `RealRemoteAddressResultsManager` does not log at
all: it propagates failures rather than catching them, so `SearchStopViewModel` can tell
"the call failed" apart from "the call succeeded with zero results", and there is a
single redacted log site rather than two.

## Analytics

Each resolved, non-stale address fetch fires `search_stop_query` with
`resultSource = address`, `queryLength`, `resultsCount` (or `isError` on failure), and
the same `searchSessionId` as the local pipeline's firing for that query. With no cache
in the path, that is one firing per network call. Raw query text is never sent, with one
exception: the zero-result carve-out in `SearchQueryAnalyticsRedaction` (zero results in
both pipelines, no digits, 25 chars or fewer) - the address completion site owns that
decision for address-eligible queries because only it knows both pipelines' counts. See
`docs/ANALYTICS_REGISTRY_HANDOFF.md` for the param registry status.

Three params exist to make the length threshold tunable from data rather than guesswork:

| Param | Firing | Meaning |
|---|---|---|
| `localResultsCount` | address | On-device stop matches for the query, so the gate can be scored without a `searchSessionId` join |
| `addressSearchGate` | local | What the address pipeline decided. The local firing happens for **every** settled query, so this is the only record of calls that were *not* made — without it, a threshold that went too far is invisible |
| `queryHasDigit` | local (both success and error) | Whether the query contains a digit. A house number is the cheapest address signal there is; a bool, never the text |

`AnalyticsEvent.SearchStopQuery.AddressGate` mirrors `AddressSearchGate` exactly. Its
`STOPS_ALREADY_SUFFICIENT` and `CACHE_HIT` values were emitted briefly and removed with
the stop-count gate and the cache; historical rows may still carry them.

## Remote Config

| Key | Type | Fallback | Valid range |
|---|---|---|---|
| `search_stop_address_search_enabled` | Boolean | `false` | - |
| `search_stop_address_min_query_length` | Integer | `6` | `2..12`, else fallback |

`search_stop_address_max_local_stops` was removed with the stop-count gate; a value left
set server-side is now ignored.

The resolver range-checks in `Long` space before narrowing to `Int`, so an oversized
remote value can't wrap around into a false in-range result, and it **falls back rather
than clamps** — a typo must not quietly become a valid-looking setting.
