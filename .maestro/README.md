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

## Three lanes

| Lane | Runs | Contents |
|---|---|---|
| `smoke/` | With the `nightly/` lane, in the weekly cron run (it no longer runs on pull requests, see `docs/testing/LAYERS.md`) | Launch, and plan a trip. Short enough that it could gate a merge again. |
| `nightly/` | Weekly cron (Sunday 03:00 AEST), `prod/**`, manual dispatch | Rotation, process lifecycle and permission-denial cases. Slower, and not worth blocking a PR on. |
| `weekly/` | Weekly cron (Thursday 03:00 AEST), manual dispatch | Real taps on every key control, on the device types the other lanes never see: the iPhone Duo outer screen, an iPad, a small iPhone, an Android tablet and an Android foldable. See [The weekly device run](#the-weekly-device-run). |

`weekly/` is deliberately not listed in `config.yaml`. The nightly passes `.maestro/` on Android and the explicit smoke and
nightly directories on iOS, so a lane that is missing from that file is never picked up by it. The weekly workflow passes
`.maestro/weekly/` itself.

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
| Weekly iOS | once | Same as the nightly iOS leg, including the simulator restart between the attempts. |
| Weekly Android | once | Same as the nightly Android leg, for each posture the device has. |

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
| `smoke/01-launch-home.yaml` | Cold launch reaches home, the saved-trips list, the label row and the search capsule render, the title bar has its actions. |
| `smoke/02-plan-trip.yaml` | The core journey: pick a destination, pick an origin, get real journey results back from the API, then (Android) back lands on Search Stop with both bars filled and back again on home. The home is the classic one: the label row, then the list, with one search capsule at the bottom. |
| `nightly/03-rotation-sweep.yaml` | Home, search stop and the timetable each survive rotation, including with results and journey data loaded, and back from the rotated timetable still lands on Search Stop. |
| `nightly/04-background-foreground.yaml` | Backgrounding and returning lands the rider on the screen they left, not on home. |
| `nightly/05-kill-relaunch.yaml` | After a force-stop the app recovers to home and navigation still works. |
| `nightly/06-permissions-denied.yaml` | With location denied the app still runs and a trip can still be planned by typing. |
| `weekly/01-intro-buttons.yaml` | From a clean install, one tap on every button of the five Intro screens, each followed by the next screen as proof. On Android, system back is pressed on every screen after the first and the screen must still be there. Ends with the trip saved by Yes, save it on Saved Trips. |
| `weekly/02-saved-trips.yaml` | A saved trip card opens its timetable, the timetable's back arrow returns to Saved Trips, and the search capsule opens Search Stop. The card steps are skipped when the device has no saved trip. |
| `weekly/03-search-stop-controls.yaml` | The clear X empties the field and keeps the screen, Select on map returns to the map state, the swap trades From and To (read from the real stop names in their labels), and the go circle opens the timetable. |
| `weekly/04-search-stop-options.yaml` | With location granted, Options is drawn, opens the mode block and shuts it again. The only weekly flow that does not deny location. |
| `weekly/05-timetable-controls.yaml` | Reverse trades the origin and destination, the star saves the trip and takes it back, and the back arrow returns to Search Stop with both stops and the go circle. |
| `weekly/06-settings-rows.yaml` | The gear opens Settings, Change Theme, Manage stop labels, Our story and (on a debug build) Debug Config and its Network tile each open and their back arrow returns, and the Settings back arrow returns to Saved Trips. Invite and Rate are asserted but not tapped, because they leave the app. |
| `weekly/07-stop-card-set-destination.yaml` | A tap on a map pin opens the stop card and Set as destination closes it and fills the To bar. The one flow that taps points, as percentages, because map pins are not in the accessibility tree. |
| `shared/pick-trip-stops.yaml` | Not a test. Picks a destination and then an origin on Search Stop from two typed queries and waits for the swap and the go circle. Used by the weekly flows only. |
| `shared/dismiss-intro.yaml` | Not a test. Walks the first-run Intro so a clean device can reach the app: No to Park & Ride, a destination, an origin, Not now to saving the trip on the confirm screen, and a colour on the last screen, which ends the Intro. |

## The weekly device run

The nightly runs on an iPhone 17 simulator and a Pixel 6 emulator. Neither has a side inset, so neither could show the bug
that motivated this lane: on the outer screen of the iPhone Duo (466 by 678 pt, right safe area 84 pt, bottom 34 pt) the swap and
the go circle of Search Stop sat under the system status pill, and a tap on either opened Control Center and never reached
the app. A flow that taps a control and then asserts what the tap did catches that class of bug, because a tap that does not
arrive leaves everything where it was. The weekly run is the nightly's idea on more screens. The nightly is unchanged, and the
two share nothing but the shared flows and `ci/run-flows.sh`.

The workflow is [`maestro-weekly-devices.yml`](../.github/workflows/maestro-weekly-devices.yml). It runs on Thursday at 03:00 AEST,
three days from the nightly so the two never wait on each other for a macOS runner, and by hand from the Actions tab with a
`devices` input to run a subset.

| Leg | Device | How it is made |
|---|---|---|
| `duo` | iPhone Duo, the outer screen | iOS simulator, device type `iPhone-Duo`, on the Duo's own runtime |
| `ipad` | iPad Pro 13-inch (M5), wide enough for the two pane layouts | iOS simulator |
| `small-iphone` | iPhone SE (3rd generation), the smallest screen | iOS simulator |
| `tablet` | Pixel Tablet | Android emulator, profile `pixel_tablet` |
| `fold` | Pixel 9 Pro Fold, on the cover display and then the inner display | Android emulator, profile `pixel_9_pro_fold`, postures set with `adb shell cmd device_state` |

The devices are listed in one place, `ci/plan-weekly-matrix.sh`, and adding one is a line there. Each leg is its own job, so one
failing leg does not hide the others.

How a run goes:

- **The iOS app is built once.** The build is most of the iOS lane's time (the xcodebuild step is the long one), so one job builds
  it and every iOS leg installs the same build. A simulator build does not depend on the simulator device type.
- **A device type the runner cannot make is skipped, never silently.** Each iOS leg first runs `ci/probe-ios-device-type.sh`,
  which lists the device types and runtimes the runner image has and checks for the leg's device type and for a runtime that
  supports it. If either is missing the leg records "skipped: device type not on this runner image" in the job summary and as a
  warning, runs nothing, and ends green. Run that device from a Mac instead, as below.
- **The Android fold leg reads the postures from the device.** It runs the flows on the inner display and then the cover
  display, switching with `cmd device_state`. If the system image does not report both a CLOSED and an OPENED state it runs once,
  in whatever posture it booted in, and says so in the summary.
- **Retries are the nightly's:** once, with the simulator restarted between the attempts on iOS.

### Reading the summary

Open the run in the Actions tab. Each leg's job summary has a table with one row for every flow in `weekly/`: `pass`, `FAIL` with
the command that failed and its selector, or `not run` for a flow that never started. A skipped device says so at the top of its
own summary. The Android fold leg has one table for each display. Then go to the leg's artifact, named
`maestro-weekly-ios-<leg>-<run id>` or `maestro-weekly-android-<leg>-<run id>`, and read it in the order
[`docs/MAESTRO_TRIAGE.md`](../docs/MAESTRO_TRIAGE.md) gives. The weekly artifacts are kept for 14 days, so one run is still there
for the next.

### Running it locally

For a device the CI runner cannot make, which may well include the Duo, run the same flows on a simulator you have booted. The
script does not boot, create or erase a simulator, and it stops with the command to run if the one you named is not ready.

```sh
xcrun simctl list devicetypes | grep -i duo        # does this Mac have the type
# the runtime is the one `xcrun simctl list runtimes` shows for the Duo, which supports that device type alone
xcrun simctl create duo com.apple.CoreSimulator.SimDeviceType.iPhone-Duo \
  com.apple.CoreSimulator.SimRuntime.iOS-27-2-iPhone19-4
xcrun simctl boot <udid> && xcrun simctl bootstatus <udid> -b
xcrun simctl install <udid> path/to/iosApp.app      # a simulator build of the app

scripts/run_device_matrix_locally.sh <udid>                                          # every weekly flow
scripts/run_device_matrix_locally.sh <udid> .maestro/weekly/03-search-stop-controls.yaml   # one flow
```

It prints the same table the CI summary shows and leaves what Maestro wrote in `build/maestro-weekly/<udid>`. The first flow
erases the app's data to walk the Intro, and the others expect the Intro to be finished, so a single flow is run after the
Intro has been finished once. `APP_ID` and `MAESTRO_RETRIES` can be set in the environment.

An Android device is run with the command in [Running locally](#running-locally), pointed at `.maestro/weekly/`. A foldable's
posture is switched with `adb shell cmd device_state state <id>`, and `adb shell cmd device_state print-states` lists the ids.

### What the flows assume

They were written against the app's source and the Maestro syntax check, and a device run is what proves them, so expect the first
runs on each device to find a wait or a selector that needs adjusting. Two things to know. The back arrows are found by their
label, "Back", because the shared title bar draws them without a tag, and on Android that label is also the one the system
navigation bar uses on a three button device. And `weekly/07-stop-card-set-destination.yaml` taps points on the map, which lands
on a pin by chance of the tiles and the screen, and tries up to eight points before it fails.

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
  in landscape, which is a different layout, and every later flow in the run
  fails looking for nodes that are not on screen. One failure becomes three.
- **Settle before you look, and never guard on a node the app has not drawn yet.** The intro
  dismissal used to be a caller-side `runFlow` conditioned on `intro.screen`. `launchApp`
  returns once the process is foregrounded, not once Compose has drawn, so on a freshly booted
  CI emulator that condition was evaluated against an empty hierarchy: it skipped, the Intro
  stayed up, and every flow failed on its first assertion. `shared/dismiss-intro.yaml` now waits
  for the first Intro screen to be drawn and no-ops by itself, and callers invoke it unconditionally. A condition
  is only as good as the frame it is tested against.

## Permission prep

`06-permissions-denied.yaml` sets the permission itself via `launchApp: permissions:`. It
deliberately does **not** use `clearState: true`, which would also wipe the "intro seen" flag
and spend the flow walking the Intro instead of testing a denial.

If a device already granted location and you want to be certain the denial is real:

```sh
adb shell pm revoke xyz.ksharma.krail.debug android.permission.ACCESS_FINE_LOCATION
adb shell pm revoke xyz.ksharma.krail.debug android.permission.ACCESS_COARSE_LOCATION
```

## Debugging a failure

Maestro writes the hierarchy, logs and screenshots for every run to `~/.maestro/tests/<timestamp>/`.
CI copies that into the workspace and uploads it as an artifact on every run, pass or fail. The weekly iOS legs write to a
directory of their own instead (`--test-output-dir`), with the same layout.

**[`docs/MAESTRO_TRIAGE.md`](../docs/MAESTRO_TRIAGE.md) is the procedure**: what the artifact
holds, the order to read it in, what it deliberately does not capture, and a table of failure
signatures that have each already cost a wrong diagnosis. Read that first when a lane is red.

The rest of this section is about why the artifact exists at all, which is a separate trap:

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
