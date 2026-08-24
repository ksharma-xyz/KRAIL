# AI search input — rules, failure modes, and what is actually tested

Covers the pipeline behind the Ask KRAIL surface (`search/ai/`): extraction, resolution, and
every way both of them fail. Read this before changing what a sentence turns into.

What the screen SAYS and how it is laid out is a separate concern and lives in
`ASK_KRAIL_UX.md` — the suggestion line, the label vocabulary, the speech rules and the layout
invariants are all there. The surface was called "Where are you going?" when this file was
written; it is `AskKrailScreen` now.

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
| 1 | Feature flag off | **No way in.** The mic is not drawn | `with the flag off there is no way in`, `with the flag on the way in is there and opens`, `starting over keeps the way in`, plus `submit while flag is off does nothing` |
| 2 | Model not downloaded, or downloading | "still downloading… try again in a moment" | `model downloadable lands in DOWNLOADING`, `model downloading lands in DOWNLOADING too` |
| 3 | Device unsupported | Generic unresolved message | `unsupported device lands in UNRESOLVED, not DOWNLOADING` — reads as a parse failure rather than "not available here" |
| 4 | Model returns nothing usable | "did not come through, have another go" | `failed extraction resolves to UNRESOLVED`, `a model that gives nothing back is its own kind of failure` |
| 5 | Parsed, no place named | Example stop offered | `a sentence with no place in it says so`, `a sentence about nothing does not fill the origin with wherever the rider is` |
| 6 | Place named, no stop matches | Name quoted back when the rider wrote it | `a named place that matches no stop is quoted back` |
| 7 | Place named, matches the **wrong** stop | Field filled, silently. No failure shown | Partly: `an unlabelled word does not become a stop that merely contains it` plus `StopTextResolverTest`. Nothing can test that a confident match is *correct* |
| 8 | Only one end resolves | That field fills, the other is left | `one stop resolving is still a result` |
| 9 | Origin unstated, destination found, GPS off or denied | From left empty | `nearby-stop resolver failure leaves origin unresolved, not a crash` |
| 10 | Microphone denied, blocked, unsupported, or the recogniser errors | A different message for each; a blocked mic opens Settings | `speech unavailable surfaces the reason`, `a recogniser error is reported as itself` |
| 11 | Recogniser never reports an end | Stops itself at 10s, or 15s if words were still arriving. This is a backstop: every path ends an ordinary session on its own. See "How each platform decides you stopped" below | `listening stops itself once it hits the ceiling`, `a rider still speaking at the ceiling is given longer`, `words that stopped before the ceiling do not earn the extension` |
| 12 | Time understood ("by 6pm") | Shown on the search row, and the timetable opens with it | `resolves a time intent into the confirm state`, plus three in `TimeTableViewModelTest` covering the route carrying it, no time meaning now, and unreadable JSON falling back to now |
| 13 | Dismissed mid-flight | Mic stopped, draft discarded | `closing the box while listening stops the mic`, `closing the box throws the draft away` |
| 14 | Recogniser reports a blank transcript | Nothing. The words already heard stay in the field; a session that heard nothing at all ends as a recogniser error | `a blank final does not wipe the sentence the rider watched arrive`, `a blank partial does not wipe the words already heard` |

### How the From stop is decided

The rider names one end far more often than two ("get me home", "I want to go to Bondi"), so
the other end is worked out. The ladder is ordered by how much each signal knows about **right
now**, and a guess never overrules something known.

| | Signal | Knows | Where |
|---|---|---|---|
| 1 | What the rider said | Their intent, certainly | `StopTextResolver` chain, via `resolveTripOrigin` |
| 2 | A labelled stop within walking distance | Their position, certainly. The label is the platform they actually use, where the nearest stop is often a shelter nobody uses | `LabelledStopLocator.nearestLabelledStop` |
| 3 | The nearest stop | Their position | `NearbyStopsRepository` |
| 4 | **Home, if set** | Only their habit | `LabelledStopLocator.homeStop` |
| 5 | Nothing. Field left blank | | |

**Location always beats Home.** A trip starts where you are: a rider at work asking for the
airport is served correctly by position and wrongly by habit. Home is reached only when there is
no location at all, which is the same null whether permission was denied, restricted, or the fix
timed out.

**Two cases deliberately stay blank rather than reaching for Home.** Standing at the destination,
and a known location with no stop near it. Both are things the app knows about the rider right
now, and a guess must not contradict them.

**Home is the only label used this way**, because with no location there is no distance by which
to rank any other. It is also the one label the app treats as permanent: it cannot be renamed,
only reassigned or cleared, which is what makes keying behaviour on it safe. One spelling of it,
in `HOME_STOP_LABEL`.

Filling this in is answering a question the rider asked, not inventing one: it happens only when
a destination was actually understood. A sentence that resolved to no places takes no fallback
at all, which is failure mode 5.

Every branch logs under `[AI_ORIGIN]`, including why a location was unavailable, because all of
them otherwise arrive as the same blank field with nothing to tell them apart.

### How each platform decides you stopped

Three code paths, and they do not measure the same thing. The numbers being equal is a
coincidence worth not relying on.

| Path | What it measures | Window | Where |
|---|---|---|---|
| Android | silence, inside the OS recogniser | 3s after a complete-sounding phrase, 4s otherwise | `AndroidSpeechToTextService` intent extras |
| iOS 26 | silence, from Apple's `SpeechDetector`, in audio time | 3s | `SpeechActivityWatch` |
| iOS below 26 | the transcript gaining a **word**, which lags speech by however long recognition takes | 3s | `TranscriptWatch` |

The third is the weakest and is the fallback for exactly that reason:
`SFSpeechAudioBufferRecognitionRequest` never decides a speaker has finished, so the end of a
sentence has to be inferred from how text arrives. iOS 26's `SpeechAnalyzer` supplies the real
signal, which is why the app prefers it wherever it exists (`PreferredSpeechToTextService`).
`core/speech-to-text/README.md` has the full comparison.

**#14 was iOS only, and rider-facing.** `endAudio` asks `SFSpeechRecognizer` to finish, and the
result that comes back can carry an empty transcription for the segment that was open rather
than a repeat of the sentence — the same shape a pause mid-sentence produces, and this service
waits seconds of quiet before finishing, so it was the ordinary ending rather than a rare one. Written through, it cleared the rider's sentence a beat after they watched it arrive.
Android never reached it because its recogniser reports a missing result as an error instead.
Held in three places now: `SpeechToTextResult` says transcripts always carry words,
`IosSpeechToTextService` sends the last words heard instead of a blank final (and
`NO_RESULT` when there were none), and the ViewModel refuses to write a blank transcript into
the field whichever platform produced it.

Both of the open defects recorded here are now fixed.

**#1.** The flag now reaches the state as `isFeatureEnabled`, the row does not draw the mic
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

### What the time grammar reads

Clock times ("9am", "6:30pm"), relative offsets ("in 20 minutes", "in 2 hours"), and named
days: "today", "tonight", "tomorrow", and weekday names including the short forms riders
actually write ("fri", "tues"). A named day beats the next-occurrence rollover, because a
rider who said Friday meant Friday and guessing past their own word overrules them. Day
matching is on word boundaries so "sun" is not read out of "sunset".

Both platform prompts are told to keep the day word in `timeText`. They do not always: "10am
Monday work" came back with Monday swallowed into the place, leaving "10am", which had already
passed and rolled to tomorrow. So the day is looked for in the **rider's whole sentence** as
well, and the prompt is now an optimisation rather than the thing correctness rests on. The
extracted phrase still wins when both name a day, being the more precise signal.

The general lesson: anything a prompt is asked to preserve should have a deterministic path
that does not need the prompt to have worked.

### Still to do on the time path

- The chip clears on tap. It should open the existing date/time picker, which today is a
  `ModalBottomSheet` owned by `TimeTableEntry` and has no way to be opened from the home
  screen.
- A day on its own still resolves to nothing. A date with no time is not a departure, and
  picking an hour for it would be a guess.
- Vague day-parts stay unresolved on purpose. "morning = 9am" is invented precision. If they
  are ever handled, the phrase should reach the chip unresolved and open the picker at roughly
  that time, so the rider supplies the precision and the app only supplies the shortcut.

## Parked, deliberately

**Offer a choice when confidence is low.** For a match that is not clearly right, ask rather
than commit: two or three candidates with the reason each was suggested (your label, a saved
trip, a name that looks similar).

This note used to say the groundwork was exposing `FuzzyStopRanker`'s score, which it computes
and discards at the resolver boundary. **That was wrong**, and it is written down here so the
same reasoning does not produce the same plan again:

1. **The score is not the boundary.** `RealStopResultsManager` returns exact SQL matches first
   and only falls back to the fuzzy ranker when there are few of them. The results this path
   picks from are usually not scored at all, so there is no score to expose for them.
2. **The score does not encode the safety property.** `StopSearchTextResolver`'s word-boundary
   guard is not a duplicate of the ranker. The ranker scores "work" against *70 Powderworks Rd*
   at ~1.0, through the longest-common-substring signal it uses on purpose so a rider reading a
   list gets loose suggestions. Replacing the guard with a score threshold reintroduces exactly
   the bug the guard exists for.
3. **The score does not encode priority.** For "central", *Central Station* and *Central Ave*
   score identically. What puts the station first is `prioritiseByRelevance`, which knows about
   major interchanges. Picking by score would be worse than picking by the order the manager
   already returns.

So the ranker's score is not the missing piece, and the guard should stay. A real version of
this needs a genuine ambiguity signal, and the ordering the manager already produces is a
better starting point than any number the ranker computes.

Worth noting the failure is also milder than "silent": the sheet writes into the visible From
and To fields and stops there, so a rider sees the wrong stop before pressing Search. Loading
the timetable was deliberately left as their action.
