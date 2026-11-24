# Layout and window insets

Rules for anything that fills space or moves for the keyboard, and the playbook for
diagnosing it when it goes wrong. Read this before changing a screen's root layout, adding
a bottom-anchored input, or touching an inset modifier.

Every rule here is a bug that shipped in this repo, and every mechanism stated here was
measured rather than recalled — one of these rules was originally written from memory and was
wrong. `docs/learning/` records how each was found; this file is the short version you act on.

---

## Part 1 — Rules that stop it happening

### 1.1 One inset authority per screen

Exactly one composable in a screen's hierarchy applies the keyboard inset. Every other
level applies none.

```kotlin
// The screen's root column. It must END where the keyboard starts.
Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) { ... }
```

Do **not** also put `imePadding()` on the input inside it. `imePadding()` on a child adds
the keyboard's height *to that child*, inside a parent that is still full height, so the
child carries a keyboard-sized void beneath it and floats that far up the screen.

If a screen renders another full-screen surface as a sibling (a cover, a sheet-like
overlay), that surface owns its own inset and must not be a child of the host's padded
box:

```kotlin
Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().imePadding()) { HomeContent() }   // host's authority
    if (coverOpen) CoverSurface()                                // its own authority
}
```

`safeDrawingPadding()` = system bars + IME + cutout. `systemBarsPadding()` deliberately
excludes the IME; only use it when something else is handling the keyboard.

### 1.2 `MainActivity` must declare `adjustResize`

```xml
<activity
    android:name=".MainActivity"
    android:windowSoftInputMode="adjustResize">
```

The app calls `enableEdgeToEdge()` and `WindowCompat.setDecorFitsSystemWindows(window, false)`,
so Compose handles the IME itself. Without this attribute the mode defaults to
`ADJUST_UNSPECIFIED`, the system resolves it to **pan**, and both mechanisms compensate for
the same keyboard: the layout shrinks by the keyboard height and the system then slides the
whole window up by it again.

`scripts/check_layout_invariants.py` fails if the attribute goes missing. It runs in CI on every
PR, in the "Layout invariants and analytics assumptions" step of
`.github/workflows/code-quality.yml`, and locally as the first step of
`scripts/fullQualityChecks.sh`.

### 1.3 Unbounded constraints: know which parents actually do it

A parent that measures a child with an **infinite** main axis makes `fillMaxHeight` /
`fillMaxSize` a silent no-op, because those check `constraints.hasBoundedHeight`. The child
wraps its content instead, and nothing warns you.

The parents that do this are `verticalScroll` (and `horizontalScroll` on the other axis),
`LazyColumn` / `LazyRow` item slots, and anything measuring with `Constraints()` defaults.

**A plain `Column` is NOT one of them.** It hands a non-weighted child the *remaining*
bounded height. Verified on this repo's setup: a non-weighted child of a fixed 800dp `Column`
holding a 100dp spacer is measured with `maxHeight=1837 bounded=true`. So `fillMaxSize()` on
a Column child fills the leftover space correctly, and swapping it for `weight(1f)` changes
nothing about the result.

Prefer `weight(1f)` anyway when a child should take the leftover space: it states the intent,
and it keeps working if a sibling is added below. But do not go hunting for this as the cause
of a misplaced child in a `Column` — it will not be.

### 1.4 Measurement order: what must survive a squeeze has to be measured first

A `Column` measures its **non-weighted children first**, against the height it was given, and
divides what is left among the weighted ones. A child measured past that bound is still laid
out, at a position past the parent's bottom edge, and a `clip()` on the parent then removes it
from the screen. The node is not off-screen and not missing: it is **inside its parent, below
its parent's edge**.

So in any container holding growable content plus controls, **the growable part is the one that
gets the weight**, not the controls:

```kotlin
Column(modifier = Modifier.clip(shape)) {
    TextField(
        // Measured after the Row below, from what is left. fill = false so a short
        // sentence still makes a short bar.
        modifier = Modifier.weight(weight = 1f, fill = false),
        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 6),
    )
    Row { /* mic, send — unweighted, so measured first and always fit */ }
}
```

Without the weight the field takes its natural height up to its line limit, and the controls
fall off the bottom the moment the container is squeezed: keyboard up, something above it, or
landscape. `AiInputBar.kt` shipped that bug; see
`docs/learning/2026-08-16-clipped-inside-its-own-parent.md`.

**Test it with `assertIsDisplayed()`, never with bounds arithmetic.**
`getUnclippedBoundsInRoot()` reports where a node was laid out, not whether any of it can be
seen — a clipped node returns perfectly legal on-screen bounds. Only a display assertion walks
the clip chain. Render the surface at the height a keyboard actually leaves, with content long
enough to overflow, then mutation-check the probe in both directions: remove the fix and watch
it go red, restore it and watch it go green. A probe that cannot fail and a probe that cannot
pass both look exactly like a probe that works.

### 1.5 One scroller per axis

A `verticalScroll` child measured by a `verticalScroll` parent gets an infinite height
constraint and throws. If the content scrolls, the wrapper must not.

### 1.6 Floating cards respect the horizontal safe area

Anything that floats over a full-bleed surface (the Search Stop box, its notice, its list, the Options card and any card or
control like them) stands inside the horizontal safe area. The surface under it, the map, still draws edge to edge; only the
cards and their controls are inset. Use the one inset the rest of the app uses for the sides:

```kotlin
Modifier.sideSafeInsets()
// taj, xyz.ksharma.krail.taj.modifier. It is
// Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
```

Not `WindowInsets.displayCutout.only(Horizontal)`: `scripts/check_layout_invariants.py` fails on it anywhere outside tests. Measured on an iOS simulator of a foldable iPhone (its outer screen, where a status pill runs
down the right edge), Compose on iOS reported `displayCutout` 0 at both sides, `statusBars` 0, and the
84 dp side safe area only in `systemBars` and `safeDrawing`. A card padded by the cutout alone ran under the pill. The pill is a
system touch target: a tap on it opened Control Center, so the swap and the go circle of the From and To card, which stood under
it, could not be pressed at all. A phone in portrait has no side inset, so nothing moves there.

The inset goes on the container of the floating group, once, and the keyboard stays with the screen's single `imePadding()`: the
keyboard has no side, so `safeDrawing` horizontal adds nothing for it and §1.1 is not broken.

**Test it with a non zero horizontal inset, never with a screen that has none.** Dispatch a real inset to the window and assert,
with `getVisibleBoundsInRoot()` (§1.4), that every card, control and notice is inside the window the inset leaves, then touch the
controls at their own coordinates with a node standing over the inset column that takes the touch, as the system does. Two
traps, both hit writing `SearchStopSideInsetLayoutTest`:

- `PreviewTheme` pads for the system bars itself, so a screen inside it is handed insets already used up and a screen that
  ignores insets passes. Use the bare theme.
- Hold the inset on the decor view, so every dispatch carries it, and make the window edge to edge
  (`WindowCompat.setDecorFitsSystemWindows(window, false)`), or the window pads its content and the test cannot fail.

The inset that matches iOS on Android is the caption bar type: no other padding call on the screen reads it. Mutation check
the probe both ways: put `displayCutout` back and the tests must go red.

That probe is now a shared helper, run on every device profile at once, so a new screen does not write its own. On iOS the side
safe area is in `systemBars`, `safeDrawing`, `safeContent` and `safeGestures`, and `displayCutout` is 0, so anything that pads
by the cutout alone is wrong on iPhone; on an Android phone in landscape the same inset arrives as `displayCutout`, and on a
three button bar in landscape it arrives as `navigationBars`. Call `forEachDeviceProfile(screen = "XxxScreen")` in a host test
and the screen is composed on all of them, with every control held out of the system regions and a real touch sent at the
ones that matter. `DeviceMatrixAdoptionGuardTest` fails the build on a `*Screen` with no such test. The profiles, which are
measured and which are assumed, and what the matrix cannot see are in
[testing/DEVICE_PROFILES.md](testing/DEVICE_PROFILES.md).

#### Sheets

A `ModalBottomSheet` is the one surface the rule above does not reach from the screen. The sheet opens its own window, which
runs edge to edge (Compose Multiplatform opens it with `usePlatformInsets = false`), Material 3 pads its content by the vertical
insets only, and every caller passes empty `contentWindowInsets`, so a sheet as wide as a narrow window drew its content under a
side strip. On the outer screen of the iPhone Duo, at full height, the Plan your trip sheet's Reset button and its next day
chevron stood under the status pill, and a tap on Reset opened Control Center. Half height was fine, because the sheet's content
stood below the pill.

The fix is one place, taj's `SheetContent`, which both platform sheets put their content in: the sheet still draws edge to edge
and its content is padded by `sideSafeInsets(clearOnEachSide = ...)`. A centred sheet is no wider than
`BottomSheetDefaults.SheetMaxWidth` (640 dp), so in a wider window it has room at each side already and only the part of an inset
past that room is padded. Without that, an Android phone in landscape (a camera cutout 46 dp on one side, a sheet 137 dp from
it) would get sheet content 46 dp narrower and off centre. The insets and the width are read in the caller's composition, before the
sheet's window opens, and passed in, because the sheet's own composition has been seen to report an inset as 0. A sheet body
does not call `sideSafeInsets()` and needs no `contentWindowInsets` for the sides. Test a body by composing it through
`SheetContent` in a matrix test; `DateTimeSelectorDeviceMatrixTest` and `ModalSheetContentDeviceMatrixTest` are the examples.

Cost: layout only. No API call, no payload, no storage, no timer and no battery change.

---

## Part 2 — Diagnosing it when it happens

### 2.1 The question that ends the argument

**Is the node laid out where you think, and is it drawn where it is laid out?**

Those are two different questions and reading the code answers neither. Measure both:

```kotlin
Modifier.onGloballyPositioned {
    log("[LAYOUT] node y=${it.positionInRoot().y} h=${it.size.height}")
}
```

Put it on every level from the screen root down to the misplaced node, plus the raw insets:

```kotlin
val density = LocalDensity.current
log(
    "[LAYOUT] insets ime=${WindowInsets.ime.getBottom(density)} " +
        "safeTop=${WindowInsets.safeDrawing.getTop(density)}",
)
```

Then take a screenshot at the same moment:

```sh
adb -s <serial> shell screencap -p /sdcard/s.png
adb -s <serial> pull /sdcard/s.png <scratch>/s.png
```

### 2.2 Reading the result

Compare the logged `y` against where the screenshot actually paints the node.

| What the numbers say | What it means | Where to look |
|---|---|---|
| Logged `y` is wrong, drawn where logged | Layout bug. The tree is measured wrong. | Constraints from the parent: a scroller or Lazy slot measuring unbounded (§1.3), wrong arrangement, a fixed height |
| Logged `y` is right, drawn somewhere else | **The window moved, not the layout.** | `windowSoftInputMode`, a parent `graphicsLayer`/offset, a `Dialog`'s own window |
| Heights don't sum to the parent's height | A child is measured against the wrong constraint | The first level where the sum breaks |
| Heights sum correctly and everything is right | You are looking at the wrong composable | Is a second copy composed? Is this the dialog branch, not the full-screen one? |

The second row is the one that costs days, because the Compose code is correct and reads
correct. A gap between measured position and drawn position can only be the window. No
amount of re-reading modifiers will show it.

### 2.3 Order of work

1. Reproduce on a device and capture a screenshot. Not a preview, not a description.
2. Instrument positions at every level. One build, all levels at once.
3. Compare logged position against drawn position **before forming any theory**.
4. Fix the level the numbers point at.
5. Rebuild, re-capture, confirm against the numbers again.
6. Strip the instrumentation.

Do not skip to step 4. Each unmeasured guess costs a full build, an install, and a round
trip with whoever is holding the phone, and a wrong guess that half-works is worse than no
fix because it moves the symptom.

### 2.4 Why the usual checks will not save you

`detekt`, unit tests and both compile targets pass on every bug in Part 1. They were all
green while the input bar sat a keyboard's height above the keyboard. Static checks verify
the code; these are defects in what the code *means* at measure and draw time, on a real
window, with a real IME.

The device QA checklist in `CLAUDE.md` is the layer that catches them. For any screen with
a text field, the keyboard-open state is a required check, not an optional one.
