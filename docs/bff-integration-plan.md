# KRAIL ↔ BFF Integration — Plan (v2)

Draft · 2026-05-10 · current branch `feat/bff-local-debug-override`

> **Morning runbook**: see [`BFF_PHASE_A_MORNING.md`](./BFF_PHASE_A_MORNING.md)
> for copy-paste-runnable smoke-test steps. That doc is the single page to
> read on wake-up; this plan is the long-form reference.

> **Phase A integration report**: see
> [`BFF_PHASE_A_INTEGRATION_REPORT.md`](./BFF_PHASE_A_INTEGRATION_REPORT.md)
> for the handover back to the BFF team (what worked, captured latencies,
> asks for next phase).

> Sources (authoritative): `KRAIL-BFF/docs/handover/KRAIL_APP_INTEGRATION_HANDOVER.md`
> (the playbook) and `KRAIL-BFF/docs/handover/KRAIL_API_REFERENCE.md` (real
> captured wire format, field-by-field). Background: `BFF_ADOPTION_GUIDE.md`,
> `API_SCHEMA_DESIGN.md`, `STATUS.md`.
>
> v2 supersedes v1 — the BFF docs were rewritten between drafts.

---

## Branch & PR roadmap

This is the canonical map of what code lands where. Two branches active /
planned right now; future phases will stack on the second.

> **Rule** (from the user): BFF-side feedback that triggers KRAIL changes
> goes back onto **Branch 1**, never a fresh branch. Branch 1 stays the
> single point of truth for the BFF-integration plumbing until merged.

### Branch 1 — `feat/bff-local-debug-override` (in flight, 3 commits)

| Item | Status |
|---|---|
| Phase A wiring (trip / departures / park-ride / GTFS-RT) | ✅ committed (`b8c9811e4 prep` + `1028b5d1b`) |
| Cleartext debug-only config (`androidApp/src/debug/`) | ✅ committed |
| `KrailNetwork:` logging (Ktor `Logging` plugin + pre-call helper) | ✅ committed (`1028b5d1b`) |
| Phase C foundation (`KRAIL-API-PROTO` submodule + `:io:bff-api` Wire codegen) | ✅ committed (`1028b5d1b`) |
| Detekt + arrow scrub fix (no `→` anywhere; `error()` instead of `NotImplementedError`) | ✅ committed (`cf51b457b`) |
| Quality checks + test suite + APK build verified green | ✅ run by user |
| Smoke-test on AVD against running local BFF | ✅ done — 24 calls / 0 failures |
| **`exclMOT_X` underscore bug fix** | 🔲 next commit on this branch |
| **BFF-feedback tweaks** (whatever lands over next ~couple days) | 🔲 land here as additional commits |
| Push + open PR (`gt submit`) | 🔲 when user authorises |

**Rough total diff** (excluding the docs noise): ~600 lines net new across
network plumbing, services, tests, and the `:io:bff-api` skeleton. Single
PR; under the 500-line rule once docs are excluded from review.

### Branch 2 — `feat/debug-settings` (stacked on Branch 1)

The 3-source runtime selector — debug-only Settings entry that lets the
developer pick `NSW_DIRECT` / `BFF_LOCAL` / `BFF_PROD` per endpoint, plus
a global kill switch. Replaces the build-time `local.properties` opt-in
with a DataStore-backed runtime store.

Likely needs **2 stacked PRs** to stay under 500 lines each:

#### PR 2a — module skeleton + store + service refactor (~350 lines)

| Item | Lives in |
|---|---|
| New module `feature/debug-settings/state` (enums + state/event types) | new module |
| New module `feature/debug-settings/store` (`DebugNetworkConfigStore` + KMP DataStore) | new module |
| `EndpointScope`, `NetworkTarget` enums | `state` module |
| `KRAIL_BFF_PROD_BASE_URL` BuildKonfig field (empty until BFF deploys) | `core/network` |
| Refactor `Real*Service` constructors to take `AppInfoProvider` + store | 4 services |
| `baseUrl()` resolver: read store in debug, fall through to BuildKonfig in release | 4 services |
| Wire DI bindings | each network module |
| Tests for the resolver (target × build-type matrix) | each module's `commonTest` |

#### PR 2b — UI + nav entry + integration (~250 lines)

| Item | Lives in |
|---|---|
| New module `feature/debug-settings/ui` (Compose) | new module |
| `DebugSettingsScreen` (top-level list, taj design) | `ui` module |
| `NetworkSelectorScreen` (per-scope radio rows) | `ui` module |
| `DebugSettingsViewModel` | `ui` module |
| Nav entry `DebugSettingsEntry` | `ui` module |
| Row in main `SettingsScreen` gated on `appInfoProvider.getAppInfo().isDebug` | `feature/trip-planner/ui` |
| `X-Krail-Version` default header in shared HttpClient | `core/network` |
| Compare-mode toggle (off by default) | `ui` + `store` |

### Future phases (later branches; not yet started)

Each follows a similar branch-per-phase pattern; will be added to this
roadmap as we approach them.

| Phase | Branch | Depends on | Scope sketch |
|---|---|---|---|
| Phase C consumer | `feat/proto-trip-results` | Branch 1 (foundation already laid) | Implement `JourneyListMapper` (`JourneyList` → `TripResponse`); add `implementation(projects.io.bffApi)` to trip-planner network; flip `IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED` (or wire to debug-settings store); snapshot-test fixture |
| Phase D | `feat/local-stop-search` | Branch 1 | Manifest fetch + cache + SHA-256 verify; `StopsDataset.pb` consumption (already in `:io:bff-api`); replace `RealTripPlanningService.stopFinder()` with local search; delete NSW direct call |
| Phase B production | `feat/firebase-rc-rollout` | BFF deployed + Branch 2 | Firebase RC flags `bff_kill_switch` + `bff_use_for_*`; release-build resolver reads RC instead of debug store; cohort 0/10/50/100 documented |
| Phase E | `feat/remove-nsw-key` | B + D at 100% for ≥2 weeks | Remove `NSW_API_KEY` BuildKonfig field, `local.properties` entry, CI secret; revoke key in NSW Open Data console |

---

## TL;DR

Three phases, only Phase A is gated by the BFF docs handover; B is your
debug-screen ask; C waits on upstream.

| Phase   | What                                                                                                                                                                         | Blocking                                                                                                                | Effort                                                                                            |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| **A**   | Finish the 4-endpoint pass-through wiring (trip, departures, park-ride, GTFS-RT) — the BFF team's actual handover.                                                           | Local BFF (already running).                                                                                            | **✅ Code complete** on `feat/bff-local-debug-override`; pending only user smoke-test + commit.    |
| **B**   | New `feature/debug-settings` module. Debug-only screen with per-endpoint 3-target selector (NSW / BFF local / BFF prod) + kill-switch. Replaces the local.properties opt-in. | Phase A done.                                                                                                           | ~1–2 days.                                                                                        |
| **C**   | Screen-shaped protobuf (Wire codegen, `JourneyList` proto, BFF-shaped mapper).                                                                                               | BFF deploys + screen-shaped endpoints actually built. `krail-api-proto v0.1.0` published; submodule + `:io:bff-api` in. | **Foundation landed** (submodule + Wire module). Mapper + flag flip pending; ~1 weekend post-BFF. |

---

## Reality check on the wire format

Your earlier framing — "BFF only responds in protobuf, gives screen-shaped data
for entire screens rather than just endpoint pass-through" — describes the
**future vision** in `KRAIL-BFF/docs/reference/API_SCHEMA_DESIGN.md` (a "review
doc, nothing here is implemented yet"), not the current contract.

What the BFF actually serves today, per `KRAIL_API_REFERENCE.md`:

| Endpoint                                   | Wire format     | Shape                                   | Existing app parser works?        |
|--------------------------------------------|-----------------|-----------------------------------------|-----------------------------------|
| `/v1/tp/trip`                              | JSON            | NSW pass-through                        | ✅ `TripResponse`                  |
| `/v1/stops/{id}/departures`                | JSON            | NSW pass-through                        | ✅ `DepartureMonitorResponse`      |
| `/v1/parking/facilities`                   | JSON            | NSW pass-through (`Map<String,String>`) | ✅ inline deserialize              |
| `/v1/parking/facilities/{id}/availability` | JSON            | NSW pass-through                        | ✅ `CarParkFacilityDetailResponse` |
| `/v[12]/gtfs/realtime/{feed}`              | binary protobuf | NSW pass-through                        | ✅ `FeedMessage` (Wire)            |
| `/v2/gtfs/vehiclepos/{feed}`               | binary protobuf | NSW pass-through                        | ✅ `FeedMessage` (Wire)            |
| `/api/v1/trip/plan-proto`                  | binary protobuf | `JourneyList` (BFF-shaped)              | ❌ no parser yet — Phase C         |
| `/v1/screens/trip-results` etc.            | (designed only) | screen-shaped proto                     | ❌ unbuilt on BFF                  |

The screen-shaped proto endpoints are documented in `API_SCHEMA_DESIGN.md` but
**not implemented**. The only existing BFF-shaped proto is `JourneyList` at
`/api/v1/trip/plan-proto` — also not in scope for the handover.

So Phase A is "swap base URL, keep parsers." Phase C is "introduce a parallel
proto path." The two are decoupled — don't conflate them.

---

## Status of the current branch

`feat/bff-local-debug-override` (off `main`):

- ✅ BuildKonfig field `KRAIL_BFF_BASE_URL` (sourced from `local.properties`).
- ✅ `BaseUrl.kt` exposes `KRAIL_BFF_BASE_URL` + `IS_BFF_LOCAL_OVERRIDE_SET`.
- ✅ `RealTripPlanningService.trip()` routes via BFF when override set.
- ✅ `RealDeparturesService.departures()` routes via BFF when override set.
- ✅ `RealParkRideService.fetchCarParkFacilities()` (both overloads) route via BFF when override set.
- ✅ `RealGtfsRealtimeService.buildUrl()` routes via BFF when override set, for all three feed-type cases.
- ✅ Cleartext config under `androidApp/src/debug/` (Android) + Info.plist (iOS).
- ✅ Unit tests for the Park & Ride URL builders (`ParkRideUrlBuilderTest`) and the GTFS-RT URL builder (`BuildGtfsUrlTest`). Pure-function coverage; no mock HttpClient infrastructure introduced.

All four target services are wired. The handover §16 17-box checklist
(see master plan) is fully ticked from the code side; remaining boxes
are user-pending verifications (build pass, simulator build, smoke test,
git hygiene) and are listed under "What's left for the user" below.

---

## Phase A — Finish the handover (~1 hour)

Strictly the BFF team's playbook. No new modules. No DI changes. One-line URL
swap per service, gated on `IS_BFF_LOCAL_OVERRIDE_SET`.

### TODO

- [x] **Park & Ride list** — `RealParkRideService.fetchCarParkFacilities()`
  no-arg overload. NSW `/v1/carpark` to BFF `/v1/parking/facilities`.
  Same `Map<String, String>` response.
- [x] **Park & Ride detail** — same file, `facilityId` overload.
  NSW `/v1/carpark?facility={id}` to BFF `/v1/parking/facilities/{id}/availability`.
  Same `CarParkFacilityDetailResponse` parser. Note: BFF preserves the NSW
  quirk wrapper (`success: false` envelope, numbers-as-strings) per
  `KRAIL_API_REFERENCE.md §6`.
- [x] **GTFS-RT** — `RealGtfsRealtimeService.buildUrl()`. Single
  `gtfsBaseUrl` val gated on the override; reused for all 3 variants
  (`/v1/gtfs/realtime/{feed}`, `/v2/gtfs/realtime/{feed}`,
  `/v2/gtfs/vehiclepos/{feed}`). Identical paths between NSW and BFF;
  no model changes; HEAD + `If-Modified-Since` flow preserved end-to-end.
- [x] Pure-function unit tests pinning the URL shape per branch
  (`ParkRideUrlBuilderTest`, `BuildGtfsUrlTest`).
- [ ] User runs `./scripts/fullQualityChecks.sh` (KRAIL repo): Android compile
  + iOS Simulator compile + detekt. Paste tail of output.
- [ ] Smoke-test (BFF log shows the matching `GET` line for each):
  Trip search yields `GET /v1/tp/trip`;
  Departures (Saved Trips) yields `GET /v1/stops/{id}/departures`;
  Park & Ride list yields `GET /v1/parking/facilities` (need `NSW_PARK_RIDE_BETA` RC flag);
  Park & Ride detail yields `GET /v1/parking/facilities/{id}/availability`;
  Live tracking (when reachable) yields `GET /v[12]/gtfs/realtime/...` and `/v2/gtfs/vehiclepos/...`.
- [ ] Verify `RealTripPlanningService.stopFinder()` still hits NSW (untouched).
- [ ] Verify `git status` does not list `local.properties`.

That closes the handover. Ship as PR #1 of this stack.

---

## Phase B — Debug settings module (~1–2 days)

Your ask: a debug-only Network screen in the Settings UI to switch endpoints
at runtime, replacing the local.properties opt-in. Built per taj design system.

### Architecture

**New module split** (matches existing `feature/trip-planner/{state,ui,network}`):

```
feature/
  debug-settings/
    state/    EndpointScope, NetworkTarget, DebugSettingsState/Event
    store/    DebugNetworkConfigStore (DataStore-backed, KMP)
    ui/       DebugSettingsScreen, NetworkSelectorScreen, ViewModel, DebugSettingsEntry
```

Three submodules, not one. Reasons:

- `store` may want to be read by services that don't depend on UI.
- `ui` carries Compose deps that shouldn't leak into pure data layers.
- Splitting also lets release builds skip compiling `ui` if we ever want to.

Collapse to one if line counts come in <300 across all three; project precedent
says split.

### Debug-only gating (use existing `AppInfo.isDebug`)

KRAIL already exposes a debug flag through `:core:app-info`:

```kotlin
// core/app-info/src/commonMain/kotlin/.../AppInfo.kt:17
interface AppInfo {
    val isDebug: Boolean
    // ...
}
```

Already consumed in `core/network/HttpClient.kt:31`, `RealRemoteConfig.kt:41`,
`RealAnalytics.kt:19`. Reuse it — no new BuildKonfig field.

- Module compiles into all builds.
- Main `SettingsScreen` shows the "Debug" row only when
  `appInfoProvider.getAppInfo().isDebug == true`.
- `Real*Service` reads the store only when `isDebug == true`; in release the
  store is bypassed entirely and services fall through to BuildKonfig defaults
  (or to `RemoteConfig` once Firebase RC flags land).
- Single binary, no flavor source sets, no KMP iOS gymnastics.

### Per-endpoint target selector

```
Scope                Targets                                 Notes
TRIP_RESULTS         NSW_DIRECT │ BFF_LOCAL │ BFF_PROD
DEPARTURES           NSW_DIRECT │ BFF_LOCAL │ BFF_PROD
PARK_RIDE            NSW_DIRECT │ BFF_LOCAL │ BFF_PROD
TRACK (GTFS-RT)      NSW_DIRECT │ BFF_LOCAL │ BFF_PROD
STOP_FINDER          NSW_DIRECT (only)                       BFF has no /stop_finder; locked.

Plus a global KILL_SWITCH that forces every scope to NSW_DIRECT.
```

Why per-endpoint, not a single global switch:

- Mirrors the production Firebase Remote Config flag scheme exactly
  (`bff_use_for_<endpoint>`, `bff_kill_switch` per `BFF_ADOPTION_GUIDE.md`).
- Lets you isolate which endpoint regressed in compare-mode.
- Cohort rollout in production is per-endpoint anyway — same shape now means
  no rewrite later.

### Runtime override flow

```
Caller (ViewModel/Repository)
        │
        ▼
Real*Service ──► DebugNetworkConfigStore.target(scope)   (debug builds)
                              │
                              └──► NetworkBuildKonfig defaults
                                   (release builds, or no selection set)
```

`DebugNetworkConfigStore`:

```kotlin
interface DebugNetworkConfigStore {
    val targets: Flow<Map<EndpointScope, NetworkTarget>>
    suspend fun set(scope: EndpointScope, target: NetworkTarget)
    suspend fun reset()
    suspend fun killSwitch(enabled: Boolean)
}
```

The existing `IS_BFF_LOCAL_OVERRIDE_SET` constant becomes the bootstrap default
(first-launch initial value when `BFF_LOCAL` is selected). Once any user
selection is set, the store wins.

### Compose UI sketch (taj design system)

`DebugSettingsScreen` — top-level list, one row per scope:

```
┌─────────────────────────────────────────────────────┐
│  Debug · Network                              [back]│
├─────────────────────────────────────────────────────┤
│  Kill switch (force NSW direct)         [ off  on ] │
│                                                     │
│  Trip planner                              NSW ›    │
│  Departures                                BFF ›    │
│  Park & Ride                               NSW ›    │
│  Live tracking                             BFF ›    │
│  Stop search                       NSW (locked) ›   │
│                                                     │
│  Compare-mode logging                   [ off  on ] │
│  BFF base URL (local)        http://10.0.2.2:8080  │
│  BFF base URL (prod)              <not deployed>   │
└─────────────────────────────────────────────────────┘
```

Tapping a row opens `NetworkSelectorScreen` for that scope — radio rows for
the 3 targets, plus a "current value" footer. Reuse taj `TitleBar`, `Divider`,
`Klickable`, `Text`. No new components needed.

### Hooks into existing services

For each `Real*Service`, three changes:

1. Constructor takes `AppInfoProvider` + `DebugNetworkConfigStore`.
2. Resolution helper:

   ```kotlin
   private suspend fun baseUrl(): String {
       if (!appInfoProvider.getAppInfo().isDebug) return NSW_TRANSPORT_BASE_URL  // release path
       return when (configStore.target(SCOPE).first()) {
           NetworkTarget.NSW_DIRECT -> NSW_TRANSPORT_BASE_URL
           NetworkTarget.BFF_LOCAL  -> KRAIL_BFF_BASE_URL.ifBlank { NSW_TRANSPORT_BASE_URL }
           NetworkTarget.BFF_PROD   -> KRAIL_BFF_PROD_BASE_URL.ifBlank { NSW_TRANSPORT_BASE_URL }
       }
   }
   ```

3. `KRAIL_BFF_PROD_BASE_URL` is a new BuildKonfig field —
   `https://bff.krail.app` once deployed, empty string until then. Empty
   string falls back silently to NSW.

When release routing eventually lands (Firebase RC), the release branch above
becomes `return remoteConfig.bffTargetForScope(SCOPE)` — same resolver shape,
different source.

### Compare-mode (optional, debug-only)

Per `BFF_ADOPTION_GUIDE.md §Step 4`: when both NSW and BFF return successfully
in debug, log the diff. Gated behind a separate "Compare-mode logging" toggle
in the Network screen (off by default — it doubles the request count).

### TODO

- [ ] Add `KRAIL_BFF_PROD_BASE_URL: STRING` to `NetworkBuildKonfig` (empty until BFF deploys).
- [ ] Create `feature/debug-settings/state` (`EndpointScope`, `NetworkTarget`, state/event types).
- [ ] Create `feature/debug-settings/store` with `DebugNetworkConfigStore` impl + KMP DataStore.
- [ ] Create `feature/debug-settings/ui` with screens + ViewModel + nav entry.
- [ ] Wire entry into nav graph next to `SettingsEntry`.
- [ ] Add row in main `SettingsScreen` gated on `appInfoProvider.getAppInfo().isDebug`.
- [ ] Update `settings.gradle.kts` with the 3 new modules.
- [ ] Refactor `Real*Service` constructors to accept `AppInfoProvider` + the store; add per-call resolution.
- [ ] Remove the simple `IS_BFF_LOCAL_OVERRIDE_SET` checks in services (keep the constant —
  bootstrap source for `BFF_LOCAL` first-launch default).
- [ ] Add compare-mode logging path (off by default).
- [ ] Smoke-test each scope toggle on AVD.

Ship Phase B as a separate PR (or stack) after Phase A merges. Total diff
likely 400–600 lines — may need a 2-PR split per the 500-line rule.

---

## Wire format vs the kill-switch — what works when

The kill-switch only works cleanly when both branches produce the **same
domain model** (per `BFF_ADOPTION_GUIDE.md §Step 2`):

> Both branches must produce the same domain model. The BFF path's
> `.toDomain()` is mostly passthrough; the NSW path uses the existing mapper.
> The UI doesn't care which one ran.

What this means per phase:

| Phase | BFF response shape | Parallel mappers needed? | Kill-switch trivial? |
|---|---|---|---|
| **A** (current) | identical to NSW (JSON pass-through, or GTFS-RT proto bytes) | No — same shape, same parser | Yes — Phase B's selector just swaps base URL |
| **C** (future) | screen-shaped proto (`TripResultsResponse`, etc.) | Yes — `BffResponse-to-Domain` adapter alongside the existing `NSW-to-Domain` mapper | Yes — both `.toDomain()` adapters converge to the same `Domain` |

So your concern about "kill switch needs different mappers" is correct — but
only for Phase C. Phase A is shape-identical, so today's existing parsers
serve both branches.

### What BFF computes server-side (verified in BFF docs)

Per `API_SCHEMA_DESIGN.md §3` "What BFF pre-computes" — the design doc is
explicit about which app-side mappers go away in Phase C:

| Computation | Today (KRAIL app) | Phase C (BFF) |
|---|---|---|
| Line color resolution | `NswTransportLine` lookup + mode fallback | BFF ships `color_hex` per `TransitLine` |
| Mode-to-icon name | `TransportMode.iconName` mapping | BFF ships `icon_name` |
| Platform extraction (Platform/Stand/Wharf) | mode-specific regex in `DepartureMonitorMapper` | BFF runs regex once; clean string in `StopRef.platform` |
| Display text (e.g. "Burwood to Liverpool") | `resolveServiceDisplayText` | BFF ships `display_text` |
| HH:MM AEST formatting | `utcToLocalDateTimeAEST().toHHMM()` | BFF formats; client still gets UTC |
| Relative time ("in 5 mins") | `toDepartureRelativeString` | BFF snapshot; client re-renders against UTC + clock |
| Deviation label ("3 mins late") | mapper-side | BFF pre-computes |
| Service alert dedup | mapper iterates legs | BFF dedupes once |
| Park & ride availability math | `totalSpots − sum(occupancy)` | BFF computes |
| GTFS-RT vehicle and leg matching | `GtfsRealtimeMatcher` 4-tier match | BFF matches; ships only the matched vehicle per leg |

Stays client-side (correctly): "approaching" countdown, current-stop progress,
map camera bounds, "past stop" greying, theme-driven color application.

Net per `API_SCHEMA_DESIGN.md §5`: **12 app mappers reduce to 4** post-migration.

### UTC timestamps — already true today, stays true in Phase C

NSW already returns ISO-8601 UTC with the `Z` suffix
(`"departureTimeEstimated": "2026-05-09T14:32:15Z"` per
`KRAIL_API_REFERENCE.md §4`). Existing parsers handle this correctly.
Phase C protos use `scheduled_utc` + `estimated_utc` plus a pre-formatted
`display_time` — client picks whichever it needs (per `API_SCHEMA_DESIGN.md
§1 StopTime`).

### Caveat — design vs reality

`API_SCHEMA_DESIGN.md` is explicitly a "review doc — nothing here is
implemented yet." The screen-shaped endpoints don't exist on BFF today
(per `KRAIL-BFF/STATUS.md`). So:

- Phase A keeps every existing app mapper. No deletion.
- Phase C deletes mappers as endpoints ship. Per-endpoint, gated on cohort
  rollout.

---

## Phase C — Screen-shaped protobuf (deferred)

Most of the consumer work is still deferred. The infrastructure to land it is now in place.

### Phase C foundation landed (2026-05-09)

On `feat/bff-local-debug-override`, alongside the Phase A work:

- ✅ `krail-api-proto` git submodule added at repo root, pinned to `v0.1.0`
  (<https://github.com/ksharma-xyz/KRAIL-API-PROTO>).
- ✅ `:io:bff-api` Kotlin Multiplatform module added (Android + iosArm64 +
  iosSimulatorArm64), Wire 6.2.0 plugin pointed at `$rootDir/krail-api-proto/proto`.
  Pattern mirrors `:io:gtfs` exactly. Module compiles in isolation; nothing
  depends on it yet, so failure here cannot regress Phase A.
- ✅ `submodules: true` added to `actions/checkout` in every workflow that
  compiles (`build-android.yml`, `build-ios.yml`, `code-quality.yml`,
  `distribute-testflight.yml`).
- ✅ `IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED` flag added to `BaseUrl.kt`,
  hard-coded `false`. Eventual debug-settings UI flips at runtime.
- ✅ `RealTripPlanningService.trip()` proto branch scaffold added (gated on the
  flag, dead-code-eliminated today). Stub `JourneyListMapper` throws
  `NotImplementedError` with a pointer to `BFF_PHASE_A_MORNING.md` §5.

What's still pending for the consumer wiring:

- Add `implementation(projects.io.bffApi)` to
  `feature/trip-planner/network/build.gradle.kts`.
- Replace the scaffold block in `RealTripPlanningService.trip()` with a real
  `JourneyList` fetch + decode.
- Implement `journeyListBytesToTripResponse(...)` in
  `feature/trip-planner/network/.../mapper/JourneyListMapper.kt`.
- Flip `IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED` to `true`
  (or move it to the debug-settings store once Phase B lands).

### Why deferred

Per `KRAIL-BFF/STATUS.md` (refresh 2026-05-09):

- BFF not deployed (13 PRs stacked on `main`).
- `krail-api-proto` repo doesn't exist yet — protos live inside the BFF repo.
- Screen-shaped messages from `API_SCHEMA_DESIGN.md §2`
  (`TripResultsResponse`, `DepartureBoardResponse`, `JourneyResponse`, etc.)
  are designed but unimplemented.
- Only proto endpoint that exists is `/api/v1/trip/plan-proto` returning
  `JourneyList` — not screen-shaped, just compact-wire JSON-shape.

### Wire codegen — already established in this repo

KRAIL already does Wire-based KMP proto codegen. Don't reinvent:

- `gradle/libs.versions.toml:193` — `wire = { id = "com.squareup.wire", version = "6.2.0" }`
- `:io:gtfs` module hosts `.proto` files at
  `io/gtfs/src/commonMain/proto/`: `gtfs-realtime.proto`, `NswStop.proto`,
  `NswBusRoute.proto`. Wire generates Kotlin classes for `commonMain`
  (Android + iOS).
- `feature/track/network` consumes `:io:gtfs` and uses
  `FeedMessage.ADAPTER.decode(bytes)` — the canonical Wire decode path.

So Phase C proto setup is a small chore, not a new capability.

### Sharing proto contracts with KRAIL-BFF

**Share `.proto` files, not generated Kotlin.** Per `API_SCHEMA_DESIGN.md §4`:

> The proto files are the contract between KRAIL and KRAIL-BFF. They must live
> in exactly one place; both repos consume the same source.

Recommended layout (per BFF doc, ranked option #1): a separate public repo
`ksharma-xyz/krail-api-proto`, consumed as a **git submodule** in both
KRAIL-BFF and KRAIL.

```
krail-api-proto/                  separate public repo
├── version.txt                   SemVer: 0.1.0, etc.
└── proto/
    ├── core/                     LatLng, TransitLine, StopRef, StopTime, …
    └── api/                      trip_results.proto, departure_board.proto, …
```

Both consumers:

```kotlin
// KRAIL (and KRAIL-BFF) — same Wire config, same source
wire {
    kotlin {
        sourcePath { srcDir("$rootDir/krail-api-proto/proto") }
        targets { commonMain }    // KRAIL only — KRAIL-BFF targets JVM
    }
}
```

Why source files, not generated Kotlin:

- One canonical contract; each side regenerates Kotlin for its own target
  (BFF: JVM; KRAIL: KMP common + iOS + Android).
- SemVer at the schema level. Minor bump for additive fields; major for
  rename/remove (BFF must serve the prior shape for one app version's
  deprecation window per `API_SCHEMA_DESIGN.md §4 Versioning`).
- Each consumer pins a **tag** (`v0.1.0`), never a branch. Decouples Wire /
  Protobuf-Kotlin version bumps from the contract.
- Public repo is auditable — anyone (including security reviewers) can read
  the wire shape. The protos are recoverable from the APK anyway, so making
  the contract public costs nothing.

Why not the alternatives:

- **Maven artifact** (GitHub Packages / JitPack): cleaner for cross-team work,
  but overkill for a solo project. Adds publish/release infra.
- **Embed protos in BFF, KRAIL points at a relative path**: simplest now,
  brittle later — coupling BFF release cadence to KRAIL's submodule moves.
  Per the BFF doc: "biggest pain later when the contract has independent
  users — don't."

Phase C order of operations (mirrors `KRAIL-BFF/STATUS.md §2`):

1. BFF team extracts `krail-api-proto` from `KRAIL-BFF/server/src/main/proto/`,
   tags `v0.1.0`.
2. BFF replaces local protos with a submodule pointing at that tag.
3. **KRAIL adds the same submodule** at repo root.
4. KRAIL creates `:io:bff-api` (recommendation) — a new module that just
   applies Wire and points at `$rootDir/krail-api-proto/proto`. Generated
   classes available in `commonMain`. Mirrors the `:io:gtfs` config.
5. Validate iOS codegen as the first commit; fallback is
   `kotlinx-serialization-protobuf` with hand-mapped messages (last resort).

Until step 1 lands on the BFF side, Phase C is blocked. Phase A and B don't
depend on it.

### `:io:bff-api` vs extending `:io:gtfs`

Recommend **separate `:io:bff-api` module** rather than dropping BFF protos
into `:io:gtfs`:

| Concern | `:io:gtfs` | `:io:bff-api` (new) |
|---|---|---|
| Spec ownership | NSW / GTFS-RT spec | KRAIL contract (versioned by us) |
| Update cadence | Tied to GTFS spec / NSW feed format | Tied to BFF releases |
| Submodule scope | None today (vendored protos) | Yes (`krail-api-proto`) |
| Consumers | `feature/track/network` | Trip planner, departures, park-ride, journey |

Mixing them blurs ownership and makes the submodule scope unclear. Two small
modules is cleaner than one with two unrelated proto sets.

### What Phase C looks like (when prerequisites land)

- Add proto module per above.
- Add `Accept: application/x-protobuf` header on proto-targeted requests in
  the shared Ktor client.
- Per endpoint:
    - Add a `Bff<X>Client` that hits the proto endpoint and decodes the
      generated message via `<Message>.ADAPTER.decode(bytes)`.
    - Add a `<Proto>-to-DomainModel` mapper. Mostly passthrough — BFF has done
      the screen-shaping.
    - Keep the existing NSW-to-DomainModel mapper for the kill-switch path
      until the cohort rollout for that endpoint hits 100% + 2-week grace.
    - Phase B's selector picks one of `NSW_DIRECT` / `BFF_LOCAL` / `BFF_PROD`
      — the BFF branches use the proto path automatically when target is BFF.

Migration order (from `BFF_ADOPTION_GUIDE.md §Migration order`):

1. Stops dataset (manifest pattern) — once BFF deploys it.
2. Departures (screen-shaped) — once `/v1/screens/departures` ships.
3. Park & Ride (screen-shaped).
4. Trip results (`/v1/screens/trip-results`).
5. Journey + journey/live (most complex, last).

Each follows `BFF_ADOPTION_GUIDE.md §Step 1–6`: compare-mode, then cohort
rollout, then cleanup.

---

## Cross-cutting concerns

### Logging (debug-only, INFO level)

The shared Ktor `HttpClient` (in `core/network/.../HttpClient.kt`) installs
the `io.ktor.client.plugins.logging.Logging` plugin in both Android and iOS
actuals. Behavior:

- Gated on `appInfoProvider.getAppInfo().isDebug`. Release builds set
  `LogLevel.NONE`, so the plugin imposes no overhead and emits nothing.
- Debug builds use `LogLevel.INFO` (method, URL, status, timings — never
  bodies). Per `KRAIL_INTEGRATION_MASTER_PLAN.md` §13, request/response bodies
  contain stop ids and times that, in aggregate, can reveal user patterns; we
  deliberately do not log them.
- Output routes through KRAIL's existing `Log.d` (`:core:log`) with the
  prefix `KrailNetwork:` so a developer can filter the firehose:
    - Android logcat: `adb logcat -s KrailNetwork:* *:E` (or filter for
      `KrailNetwork` in Android Studio's Logcat search field).
    - iOS Simulator: filter for `KrailNetwork` in Xcode's console.

A small commonMain helper, `logNetworkCall(target, method, path)` in
`xyz.ksharma.krail.core.network.NetworkLogging`, is called by each service
right before its `httpClient.get(...)`. Output looks like:

```
KrailNetwork: BFF GET /v1/tp/trip [override=on]
KrailNetwork: NSW GET /v1/tp/stop_finder [override=off]
```

The pre-call line records which branch (BFF override on, or NSW direct) the
service took — Ktor's `Logging` plugin then logs the response status and
timing once the call returns.

### `X-Krail-Version` header

Per `KRAIL_API_REFERENCE.md §1`: recommended, not required for local dev
(floor is `0.0.0`). Required for production once `MIN_APP_VERSION` is bumped.

Add once in the shared Ktor client config (`core/network/`), not per-call:

```kotlin
defaultRequest {
    header("X-Krail-Version", NetworkBuildKonfig.APP_VERSION)
}
```

This works whether the request goes to NSW or BFF — NSW ignores unknown
headers. Add a new `APP_VERSION: STRING` field to BuildKonfig sourced from
the gradle `version` block.

Phase B is a good moment to do this, since you're already touching the
shared client wiring.

### `CF-Origin-Token` header (production only)

When BFF goes behind Cloudflare, prod requests need a shared secret in this
header. Per `KRAIL_API_REFERENCE.md §1` it's optional for local dev. Plan:

- Add a `KRAIL_BFF_ORIGIN_TOKEN: STRING` BuildKonfig field, default empty.
- Inject as `CF-Origin-Token` header only when target is `BFF_PROD` AND the
  field is non-empty.

Out of scope for Phase A/B; do when prod URL is real.

### Error handling

Per the new handover §7 — **don't change existing error parsing**. The BFF
preserves NSW's success shapes verbatim; on errors it adds an envelope
(`{ "error": { "code": "...", "message": "...", "details": null }, "correlationId": "..." }`),
but the existing services treat any non-2xx as a generic failure today and
that's fine for Phase A/B.

If we later want to surface BFF error codes in UI (e.g. show a `rate_limited`
banner), that's a separate, small change to error mappers — not blocking.

### Module choice for cleartext config

Already settled in the new handover §5 — `:androidApp` only. Current branch
is correct.

---

## Risk register

| Risk                                                                             | Mitigation                                                                                                                                                                                        |
|----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Phase B introduces new modules contrary to handover §1 "no new modules".         | Handover §1 means **for the migration itself**, not for separate UX features. Phase B is clearly debug-only UX scaffolding, not a refactor of the migration. Document this distinction in the PR. |
| KMP DataStore on iOS not configured.                                             | Inspect existing data persistence (`core/sandook/`?) for KMP-shared options before adding a new dependency.                                                                                       |
| Wire codegen on iOS (Phase C).                                                   | Phase C only; validate before committing to that path. Fallback exists (`kotlinx-serialization-protobuf`).                                                                                        |
| `KRAIL_BFF_PROD_BASE_URL` empty until BFF deploys.                               | Empty-string fallback to NSW. UI shows `<not deployed>` placeholder. No runtime crashes.                                                                                                          |
| Park & Ride beta gate hides the screen on test device.                           | Smoke-test step assumes `NSW_PARK_RIDE_BETA` RC flag is on. Document in Phase A checklist.                                                                                                        |
| Live-tracking screens "built but hidden" in app.                                 | Smoke-test step needs the user to point me at the entry — don't guess.                                                                                                                            |
| Compare-mode doubles request count.                                              | Off by default; clearly labelled in Network screen; never on in release.                                                                                                                          |
| Existing `IS_BFF_LOCAL_OVERRIDE_SET` consumers in services regress when removed. | Keep the constant; phase its consumers out file-by-file.                                                                                                                                          |

---

## Recommended migration approach (the question you asked)

**Sequence:**

1. **Today/tomorrow**: Phase A. Two services left to wire. ~1 hour. Ship as the
   completion of `feat/bff-local-debug-override`.
2. **This week**: Phase B. Debug-settings module, runtime selector, taj UI.
   1–2 days. Ship as a stacked PR (likely 2 branches per the 500-line rule).
3. **Wait on Phase C** until BFF deploys + `krail-api-proto` exists +
   screen-shaped endpoints ship.

**Per-endpoint cohort rollout** (when Phase C is ready, per
`BFF_ADOPTION_GUIDE.md §Step 5`):

```
0%:   internal devices (you, Phase B's debug screen)
10%:  real users via Firebase RC; watch error rate + "feature loaded" metric for 48–72h
50%:  promote if 10% clean
100%: promote if 50% clean
+2 weeks grace, then delete NSW path for that endpoint
```

In dev: the per-endpoint selector in Phase B's screen is the local equivalent
of Firebase RC. Same shape, same flag names — production just swaps the source.

**Order of cohort rollouts** (driven by risk + visibility):

1. Stops dataset (when shipped) — no live screen, easiest.
2. Park & Ride — already gated by beta flag.
3. Departures — visible but well-bounded.
4. Trip results — visible, daily-driver, biggest payload win.
5. GTFS-RT — most coupled to live state; last.

---

## Resolved (decisions made during planning)

- **Debug gating**: use existing `AppInfo.isDebug` from `:core:app-info`. No
  new BuildKonfig flag.
- **Proto codegen capability**: KRAIL already runs Wire 6.2.0 against
  `:io:gtfs/src/commonMain/proto/*.proto`. Pattern is established; Phase C
  is a small chore, not new infrastructure.
- **Wire format mismatch + kill-switch**: Phase A is shape-identical so the
  kill-switch is trivial; Phase C needs parallel `.toDomain()` adapters per
  `BFF_ADOPTION_GUIDE.md §Step 2`. Both branches converge to the same domain
  model.
- **UTC handling**: NSW already returns UTC (`...Z` suffix) per
  `KRAIL_API_REFERENCE.md §4`; existing parsers handle it. Phase C protos
  ship `*_utc` + pre-formatted `display_time`; client picks.

## Open questions for you

Decide before I start coding either phase:

1. **Phase B module split** — 3 submodules (`state`/`store`/`ui`) or 1
   (`feature/debug-settings`)? Project precedent says split; I lean split.
2. **Targets** — 3-state (NSW / BFF local / BFF prod) enough, or add
   `BFF_STAGING` for a separate staging URL once you have one?
3. **Kill-switch behaviour** — forces NSW only for the next call, or also
   visibly clears all per-endpoint selections? Latter is destructive but
   unambiguous.
4. **Compare-mode** — bake into Phase B as a togglable feature, or skip and
   wait for Phase C when proto vs JSON diffs become real?
5. **`X-Krail-Version` header** — add in Phase A (small change, sensible
   place) or wait for Phase B (when you're already in the shared client)?
   I lean Phase A — one extra line, removes a future "did you remember the
   header" failure mode.
6. **Phase A scope clarification** — current branch is at "trip + departures
   wired." Want me to push the remaining park-ride + GTFS-RT swaps onto the
   same branch, or split into a fresh stacked branch? Leaning same branch
   (~30 lines added, still under 500).
7. **Where the prod URL eventually lands** — `https://bff.krail.app`?
   Some other host? Just need a placeholder for `KRAIL_BFF_PROD_BASE_URL`.
8. **Phase C proto module placement** — extend existing `:io:gtfs` with the
   BFF protos, or carve a new `:io:bff-api` module? I lean separate module —
   GTFS-RT is NSW spec, BFF protos are KRAIL contract; mixing them blurs ownership.

---

## What stays out of scope for this plan

- Anything that changes release-build behavior. This is debug + future-prod
  scaffolding, not a release rollout.
- Deletion of the NSW path. That's after Phase C cohort rollout completes
  per endpoint.
- Stop finder migration to local stops dataset — separate, larger handover
  per the BFF docs.
- BFF-side work (`krail-api-proto` extraction, deployment, screen-shaped
  endpoints). Tracked in `KRAIL-BFF/STATUS.md`.
- Graphite stack split. Will decide at implementation time per the 500-line rule.

---

## What's left for the user (morning)

The agent has finished writing code; nothing has been built, tested, or
committed. To close out Phase A:

1. **Run the full quality checks** from the KRAIL repo root and paste the
   tail of the output:

   ```
   ./scripts/fullQualityChecks.sh
   ```

   This runs Android compile + iOS Simulator compile + detekt. Detekt
   auto-corrects import order and trailing commas; review the in-place
   fixes before re-staging.

2. **Open `iosApp` in Xcode** and build for the iOS Simulator. The agent
   does not drive `xcodebuild` headless per repo convention.

3. **Smoke-test the four screens** against the running BFF
   (`./scripts/dev.sh up` from the BFF repo). Confirm the BFF log shows
   the matching `GET` lines per master plan §16:
   - Trip search: `GET /v1/tp/trip`
   - Departures: `GET /v1/stops/{stopId}/departures`
   - Park & Ride list: `GET /v1/parking/facilities` (needs the
     `NSW_PARK_RIDE_BETA` Firebase RC flag enabled on the debug device)
   - Park & Ride detail: `GET /v1/parking/facilities/{id}/availability`
   - Live tracking: `GET /v[12]/gtfs/realtime/{feed}` and
     `GET /v2/gtfs/vehiclepos/{feed}`
   - Stop search keeps hitting NSW direct (proves the unmigrated path
     still works).

4. **Commit.** Suggested message:

   ```
   feat(network): debug-only KRAIL-BFF override for all NSW-covered endpoints

   Behind the new local.properties key `krail.bffBaseUrl` (empty by default,
   debug only). When set, the four NSW-direct services that have BFF
   equivalents route through the BFF instead:

   - RealTripPlanningService.trip()         to BFF /v1/tp/trip (same shape)
   - RealDeparturesService.departures()     to BFF /v1/stops/{id}/departures
   - RealParkRideService.fetchCarParkFacilities()
                                            to BFF /v1/parking/facilities[/{id}/availability]
   - RealGtfsRealtimeService (all 3 feeds)  to BFF /v[1|2]/gtfs/{realtime|vehiclepos}/{feed}

   stopFinder stays on NSW direct (BFF has no stop_finder; long-term plan
   is local search against a stops dataset, separate work).

   Release builds + non-overridden debug builds are unchanged. Cleartext
   exception scoped to androidApp/src/debug/ so release stays HTTPS-only.

   Pure-function unit tests pin the URL shape per branch for the two
   non-trivial services (Park & Ride two-overload split, GTFS-RT three
   feed-type cases).
   ```

5. **Verify `git status` does not list `local.properties`.** That key is
   gitignored and the override line is local-developer config — committing
   it would force the BFF override on every developer's machine.
