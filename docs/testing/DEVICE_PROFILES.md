# Device profiles: one screen, every device

Part of [TESTING.md](../../TESTING.md).

A screen is composed on a list of devices whose size, density, font scale and system insets are
fixed in one table, with the insets delivered the way that platform delivers them. The runner
puts a node that takes the touch over every region a system bar owns, then checks that nothing
the rider must press is under one. This page is the spec: the profiles, the helper, what it
cannot cover, and what it costs.

---

## The incident this prevents

On the outer screen of the iPhone Duo (466 x 678 pt) the system keeps a strip down the right edge
(84 pt) where its status pill floats, and a strip at the bottom (34 pt) for the home indicator.
Compose Multiplatform on iOS reported that side strip in `systemBars`, `safeDrawing`,
`safeContent` and `safeGestures`, and reported `displayCutout` as 0 at both sides. Search Stop
padded its floating cards with `WindowInsets.displayCutout.only(Horizontal)`, so on that screen
the padding was zero, the cards ran under the pill, and a tap on the swap and the go circle opened
Control Center instead of reaching the app.

Nothing we ran could see it. Android reports a side inset as `displayCutout`, so the Android host
tests, the Roborazzi goldens and the nightly Maestro run all composed the screen on one window
with no side inset and passed. Commit `15aa76a91` fixed Search Stop and wrote one test that
dispatches a real side inset. That test is the prototype for this page: the same idea, made
reusable, so the next screen does not need its own incident first.

---

## The profiles

Defined in `DeviceProfiles` (`core/testing/src/androidMain/.../core/testing/device/`). Sizes are
in dp on Android and pt on iOS, which Compose treats the same. `source` says where each number
came from: `measured` names the probe, `assumed` is a plain guess and is never promoted to
measured without a probe.

### Geometry

| Profile | Width x height | Density | Font scale | Platform shape |
|---|---|---:|---:|---|
| `Narrow320Font2` | 320 x 640 | 2.0 | 2.0 | `ANDROID` |
| `SmallPhone` | 360 x 740 | 3.0 | 1.0 | `ANDROID` |
| `LargePhone` | 411 x 891 | 3.5 | 1.0 | `ANDROID` |
| `PhoneLandscapeCutout` | 914 x 411 | 2.625 | 1.0 | `ANDROID` |
| `AndroidLandscapeSideNavBar` | 914 x 411 | 2.625 | 1.0 | `ANDROID_SIDE_NAVIGATION_BAR` |
| `FoldCover` | 443 x 995 | 2.4375 | 1.0 | `ANDROID` |
| `FoldInner` | 852 x 883 | 2.4375 | 1.0 | `ANDROID` |
| `TabletLandscape` | 1280 x 800 | 2.0 | 1.0 | `ANDROID` |
| `IPhoneSmall` | 375 x 667 | 2.0 | 1.0 | `IOS` |
| `IPhoneProPortrait` | 402 x 874 | 3.0 | 1.0 | `IOS` |
| `IPhoneProLandscape` | 874 x 402 | 3.0 | 1.0 | `IOS` |
| `IPhoneDuoOuter` | 466 x 678 | 3.0 | 1.0 | `IOS` |

### Insets the platform reports, by type

Left, top, right, bottom in dp or pt. A type not listed reports 0.

| Profile | `statusBars` | `navigationBars` | `displayCutout` | `systemBars` and `safeDrawing` | Source |
|---|---|---|---|---|---|
| `Narrow320Font2` | T24 | B48 | none | T24 B48 | assumed: the repo's 320 dp mockup frame, a three button bar |
| `SmallPhone` | T24 | B24 | none | T24 B24 | assumed: a gesture navigation bar |
| `LargePhone` | T32 | B24 | none | T32 B24 | size and density measured on the Pixel 6 Pro with `wm size` and `wm density` (1440 x 3120 at 560 dpi); insets assumed |
| `PhoneLandscapeCutout` | T28 | none | L46 | T28 (safeDrawing adds L46) | size and density measured on the phone emulator (1080 x 2400 at 420 dpi); left 46 is the emulator cutout (121 px, `displayCutoutSafeInsets` in its task info) and top 28 is its landscape window metrics (74 px), both from the 24 Nov 2026 QA logcat; the empty bottom is assumed |
| `AndroidLandscapeSideNavBar` | T24 | R48 | none | T24 R48 | assumed: a three button bar moved to the side in landscape |
| `FoldCover` | T28 | B24 | none | T28 B24 | size and density measured on the fold emulator (1080 x 2424 at 390 dpi); insets assumed |
| `FoldInner` | T28 | B24 | none | T28 B24 | size and density measured on the fold emulator (2076 x 2152 at 390 dpi); insets assumed |
| `TabletLandscape` | T24 | B48 | none | T24 B48 | size and density measured on the tablet emulator (2560 x 1600 at 320 dpi); insets assumed |
| `IPhoneSmall` | T20 | none | none | T20 | assumed: a status bar and no home indicator |
| `IPhoneProPortrait` | T62 | B34 | none | T62 B34 | size measured on the iPhone 18 Pro simulator (1206 x 2622 px at 3x); insets assumed because the probe ran on the Duo only |
| `IPhoneProLandscape` | none | B21 | none | L62 R62 B21 | assumed, side strips taken to arrive like the Duo's, in `systemBars` only |
| `IPhoneDuoOuter` | none | B34 | none | R84 B34 | measured: Compose inset probe on the Duo simulator (iOS 27.2), 24 Nov 2026. `safeContent`, `safeGestures` and `systemGestures` read R84 B34 too; `statusBars`, `displayCutout`, `tappableElement` and `waterfall` read 0 |

### How the shape delivers a side inset

| Shape | Top | Bottom | Left and right |
|---|---|---|---|
| `ANDROID` | `statusBars` | `navigationBars` | `displayCutout` (a camera cutout in landscape) |
| `ANDROID_SIDE_NAVIGATION_BAR` | `statusBars` | `navigationBars` | `navigationBars` |
| `IOS` | `statusBars` | `navigationBars` | the caption bar type, which `systemBars` and `safeDrawing` include and no other padding call reads; `displayCutout` is 0 |

The caption bar is how an iOS side inset is reproduced on Android: it is the one system bar type
whose only readers are `systemBars` and `safeDrawing`, which is exactly where the Duo put its 84 pt.
A screen that pads by `displayCutout`, by `statusBars` or by `navigationBars` alone leaves it
unpadded on the iPhone and on this shape. A screen that pads by `safeDrawing` horizontal, which is
`Modifier.sideSafeInsets()`, does not.

The `IOS` shape also fills `systemGestures` with the side strips and the bottom, and
`mandatorySystemGestures` with the bottom, because the Duo probe read `safeGestures` and
`safeContent` as R84 B34 too. The `ANDROID` shapes do not invent gesture insets, since none was
measured for them.

---

## What the helper gives you

All in `:core:testing`, package `xyz.ksharma.krail.core.testing.device`. Why there: every module
that has host tests already depends on it, `verifyTestingModuleUsage` already keeps it out of the
app, and the Kover floors already exclude it. It needs no theme, so it does not depend on `:taj`;
the test passes its theme in.

| Piece | Job |
|---|---|
| `DeviceProfile`, `DeviceProfiles.all` | the table above as data, with `source` |
| `forEachDeviceProfile(screen, profiles) { profile -> }` | runs the block once per profile, each on a fresh window, collects every failure, and throws one error that names each failing profile and where its numbers came from |
| `setContent(theme) { }` in the block | composes under the profile: window size, density, font scale, edge to edge, the real insets dispatched, the blockers over the system regions |
| `assertNothingInteractiveUnderSystemBars()` | walks the semantics tree; every node that is clickable, toggleable, a button or a text field must be wholly inside the safe area, measured with `getVisibleBoundsInRoot()` |
| `assertTouchReaches(node)` | a real pointer press at the node's centre through the blockers; fails if a blocker took it |
| `assertScrollEndClearsBottomBar(list)` | scrolls the list to its end; its last item must clear the bottom bar |

The tests that prove the helper can go red are `DeviceMatrixTest` in `:core:testing`: a screen that ignores every inset, one that pads by `displayCutout` only (the incident, red on exactly the two profiles that deliver the side strip elsewhere), and a list with no end padding each fail, and the fixed versions pass on every profile.

Sanity checks run on every profile, so the helper cannot pass by doing nothing: the window the
test got must be the profile's size, and the insets the composition reads back must be the
profile's.

### A device matrix test in ten lines

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class SettingsDeviceMatrixTest {
    @get:Rule val matrix = DeviceMatrixRule()

    @Test fun `every control stands inside the safe area`() =
        matrix.forEachDeviceProfile(screen = "SettingsScreen") { profile ->
            setContent(theme = { BareKrailTheme(it) }) { SettingsScreen(/* real state */) }
            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(SETTINGS_BACK))
        }
}
```

The `screen =` string is the link the adoption guard reads (see below), so it must be the
composable's name.

---

## Traps the runner already handles

Found writing `SearchStopSideInsetLayoutTest`. They are in the runner so a test author cannot hit
them; they are listed here so nobody removes them.

- **`PreviewTheme` pads for the system bars itself.** A screen inside it is handed insets that are
  already used up, so a screen that ignores insets passes. The runner never wraps in it; the test
  supplies a bare theme.
- **The window must be edge to edge and keep the inset.** `WindowCompat.setDecorFitsSystemWindows(window, false)`,
  and the insets held on the decor view, so every dispatch carries them. Otherwise the window pads
  its content, or the inset is lost after the first tap, and the test cannot fail.
- **The window size is set before the activity starts.** The runner sets the Robolectric
  qualifiers (width, height, density, orientation) and the font scale, then launches a new
  activity for each profile. Changing them under a running activity does not resize its window.
- **Bounds must be the visible ones.** `getUnclippedBoundsInRoot()` says where a node was laid out,
  not whether the rider can see it. Every check here uses `getVisibleBoundsInRoot()`.
- **Blockers are full strips.** The real pill is smaller than the 84 pt strip, so a strip is the
  stricter rule: the strip is the system's, whatever it draws in it.
- **A scroller may run under a bar along its own axis.** A list row partly under the bottom bar
  at rest is fine if the list can scroll it clear, which `assertScrollEndClearsBottomBar` checks.
  Across the scroll axis there is no such escape, and the generic check applies.
- **The keyboard is not modelled.** The root `imePadding()` stays the one keyboard authority
  (`docs/LAYOUT_AND_INSETS.md` 1.1), and a keyboard has no side.

---

## The guards that keep it honest

| Guard | Catches | Where |
|---|---|---|
| `check_layout_invariants.py`, raw cutout check | `WindowInsets.displayCutout` or its import in non test Kotlin anywhere but `Modifier.sideSafeInsets()` | CI, `fullQualityChecks.sh`, pre push |
| `DeviceMatrixAdoptionGuardTest` | a top level `XxxScreen` in a `*/ui/src/commonMain` source set with no matrix test naming it, unless it is in `config/device-matrix-baseline.txt` with a reason; and a stale baseline entry | `testAndroidHostTest` |

The baseline only shrinks (the ratchet contract in [GUARDS.md](GUARDS.md)). Adding a screen means
adding its matrix test, not a line to the baseline.

---

## What it cannot cover

- **The inner screen of the Duo** (669 x 951 pt) and the fold hinge. The simulator cannot be driven
  headless to its inner screen, and a host test has no hinge or posture change. The fold profiles
  cover the two window sizes, not the move between them.
- **A real edge swipe.** Back swipes, the home indicator swipe and Control Center are system
  gestures. The blockers model a tap that lands on a system region, not a gesture the system
  steals from the edge.
- **A future iOS, or a Compose Multiplatform upgrade, changing what is reported.** The `IOS` shape
  is what Compose reported on the Duo on 24 Nov 2026. When the Compose version moves, run the inset
  probe again and update the measured rows; the matrix tests then fail on any screen the new
  reporting breaks.
- **The pill's true extent, the status bar's drawing, Dynamic Type, map tiles.** The strip is the
  region; what the system draws inside it is not reproduced.
- **Every `assumed` row.** An assumed inset is a guess about a device nobody has probed. It
  exercises the code path (a side inset in each type), not the exact pixels.

---

## Cost

| | |
|---|---|
| App runtime | None. The helper is in `:core:testing`, which never ships. `Modifier.sideSafeInsets()` is the same `windowInsetsPadding` call every site made before, so no extra layout pass. |
| Network, storage, timers, battery | None. |
| CI | Measured, on a warm Gradle daemon. See the table below. |

CI cost is filled from a real run in the commit that adds the adoption tests.

---

## Adding or changing a profile

1. Probe the device or simulator and record the numbers (the Duo probe printed every `WindowInsets`
   type next to the UIKit safe area). Never type a number into the table because it looks right.
2. Add the member to `DeviceProfiles`, set `source` to `measured: <where and when>` or
   `assumed: <why>`, add it to `DeviceProfiles.all`, and add its rows to the two tables above in
   the same change.
3. Run every matrix test. A screen that fails on the new profile has found something real or the
   profile is wrong; settle which before touching the screen.
