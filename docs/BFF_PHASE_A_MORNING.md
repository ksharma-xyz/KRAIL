# BFF Phase A — Morning Runbook

Single page to read on wake-up. Verifies the overnight work, gets a debug build
on the AVD, walks the four screens, confirms each one routes through the local
BFF.

---

## §1 · TL;DR

Phase A code is complete on `feat/bff-local-debug-override`; nothing committed.
First command of the day: `curl -s localhost:8080/health` to confirm the local
BFF is still alive (it is, unless the laptop slept hard).

---

## §2 · State of the branch

- **Branch**: `feat/bff-local-debug-override`
- **Base**: `main`
- **State**: working tree dirty, nothing staged, nothing committed.
- **`local.properties`**: holds `krail.bffBaseUrl=http://10.0.2.2:8080`.
  Gitignored. Do not stage.

Files changed or added overnight:

- `core/network/src/androidMain/.../HttpClient.kt` and `iosMain/.../HttpClient.kt`
  — Ktor `Logging` plugin downgraded from `BODY` to `INFO` (no body logging
  per `KRAIL_INTEGRATION_MASTER_PLAN.md` §13). Output now prefixed with
  `KrailNetwork:` for filtering.
- `core/network/src/commonMain/.../NetworkLogging.kt` — new helper
  `logNetworkCall(target, method, path)` that logs one line per call.
- `core/network/src/commonMain/.../BaseUrl.kt` — added
  `IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED: Boolean = false` for Phase C.
- `feature/{trip-planner,departures,park-ride,track}/network/.../Real*Service.kt`
  — each service calls `logNetworkCall(...)` immediately before its
  `httpClient.get(...)` so the BFF/NSW branch decision is visible in logcat.
- `feature/trip-planner/network/.../api/mapper/JourneyListMapper.kt` — new
  stub. Throws `NotImplementedError`. Wired into the trip service behind the
  proto flag (currently `false`).
- `feature/trip-planner/network/.../api/service/RealTripPlanningService.kt`
  — Phase C scaffold added (gated on flag, dead-code-eliminated today).
- `krail-api-proto/` — new git submodule pinned to `v0.1.0` from
  <https://github.com/ksharma-xyz/KRAIL-API-PROTO>.
- `.gitmodules` — new file.
- `io/bff-api/` — new KMP module with Wire codegen reading the submodule.
  Mirrors `:io:gtfs`. Compiles in isolation; no other module depends on it
  yet, so a Wire failure here cannot regress Phase A.
- `settings.gradle.kts` — `:io:bff-api` registered.
- `.github/workflows/{build-android,build-ios,code-quality,distribute-testflight}.yml`
  — `submodules: true` added to every `actions/checkout` that compiles.
- `docs/bff-integration-plan.md` — Phase A marked complete; Phase C
  foundation noted; Logging cross-cutting section added.

---

## §3 · Steps to verify the integration works

Run in order. Most are copy-paste-runnable.

1. **Confirm BFF is up.** Should print `{}` (the empty health body).

   ```sh
   curl -s localhost:8080/health
   ```

   If it errors: `cd /Users/ksharma/code/apps/KRAIL-BFF && ./scripts/dev.sh up`.

2. **Run KRAIL quality checks.** Compiles Android + iOS Simulator + runs
   detekt. Detekt auto-corrects import order and trailing commas; review
   diffs before staging.

   ```sh
   cd /Users/ksharma/code/apps/KRAIL
   ./scripts/fullQualityChecks.sh
   ```

3. **Open an Android emulator.** Android Studio → Tools → Device Manager →
   start any AVD with API 34 or newer. Or from CLI:

   ```sh
   ~/Library/Android/sdk/emulator/emulator -list-avds
   ~/Library/Android/sdk/emulator/emulator -avd <name>
   ```

4. **Build and install KRAIL on the running emulator.**

   ```sh
   cd /Users/ksharma/code/apps/KRAIL
   ./gradlew :androidApp:installDebug
   ```

   APK will land at `androidApp/build/outputs/apk/debug/`. Launch by tapping
   the icon on the AVD or:

   ```sh
   adb shell am start -n xyz.ksharma.krail.debug/xyz.ksharma.krail.MainActivity
   ```

5. **Tail KRAIL logcat for network lines.** New terminal:

   ```sh
   adb logcat | grep -i KrailNetwork
   ```

   Expected on every BFF/NSW call:

   ```
   D KrailNetwork: → BFF GET /v1/tp/trip [override=on]
   I KrailNetwork: REQUEST: http://10.0.2.2:8080/v1/tp/trip
   I KrailNetwork: METHOD: GET
   I KrailNetwork: RESPONSE: 200 OK
   ```

6. **Tail BFF log in a third terminal.**

   ```sh
   tail -f /Users/ksharma/code/apps/KRAIL-BFF/build/dev/bff.log
   ```

   Or:

   ```sh
   cd /Users/ksharma/code/apps/KRAIL-BFF && ./scripts/dev.sh logs
   ```

7. **Smoke-test each screen.** Confirm each KRAIL `→ BFF` log line is
   matched by a server-side `GET …` line.

   - **Trip search**: search "Town Hall" as origin, "Central" as destination.
     Tap the suggestion result. KRAIL log: `→ BFF GET /v1/tp/trip`.
     BFF log: `GET /v1/tp/trip … 200`.
   - **Departures (Saved Trips)**: tap any saved trip card on the home
     screen, or tap a stop card to open its departure board. KRAIL log:
     `→ BFF GET /v1/stops/{stopId}/departures`. BFF log:
     `GET /v1/stops/200060/departures … 200`.
   - **Park & Ride list**: requires `NSW_PARK_RIDE_BETA` Firebase RC flag
     to be ON for the device. Open the Park & Ride entry tile.
     KRAIL log: `→ BFF GET /v1/parking/facilities`.
     BFF log: `GET /v1/parking/facilities … 200`.
   - **Park & Ride detail**: tap any car park in the list.
     KRAIL log: `→ BFF GET /v1/parking/facilities/{id}/availability`.
     BFF log: `GET /v1/parking/facilities/{id}/availability … 200`.
   - **Live tracking**: trigger a journey screen with live data
     (the screens are built but may need a deeplink or hidden entry to reach
     — see §7). KRAIL log: `→ BFF GET /v[12]/gtfs/realtime/{feed}` and
     `→ BFF GET /v2/gtfs/vehiclepos/{feed}`. BFF log: matching `GET` lines.
   - **Stop search**: search any stop name in trip planner.
     KRAIL log: `→ NSW GET /v1/tp/stop_finder` (deliberately stays on NSW —
     BFF has no `stop_finder`; Phase D handles it). BFF log: nothing.

8. **Cross-check counts.** If everything routed correctly, the BFF log line
   count should roughly match the count of `→ BFF` lines from KRAIL logcat
   over the same window. Off-by-one is fine (timing / HEAD requests).

---

## §4 · How to switch BFF off (sanity-check NSW direct still works)

Proves Phase A's debug-only design: when `krail.bffBaseUrl` is unset, the
app silently falls back to NSW direct.

1. Open `local.properties`. Comment the `krail.bffBaseUrl` line:

   ```properties
   # krail.bffBaseUrl=http://10.0.2.2:8080
   ```

2. Rebuild + reinstall:

   ```sh
   ./gradlew :androidApp:installDebug
   ```

3. Re-run the §3 step 7 smoke tests. Same data should render. KRAIL logs
   should now read `→ NSW …` for every line. BFF log should be silent.

4. Restore `local.properties` (uncomment) before committing.

---

## §5 · Phase C readiness

What landed overnight:

- `krail-api-proto` git submodule, pinned at `v0.1.0`.
- `:io:bff-api` KMP module with Wire 6.2.0, sourcePath at
  `$rootDir/krail-api-proto/proto`. Mirrors `:io:gtfs`. Compiles in isolation.
- `IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED` flag in `BaseUrl.kt`, hard-coded
  `false`. Gates a scaffold branch in `RealTripPlanningService.trip()`.
- Stub `JourneyListMapper.kt` with the right function signature; throws
  `NotImplementedError` until wired up.

What is NOT wired:

- `feature/trip-planner/network` does not yet `implementation(projects.io.bffApi)`.
  Deliberately left out so Phase A is unaffected if the dependency causes
  any compile or codegen visibility issue.
- The proto branch in `RealTripPlanningService.trip()` is `error(...)`-only —
  it never runs because the flag is `false`.

Two-step plan to flip on next session:

1. Implement `journeyListBytesToTripResponse(...)` in `JourneyListMapper.kt`.
   Add `implementation(projects.io.bffApi)` to
   `feature/trip-planner/network/build.gradle.kts`. Replace the parameter
   type with `JourneyList` (import `app.krail.bff.proto.JourneyList`).
   Replace the `error(...)` scaffold with the real `httpClient.get(...)` →
   `ByteArray` → `JourneyList.ADAPTER.decode(bytes)` → mapper call chain.
2. Flip `IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED` to `true`, or move it into
   the debug-settings store once Phase B lands.

---

## §6 · How to commit when ready

After §3 has shown the four screens routing through BFF, and §4 has shown
NSW fallback is intact:

```sh
cd /Users/ksharma/code/apps/KRAIL

# Verify local.properties is NOT in the list of changes:
git status

# Stage explicitly to avoid accidentally staging local.properties or other
# gitignored files:
git add \
  .gitmodules krail-api-proto \
  io/bff-api \
  settings.gradle.kts \
  core/network \
  feature/trip-planner/network \
  feature/departures/network \
  feature/park-ride/network \
  feature/track/network \
  androidApp/src/debug \
  iosApp/iosApp/Info.plist \
  .github/workflows \
  docs/bff-integration-plan.md \
  docs/BFF_PHASE_A_MORNING.md

# Commit. Suggested message (multi-line, follows the master plan §6.11 example):
git commit -m "$(cat <<'EOF'
feat(network): debug-only KRAIL-BFF override + Phase C foundation

Phase A: behind the new local.properties key krail.bffBaseUrl (empty by
default, debug only). When set, the four NSW-direct services that have BFF
equivalents route through the BFF instead:

  - RealTripPlanningService.trip()       to BFF /v1/tp/trip (same shape)
  - RealDeparturesService.departures()   to BFF /v1/stops/{id}/departures
  - RealParkRideService.fetchCarParkFacilities()
                                         to BFF /v1/parking/facilities[/{id}/availability]
  - RealGtfsRealtimeService (3 feeds)    to BFF /v[1|2]/gtfs/{realtime|vehiclepos}/{feed}

stopFinder stays on NSW direct (BFF has no stop_finder; Phase D will move
it to local search against a stops dataset).

Release builds + non-overridden debug builds are unchanged. Cleartext
exception scoped to androidApp/src/debug/ so release stays HTTPS-only.

Phase C foundation: krail-api-proto added as a submodule pinned to v0.1.0;
new :io:bff-api KMP module wires Wire codegen at $rootDir/krail-api-proto/proto.
IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED flag added (hard-coded false) and a
scaffold branch in RealTripPlanningService.trip() is gated on it. Stub
JourneyListMapper compiles but throws NotImplementedError until the mapper
is wired up.

Logging: shared Ktor client now installs the Logging plugin at INFO level
(method + URL + status, never bodies — per master plan §13). Output is
prefixed "KrailNetwork:" for adb logcat / Xcode console filtering.

CI: actions/checkout in build-android, build-ios, code-quality, and
distribute-testflight workflows now use submodules: true so Wire codegen
finds krail-api-proto on the runner.

Pure-function unit tests pin the URL shape per branch for the two
non-trivial services (Park & Ride two-overload split, GTFS-RT three
feed-type cases).
EOF
)"
```

**Verify before pushing**: `git status` should be clean; `git log -1 --stat`
should not list `local.properties`.

---

## §7 · Known gotchas

- **`NSW_PARK_RIDE_BETA` Firebase RC flag** — Park & Ride list is hidden
  unless this RC flag is on for the test device. Toggle from Firebase
  console or via a debug build with the flag wired locally.
- **Live tracking screens "built but hidden"** — the trip-tracking entry
  may not be reachable from the main UI on every build. If you can't get
  to a `Track` screen, the GTFS-RT smoke test in §3 step 7 is OK to skip
  for tonight; the URL builders are unit-tested and the BFF log will
  confirm it once a path opens up.
- **iOS Simulator path differs from AVD** — iOS uses `localhost:8080`,
  AVD uses `10.0.2.2:8080`. Already handled in `iosApp/iosApp/Info.plist`
  and `androidApp/src/debug/res/xml/network_security_config.xml`.
- **BFF rate limit** — 5 RPS / 10 burst per IP. Manual smoke testing will
  not trip it. If it does, the response will be HTTP 429 with
  `error.code == "rate_limited"`.
- **Submodule on cold checkout** — if you clone fresh elsewhere, run
  `git submodule update --init` or pass `--recurse-submodules` on clone.
  CI is already wired (`submodules: true` on every compiling workflow).
- **Wire on KMP-iOS** — `:io:gtfs` is the precedent and ships green; the
  new `:io:bff-api` mirrors it exactly. If iOS codegen ever breaks, the
  fallback per master plan §13 is `kotlinx-serialization-protobuf` with
  hand-mapped messages.
- **`logNetworkCall` runs in commonMain** — same code path on Android and
  iOS. The only platform-specific bit is the underlying `Log.d` / kermit
  console emit.

---

## §8 · Deferred to next session

- Debug-settings UI module (Phase B prep — runtime per-endpoint target
  selector, replaces the `local.properties` opt-in).
- `JourneyListMapper` implementation (Phase C consumer wiring).
- Firebase Remote Config flag wiring + `bff_kill_switch` (Phase B production).
- `X-Krail-Version` and `CF-Origin-Token` default headers in the shared
  Ktor client (Phase B, when the BFF is deployed behind Cloudflare).
- Phase D — local stop search against the published stops dataset; lets
  `stopFinder` come off NSW direct.
- `proto-bump.yml` daily auto-bump workflow (mirrors KRAIL-BFF's same-named
  workflow). Open a PR when KRAIL-API-PROTO cuts a new tag.
