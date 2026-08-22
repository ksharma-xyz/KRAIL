# The same platform-divergence bug, found four times in one day

**2026-08-22** · **KMP / expect-actual contracts** · **Cost: four separate fixes, one rider-facing message that was wrong for months**

## Symptom

Four iOS bugs reported together, all in Ask KRAIL. They looked unrelated:

1. The dialog could not be dismissed by tapping outside it.
2. Listening ran for ten to fifteen seconds after the rider stopped talking; Android took four.
3. Some riders could not use the feature at all, and were told to reword their sentence.
4. "lets go to work" filled both From and To with the rider's Work stop.

Every one of them worked correctly on Android.

## Root cause

One shape, four times: **a contract that both platforms are supposed to honour, where only one
of them actually does, and nothing fails when the other does not.**

1. **Dismiss.** Compose Multiplatform decides "outside" geometrically:
   `layer.boundsInWindow` is set to the measured size of the dialog's content, and only
   pointers landing outside that rectangle reach the dismiss listener. The content filled the
   window, so nothing was ever outside. Android survived on `dismissOnBackPress`; iOS has no
   back press, so the card had no exit at all.

2. **Silence.** `SFSpeechAudioBufferRecognitionRequest` never decides the rider has finished.
   `isFinal` arrives only after `endAudio()`, and nothing called it. Android's recogniser ends
   itself on silence. The ViewModel ceiling, written as a backstop "for a recogniser that never
   reports an end", had quietly become the only thing ending any iOS session.

3. **Availability reasons.** Android reported ML Kit's words (`downloadable`, `downloading`,
   `unsupported_device`); iOS reported Swift's string interpolation of an enum
   (`modelNotReady`, `appleIntelligenceNotEnabled`, `deviceNotEligible`). Both callers tested
   for the Android spellings only. On iOS nothing ever matched, so a model that was merely
   still downloading was reported as a permanent failure, and the banner told riders on a
   phone that cannot run the model to rename the places in their sentence. Unanswerable advice,
   repeatable forever.

4. **Prompt.** The Android extraction prompt lists `"let's go to X"` in its lone-place rule and
   carries three worked examples. The iOS prompt was a shortened paraphrase with neither, and
   nothing telling the model not to put one place in both fields.

Two more of the same shape turned up while fixing these: iOS left
`requiresOnDeviceRecognition` unset, so every spoken trip was **streamed to Apple's servers**
while Android's `EXTRA_PREFER_OFFLINE` kept it local; and no `testTag` inside any
`ModalBottomSheet` was ever published as a resource id
(see [a lane that never ran](2026-08-22-a-lane-that-never-ran.md)).

## Why it took so long

**Nothing fails when a platform quietly disagrees.** Every case compiles, passes detekt, and
passes unit tests. The Kotlin type system enforces that an `actual` exists, never that it
behaves the same. `SpeechUnavailableReasons` already existed as a shared vocabulary for exactly
this reason, and `AiAvailability` still went out with free-form strings on both sides.

**The tests asserted the phase, never the message.** `unsupported device lands in UNRESOLVED,
not DOWNLOADING` was green the entire time riders were reading the wrong sentence, because
UNRESOLVED was the correct phase. The reason field, which is what the banner renders, was never
asserted.

**Matching the numbers hid a mismatched mechanism.** The silence windows were made identical
across platforms, 4000ms on both. It still felt wrong, because the constants matched while the
pipeline underneath did not: Android measured silence on device, iOS measured transcript change
against a server round trip. **Two platforms agreeing on a constant is not the same as agreeing
on behaviour**, and the matching numbers made the remaining difference harder to see, not
easier.

**The prompts were the same "in spirit".** Nothing compares them, so they drifted at exactly
the phrase the rider used.

## What would have caught it sooner

- **A shared constants object per cross-platform contract, always.** If both sides return a
  string a caller branches on, that string belongs in `commonMain` as named constants. This is
  now the second time (`SpeechUnavailableReasons` was the first) and it should be the last.
- **Assert what the rider reads, not what the state machine holds.** A phase test passes while
  the message is wrong. Test the message.
- **When an `actual` exists on both platforms, ask what the OTHER one does by default.** Three
  of these are an iOS default nobody chose: `requiresOnDeviceRecognition = false`,
  `taskHint = .unspecified`, no silence detection. Android set an explicit flag; iOS inherited
  a different one.
- **A prompt is advice, never a contract.** `SinglePlaceIntent.kt` already said so in a comment,
  from an earlier version of this same bug. The guard against a stop resolving to itself is
  deterministic code with a test; the prompt fix is the other half, not the fix.

## Actions taken

- [x] `AiUnavailableReasons` in `core/ai-text` commonMain, modelled on
      `SpeechUnavailableReasons`. Both platforms map onto it; `AlertSummaryViewModel`, which
      had the identical hole, reads it too.
- [x] `UnresolvedReason.MODEL_UNAVAILABLE` and `MODEL_NEEDS_SETTING`, with messages naming
      something the rider can act on, plus tests that assert the **reason**, not just the phase.
- [x] The Ask KRAIL entry point is gated on device capability, not the flag alone. A button
      that always fails is worse than no button.
- [x] `resolveTripOrigin` refuses an origin equal to the destination, by id **or** name, since
      one station carries several ids here. Tested.
- [x] iOS sets `requiresOnDeviceRecognition` when the recogniser supports it, and
      `taskHint = .dictation`, mirroring `EXTRA_PREFER_OFFLINE` and `LANGUAGE_MODEL_FREE_FORM`.
- [x] iOS silence detection with windows matched to Android's, kept in step by hand and
      commented on both sides.
- [x] The audio session is released with `NotifyOthersOnDeactivation`. It was left in the
      record category, so a rider who used the mic once lost their music until the app was
      killed.
- [x] `AiUnavailableReasonCoverageTest` walks the vocabulary and fails if any reason falls
      through to the generic banner, or decides the wrong thing about hiding the entry point.
      Mutation-checked: reverting the DEVICE_UNSUPPORTED arm to `null` fails it.
- [ ] Android's `POSSIBLY_COMPLETE_SILENCE_LENGTH` has no iOS equivalent, so iOS stays about a
      second slower to finish. No way to close it from here; documented in the service.
- [ ] Nothing yet compares the two extraction prompts. They are two string literals in two
      languages and will drift again.
