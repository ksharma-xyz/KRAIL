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
