# End-to-end flows

[Maestro](https://maestro.mobile.dev) flows that drive the real app on a real device or
simulator. They cover the things unit tests and detekt structurally cannot: whether the app
launches, whether a rider can actually plan a trip, and whether a screen survives having its
Activity destroyed underneath it.

Verified against **Maestro 2.8.0**. `setOrientation` needs 2.x; on 1.x the rotation sweep will
not parse.

```sh
curl -fsSL "https://get.maestro.mobile.dev" | bash
export PATH="$PATH:$HOME/.maestro/bin"
maestro --version   # expect 2.x
```

## Two lanes

| Lane | Runs | Contents |
|---|---|---|
| `smoke/` | Every PR | Launch, and plan a trip. Fast enough to gate a merge. |
| `nightly/` | Nightly cron, `prod/**`, manual dispatch | Rotation, process lifecycle and permission-denial cases. Slower, and not worth blocking a PR on. |

**Rotation moved out of the PR lane.** The sweep is a real test, but rotating a
software-rendered CI emulator was the dominant source of flakes in a lane that must never
retry, and a rotation flake blocking an unrelated merge costs more than the sweep buys per
PR. What it covers is already covered per-PR host-side, where recreation is deterministic:
`SavedTripsParkRideRestoreTest` and `TimeTableStopSheetRestoreTest` drive
`StateRestorationTester` over the same screens, and `NavKeySerializationConfigTest` catches
the unregistered-route crash that rotation would otherwise be the first to find. The sweep
stays nightly, where it still exercises the real window-level path those cannot.

`shared/` holds helper flows called via `runFlow`. It is deliberately outside both lanes so a
directory run never picks it up as a test.

### Retries

| Lane | Retries | Why |
|---|---|---|
| PR smoke | **none** | A flake that can block a merge gets fixed, not re-run. |
| Nightly Android | once | Reports rather than gates, so one retry buys signal without hiding a real break: a genuine regression fails twice. |
| Nightly iOS | once | Same, plus simulator runs are meaningfully flakier than emulator runs (boot races, window-server hiccups). |

Both nightly retries are one extra attempt, no more. The Android one is
`MAESTRO_RETRIES=1` on the `run-flows.sh` invocation; the iOS leg wraps its own suite in
bash. Each retry force-stops the app first, so the second attempt starts from a launch
rather than from wherever the failed flow abandoned it.

## Running locally

The app id is a parameter, so one set of flows serves both platforms. It **must** be passed
with `-e`; a shell variable alone will not reach the flow, and an in-file `env:` default would
silently win over `-e` and make the parameter unoverridable.

```sh
# Android (note the .debug suffix on the debug build)
./gradlew :androidApp:installDebug
maestro test -e APP_ID=xyz.ksharma.krail.debug .maestro/smoke/

# iOS simulator
maestro test -e APP_ID=xyz.ksharma.krail .maestro/smoke/

# One flow, on a chosen device
maestro --device emulator-5554 test -e APP_ID=xyz.ksharma.krail.debug .maestro/smoke/02-plan-trip.yaml
```

| Target | `APP_ID` |
|---|---|
| Android debug | `xyz.ksharma.krail.debug` |
| Android release | `xyz.ksharma.krail` |
| iOS | `xyz.ksharma.krail` |

## What each flow pins

| Flow | Pins |
|---|---|
| `smoke/01-launch-home.yaml` | Cold launch reaches home, the saved-trips list and the search row render, the title bar has its actions. |
| `smoke/02-plan-trip.yaml` | The core journey: pick an origin, pick a destination, get real journey results back from the API. |
| `nightly/03-rotation-sweep.yaml` | Home, search stop and the timetable each survive rotation, including with results and journey data loaded. |
| `nightly/04-background-foreground.yaml` | Backgrounding and returning lands the rider on the screen they left, not on home. |
| `nightly/05-kill-relaunch.yaml` | After a force-stop the app recovers to home and navigation still works. |
| `nightly/06-permissions-denied.yaml` | With location denied the app still runs and a trip can still be planned by typing. |
| `shared/dismiss-intro.yaml` | Not a test. Walks the first-run carousel so a clean device can reach the app. |

## Selectors

Flows select on `testTag` ids, never on visible copy, so a wording change cannot break a flow
and a flow failure always means behaviour changed. The tags are declared in
`TripPlannerTestTags` and `DebugSettingsTestTags` and are **public API** to this directory:
grep here before renaming one.

On Android the tags reach the accessibility tree as `resource-id` because the app root opts in
via `exposeTestTagsToUiAutomation()`. On iOS, Compose Multiplatform publishes them as
`accessibilityIdentifier` already. Same tag, same `id:` selector, both platforms.

Per-item tags carry their domain id (`timetable.journey.<journeyId>`), so a flow matches the
set with a regex and picks by index:

```yaml
- tapOn:
    id: "searchstop.result..*"
    index: 0
```

## Conventions worth keeping

These are all fixes for failures this suite actually hit, not style preferences.

- **Wait, do not assert, after anything asynchronous.** Rotation, navigation and network all
  have a window where a node is legitimately missing. `extendedWaitUntil` passes on a warm
  device *and* a cold one; `assertVisible` in the same spot is green locally and red on CI's
  first run.
- **Scroll to list items rather than asserting them in place.** Maestro only matches what is on
  screen. A "Save this trip?" prompt above the results pushes every journey card below the fold
  on a short viewport, so an in-place assert measures device height, not behaviour.
- **Restore device state in `onFlowComplete`.** A flow that fails mid-rotation leaves the device
  in landscape, where the search row collapses to a single pill, and every later flow in the run
  fails looking for fields that are not on screen. One failure becomes three.
- **Settle before you look, and never guard on a node the app has not drawn yet.** The intro
  dismissal used to be a caller-side `runFlow` conditioned on `intro.screen`. `launchApp`
  returns once the process is foregrounded, not once Compose has drawn, so on a freshly booted
  CI emulator that condition was evaluated against an empty hierarchy: it skipped, the carousel
  stayed up, and every flow failed on its first assertion. `shared/dismiss-intro.yaml` now waits
  for the app to settle and no-ops by itself, and callers invoke it unconditionally. A condition
  is only as good as the frame it is tested against.

## Permission prep

`06-permissions-denied.yaml` sets the permission itself via `launchApp: permissions:`. It
deliberately does **not** use `clearState: true`, which would also wipe the "intro seen" flag
and spend the flow walking the carousel instead of testing a denial.

If a device already granted location and you want to be certain the denial is real:

```sh
adb shell pm revoke xyz.ksharma.krail.debug android.permission.ACCESS_FINE_LOCATION
adb shell pm revoke xyz.ksharma.krail.debug android.permission.ACCESS_COARSE_LOCATION
```

## Debugging a failure

Maestro writes the hierarchy, logs and screenshots for every run to `~/.maestro/tests/<timestamp>/`.
CI copies that into the workspace and uploads it as an artifact on every run, pass or fail.

Two things had to be true before that artifact actually appeared, and both are easy to
reintroduce:

- **Collect while the device is alive.** `reactivecircus/android-emulator-runner` kills the
  emulator as soon as its step ends, so a later step calling `adb` blocks on
  `- waiting for device -` until the job times out. Everything needing a device goes in the
  action's own `script:`.
- **That `script:` runs one line at a time**, each in its own `sh -c`, so shell state does not
  survive between lines and the step aborts on the first non-zero line. Anything needing real
  control flow lives in [`ci/run-flows.sh`](ci/run-flows.sh) and is invoked as a single line.

`actions/upload-artifact` also does not expand `~`, so artifact paths point at the workspace
copy, never at the home directory.

To see what the driver sees on a live device:

```sh
maestro --device emulator-5554 hierarchy | grep '"resource-id"' | sort -u
```
