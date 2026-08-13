# core:text-recognition

On-device OCR for the "Paste" tab of the AI search-input flow
(`docs/investigations/ai_search_input_mockup.html` stage 06, `AiScreenshotExtractCard`).
Wraps each platform's own text-recognition API behind [`TextRecognitionService`](src/commonMain/kotlin/xyz/ksharma/krail/core/textrecognition/TextRecognitionService.kt),
same `expect`/`actual`-per-platform shape as `core:ai-text` and `core:speech-to-text`.

**Status: implemented on both platforms**, hand-rolled against ML Kit Text Recognition v2
(Android) and the Vision framework (iOS) rather than a third-party KMP wrapper — see below
for why. Not yet wired into `AiScreenshotExtractCard`'s Paste tab.

## Why a new module, not an existing library

Researched klibs.io plus the wider ecosystem before writing any of this, looking for a
maintained KMP library wrapping ML Kit Text Recognition (Android) + Vision framework (iOS)
under one API — this is the one category of the three researched (speech-to-text, OCR,
natural-language date parsing — see `feature/trip-planner/ui/.../search/ai
/AiTripIntentTimeResolver.kt`'s doc comment for the date-parsing findings) where a real
candidate exists:

- **[`MLKit-KMP`](https://github.com/RufenKhokhar/MLKit-KMP)**
  (`io.github.rufenkhokhar:mlkit-text-recognition:v0.2.0`) — directly wraps ML Kit Text
  Recognition on Android (API 23+) and iOS (15+) via one common API, exactly this module's
  use case. **Not adopted, but flagged as worth a spike**: pre-1.0 (v0.2.0), 16 stars, 1
  fork, 31 commits, no evidence found of production usage at scale. Same bus-factor concern
  as `speechtotextkit` in `core:speech-to-text`'s README — a single/small-maintainer library
  this early is a real risk for something a shipped feature calls on every paste.
  **Recommendation for whoever implements this module for real**: read `MLKit-KMP`'s source
  first as a reference (or consider vendoring/forking specific pieces of it) rather than
  either blindly taking it as a live dependency or ignoring it and re-deriving the same ML
  Kit wiring from scratch — it's the closest thing to prior art this research found.

## Platform notes for the real implementation

- **Android**: ML Kit Text Recognition v2 (`com.google.mlkit:text-recognition`) — on-device,
  bundled model, no separate on-demand download step (unlike `core:ai-text`'s Gemini Nano
  dependency, which downloads on first use).
- **iOS**: Vision framework's `VNRecognizeTextRequest`, run via `VNImageRequestHandler`. A
  plain Objective-C-compatible framework — directly callable from Kotlin/Native, **no SPM
  cinterop bridge needed** (same reasoning as `core:speech-to-text`'s `SFSpeechRecognizer`
  vs. the Foundation Models bridge `IosAiTextService` actually needs).
- **Image representation** (`recognizeText(imageBytes: ByteArray)`) is a placeholder — see
  the interface's doc comment. Whichever platform API ends up backing this may prefer a
  platform `Bitmap`/`UIImage` wrapper over a PNG/JPEG byte round-trip; don't treat the
  current signature as settled.
