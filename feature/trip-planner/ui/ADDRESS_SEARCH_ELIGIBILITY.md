# Address search eligibility, cache, and staleness

Implements the policy in `docs/plans/ADDRESS_SEARCH_API_CALL_POLICY.md`. Read that doc
first for the "why" (data-backed threshold, rejected alternatives, rollout plan); this
file documents the classes that came out of it and what they deliberately don't do.

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
result, 30s for an empty one (`AddressSearchCache` companion constants). Checked after
the eligibility gate and before the debounce delay — a cache hit renders immediately
with no loading flicker and no network call.

The cache lives on the ViewModel instance, not behind DI — it must not outlive the
screen or be shared across `SearchStopViewModel` instances (see "Session cache
proposal" in the policy doc: no persistence, no cross-session sharing).

## Staleness guard, not request coalescing

`SearchStopViewModel` keeps a single monotonic `addressSearchRequestToken` (an `Int`
incremented each time a request is scheduled). A response only updates UI state if the
token it captured is still current. This satisfies the policy doc's "a late result has
no query-version check" concern.

**Deliberately not implemented**: `Deferred`-based duplicate-request coalescing (the
policy doc's "a request for that exact normalized query is already pending" row).
`SearchStopViewModel` only ever has one live `addressSearchJob`, and it is cancelled
before every new one is launched — so two concurrent in-flight requests for the same
query cannot occur under the current architecture. Adding coalescing machinery for a
race that can't happen would be speculative complexity. Revisit only if the ViewModel's
single-job-cancel-and-replace invariant changes (e.g. a future multi-source merge).

## Redaction

Both the ViewModel's and `RealRemoteAddressResultsManager`'s failure logs record
`normalizedQuery.length`, never the query text — addresses can be personal data (see
policy doc's release gates).

## Remote Config

| Key | Type | Fallback | Valid range |
|---|---|---|---|
| `search_stop_address_search_enabled` | Boolean | `false` | - |
| `search_stop_address_min_query_length` | Integer | `6` | `2..12`, else fallback |

`resolveAddressSearchMinQueryLength` range-checks in `Long` space before narrowing to
`Int`, so an oversized remote value can't wrap around into a false in-range result.
