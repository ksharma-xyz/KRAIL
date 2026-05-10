# Phase C — morning runbook

> Wake-up reading. Phase C trip + departures + park-ride proto consumers
> all landed overnight on `feat/proto-trip-results`. APK on Desktop.
> Five steps to validate end-to-end.

---

## §1 · TL;DR

Three new consumers behind a single `IS_BFF_PROTO_ENABLED` flag (currently
hard-coded `true`). All BFF v0.3.0 proto endpoints wired:

- `/api/v1/trip/plan-proto` — fixes the journey-map blank-polyline bug
- `/api/v1/stops/{id}/departures-proto` — 93% smaller wire vs JSON
- `/api/v1/parking/availability-proto?stopIds=` — 79% smaller wire

23 mapper tests, all green. `assembleDebug` succeeded. APK staged.

First command:

```sh
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r ~/Desktop/local_api_test.apk
```

---

## §2 · Branch state

```
1546e229b feat(park-ride): adopt BFF proto endpoint /api/v1/parking/availability-proto
bbd257260 feat(departures): adopt BFF proto endpoint /api/v1/stops/{id}/departures-proto
f6dc5efca refactor(network): rename IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED to IS_BFF_PROTO_ENABLED
8a9087f07 feat(trip-planner): adopt BFF proto endpoint for trip results
b42bc1c0f feat(park-ride): adopt BFF batch endpoint /v1/parking/availability        ← Branch 1
```

Branch `feat/proto-trip-results` (stacked on `feat/bff-local-debug-override` =
PR #1582). Local commits, **not pushed** — you review and `gt submit` when ready.

`git status` is clean. APK at `~/Desktop/local_api_test.apk` (108 MB).

---

## §3 · Steps to verify the journey-map polyline fix (the user-visible bug)

Same flow as Phase A morning — easier this time because the BFF dev
script is already running.

1. **BFF up?**
   ```sh
   curl -s localhost:8080/health
   # expect: {}
   ```
   If not: `cd ~/code/apps/KRAIL-BFF && ./scripts/dev.sh up`.

2. **Install the new APK.**
   ```sh
   ~/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r ~/Desktop/local_api_test.apk
   ```
   (If "not enough space": `adb -s emulator-5554 shell pm trim-caches 1G` first.)

3. **Watch logcat for proto traffic.**
   ```sh
   ~/Library/Android/sdk/platform-tools/adb logcat -s KrailNetwork:V '*:S'
   ```
   Expected on each screen:

   | Screen | Expected log line |
   |---|---|
   | Trip search | `KrailNetwork: BFF GET /api/v1/trip/plan-proto [override=on]` |
   | Departures | `KrailNetwork: BFF GET /api/v1/stops/{id}/departures-proto [override=on]` |
   | Park & Ride home cards | `KrailNetwork: BFF GET /api/v1/parking/availability-proto [override=on]` |

   The `-proto` suffix is the proof you're on the new path. Phase A's
   non-proto endpoints (`/v1/tp/trip`, `/v1/stops/.../departures`,
   `/v1/parking/availability`) should NOT appear in logs while
   `IS_BFF_PROTO_ENABLED = true`.

4. **Smoke-test each screen.**
   - Search Town Hall to Bondi Junction. Trip cards render. Tap one.
   - **Tap the map button** on the timetable. **Polyline should now
     render between stops.** This is the bug fix from yesterday.
   - Open a saved trip's departures. List populates with real data.
   - Open Park & Ride home tile. Cards show availability per facility.

5. **Watch BFF log in another terminal** for cross-correlation:
   ```sh
   tail -f ~/code/apps/KRAIL-BFF/build/dev/bff.log
   ```
   Each KRAIL log line should match a server-side `GET ...-proto`
   line within ~700 ms.

---

## §4 · How to switch back to JSON pass-through (sanity check)

Edit `core/network/src/commonMain/kotlin/xyz/ksharma/krail/core/network/BaseUrl.kt`:
flip `IS_BFF_PROTO_ENABLED = true` to `false`. Rebuild + reinstall.

```sh
sed -i.bak 's/IS_BFF_PROTO_ENABLED: Boolean = true/IS_BFF_PROTO_ENABLED: Boolean = false/' core/network/src/commonMain/kotlin/xyz/ksharma/krail/core/network/BaseUrl.kt
./gradlew :androidApp:assembleDebug
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Now you should see the **non-proto** paths in logcat (`/v1/tp/trip`,
etc.) and the journey-map polyline disappears (since the JSON pass-through
doesn't carry `coords[]`). Restore the flag (`mv BaseUrl.kt.bak BaseUrl.kt`)
to switch back.

---

## §5 · BFF integration report

Wrote `docs/BFF_PHASE_C_INTEGRATION_REPORT.md` for the BFF team — the
symmetric counterpart to `BFF_PHASE_A_INTEGRATION_REPORT.md`. Drop into
`KRAIL-BFF/docs/handover/` when sending. Highlights:

- All three v0.3.0 consumers validated.
- Wire 6.2.0 + KMP-iOS codegen worked first try; no fallback needed.
- Field gaps documented per consumer (e.g. `DepartureRow.trip_id` has
  no JSON-model sink; `is_realtime` boolean is parsed-but-unused
  client-side because KRAIL's existing model infers from
  `departureTimeEstimated`).
- Asks for next phase: BFF deploy + `MIN_APP_VERSION = 0.0.0` hold
  + low-priority schema gaps to consider in a future minor bump.

---

## §6 · Suggested commit / push sequence

1. `git diff` to review the four Phase C commits + the new doc.
2. `gt submit --publish` while on `feat/proto-trip-results` — opens
   stacked PR off PR #1582 (Branch 1).
3. (Optional) Add the new `BFF_PHASE_C_INTEGRATION_REPORT.md` as a
   doc-only commit on this branch before submitting, OR send it to
   the BFF team independently as a Slack / repo-issue-attachment.

---

## §7 · Known gotchas

- **`NSW_PARK_RIDE_BETA` Firebase RC flag** — Park & Ride home cards
  are still gated on this. Toggle on for your test device.
- **Proto path skips fields the JSON path provides.** Detailed
  `occupancy.zones[]` for parking, `properties` block on departures,
  per-stop UTCs on intermediate trip legs — none rendered today, all
  documented in `BFF_PHASE_C_INTEGRATION_REPORT.md §4`. If a future
  feature needs them, schema bump on the BFF side.
- **`stopFinder()` still on NSW direct.** That's Phase D (local stops
  dataset). Out of scope for tonight.
- **Phase B (Firebase RC `enable_proto_bff`)** — not wired yet. The
  flag is `IS_BFF_PROTO_ENABLED = true` hard-coded in BaseUrl.kt.
  Production wiring lands in a future branch when BFF is deployed.

---

## §8 · Pending after this branch

- `feat/debug-settings` (Branch 2, 5 commits, unpushed) — runtime
  selector for `Follow RC / NSW Direct / BFF Local / BFF Prod`.
  Waits on Phase B BFF deploy to be useful in release builds.
- `feat/x-krail-version-header` (Phase B prep) — adds the default
  header in the shared Ktor client. Coordinate with BFF team's
  `MIN_APP_VERSION` floor.
- `feat/local-stop-search` (Phase D) — manifest fetch + cache + local
  search. Eliminates the last NSW-direct call.
- `feat/firebase-rc-rollout` (Phase B production) — wire
  `enable_proto_bff` reader; cohort 0/10/50/100.
- `feat/remove-nsw-key` (Phase E) — after Phase B + D at 100% +
  2-week grace.
