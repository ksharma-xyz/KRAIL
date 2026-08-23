# iOS cleared the rider's spoken sentence a beat after they watched it arrive

**2026-08-23** · **KMP / cross-platform contracts, speech-to-text** · **Cost: one report, one read of two service implementations side by side**

## Symptom

Speak into Ask KRAIL on iOS. The words appear in the field as they are recognised, exactly as
intended. Stop talking. A beat later the field empties itself. Nothing is said about why, no
banner appears, and the only way back to the sentence is to say it again.

Android does the same thing correctly with the same ViewModel, the same field and the same
state.

## Root cause

`SpeechToTextResult.Partial` and `Final` carry a `String`, and nothing said that string always
has words in it.

The ViewModel writes every transcript straight into `typedText`, which is the field the rider is
looking at. That is deliberate: partials landing in the field is what makes speaking feel like
it is being heard. A blank transcript written the same way is the rider's sentence being erased.

`AndroidSpeechToTextService` never produced one, by accident rather than by design: it reads the
recogniser's first candidate through a nullable helper, and reports a missing candidate as
`Error(NO_RESULT)` instead of a result.

`IosSpeechToTextService` passed `result.bestTranscription.formattedString` through unguarded.
`endAudio()` asks `SFSpeechRecognizer` to finish, and the result that comes back can carry an
empty transcription for the segment that was open at the time rather than a repeat of the
sentence. This service waits four seconds of quiet before calling `endAudio()`, so that is the
**ordinary** ending of a session, not a rare one.

## Why it took so long

**The type allowed it and the tests agreed with the type.** `Final(text: String)` accepts `""`
happily. Every existing speech test emitted a realistic sentence, so nothing ever asked what the
field does with a blank one. Detekt, the unit tests and both compiles were green the whole time.

**Wrong theories, in the order they looked plausible.** First: the Compose Multiplatform iOS
text-input session clobbering a programmatic `setTextAndPlaceCursorAtEnd` with a stale empty
value — plausible because the state round-trips through `TextFieldState` and back into the
ViewModel as `TypedTextChanged`, and because that round trip really is the only path that can
write an empty string into `typedText` from the UI. Second: the dialog recomposing and rebuilding
an empty `TextFieldState`, whose first `onTextChange` would report `""`. Both were theories about
Compose, and both survived reading the Compose code because the Compose code is not where the
`""` comes from.

**What ended it was reading the two service implementations against each other rather than
reading the shared caller.** The asymmetry is four lines apart in two files that no build step
compares.

## The same session, two more ways it did not end

Reported straight after the clearing bug: iOS also would not notice that the rider had finished.
Two separate causes, both in the silence watcher, neither of them the window length everyone
reaches for first.

**Audio was still being appended to a request that had been told it was closed.** The watcher
called `endAudio()` and nothing else, while the `AVAudioEngine` tap kept running underneath and
kept calling `appendAudioPCMBuffer`. `endAudio` means "that was all the audio there is", and a
recogniser is under no obligation to finish a request still being written to. `stopListening()`
had always taken the microphone away first; the watcher, which is the path that ends almost every
session, did not. Both now call the same `stopFeedingAudio()`.

**The quiet window was reset by the recogniser talking to itself.** It reset on any *changed*
transcript, and a recogniser goes on revising what it already heard for seconds after the rider
stops: capitalising a station name, swapping a homophone, re-punctuating. Each revision looked
like the rider still speaking. It now resets only when the transcript gains a **word**.

**Then, and only then, the window.** It was 4000ms, matched by hand to Android's
`COMPLETE_SILENCE_LENGTH`. Matching it was the mistake: Android sets *two* windows and the one
that fires for a trip sentence is the 3000ms `POSSIBLY_COMPLETE_SILENCE_LENGTH`, since a trip said
out loud ends on a complete-sounding phrase. iOS cannot tell the two apart, and what it measures
is not silence but the recogniser's last word *arriving*, which already lags the rider's last word.
Matching the *wrong* number therefore cost a full second on every sentence. It is now **3000ms**,
the same number as the Android window that actually fires, with the reasoning written at the
constant — including the warning that the two platforms measure different events, so the numbers
being equal is a coincidence and not a contract.

2000ms was tried first and is the floor. Below it, "from Central to…" *(thinking)* starts being cut
off, which is a rider-reported bug on the Android side already and the reason those windows were
widened rather than narrowed.

## What would have caught it sooner

- **A shared type that only allows valid values, or a documented contract on the type when it
  cannot.** This is the third time (`SpeechUnavailableReasons`, then `AiUnavailableReasons`, now
  this) that a cross-platform result type carried a value one platform produces and the other
  never does. The type is the only place both sides look.
- **Test the degenerate value, not just the realistic one.** Every speech test emitted a
  sentence. A blank, a whitespace-only string and a very long one are three tests, and one of
  them was this bug.
- **When an `actual` exists on both platforms, ask what the OTHER one does by default.** Written
  as the top lesson of [the four-in-one-day entry](2026-08-22-two-platforms-two-vocabularies.md)
  the day before, and this is the same shape again: Android's guard was incidental, iOS inherited
  the platform's raw output.

## Actions taken

- [x] `SpeechToTextResult` states the contract on the type: transcripts always carry words, and a
      session that heard nothing ends as `Error(NO_RESULT)` rather than as a result that is
      nothing.
- [x] `IosSpeechToTextService` sends the last words actually heard for a blank final, and
      `NO_RESULT` when there were none. The session bookkeeping moved into a `TranscriptWatch`
      that owns the rule, so the callback cannot forget it.
- [x] `AndroidSpeechToTextService` treats a blank candidate as no candidate, so an OEM recogniser
      returning one is answered the same way.
- [x] `AiSearchInputViewModel` refuses to write a blank transcript into the field whichever
      platform produced it, with `a blank final does not wipe the sentence the rider watched
      arrive` and `a blank partial does not wipe the words already heard`.
- [x] Failure mode #14 in `feature/trip-planner/ui/AI_SEARCH_UX.md`.
- [x] The two session rules moved out of `iosMain` into a `TranscriptWatch` in `commonMain`,
      which put them somewhere a test can reach: `TranscriptWatchTest` covers the blank partial,
      the blank final, the session that heard nothing, and — the ending bug — a revision that
      adds no words not restarting the quiet window. It takes a `TimeSource` so the window is
      tested without waiting through it.
- [x] `stopFeedingAudio()` is the one way to finish listening, so the watcher and the rider's
      stop button cannot diverge again.
- [ ] The window length itself is still a hand-tuned number with no test that could tell 2000ms
      from 3000ms. What "too soon" means is a rider mid-thought, and nothing here can measure
      that; it is a device-QA question and stays one.
