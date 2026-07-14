# Address search API call policy

## Status

Exploratory design only. This document makes no runtime behaviour change. It is
based on the address/POI search introduced on the parent branch and should be
reviewed before implementing a follow-up.

## What exists today

`SearchStopViewModel` starts a separate address-search coroutine for every
non-blank text change when `search_stop_address_search_enabled` is enabled. It
waits 350 ms, then calls `RemoteAddressResultsManager`. The manager trims the
query, limits it to 64 characters, and makes an NSW `stop_finder` request only
when the trimmed value has at least two characters. It filters transit-stop
results out of that response.

This gives local stop search and remote address search independent failure and
rendering behaviour. It also leaves three avoidable costs:

- One- and two-character values still create a coroutine and loading state
  before the manager rejects them.
- Backspacing or returning to a query can call the API again; there is no
  session cache or duplicate-request coalescing.
- Cancelling the coroutine relies on the HTTP call honouring cancellation. A
  late result has no query-version check before it updates UI state.

The API call must remain independent of local stop-result count. A local match
such as “Central” does not mean that an address/POI match is irrelevant; gating
on a local no-match would hide valid locations and make results inconsistent.

## Data-backed minimum-length analysis

### Baseline available today

The local KRAIL-Analytics database was queried on 2026-07-13 for Australian
events from 2026-03-18 through 2026-07-11. The sample contains 15,415
`search_stop_query` events from 426 users and 3,683 direct `stop_selected`
events that carry a non-empty `searchQuery` (recents excluded).

| Query-length view | `< 3` | `< 4` | `< 5` | `< 6` |
|---|---:|---:|---:|---:|
| Local search-query events suppressed by a gate | 27.4% | 41.1% | 54.7% | **64.9%** |
| Direct local-stop selections occurring below the gate | 5.8% | 24.2% | 46.3% | **61.4%** |

The distribution makes the trade-off clear: a six-character gate prevents the
remote address call for almost two-thirds of the local typing traffic, and
short strings are demonstrably valuable to local stop search. Local search must
therefore remain available at every length; only the optional remote address
section is gated.

This is **not** evidence that 61.4% of address selections are short. Historical
`stop_selected` rows do not include `location_kind`, and the address feature was
not broadly enabled during this sample. We must not infer address intent from a
local stop selection or copy raw query text into new analytics.

### Provisional default: six characters, remotely tunable

Use a minimum address-query length of **six characters** (`length >= 6`) for
the first controlled release. Six is both the app fallback and the initial
Remote Config value for the proposed
`search_stop_address_min_query_length` integer. The effective threshold must
be read from that parameter so it can be changed remotely, for example to two
or eight, without an app release.

Accept only integer Remote Config values in the proposed `2..12` range. A
missing, malformed, non-integer, or out-of-range value must fall back to six;
do not silently clamp it. This is a traffic-safety default, not a permanent
product claim about address intent.

The following SQLite query reproduces the baseline from
`KRAIL-Analytics/data/krail.db`; it deliberately outputs only aggregates, never
individual query text:

```sql
WITH selected AS (
  SELECT json_extract(params, '$.searchQuery') AS query
  FROM raw_events
  WHERE event_name = 'stop_selected'
    AND COALESCE(json_extract(params, '$.isRecentSearch'), 0) = 0
    AND country = 'Australia'
), typed AS (
  SELECT length(trim(query)) AS query_length
  FROM selected
  WHERE query IS NOT NULL AND trim(query) <> ''
)
SELECT
  COUNT(*) AS direct_search_selections,
  ROUND(100.0 * SUM(query_length < 6) / COUNT(*), 1) AS under_six_pct
FROM typed;
```

### Evidence required to keep, lower, or raise six

Start at the six-character default and observe bounded address-selection
outcomes. Change the Remote Config threshold one bounded value at a time only
when those outcomes show a clear reason to do so. Do not use Firebase to record
every remote request or raw query text. Request volume, latency, and errors may
be consulted when operational aggregates are readily available, but are not a
release blocker for this small-user-base rollout.

For successful address selections only, extend the existing selection outcome
with bounded fields: `location_kind = address`, `query_length_bucket = 3_5 |
6_8 | 9_12 | 13_plus`, and the resolved threshold value. Do not send the query,
address text, or opaque address ID.

Review each threshold setting using these measures:

| Decision measure | Interpretation |
|---|---|
| Share of address selections in `3_5` | The address value that a six-character gate would hide. |
| Address selection per enabled search session by threshold | Whether the higher gate materially lowers useful outcomes. |
| Remote requests and failure/latency per enabled session by threshold, if available | The traffic and reliability cost of a lower gate. |

Keep six when lowering the configured threshold shows negligible address
selection in `3_5`. Lower to the smallest supported threshold only when the
recovered address selections demonstrate useful value; factor in request cost
when it is available. Numerical traffic budgets are not required for this
initial rollout and cannot be derived from the local-stop-only baseline.

## Recommended eligibility policy

Put this policy at the ViewModel boundary, before a loading state or a network
job is created. `RemoteAddressResultsManager` should retain its own defensive
length and maximum-length checks as a second line of defence.

| Condition | Action |
|---|---|
| Address-search kill switch is off | Cancel any pending work, clear only the address section, make no request. |
| Text is blank | Cancel work, clear address results, show recents as today. |
| Normalized text has fewer than the effective configured threshold (six by default) | Cancel work and clear the address section without showing its loading UI or calling the API. |
| Exact normalized query is in the session cache | Render the cached result immediately; do not call the API. |
| A request for that exact normalized query is already pending | Keep the existing request; do not start a duplicate. |
| Otherwise | Wait 350 ms after the last text edit, re-check the conditions, then make one request. |

“Normalized” means trim leading/trailing whitespace locally before eligibility,
cache lookup, and request construction. Use a case-folded form only as the
cache/coalescing key unless NSW API testing proves case-insensitivity.

Six characters is the provisional, data-backed app fallback and initial Remote
Config value described above, not an API guarantee. The comparison cohort must
test whether three-to-five-character address/POI selections justify changing
the remote threshold. Test short numeric addresses and short place names
against the NSW API before lowering it; document any exception rather than
silently changing the global setting.

## Remote Config contract

Keep `search_stop_address_search_enabled` as the only availability kill switch.
`search_stop_address_min_query_length` is a bounded tuning value, not a second
way to enable the API. An address request is eligible only when the kill switch
is enabled and the normalized query meets the resolved threshold.

| Parameter | App fallback | Initial Remote Config value | Valid remote values |
|---|---:|---:|---|
| `search_stop_address_search_enabled` | `false` | Existing rollout-controlled value | Boolean |
| `search_stop_address_min_query_length` | `6` | `6` | Integer `2..12` |

When Remote Config changes, re-evaluate the current query through the normal
debounce path. Raising the threshold cancels any now-ineligible pending request
and clears only the address section. Lowering it may schedule the current query
after the debounce; it must still honour the kill switch, cache, and duplicate
coalescing rules. Record the resolved threshold only as a bounded operational
metric dimension or cohort property—never alongside raw query text.

## Request lifecycle

1. A new eligible query cancels the delayed or in-flight request for the old
   query.
2. After the debounce, the ViewModel rechecks that the query is still current,
   enabled, and eligible. This prevents a queued request after a flag change or
   text edit.
3. Assign a monotonically increasing request token before dispatch. Apply a
   result only when its token and normalized query still match current state.
4. Do not automatically retry errors. Retrying while a user types multiplies
   traffic and an error result is optional UI. The next deliberate text change
   may make a fresh request; any explicit retry affordance would be a separate
   UX decision.
5. Clear loading on cancellation/error, but never replace a newer query's
   results with an older response.

Use a flow operator such as `debounce` plus `mapLatest`, or an equivalent
explicit token implementation. The tests must prove the late-response rule even
if the underlying HTTP client does not cancel promptly.

## Session cache proposal

Use an in-memory, per-SearchStop-ViewModel LRU cache only. Do not persist API
results: display names and address IDs belong to a live upstream source and
recents already capture the user-selected locations.

Initial values to validate in rollout:

| Setting | Proposed value | Why |
|---|---:|---|
| Maximum entries | 20 | Covers normal type/backspace/refine behaviour without retaining an unbounded query history. |
| Successful-result TTL | 2 minutes | Avoids immediate duplicate traffic while keeping data fresh during a search session. |
| Empty-result TTL | 30 seconds | Suppresses repeated broad/no-match prefixes without treating an empty response as durable truth. |
| Error cache | None | A transient failure must not block the next eligible query. |

The cache key must include every input that can change the response. Today that
is the normalized query; add locale, coordinates, or search filters if the API
contract later uses them. Cache values must not be written to analytics or
diagnostic logs.

## Deliberately rejected gates

- **Only call when local stop search has no results.** It saves calls but loses
  address/POI discovery whenever a transit stop happens to match the same text.
- **Call on every keystroke.** This makes partial typing the dominant traffic
  source and produces flickering/stale results.
- **Use a persistent cache.** It creates a long-lived record of user-entered
  location text and risks stale upstream values for little benefit.
- **Automatically retry failures.** It turns an upstream incident into more
  client traffic without improving the optional search section.

## Decisions and implementation follow-ups

- Do not introduce a client-side quota, concurrency, or rollout-percentage
  constraint at this stage. The user base is small; the configured threshold,
  debounce, cancellation, cache, and Remote Config kill switch are sufficient
  initial traffic controls.
- Trim leading/trailing whitespace locally for eligibility, caching, duplicate
  coalescing, and request construction. The NSW endpoint does not need to
  define this behaviour for the client to be consistent.
- The app calls the NSW API directly, not through the BFF. Before implementation,
  inspect the direct NSW API client's timeout and cancellation configuration and
  ensure a late response cannot update a newer query's state; this is a code
  correctness check, not a rollout blocker.
- Operational request/latency/error aggregates are useful if readily available,
  but are not required to launch this small-user-base rollout. Do not add raw
  query or address data to Firebase to compensate.

The first threshold review can rely on bounded selection outcomes and the
observed request behaviour available to the team. Adjust the remote threshold
only when it demonstrates a clear product or traffic benefit.

## Implementation and test checklist

- Extract a pure `AddressSearchEligibility` decision so boundaries are unit
  testable without a ViewModel.
- Add tests for disabled, blank, below-threshold, debounce, cache hit, duplicate
  coalescing, cancellation, stale response rejection, empty-result TTL, and
  error-not-cached behaviour.
- Confirm an address request cannot change the local-search loading/error state.
- Confirm a flag flip while a debounce is pending produces no request.
- Redact the query from address-search failure logs before enabling a cohort;
  address text can be personal data.
