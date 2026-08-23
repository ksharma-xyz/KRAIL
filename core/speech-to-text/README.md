# core:speech-to-text

On-device speech-to-text for the "Speak" tab of the AI search-input flow
(`docs/investigations/ai_search_input_mockup.html` stage 03, `AiSearchInputViewModel`).
Wraps each platform's own speech recognizer behind [`SpeechToTextService`](src/commonMain/kotlin/xyz/ksharma/krail/core/speechtotext/SpeechToTextService.kt),
the same `expect`/`actual`-per-platform shape as `core:ai-text`'s `AiTextService`.

**Status: both platforms implemented.** iOS has two, chosen at runtime by
`PreferredSpeechToTextService`: `SpeechAnalyzerSpeechToTextService` on iOS 26 and
`IosSpeechToTextService` (`SFSpeechRecognizer`) everywhere else. See "The two iOS paths" below.

## Why a new module, not an existing library

Researched klibs.io (the Kotlin Multiplatform library search index — confirmed it's purely
a search index over Maven Central/GitHub, not a separate registry) plus the wider ecosystem
before writing any of this, specifically looking for a maintained KMP library that already
wraps Android `SpeechRecognizer` + iOS `SFSpeechRecognizer` under one API:

- **[`speechtotextkit`](https://github.com/eslamwael74/speechtotextkit)**
  (`io.github.eslamwael74.speechtotextkit:speechToText:1.0.0`) — the closest fit on paper,
  wraps exactly these two platform APIs directly. Rejected as too immature to depend on: 20
  stars, 7 commits total, a single `1.0.0` release with no visible activity since. Worth
  reading as a reference implementation, not worth a live dependency — a single-maintainer
  library with this little history is a real bus-factor risk for something a shipped
  feature would call on every use.
- **[`kodio`](https://klibs.io/project/dosier/kodio)** (`space.kodio:core` +
  `space.kodio.extensions:transcription`) — actively maintained (released ~1 month before
  this research, 37 stars), broad platform support (Android/iOS/JVM/macOS/JS/Wasm). **Doesn't
  fit at all**: its transcription is a cloud OpenAI Whisper API call, not on-device platform
  recognition — the opposite of what this feature needs (on-device is the whole point, same
  as `core:ai-text`).
- Nothing else found wraps `SpeechRecognizer`/`SFSpeechRecognizer` specifically.

Same conclusion `core:ai-text`'s own research reached for on-device generative AI: no KMP
library abstracts platform-specific on-device capability here, so a thin hand-rolled
`expect`/`actual` wrapper — small, fully owned, no external bus-factor risk — is the right
call, not a smell. Reusing `speechtotextkit`'s *source* as a reference while writing the
real Android/iOS actuals is reasonable; taking it as a live dependency is not.

## Platform notes for the real implementation

- **Android**: `android.speech.SpeechRecognizer`, not an ML Kit module — ML Kit GenAI
  doesn't cover speech-to-text at all (confirmed while researching `core:ai-text`'s
  `genai-prompt` integration; the GenAI family is generation-focused, not transcription).
  `RecognizerIntent.EXTRA_PREFER_OFFLINE` requests on-device recognition where the device
  supports it, falling back to network otherwise. Needs the `RECORD_AUDIO` runtime
  permission.
- **iOS**: two implementations. See below.

## The two iOS paths

`SFSpeechRecognizer` is a plain Objective-C-compatible framework, directly callable from
Kotlin/Native with no bridge. It is also, as a design, a transcription **stream**: it never
decides a speaker has finished, because `isFinal` arrives only after the app calls
`endAudio()`. Everything about end-of-speech therefore had to be inferred from how text
happened to arrive, and every fault in
[the 2026-08-23 learning entry](../../docs/learning/2026-08-23-the-blank-transcript-that-cleared-the-field.md)
came from that inference.

iOS 26 replaced it with `SpeechAnalyzer`, a modular analyzer. Two of its modules are exactly
the jobs that were being done by hand:

| Module | What it does | Replaces |
|---|---|---|
| `DictationTranscriber` | short queries, same on-device model `SFSpeechRecognizer` uses | the transcription half |
| `SpeechDetector` | voice activity detection, reported in **audio time** | the hand-rolled silence watcher |

`SpeechAnalyzer` is a Swift `actor` with `AsyncSequence` results and no Objective-C surface,
so unlike `SFSpeechRecognizer` it **does** need an SPM cinterop bridge: `src/swift/speechBridge/`,
same shape and same reason as `AiTextBridge` in `core:ai-text`.

The bridge owns mechanism only. It reports what Apple's modules say and never decides when the
rider has finished; that rule is [`SpeechActivityWatch`](src/commonMain/kotlin/xyz/ksharma/krail/core/speechtotext/SpeechActivityWatch.kt)
in `commonMain`, because Swift source here has no test task. Same reason
[`TranscriptWatch`](src/commonMain/kotlin/xyz/ksharma/krail/core/speechtotext/TranscriptWatch.kt)
lives there and is shared by both paths.

Permissions differ between the two, which is a rider-visible difference: `SFSpeechRecognizer`
needs both `NSSpeechRecognitionUsageDescription` and `NSMicrophoneUsageDescription`, and per
this project's `ios_permission_must_request.md` lesson its wrapper must actually call
`requestAuthorization` rather than only inspecting `authorizationStatus()`. `SpeechAnalyzer`
needs the microphone alone, so the iOS 26 path shows one system prompt where the legacy path
shows two. Both Info.plist keys stay, because the legacy path is still live below iOS 26.
