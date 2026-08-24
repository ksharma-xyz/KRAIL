# Ask KRAIL — the surface, the suggestion, and the rules behind both

Covers `components/ai/`: the screen a rider types or speaks a whole journey into, and the one
line above the field that shows them it is possible.

The pipeline that turns their sentence into stops is a separate concern and lives in
`AI_SEARCH_UX.md`, including the rule that nothing the model produces is ever displayed. Read
that one before changing extraction or resolution; read this one before changing what the
screen says or looks like.

Every rule here was a defect first. The reasoning is recorded because most of these look
arbitrary from the code alone, and several of them were re-broken during the same week they
were fixed.

---

## 1. What is on the screen, and why each thing is there

Three pieces of text, doing three different jobs. That distinction is load-bearing: an earlier
version had all three saying a version of "ask KRAIL", which read as one instruction repeated
until the rider felt shouted at.

| Piece | Says | Job |
|---|---|---|
| Title bar | `Ask KRAIL` | Names the screen, like every other screen in the app |
| Suggestion | `Try "Home to Work by 9am"` | Shows what a sentence this surface understands looks like |
| Field placeholder | `Where to, and when?` | Says what KIND of thing goes in the box |

**The placeholder is not the suggestion.** They were the same string once, and the personalised
example was ghost text that vanished on the first keystroke — exactly when a rider is still
reading it. The suggestion moved above the field and the placeholder went static.

### The suggestion is a demonstration, not a prediction

It is framed `Try "…"` in quotes deliberately. Unframed, the same line reads as the app guessing
today's journey: mildly useful when right, broken-looking when wrong. Framed, a wrong guess is
just an example, and the line's real job — showing that a whole trip and a time go in one
sentence — survives being wrong about the destination.

---

## 2. Which journey the suggestion names

Two dimensions, kept apart because they vary independently.

**When it applies, and what shape it takes** is a table in `AiSuggestionSituations.kt`, ordered,
first match wins. Adding a case is one row.

**Which two stops** is a fallback ladder in `AiGreeting.kt`, and it runs the same way at every
hour, which is why it is not in the table.

### The ladder

1. **The rider's labels, in their own words** — `Home to Work`. This is the only place in the
   app that shows typing "work" resolves to their stop. Nobody discovers that alone.
2. **A saved trip**, as station names with the `" Station"` suffix trimmed.
3. **Home-bound** — `Get me home by 9pm`.
4. **Two known stations** — first launch only, where anything personal would be invented.

Rung 3 is the one that matters, and it exists because of a real bug. A rider whose only data is
their commute had their labels disqualified by the weekend rule AND their saved trip
disqualified by the same rule, and fell straight to rung 4. Every Saturday and Sunday showed
`Central to Parramatta`: two stations they have nothing to do with.

**Falling back to the commute instead would have undone the weekend rule.** That was tried and a
test caught it suggesting `Home to Work` on a Saturday. Home is the answer that uses what the
rider has without resurrecting what the day excluded.

### Home-bound earns its place three times over

`Get me home by 9pm` is short (never trips the length cap), true whether or not the rider is
actually out, and it is **the only line that shows the origin can be left out entirely**. The
nearby-stop resolver has always supported an unstated origin; nothing on screen ever
demonstrated it.

It is only ever used when a home stop is genuinely pinned. There is no promising a home that is
not set.

---

## 3. The weekend rule, and its one exception

Commute labels (`work`, `uni`, `school`, and every synonym of those — see §5) are excluded at
weekends. Suggesting the trip to work on a Saturday is the app saying out loud that it has not
been paying attention.

The exclusion follows **stop IDs, not label names**. Names alone left a hole: a rider whose only
saved trip IS the trip to work saw exactly that suggested on a Sunday, because the label rung
was skipped and the saved-trip rung had no opinion about the day.

**The exception: Sunday from 15:00.** `Home to Work by 9am tomorrow`. That is the one weekend
moment when the commute is what somebody is actually thinking about. It must say *tomorrow* or
it reads as a trip being suggested for tonight.

---

## 4. Every suggestion carries a time, and none of them point backwards

The time is the only thing this surface understands that the ordinary search row does not. A
rider who never sees it demonstrated has no reason to believe they can type it.

So every row carries a clause, and **each clause has to still be true at every hour in its
band**:

| Row | Clause |
|---|---|
| Weekday 5–9 | `by 9am` |
| Weekday 9–15 | `in 20 minutes` |
| Weekday 15–18 | `after 6pm` |
| Weekday 18–21 | `by 9pm` |
| Sunday 15–23 | `by 9am tomorrow` |
| Weekend 5–15 | `in 20 minutes` |
| Weekend 15–21 | `by 9pm` |
| Any day 21–5 | `in 20 minutes` |

The `weekday-morning` row originally ran to hour 10 while offering `by 9am`, so a rider opening
the screen at 09:30 was told to try arriving by 9am — a trip into the past. A test now asserts
no absolute clause appears in a band that outruns it, with `tomorrow` exempt for the obvious
reason.

Relative clauses (`in 20 minutes`) are correct at any hour, which is why the late bands use
them.

---

## 5. A rider's own word for a place

Labels are matched **exactly** — case- and space-insensitive, but whole-word. That is what stops
"home" capturing "Homebush".

Exact does not mean the app's vocabulary only. `LabelSynonyms` groups `work/office/job/
workplace`, `home/house`, `uni/university/campus/college`. A rider whose label is `Work` says
"office by Monday morning" and means it. Every comparison is still whole-word exact; only the
vocabulary widened.

**Two passes, and the order matters.** Literal first, synonym second: a rider with both a `Work`
and an `Office` label has named two different stops, and "office" has to reach the one they
called Office.

The groups are deliberately small. A synonym that is nearly right is worse than none — it sends
a rider to a stop they did not name and gives them no way to see why. "Place", "mine" and "the
city" are all excluded for that reason.

The weekend commute list is derived from these same groups, so an `Office` label counts as a
commute exactly as `Work` does. Two lists would have drifted the moment one gained a word.

---

## 6. Speaking

- **Speech never submits.** It fills the field and stops. The recogniser deciding it has heard a
  full sentence is not the rider deciding they have finished saying one, and a mis-heard word
  was already on its way to a search before they could look at it. Send stays theirs to press.
- **Partial transcripts go into `typedText`**, not just `speechTranscript`. The field renders
  `typedText`; writing only the transcript meant a rider watched an empty box while they talked
  and everything appeared at once when they stopped.
- **Speaking adds to the field, it does not replace it.** Whatever is already there when the mic
  is tapped is kept, and the spoken words are joined onto it (`joinSpokenText`). Replacing meant
  a rider who typed half a sentence and then tapped the mic watched their own words vanish, and
  the field is the only copy. Every mic that lives inside a text field works this way, from the
  iOS keyboard's own dictation to Gboard's, because a mic beside a keyboard is another way to
  type rather than a different way to ask. Re-tapping to redo a mis-heard sentence therefore
  gives the sentence twice: the worse reading of that tap, but the better failure, because it is
  on screen and one gesture to clear where lost typing is silent.
- **A blank transcript is never written in.** A recogniser reporting one is saying it has nothing
  to add, not that the rider unsaid what they said. See failure mode 14 in `AI_SEARCH_UX.md`.
- **Listening stops itself** at 10 seconds, extended to 15 if words were still arriving in the
  last 4. The recogniser does not always report an end, and a mic that stays open with no way
  out is worse than one that closes early. The rider's stop button is the real control; the
  ceiling only decides when to stop waiting for someone whose recogniser has not noticed they
  finished.
- **No invented time.** A sentence with no time in it produces no `DateTimeSelectionItem`, so no
  chip appears on the search row. It used to default to now, and the rider saw a departure time
  they never asked for.

---

## 7. The surface is a dialog, and it closes itself only on success

`AskKrailScreen` is a centred dialog on every device, wearing the theme's AI pair as its own
border: a quiet ring at rest, brought to full strength and spun while a sentence is worked
out (`rememberWorkingBorder` drives both the dialog frame and, on the full-screen fallback,
the input bar — never both at once, see `AiInputBar.showWorkingBorder`). The one exception is
font scales past `ACTIONS_STACK_SCALE`, where the full screen comes back because a floating
card taller than the screen is worse than the screen.

**A resolve is a handoff.** The stops and the time are written into the home row on the
RESOLVED emission (`SavedTripsEntry`), the dialog stays up for one settle beat
(`closeAfterHandoff`, 1.5s) so the border can finish, then closes itself onto the row it
filled. The row's mic ring turns once as it lands (`isAiHandoffSettling`), so the eye follows
the answer. There is no result card: the row IS the result, and a copy of it inside the
dialog was a second thing to keep in step with the first.

Rules that hold this together:

- **Failures never close the dialog.** DOWNLOADING, every UNRESOLVED reason and every speech
  problem keep the sentence in the field with a message; only success or the rider's own
  dismiss takes it down.
- **Rewording during the settle beat cancels the close.** Typing steps RESOLVED down to IDLE,
  so the timed close cannot pull the dialog out from under an edit.
- **Reopening is a fresh prompt.** `OpenInput` resets the state; the last answer belongs to
  the row now, and showing it back would be a stale copy of a row the rider may have edited.
- **A handoff is consumed, not observed.** The row's writes fire only while
  `isHandoffActionable` (phase RESOLVED with a resolve present), and the phase steps down to
  IDLE in the same emission that closes the dialog. The writing effect re-launches whenever
  the home entry recomposes — back-navigation from the stop-search screen included — and a
  state left at RESOLVED replayed the AI's stops over the rider's own later picks. Pinned by
  `the row-write gate closes with the dialog`.

## 8. Layout rules that are not negotiable

Several of these describe the full-screen surface, which since the dialog change is only the
large-font-scale fallback — they still hold there, and the inset and IME rules are why the
fallback cannot be deleted casually.

- **The field never moves.** The stage above it is a fixed-height slot, so a greeting changing
  length or a rider starting to speak cannot nudge the input under their thumb.
- **One inset authority**, and it is the surface's own column. This only works because
  `MainActivity` declares `android:windowSoftInputMode="adjustResize"` — see
  `docs/LAYOUT_AND_INSETS.md`, and do not remove that attribute.
- **The cloud field is mirrored and ime-padded**, so the light rises with the input bar instead
  of sitting behind the keyboard.
- **The bar is opaque, and a different shade per theme for opposite reasons.** Light mode is
  plain `surface` (white), because the moving colour behind it is what gives it an edge. Dark
  mode darkens, because `surface` there is already near black and the field's light passes
  through anything not sitting below it. A translucent tint fails in both: it takes its
  appearance from whatever happens to be behind it, and behind this is a gradient that moves.
- **`ImeAction.Default`, never `Search`.** Search makes the IME replace its enter key, so a
  rider writing more than one line has no way to start one.
- **The way in is a mic, not the wheel.** `AiMicButton` keeps the glyph and the fill in the
  theme's own content and surface colours and puts the AI gradient in a ring around the button's
  edge, so the control reads as a mic first and an AI surface second. The wheel (`AiWheelMark`)
  is KRAIL's mark for *on-device AI produced this*, which is right on the alert-summary card and
  wrong on a door: on the way in it named the technology to a rider looking for a way to say
  where they are going. The ring is drawn at rest and turns for one beat on handoff, which is
  the same motion the wheel had.
- **The way-in slot is never empty.** That mic is the top of a two-button column in
  `SearchStopRow`, with Search below it. Where `isWayInAvailable` is false the slot holds the
  reverse-stops button instead, so Search does not rise into the mic's place. Two devices
  differing only in a capability the rider never chose should not have differently shaped rows,
  and the button that commits the trip should not move next to a different field.
- **No second speak control.** A worded `Speak` button used to appear below the bar at large
  font scales, from when the actions were a row underneath the field. Once mic and send folded
  into the bar it became a second way to do the same thing directly under the first.

---

## 9. Not built yet

Recorded so the reasoning is not lost, not as a commitment.

- **Prompt to set a missing label.** A rider who says "office" with no Work stop pinned gets a
  generic failure. Better: name what is missing and offer to set it, but only when the unmatched
  word is a label word AND no stop is pinned for that group. If the action navigates, the typed
  sentence has to survive it — `SavedStateHandle`, not just the ViewModel, or process death
  while they are on the labels screen loses it. Opening the editor as a sheet over this surface
  avoids the problem instead of solving it. Do not auto-resubmit on return.
- **Location-aware origin.** "Get me home" while standing near a labelled stop should start from
  that label, not from whatever bus stop is nearest. A labelled stop is a place the rider
  deliberately named, which is stronger evidence than proximity. `NswStops` holds the
  coordinates and the lookup is one indexed read. Needs three guards: never origin == destination,
  ignore fixes too inaccurate for the radius, and never prompt for permission from this screen.
- **Labels as segmentation hints in the prompt.** The model has no idea "office" is a place-word
  for this rider, which is why "10am Monday work" put Monday inside the place. Passing the
  labels as hints fixes boundaries without granting the model any authority over stop identity.

---

## 10. What is actually tested

| Area | Held by |
|---|---|
| Situation table completeness, ordering, clause validity | `AiSuggestionSituationsTest` (10) |
| The ladder, weekend rules, home-bound, length cap | `AiGreetingTest` (17) |
| Synonym groups, and that Homebush still is not home | `LabelSynonymsTest` (8) |
| Label resolution including literal-beats-synonym | `StopLabelTextResolverTest` |
| Speech: no auto-submit, partials, ceiling, extension | `AiSearchInputViewModelTest` |
| The bar staying at the bottom of its host | `AiInputContentLayoutTest` |

`AiInputContentLayoutTest`'s KDoc states its scope honestly, including what it does **not**
catch. It does not distinguish `weight(1f)` from `fillMaxSize()` at the call site — that was
checked by mutation, and the result disproved a diagnosis that had already been written up as
fact. See `docs/learning/2026-08-16-ime-pan-and-unbounded-column.md`.
