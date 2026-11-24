# Building and changing taj components and the screens made from them

A checklist to run before and after you build or change a taj component, or a screen made of them. It collects rules that
already live in the repo and points at the page each one comes from. Where this page states a rule, the rule exists and
the source is named. Anything that would be a new rule is under "Proposed, needs the operator's decision" at the end, with
its cost, and is not a rule yet.

For colour, read [THEME_COLOUR_ROLES.md](THEME_COLOUR_ROLES.md) and [THEMING.md](THEMING.md). This page is about size, space and
the device.

---

## 1. Who owns the insets

A screen owns the system insets. A component does not read them. That is how taj is built today, checked by searching the
module on 24 Nov 2026: only two components read system insets, because they are system surfaces. They are `TitleBar` (it pads for
the status bar and for the sides of the system bars, and clears the top leading corner) and `ModalBottomSheet` (one
implementation each for Android and iOS). `PreviewTheme` pads for the system bars so a preview is not drawn under them, which is
also why a layout test must not use it (see section 4). The two modifiers below are the rest.

- [ ] The component takes its space from its parent. It has no `statusBarsPadding()`, `navigationBarsPadding()`,
      `systemBarsPadding()`, `safeDrawingPadding()` or `WindowInsets` read of its own.
- [ ] The screen has one inset authority for the keyboard, and every other level applies none
      ([LAYOUT_AND_INSETS.md](../docs/LAYOUT_AND_INSETS.md) 1.1). `imePadding()` goes once, on the screen, never on the input
      inside it.
- [ ] Anything that floats over a full-bleed surface, such as a map, stands inside the horizontal safe area with
      `Modifier.sideSafeInsets()` from `xyz.ksharma.krail.taj.modifier` (LAYOUT_AND_INSETS.md 1.6). It reads `safeDrawing`,
      the one inset that carries a side on both Android and iOS.
- [ ] Never `WindowInsets.displayCutout`. On iOS it reports 0 at the sides, so a card padded by it ran under the iPhone Duo
      status pill. `scripts/check_layout_invariants.py` fails the build on it in any non test Kotlin file.
- [ ] A title or control at the very top, where the status bar inset may be zero, uses `cornerClearanceTop()` or
      `cornerSafeTopPadding()` from the same package (`CornerSafeInsets.kt`), so it stays out of the rounded top leading corner of
      the iPhone Duo outer screen. `TitleBar` already does. Add it after `statusBarsPadding()`, never in place of it.
- [ ] A bottom anchored control stands above the navigation bar (`navigationBarsPadding()` on the screen or the control's
      container), and a scrolling list ends with room for it, so its last row can be scrolled clear. A fixed end padding
      smaller than a three button bar is the fault the matrix found in the Search Stop list.

---

## 2. Width and text

- [ ] Nothing has a fixed width that the text or the window can outgrow, and no fixed height sits around content that scales
      with text ([FEATURE_QUALITY_CHECKLIST.md](../docs/FEATURE_QUALITY_CHECKLIST.md) section 6, Text scaling).
- [ ] Text wraps rather than truncating, and a taller row beats an unfinishable one (the same section). When width runs short
      the layout switches mode: it goes vertical, or wraps whole items in a `FlowRow`. The map home does this for its chips,
      notices and car park blocks, which "wrap whole at every text size" and never squeeze
      ([MAP_HOME_UX.md](../docs/MAP_HOME_UX.md)).
- [ ] Touch targets are at least 48 dp (the ui-mockups skill, section 4).
- [ ] Looked at 320 dp wide at 200 percent text, the frame where layouts break first (the ui-mockups skill, section 2), and the
      fallback mode for it is defined. In the device matrix that size is the `Narrow320Font2` profile, which runs on every screen
      automatically.
- [ ] The keyboard, screen reader and contrast items in FEATURE_QUALITY_CHECKLIST.md section 6 are ticked.

---

## 3. Device types the app really branches on

The app branches on the window it is given, not on the device. The breakpoints are in
`core/adaptive-ui/.../AdaptiveLayoutInfo.kt` and are explained, per screen, in
[TABLET_FOLDABLE_UX.md](../docs/TABLET_FOLDABLE_UX.md).

| Window | What the app does | Where it is written |
|---|---|---|
| Width under 840 dp, or height under 480 dp | Single pane. The map is reached through its own button. | `shouldShowDualPane` is false |
| Width 840 dp or more and height 480 dp or more | Dual pane. The list pane is a fixed 480 dp, the map takes the rest. | `shouldShowDualPane`, TABLET_FOLDABLE_UX.md section 1 |
| Height under 480 dp | Phone in landscape. Single pane at any width, and the chips on the map home go. | `isCompactHeight`, `isPhoneLandscape` |
| iPhone Duo outer screen, 466 by 678 pt | A system strip 84 pt wide down the right edge and 34 pt at the bottom. The top inset is zero, so the top leading corner needs clearance. | [DEVICE_PROFILES.md](../docs/testing/DEVICE_PROFILES.md), `CornerSafeInsets.kt` |
| Foldable, cover and inner display | The cover is phone sized and the inner display is almost square. The same screen must be right on both. | DEVICE_PROFILES.md, `FoldCover` and `FoldInner` |
| iPad windows, 744, 1032, 1133 and 1376 pt wide | Single pane only at the iPad mini in portrait, dual pane at the others. | TABLET_FOLDABLE_UX.md, "What real devices get" |

Which devices have an automated check, which have a hand check and which have neither is in
[DEVICE_COVERAGE.md](../docs/testing/DEVICE_COVERAGE.md). Read its Gaps list before you decide a layout is verified.

---

## 4. What to run

Previews. Use `@PreviewComponent` for a component and `@PreviewScreen` for a screen, from `xyz.ksharma.krail.taj.preview`, never
a bare `@Preview` (CLAUDE.md, Preview Annotations). A component gets light, dark and 2.0 font scale on a phone. A screen gets
those and a tablet.

Snapshots. A `@ScreenshotTest` preview records three goldens: light, light at 2.0 font scale, and dark.

```bash
./gradlew :taj:testAndroidHostTest -Proborazzi.test.record=true     # record
./gradlew :taj:verifyRoborazziAndroidHostTest                       # verify, as CI does
./gradlew :feature:trip-planner:ui:verifyRoborazziAndroidHostTest   # the same for the screens
```

The device matrix. Every screen needs a host test that calls `forEachDeviceProfile(screen = "XxxScreen")`, which composes it on
every profile in [DEVICE_PROFILES.md](../docs/testing/DEVICE_PROFILES.md) with real system bar and side insets and checks that
nothing the rider presses stands under a system bar. A component that floats at an edge of the screen, such as the stop card,
gets one as well. Use the bare theme (`BareKrailTheme` in the matrix tests), not `PreviewTheme`, or the screen is handed insets
that were already used up and a screen that ignores them passes.

```bash
./gradlew :feature:trip-planner:ui:testAndroidHostTest --tests "xyz.ksharma.krail.trip.planner.ui.devicematrix.*"
./gradlew :core:testing:testAndroidHostTest --tests "*DeviceMatrixAdoptionGuardTest"
python3 scripts/check_layout_invariants.py
```

`DeviceMatrixAdoptionGuardTest` fails the build when a top level `XxxScreen` has no matrix test and is not on
`config/device-matrix-baseline.txt`. That list only shrinks. A new screen gets a test, not a line on the list.

---

## 5. Done means

- [ ] No component reads a system inset, and no screen pads by `displayCutout`.
- [ ] Sides stand inside `sideSafeInsets()`, and the bottom clears the navigation bar with the list able to scroll clear of it.
- [ ] Nothing is fixed width, clipped or truncated, and the 320 dp at 200 percent text view reads correctly.
- [ ] Previews use `@PreviewComponent` or `@PreviewScreen`, and the snapshot verify task is green with no golden changed by
      accident.
- [ ] A new screen has its `forEachDeviceProfile` test, and the matrix and the adoption guard are green.
- [ ] I read the Gaps in DEVICE_COVERAGE.md and, for anything near an edge or a side, checked the rows no automation covers.

---

## Proposed, needs the operator's decision

These are not rules. Each has a cost, so each is the operator's call.

1. **A fourth snapshot variant at 320 dp wide and 200 percent text.** The matrix already runs that size for screens, but a
   taj component only sees it in the 2.0 font scale golden at phone width. Cost: one more golden per `@ScreenshotTest` preview.
   There are about 38 such previews in taj and about 212 across the other modules, so on the order of 38 more goldens in taj
   (git LFS, about 3.5 MB for the 126 there now) and up to 212 more elsewhere, plus the record and verify time for each.
2. **A matrix profile one dp under the compact height gate.** The width edge of the dual pane gate is covered by the
   `Gate839` and `Gate840` profiles. The height edge, 479 against 480 dp, is covered only by the phone landscape profiles, which
   are far below it. Cost: one more profile on every matrix test, about 0.1 to 0.4 s per screen test.
3. **A component level matrix.** Components that sit at a screen edge are covered only where someone wrote a test for them, as
   for the stop card. A rule that every taj component is composed on the profiles would be new, and most components are not at an
   edge. Cost: a test per component and a decision on what "stands inside the safe area" means for a component with no screen.
