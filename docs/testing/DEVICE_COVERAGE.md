# Device coverage: which device is checked by what

Part of [TESTING.md](../../TESTING.md). Written 24 Nov 2026.

This page answers one question: for each kind of device and posture the app runs on, what checks it, and what does not. Read
it before you decide a layout is verified, and read the Gaps at the end before a release.

Each cell says **covered**, **partial** or **NOT COVERED**.

- **Covered** means a check that runs on a schedule, or on every pull request, exercises this device shape, with the numbers it
  uses measured.
- **Partial** means something runs but not the whole shape: the numbers are guessed, the size is a stand in, only some flows run,
  or the check is written but not merged yet.
- **NOT COVERED** means nothing here runs it.

The four layers, from cheapest to most real:

| Layer | What it is | Runs |
|---|---|---|
| Host matrix | `forEachDeviceProfile` composes each screen in a window of the device's size, density and font scale, with its real system bar and side insets, and fails if a control stands under a system bar. Profiles are in [DEVICE_PROFILES.md](DEVICE_PROFILES.md). | Every pull request, inside `testAndroidHostTest` |
| Weekly device run | The same app driven by real taps on seven key flows, on an iPhone Duo simulator, an iPad Pro 13 inch, an iPhone SE, a Pixel Tablet and a Pixel 9 Pro Fold, which runs on the cover display and then the inner display. See `.maestro/README.md`, "The weekly device run". | Wednesday 17:00 UTC (Thursday 03:00 AEST) once merged. **Written on the branch `ci/maestro-weekly-device-matrix` and not merged, so it has not run on this branch's base.** |
| Nightly Maestro | Launch, plan a trip, rotation, background and foreground, kill and relaunch, location permission denied, on an iPhone 17 simulator and a Pixel 6 emulator. | Saturday 17:00 UTC (Sunday 03:00 AEST), on pushes to `prod/**`, and by hand |
| Hand QA | A person drives the app on a device, emulator or simulator. | When someone does it. Not scheduled. |

`.maestro/README.md` says the smoke lane (launch, plan a trip) runs on every pull request. No workflow in `.github/workflows` other
than the nightly runs Maestro, so this page counts the smoke flows only as part of the nightly. Either the README or the workflows
need to change; until then, do not rely on a pull request running any device flow.

---

## The map

| Device and posture | Host matrix, every PR | Weekly device run | Nightly | Hand QA |
|---|---|---|---|---|
| Android phone, portrait | **covered**: `Narrow320Font2`, `SmallPhone`, `LargePhone`. Sizes of the last are measured, insets are guessed | **NOT COVERED**: no phone leg | **covered**: Pixel 6 emulator, portrait | Pixel 6 Pro and Pixel 10 Pro phones, and the phone emulator, by hand. **partial**: not scheduled |
| Android phone, landscape | **covered**: `PhoneLandscapeCutout` (cutout on the left, measured on the emulator) and `AndroidLandscapeSideNavBar` (a three button bar at the side, guessed) | **NOT COVERED** | **covered** for rotating and coming back, on the Pixel 6 emulator, which has no cutout | Phone emulator in landscape, 24 Nov 2026 pass. **partial** |
| Android tablet | **partial**: `TabletLandscape` (size measured, insets guessed). Portrait is only the window widths `Medium720`, `Gate839`, `Gate840` | **partial**: Pixel Tablet leg, once merged | **NOT COVERED** | Tablet emulator, by hand. **partial** |
| Android fold, closed (cover display) | **covered**: `FoldCover` (size measured, insets guessed) | **partial**: the fold leg runs the flows on the cover display, once merged | **NOT COVERED** | Pixel 9 Pro Fold emulator, cover display, by hand (sizes read in the 24 Nov 2026 pass). **partial** |
| Android fold, open (inner display) | **covered**: `FoldInner` (852 by 883, so dual pane; size measured, insets guessed) | **partial**: the fold leg runs the flows on the inner display, once merged | **NOT COVERED** | Pixel 9 Pro Fold emulator, inner display, by hand (sizes read in the 24 Nov 2026 pass). **partial** |
| Android fold, folding or unfolding during a session | **NOT COVERED**: a host test has no posture change | **NOT COVERED**: the leg changes posture between suite runs, not during one | **NOT COVERED** | Not recorded as done |
| iPhone small (SE class) | **partial**: `IPhoneSmall`, every number guessed | **partial**: iPhone SE (3rd generation) leg, once merged | **NOT COVERED**: the nightly is an iPhone 17 | Not done |
| iPhone Pro, portrait | **partial**: `IPhoneProPortrait` (size measured on the iPhone 18 Pro simulator, insets guessed) | **NOT COVERED**: no iPhone Pro leg | **covered**: iPhone 17 simulator, a status bar and a home indicator, no side inset | iPhone Pro simulator, by hand |
| iPhone Pro, landscape | **partial**: `IPhoneProLandscape`, insets guessed to arrive like the Duo's | **NOT COVERED** | **partial**: the rotation sweep turns the iPhone 17 simulator to landscape | iPhone Pro simulator, by hand. **partial** |
| iPad mini | **partial**: `IPadMiniPortrait` (single pane) and `IPadMiniLandscape` (dual pane). Widths measured, heights and insets guessed | **NOT COVERED**: the iPad leg is the Pro | **NOT COVERED** | iPad simulators, window widths checked 11 Oct 2026. **partial** |
| iPad Pro 13 inch | **partial**: `IPadPro13Portrait` and `IPadPro13Landscape`, both dual pane. Widths measured, heights and insets guessed | **partial**: iPad Pro 13 inch (M5) leg, once merged | **NOT COVERED** | iPad simulators, window widths checked 11 Oct 2026. **partial** |
| iPhone Duo, closed (outer screen) | **covered**: `IPhoneDuoOuter`, insets **measured** by a Compose probe on 24 Nov 2026. The Plan your trip sheet body and every sheet's container (`SheetContent`) run on it as well, composed as the sheet places them, since a sheet's own window cannot be shown in a host test | **partial**: Duo leg, once merged, and skipped with a warning if the runner image has no such device type | **NOT COVERED** | iPhone Duo simulator, outer screen: the inset probe, and the swap and go taps before and after the fix, 24 Nov 2026, and a full walk the same day that found the Plan your trip sheet at full height drawing Reset and the date chevron under the status pill (fixed in taj's `SheetContent`, not yet re-checked on the simulator). **covered** by hand |
| iPhone Duo, open (inner screen) | **partial**: `IPhoneDuoInnerPortrait` and `IPhoneDuoInnerLandscape` are a size only stand in, with guessed insets, never a measurement | **NOT COVERED**: outer screen only | **NOT COVERED** | **NOT COVERED**: the simulator cannot be driven to the inner screen headless |
| iPhone Duo, rotation | **NOT COVERED**: the outer screen has no landscape profile | **NOT COVERED** | **NOT COVERED**: the rotation sweep runs on the iPhone 17 | Not done |
| Half folded or table top posture | Out of scope, by decision in [TABLET_FOLDABLE_UX.md](../TABLET_FOLDABLE_UX.md): width breakpoints cover the common cases, and posture aware adaptation is revisited only if there is reason to | Out of scope | Out of scope | Out of scope |

The hand QA column is the devices a person has driven by hand, with a date only where a measurement or a note in this repo carries one.
Hand checks are never scheduled, so none of them is marked covered unless a probe or a before and after run is on record. The scenario numbers from the QA
report are not quoted here because that report was not available when this page was written. When it is linked, put the numbers in
that column so a reader can see which scenario touched which device.

The weekly device run says "once merged" because its workflow and flows exist only on a branch. Until it is merged and has run,
count every cell in that column as written but not running.

---

## Gaps

What no automation here can cover, and so what a human must check on real hardware or in Simulator.app before a release.

1. **The iPhone Duo inner screen, for real.** The simulator cannot be driven to its inner screen headless, so no insets for it
   have ever been probed. The host profiles for it are a size only stand in whose insets are copied from the outer screen. A window
   of 624 dp was measured on a Duo in the past (TABLET_FOLDABLE_UX.md, issue 2040), not the 669 the simulator display config says.
   Open the Duo simulator in Simulator.app, use the inner screen, and walk Saved Trips, Search Stop, the Timetable and Settings.
2. **iPhone Duo rotation.** No profile and no flow turns the outer or inner screen. Rotate both by hand and check that no control
   ends under the status pill strip, which moves with the turn.
3. **A real edge swipe.** The host blockers model a tap landing on a system region. They do not model the system taking a swipe from
   the edge. Check the back swipe on iOS and the back gesture on Android gesture navigation on a screen with a control near the
   left edge, and check Control Center and the home indicator swipe on the Duo's right strip.
4. **The hinge, and folding during a session.** A host test has no hinge or posture change, and the weekly leg changes posture
   between runs. On a real fold, or the fold emulator, fold and unfold with a screen open and check that state survives and that
   the layout switches between the phone sized cover and the 852 dp wide inner display.
   [MANUAL_TEST_CASES.md](MANUAL_TEST_CASES.md) 2.6 is the case.
5. **Landscape with the cutout on the other side.** The one cutout profile has it on the left. Turn a phone the other way so the
   cutout is on the right and check the same screens.
6. **Every inset that is a guess.** Only the Duo outer screen's insets, the Pixel 6 Pro's size and the emulators' sizes and
   cutout were measured. Every other inset in [DEVICE_PROFILES.md](DEVICE_PROFILES.md) marked `assumed` is a guess about a device
   nobody probed, including the iPad and iPhone Pro status bar and home indicator figures. A real device that differs will not be
   caught.
7. **Resizable windows.** An iPad or a desktop window dragged across 600 to 839 dp and across 840 dp while a screen is open is not
   run. The host profiles cover a static window on each side of the edge, and the pane test covers the switch, but not the live
   change of a screen that is already showing.
8. **The map.** The host matrix replaces the map with a placeholder, so the map's own controls, the location button on each map
   screen, and the camera are measured only by hand or by the weekly flows that tap them. Two location buttons are read in source
   and never measured: the Timetable's `JourneyMap` and the Park and Ride `ParkRideMapPane` stand theirs at the bottom end with
   `navigationBarsPadding()` and no side inset, so on the Duo outer screen they stand inside the 84 pt strip. Whether a tap there
   reaches the app is not known: the status pill is the system's, but how far down the strip the system takes the touch has been
   measured only near the top of the screen. Tap both on the Duo, at the bottom right.
9. **Dynamic Type beyond the app's own font scale.** The matrix runs the one 320 dp frame at 200 percent text. A larger system
   setting on iOS is not run.
10. **A sheet's own window.** A host test cannot show the window a `ModalBottomSheet` opens, and the iOS sheet is not compiled for
    the host tests, so the matrix proves the padding of `SheetContent` and `check_layout_invariants.py` checks that both platform
    sheets use it, but no automation drags a real sheet to full height. Do that on the Duo outer screen for every sheet (Plan
    your trip, Service Alerts, the stop sheet, Assign a label) and tap the control at the top right of each.

Before a release, run [MANUAL_TEST_CASES.md](MANUAL_TEST_CASES.md) section 2 on the hardware rows it lists, and walk gaps 1 to 5
above. For a change to a screen or a floating control, the rows to think about are in
[taj/COMPONENT_GUIDELINES.md](../../taj/COMPONENT_GUIDELINES.md) section 3.
