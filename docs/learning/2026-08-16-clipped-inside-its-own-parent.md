# The send button was clipped inside its own bar, and the probe that "proved" it lied twice

**2026-08-16** · Ask KRAIL input bar (`AiInputBar.kt`) · ~10 builds, several install cycles, most of a session

## Symptom

On the Ask KRAIL surface with the keyboard up and more than a couple of lines typed, the mic
and send buttons were gone. The sentence was there, the bar was there, and there was no way to
send it. Dismissing the keyboard brought both buttons back. The same thing happened in
landscape, where the surface is short for a different reason.

## Root cause

`AiInputBar`'s `Column` had one weighted child and one unweighted one, the wrong way round.

A `Column` measures its **non-weighted children first**, against the height it was given, and
divides what is left among the weighted ones. Here nothing was weighted: the `TextField` took
its natural height, growing to its `maxHeightInLines = 6`, and the action `Row` underneath was
measured after there was any room left for it. A child measured past the parent's bound is
still laid out — at a position past the parent's bottom edge — and the parent's `clip()`
removes it from the screen.

So the buttons were not off-screen and not missing. They were **inside the bar, below the bar's
own bottom edge, clipped by the bar's own rounded-corner clip.** The bar being squeezed is what
made it visible: keyboard up, a result card above it, or landscape all take height away from
the bar, and each one moved the row further past the edge.

The fix is to make the text the flexible child so the controls are measured first and reserve
their space:

```kotlin
modifier = Modifier
    .fillMaxWidth()
    .weight(weight = 1f, fill = false)
```

`fill = false` matters: with `fill = true` a one-line sentence would stretch the bar to the
full height available. With it false, a short sentence still makes a short bar, and a long one
scrolls inside the field instead of pushing the controls out. This is the shape every phone
messaging field has — the action row is pinned to the bottom of the bar and the text scrolls
within what is left.

## Why it took so long

The bug itself was diagnosed correctly on the first read. **The verification is what went
wrong, three times in a row, and each failure looked like a result.**

1. **The probe passed because the field was empty.** The test set `typedText` on the UI state
   and rendered the surface at a squeezed height. But `AiInputBar` measures the
   `TextFieldState` passed into it, not `state.typedText`, and the harness was handing it a
   fresh `rememberTextFieldState()`. The field was one line tall, nothing overflowed, and the
   test went green against a live bug.

2. **The probe passed because bounds are not visibility.** Once the field was seeded, the
   assertion compared `getUnclippedBoundsInRoot().bottom` against the host height. It passed.
   `getUnclippedBoundsInRoot` reports **where a node was laid out, not whether any of it is on
   screen** — the whole point of the bug is that the node has legal-looking bounds and is
   clipped by an ancestor anyway. Forcing the assertion to fail printed
   `mic bottom 372.1905.dp is past the host's 380.0.dp`, which is the number that should have
   failed the assertion in the first place and did not. Only `assertIsDisplayed()` walks the
   clip chain.

3. **The probe failed because it asserted on a content description no node carries.** After
   switching to `assertIsDisplayed()`, the mic passed and send failed, at every height, for
   several rounds. That reads exactly like a partially-working layout fix — mic saved, send
   still clipped — and it was reported as one. The send button's description is `"Continue"`,
   not `"Send"`. The probe had been asserting on a string that matched nothing since it was
   written; it would have failed on an empty screen.

The through-line: **each wrong result was consistent with the theory being tested**, which is
what made it stop the investigation instead of triggering it. A probe that cannot fail and a
probe that cannot pass look identical to a probe that is working, unless it is checked in both
directions.

## What would have caught it sooner

- **`assertIsDisplayed()`, never a bounds comparison, for "can the rider see this".** Bounds
  answer where a node was placed. Clipping, zero size and a covered ancestor are all invisible
  to them. There is now one probe per surface of this kind, not a bounds arithmetic helper.
- **Mutation-check a layout probe in both directions before believing it.** Remove the fix, see
  it go red; put the fix back, see it go green. Step 1 catches the probe that cannot fail; step
  2 catches the probe that cannot pass. Both failures above survived a one-directional check.
- **Resolve a finder against the tree, not against memory.** A `contentDescription` a test
  asserts on is a string literal in two files that nothing ties together. When a node assertion
  fails, print the semantics tree before theorising about layout: `onRoot().printToLog()` would
  have ended round three immediately.
- **Feed the component the state it actually measures.** Test harnesses that hand a
  `TextFieldState` in must seed it from the same text the UI state claims; the two are separate
  sources and only one of them affects layout.

## Actions taken

- [x] `AiInputBar`'s `TextField` takes `weight(1f, fill = false)`, with the reasoning in a
      comment at the call site pointing here.
- [x] `AiInputBarLayoutTest.shortHost_theBarsActionsStayInsideIt` renders the bar at the height
      a keyboard leaves, with six lines of text, and asserts both controls with
      `assertIsDisplayed()`. Mutation-checked: removing the weight fails it, restoring it passes.
      It lived on the result card's own test class first and went with it when the card was
      deleted; a probe outliving the composable it was written beside is exactly the kind of
      loss nothing warns you about, so it is now a class of its own named after what it guards.
- [x] The send button's real content description is named in the test's companion, with why it
      is worth naming.
- [x] Rule added to `docs/LAYOUT_AND_INSETS.md`: measurement order inside a `Column`, and
      `assertIsDisplayed` over bounds.
