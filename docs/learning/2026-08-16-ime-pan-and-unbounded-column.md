# The AI input bar floated a keyboard's height above the keyboard

**2026-08-16** · AI input surface, `MainActivity` manifest · roughly six builds, five install
cycles, four wrong fixes

## Symptom

Opening the AI input surface with the keyboard up: the input bar sat in the upper third of
the screen with a large empty gradient between it and the keyboard, and the greeting above
it was clipped off the top edge of the screen. Typing made it look roughly normal, which
made the whole thing read as a keyboard-padding problem.

## Root cause

**One bug: the window was panning.** `MainActivity` declared no `windowSoftInputMode`, so it
defaulted to `ADJUST_UNSPECIFIED` and the system resolved it to pan. The app already calls
`enableEdgeToEdge()` + `setDecorFitsSystemWindows(window, false)` and pads for the IME in
Compose, so both mechanisms compensated for the same keyboard: the column shrank by the
keyboard height, then the system slid the entire window up by it again. Fix:
`android:windowSoftInputMode="adjustResize"`.

That is why the other screens were fine: their fields sit near the top, where the pan
distance computes to about zero. The AI surface was the first screen with a field pinned to
the bottom.

### The second "root cause" that was not one

This entry originally claimed a second, stacked bug: that `Modifier.fillMaxSize()` on
`AiInputContent` was a silent no-op because `Column` measures non-weighted children with an
unbounded main axis. That was asserted from memory of the framework, never verified, and it
is **wrong**. A `Column` hands a non-weighted child the remaining *bounded* height. A
Robolectric probe settled it:

```
[PROBE] non-weighted child of fixed-height Column: maxHeight=1837 bounded=true
```

The `weight(1f)` change is equivalent to what it replaced. It is kept because it states the
intent better, but it fixed nothing.

The claim was caught by mutation-testing the unit test written to protect it: revert the call
site to `fillMaxSize()`, expect a failure, and the test passed. A test that cannot fail on the
bug it was written for is the signal that the bug was never there.

## Why it took so long

Four fixes were attempted before anything was measured. In order:

1. **`safeDrawingPadding()` on the surface, `imePadding()` removed from the parent.** Sound
   reasoning about single inset authority, and it *was* needed — but it was not the bug.
2. **Moved `imePadding()` onto the input bar itself.** Actively wrong, and instructive:
   `imePadding()` on a child adds the keyboard's height *to that child* inside a parent that
   is still full height, so the bar carried a keyboard-sized void underneath it. This
   produced a symptom nearly identical to the real one, which made it look like progress on
   the right track.
3. **Back to `safeDrawingPadding()` at surface level.** Correct, and still not sufficient,
   because the real cause was untouched.
4. **`weight(1f)`, with a confident explanation of why `fillMaxSize()` could not work.** The
   explanation was false and the change was a no-op. The screen still looked wrong.

Every one of these was reasoned from reading the code. All of them were reported to the
maintainer as fixed. The maintainer had to say "still not fixed" four times, and was the one
who eventually said to stop guessing and go read the documentation.

What made step 4 especially convincing was that it came with an explanation of framework
behaviour, delivered with confidence, that turned out to be false. A wrong mechanism that
*sounds* like deep knowledge is more expensive than admitting ignorance, because it ends the
search: it explained the symptom, so nobody looked further, and it went straight into the
docs and the memory files as fact. It survived until a mutation test contradicted it.

## What would have caught it sooner

Position instrumentation, on the first failed fix rather than the fourth. It took one build
and settled the question immediately:

```
[AI_LAYOUT] inputBar y=1031.0 h=289     ← laid out at 1031
[AI_LAYOUT] insets ime=1048 safeTop=172 ← keyboard starts at 2410-1048 = 1362
```

Laid out at 1031, ending at 1320, correctly above a keyboard starting at 1362. The
screenshot from the same moment painted it at ~363, with the greeting clipped above the
screen's top edge.

**A node measured at 1031 and drawn at 363 is not a layout bug.** Nothing inside a Compose
tree moves a node 668px away from where it was measured. Only the window does that. From
that number the manifest was the obvious and only place left to look, and it took two
minutes.

The general form: **measure whether the node is laid out where you think, and separately
whether it is drawn where it is laid out.** Those are different failures with different
causes, and reading modifiers cannot distinguish them. `onGloballyPositioned` +
`positionInRoot()` + a screenshot at the same instant answers both.

Second lesson, cheaper to state: detekt, both compile targets and the unit tests were green
throughout. They verify the code. These were defects in what the code meant at measure and
draw time, on a real window with a real IME. Only the device shows that.

Third: the fix should not have been called done four times. "Installed, please check" is
accurate; "fixed" needed evidence that did not exist yet.

Fourth, and the one with the longest tail: **do not write a mechanism into a doc until it has
been proven.** A `Layout {}` that logs the constraints it receives takes two minutes to write
and would have killed the false claim before it was documented. Every new rule in
`docs/LAYOUT_AND_INSETS.md` should be traceable to a measurement, not to recall.

## Actions taken

- [x] `docs/LAYOUT_AND_INSETS.md` — the rules and the diagnosis playbook
- [x] `scripts/check_layout_invariants.py` — fails the build if the manifest loses
      `adjustResize`, or if a file applies two inset authorities at once
- [x] Wired that script into `scripts/fullQualityChecks.sh`
- [x] Comment at the `safeDrawingPadding()` call recording why the manifest attribute must
      stay
- [x] `CLAUDE.md` points at the layout doc from the QA checklist
- [x] `AiInputContentLayoutTest` — locks the bar to the bottom of its host. Honest scope: it
      guards child ordering and anchoring, and it does **not** fail on `fillMaxSize()` vs
      `weight(1f)`, which is how the false claim above was caught
- [x] Corrected the wrong `Column` rule in `docs/LAYOUT_AND_INSETS.md` §1.3
