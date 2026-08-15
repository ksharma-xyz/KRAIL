# AI search input — rules, failure modes, and what is actually tested

Covers the "Where are you going?" surface (`components/ai/`) and the pipeline behind it
(`search/ai/`). Read this before changing either.

## The one rule that must not bend

**The model is used to look stops up. Nothing it produces is ever shown to a rider.**

What reaches the screen is a stop from our own database, or words written in this repo. The
model's `originText` / `destinationText` go into `StopTextResolver` and no further.

There is exactly one place model output comes close to the screen: the failure message quotes
the place that matched no stop ("No stop called *Hogwarts*"). That quote is only rendered when
those exact characters appear in what the rider typed, so it is their words being repeated,
never the model's. A reworded or invented name inside quote marks would read as something the
rider said.

Enforced by `a place the model invented is never quoted back at the rider`. If that test is
deleted, this rule is gone with it.

## Failure modes

Every way this can fail, what the rider gets, and whether a test holds it in place.

| # | Failure | Rider sees | Test |
|---|---|---|---|
| 1 | Feature flag off | **No way in.** The wheel is not drawn | `with the flag off there is no way in`, `with the flag on the way in is there and opens`, `starting over keeps the way in`, plus `submit while flag is off does nothing` |
| 2 | Model not downloaded, or downloading | "still downloading… try again in a moment" | `model downloadable lands in DOWNLOADING`, `model downloading lands in DOWNLOADING too` |
| 3 | Device unsupported | Generic unresolved message | `unsupported device lands in UNRESOLVED, not DOWNLOADING` — reads as a parse failure rather than "not available here" |
| 4 | Model returns nothing usable | "did not come through, have another go" | `failed extraction resolves to UNRESOLVED`, `a model that gives nothing back is its own kind of failure` |
| 5 | Parsed, no place named | Example stop offered | `a sentence with no place in it says so`, `a sentence about nothing does not fill the origin with wherever the rider is` |
| 6 | Place named, no stop matches | Name quoted back when the rider wrote it | `a named place that matches no stop is quoted back` |
| 7 | Place named, matches the **wrong** stop | Field filled, silently. No failure shown | Partly: `an unlabelled word does not become a stop that merely contains it` plus `StopTextResolverTest`. Nothing can test that a confident match is *correct* |
| 8 | Only one end resolves | That field fills, the other is left | `one stop resolving is still a result` |
| 9 | Origin unstated, destination found, GPS off or denied | From left empty | `nearby-stop resolver failure leaves origin unresolved, not a crash` |
| 10 | Microphone denied, blocked, unsupported, or the recogniser errors | A different message for each; a blocked mic opens Settings | `speech unavailable surfaces the reason`, `a recogniser error is reported as itself` |
| 11 | Recogniser never reports an end | Stops itself at 20s | `listening stops itself once it hits the ceiling` |
| 12 | Time understood ("by 6pm") | Shown on the search row, and the timetable opens with it | `resolves a time intent into the confirm state`, plus three in `TimeTableViewModelTest` covering the route carrying it, no time meaning now, and unreadable JSON falling back to now |
| 13 | Dismissed mid-flight | Mic stopped, draft discarded | `closing the box while listening stops the mic`, `closing the box throws the draft away` |

Both of the open defects recorded here are now fixed.

**#1.** The flag now reaches the state as `isFeatureEnabled`, the row does not draw the wheel
without it, and `OpenInput` refuses as well as the button being absent — a caller that has not
been told cannot open a sheet whose only action would be inert.

**#12.** The parse was never the problem: `AiTripIntentTimeResolver` has always produced a
complete `DateTimeSelectionItem`. There was nowhere to put it. The date/time picker existed
only on the timetable, so a time understood while the rider was still choosing stops had no
home, by any route, AI or not. It now lands in `SavedTripsState`, shows on the search row as a
chip built from the same `SubtleButton` and the same `toDateTimeText()` the timetable uses, and
travels to the timetable on `TimeTableRoute`.

On the route rather than handed over after navigation, for two reasons. The back stack is
serialised, so a rider whose app is killed in the background comes back to the time they chose
instead of to now. And `TimeTableViewModel.dateTimeSelectionItem` is a plain `var` outside
`uiState`, which survives rotation but not process death — the route is what makes it durable
without moving that field.

### Still to do on the time path

- The chip clears on tap. It should open the existing date/time picker, which wants
  `DateTimeSelectorRoute` reachable from the home screen.
- The grammar reads clock times and relative minutes only. Days ("tomorrow", "tonight",
  "Friday") are pure date arithmetic on a well-tested seam and are the obvious next addition.
- Vague day-parts stay unresolved on purpose. "morning = 9am" is invented precision. If they
  are ever handled, the phrase should reach the chip unresolved and open the picker at roughly
  that time, so the rider supplies the precision and the app only supplies the shortcut.

## Parked, deliberately

**Offer a choice when confidence is low.** #7 is the only failure with no visible failure, and
the honest answer to a weak match is to ask rather than commit: two or three candidates with
the reason each was suggested (your label, a saved trip, a name that looks similar), and no
auto-fill below the bar. `FuzzyStopRanker` already computes the score this needs and discards
it at the interface boundary, so the groundwork is a scored result type rather than new
matching. Not being built today; this note is the record of why it should be.
