# Address search observability and feature-flag rollout

## Status

Exploratory design only. The address/POI feature already has a default-off
Remote Config kill switch. This document separates the decisions about product
analytics, operational API health, and rollout control before any more events
or flags are added.

## Current controls and measurement

The existing `search_stop_address_search_enabled` Boolean is the correct
availability control:

- it defaults to `false` in `RemoteConfigDefaults`;
- when it is false, `SearchStopViewModel` launches no address-search job;
- debug builds can override it locally in Debug Settings; and
- release builds read the live Remote Config value.

The current generic search instrumentation is not safe to reuse as request
telemetry. `search_stop_query` fires for local search and includes raw `query`.
`stop_selected` includes `stopId` and may include the raw `searchQuery`.
Address text can identify a home, workplace, or visited place. The address
manager also currently logs the failed query. No new address implementation
should add raw query, display name, address ID, or full response data to
analytics or logs.

## Analytics decision

Do **not** add a Firebase event for every address request, response, cache hit,
or failure. Those events are typed at a high frequency, do not describe a user
outcome, and consume analytics volume without helping a product decision. The
project has a lifetime Firebase event-name cap of 500, so a new event requires
the checklist and registry step in `docs/ANALYTICS_EVENTS.md`.

The smallest product-analytics change, if the feature is rolled out, is to
extend the existing `stop_selected` event with bounded properties:

| Property | Values | Product question |
|---|---|---|
| `location_kind` | `transit_stop`, `address` | Are address/POI results selected at all? |
| `address_type` | Allowlisted NSW types such as `street`, `singlehouse`, `poi`, `unknown`; omitted for transit stops | Which result category provides value? |
| `selection_surface` | `search_results`, `recent` | Are returned results useful now and later via recents? |

This reuses a user-outcome event rather than minting a request event. It also
needs a privacy review of the existing `stopId` and `searchQuery` properties for
address selections. Prefer an allowlisted classification property over sending
the opaque address ID or text. Any resulting parameter or event change must be
registered in the external KRAIL Analytics `EVENT_REGISTRY.md` before merge and
must pass the no-double-counting check.

## Operational API health is separate

Product selection events cannot show whether the API was attempted, slow,
rate-limited, or serving empty results. Measure that in an approved operational
sink (server/upstream dashboards or privacy-reviewed aggregate client telemetry),
not by copying request details into Firebase.

Required aggregate dimensions, with no raw query or location values:

| Metric | Dimensions |
|---|---|
| Eligible request count | app version, cohort, cache status |
| Completion count and latency | app version, cohort, latency bucket |
| Empty response count | app version, cohort |
| Failure count | app version, cohort, sanitized error category/status family |
| Cancellation/stale-response count | app version, cohort |
| Requests per enabled search session | app version, cohort, bucketed count |

Use an operational metric sink for request, latency, and error aggregates when
one is readily available. It is not a release blocker for this small-user-base
rollout. Do not create a permanent Firebase event or send raw query/address data
as a shortcut.

## Flag strategy

Keep one Boolean kill switch for availability:

`search_stop_address_search_enabled`

Do not add a second Boolean whose meaning overlaps (for example,
`address_search_api_enabled`). Two independently evaluated switches make it
unclear which state is authoritative during an incident. The existing flag
already controls whether the address API can be called.

Keep the call-policy's minimum length separately tunable with the bounded
integer `search_stop_address_min_query_length`. Its app fallback and initial
Remote Config value are both `6`, so a missing or invalid Remote Config value
preserves the conservative release behaviour. Accept only integers from `2` to
`12`; fall back to `6` for any malformed, non-integer, or out-of-range value.
This enables measured changes such as `2` or `8` without making it a second
availability switch.

Debounce and cache size/TTL should remain code constants for the first
controlled release. If later evidence requires them to be remotely tuned, use a
versioned, validated configuration contract rather than unbounded independent
values. No configuration value may enable an API call while the Boolean kill
switch is off.

The kill switch takes precedence over every cache/config decision and should be
checked both before scheduling a request and immediately before dispatch.
Operational reporting must include the resolved minimum-length value as a
bounded cohort/dimension so request cost and address-selection outcomes can be
compared across threshold changes, without recording query or address text.

## Rollout plan

1. **Internal verification:** Enable only through the debug override. Exercise
   rapid typing, paste, clear, navigation away, flag changes, cache hits, and
   network errors. Confirm logs are redacted.
2. **Initial external release:** Enable through the existing Remote Config
   audience. Observe available request behaviour and bounded address-selection
   outcomes.
3. **Threshold tuning:** Change only
   `search_stop_address_min_query_length` between bounded values after comparing
   selection outcomes with the resulting request behaviour.
4. **General availability:** Enable broadly when address results show useful
   selection signal. Retain the kill switch for incident response.
5. **Post-launch:** Review whether the feature flag and debug override should
   remain permanent. Retire experiment-only controls once they no longer make a
   decision.

No fixed audience percentages, quotas, or operational-dashboard ownership are
required for this initial small-user-base rollout. Keep the kill switch and
bounded threshold as the controls for responding to unexpected traffic or poor
results.

## Release gates

- Address API calls are impossible while the kill switch is false, including
  during a pending debounce.
- Raw query/address data is absent from new analytics and address failure logs.
- Every new analytics property is allowlisted, documented, and registered.
- When operational reporting is available, it can answer request volume, latency,
  empty rate, error rate, and requests per enabled session by threshold value.
- Unit tests cover the request policy and an enabled/disabled integration test
  covers the ViewModel.
- The cohort can be disabled quickly without changing local stop search or
  persisted recents.
