# core:speech-to-text

On-device speech-to-text for the "Speak" tab of the AI search-input flow
(`docs/investigations/ai_search_input_mockup.html` stage 03, `AiSearchInputViewModel`).
Wraps each platform's own speech recognizer behind [`SpeechToTextService`](src/commonMain/kotlin/xyz/ksharma/krail/core/speechtotext/SpeechToTextService.kt),
the same `expect`/`actual`-per-platform shape as `core:ai-text`'s `AiTextService`.

**Status: interface + module boundary only. Neither platform actual is implemented yet** —
both `AndroidSpeechToTextService` and `IosSpeechToTextService` always report `Unavailable`.
See the doc comment on each actual for exactly what real implementation needs.

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
- **iOS**: `SFSpeechRecognizer` + `AVAudioEngine`. Unlike Foundation Models (Swift-only, no
  Objective-C header — the reason `IosAiTextService` needs an SPM cinterop bridge),
  `SFSpeechRecognizer` is a plain Objective-C-compatible framework and is directly callable
  from Kotlin/Native — **no SPM bridge needed for this one**. Needs both
  `NSSpeechRecognitionUsageDescription` and `NSMicrophoneUsageDescription` in Info.plist,
  and per this project's `ios_permission_must_request.md` lesson, the wrapper must actually
  call `requestAuthorization`, not just inspect `authorizationStatus()`.
