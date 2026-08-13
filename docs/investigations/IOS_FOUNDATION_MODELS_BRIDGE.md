# iOS Foundation Models bridge for :core:ai-text

Working notes for getting `AiTextService` actually working on iOS. Android is done and
device-verified (see Status). iOS is a stub. Read this before touching
`core/ai-text/src/iosMain/`.

## Status

**The bridge described below is now built** — `feat/ios-ai-text-bridge` (stacked on the
branches in the table) adds `core/ai-text/src/swift/aiTextBridge/AiTextBridge.swift` and
rewrites `IosAiTextService.kt` to call it. `checkAvailability()`, `summarize()`, and
`extractTripIntent()` all call the real `SystemLanguageModel`/`LanguageModelSession` API
now, not a hardcoded stub. See the "What's needed" section below for the shape that
shipped — it matches the sketch closely, plus a `@Generable`/`@Guide` struct for
`extractTripIntent`'s structured extraction.

**Not yet verified**: Simulator cannot run Apple Intelligence models at all (Apple's own
restriction), so this compiles and the app launches clean, but real on-device output
(`Available` + a real summary) has not been confirmed on a real, Apple-Intelligence-capable
device (iPhone 15 Pro or newer / M-series). The one physical device tested against so far
(an iPhone 11) is not Apple Intelligence-eligible hardware at all, so it only exercises the
`Unavailable` path.

Branches, stacked, not yet raised as PRs (see `feature/trip-planner/ui/ALERT_SUMMARY_UX.md`
for the feature this unblocks):

| Branch | Contains |
|---|---|
| `feat/ai-text-service` | `:core:ai-text` module, `AiTextService`, Android actual (ML Kit GenAI), iOS actual (stub), `alert_summary_enabled` flag |
| `feat/taj-alert-feedback-vote` | `:taj` `AlertFeedbackVote` thumbs-up/down component |
| `feat/alert-summary-analytics` | `alert_summary_status` event |
| `feat/alert-summary-wiring` | `AlertSummaryViewModel` wired into `CollapsibleAlert` |
| `feat/ios-ai-text-bridge` | The Swift bridge itself — `IosAiTextService` no longer a stub |

Android confirmed working on a real Pixel 10 Pro, 2026-08-09: `checkAvailability()`
returns `Available`, `summarize()` returns real Gemini Nano output, renders correctly in
`CollapsibleAlert`. `alert_summary_enabled` defaults to `false` in the current commits.

## The actual problem, confirmed not assumed

The first attempt called `platform.FoundationModels.LanguageModelSession` /
`SystemLanguageModel` directly from `iosMain`, on the theory that Kotlin/Native
auto-generates bindings for any system framework. It does not, for this one:

```
e: Unresolved reference 'FoundationModels'.
e: Unresolved reference 'LanguageModelSession'.
e: Unresolved reference 'SystemLanguageModel'.
```

`compileKotlinIosSimulatorArm64` failed outright. Foundation Models (iOS 26+) is a
Swift-only framework built on new language features (`@Generable`, `@Guide` macros) and,
as far as this investigation got, does not ship an Objective-C compatibility header —
which is what Kotlin/Native's cinterop tooling actually reads to generate bindings.
System frameworks with an ObjC header (StoreKit, CoreLocation, etc. — see
`core/app-review/src/iosMain/.../IosAppReviewRequester.kt` for an example already working
in this codebase) just work. Foundation Models does not.

## What's needed

A small Swift shim that:
1. Wraps `LanguageModelSession` / `SystemLanguageModel` behind plain, ObjC-compatible
   entry points — primitive types and closures only, no Swift generics/macros in the
   public surface.
2. Gets compiled and exposed to Kotlin/Native as a cinterop dependency.

This project already does exactly this shape of integration for MapLibre Native — see
`spmForKmp` / `maplibreNativeDistributionSpm` in `gradle/libs.versions.toml` and however
`core/maps/*` wires it up. That's the concrete precedent to copy, not a new pattern to
invent.

Sketch of the shim's public surface (exact shape TBD once someone is iterating in Xcode):

```swift
@objc public class AiTextBridge: NSObject {
    @objc public func checkAvailability(completion: @escaping (Bool, String) -> Void)
    @objc public func summarize(text: String, completion: @escaping (String?) -> Void)
}
```

`IosAiTextService.kt` then calls into this instead of `platform.FoundationModels.*`
directly, wrapping the callback in `suspendCancellableCoroutine` the same way the
original (failed) attempt did.

## What's left

1. ~~Open `core/ai-text/` in Xcode... confirm the actual public surface~~ — done, on a
   machine with Xcode. The shim compiles against the real `LanguageModelSession`/
   `SystemLanguageModel`/`@Generable` API, not a guess.
2. ~~Write the shim following the `spmForKmp` integration pattern~~ — done,
   `core/ai-text/src/swift/aiTextBridge/`.
3. ~~Rewrite `IosAiTextService.kt` against the shim, remove the stub~~ — done.
4. **Still open**: verify on a real, Apple-Intelligence-capable device (iPhone 15 Pro or
   newer / M-series) — the only physical device tested so far isn't eligible hardware.
5. Only then fold the branches above into PRs and raise the stack.
