# Xcode and the iOS toolchain

What to do when a new Xcode or iOS SDK arrives, on a laptop or on the CI runners. Start with:

```sh
./scripts/ios_toolchain_report.sh
```

It prints every pin listed below and warns when the local Xcode and CI disagree.

---

## 1. What pins the toolchain

Four independent things, and a new Xcode can break any one of them:

| Pin | Where | Breaks as |
|---|---|---|
| Kotlin/Native | `kotlin` in `gradle/libs.versions.toml` | Kotlin compile or link errors, an "unsupported Xcode" warning |
| spmForKmp | `spmForKmp` in `gradle/libs.versions.toml` | `SwiftPackageConfig...CompileSwiftPackage...` fails with only "spmForKmp failed when running buildPackage" |
| CI Xcode | `runs-on:` and `setup-xcode`'s `xcode-version:` in `build-ios.yml`, `ios-unit-tests.yml`, `distribute-testflight.yml`, `maestro-nightly.yml` | green locally, red in CI (or the reverse) |
| App deployment target | `IPHONEOS_DEPLOYMENT_TARGET` in `iosApp/iosApp.xcodeproj/project.pbxproj` | Xcode refusing a target below its supported range |

spmForKmp builds two Swift packages of our own, `aiTextBridge` (`:core:ai-text`) and
`speechBridge` (`:core:speech-to-text`), with its own deployment target. That target is not the
app's 17.0, so the app's setting can be fine while the bridges are not.

## 2. Order of work

1. **Read the official compatibility row, not a summary of it.** The KMP compatibility guide
   has a Kotlin to Xcode table:
   <https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html>. A pasted
   summary once claimed Kotlin 2.4.20 "supports Xcode 27". The table pairs it with Xcode 26.4.
2. **Check spmForKmp's releases** for "xcode N support":
   <https://github.com/frankois944/spm4Kmp/releases>. Xcode 27 support arrived in 1.9.7.
3. **Find the failing layer before changing anything.** Gradle hides the Swift error. Re-run the
   bridge build by hand to see it:
   ```sh
   ./gradlew :core:ai-text:SwiftPackageConfigAppleAiTextBridgeCompileSwiftPackageIosSimulatorArm64 --info
   # copy the `xcrun ... swift build ...` line it prints, run it from
   # core/ai-text/build/spmKmpPlugin/aiTextBridge with -v instead of -q
   ```
4. **Try the version bump in a worktree**, not on a feature branch, and prove it with a full
   `xcodebuild` of the `iosApp` scheme plus a launch. A Kotlin-only compile does not build the
   bridges' final link.
5. **CI in its own PR.** Runner images lag: a new Xcode first appears as a *preview* label
   (`runs-on: xcode-27`), not on `macos-latest`. Move the PR-check workflows first.
6. **TestFlight last**, and only on a runner image that is out of preview. It is the release
   pipeline, and App Store Connect's SDK rules decide what it must build with, not us.

## 3. Rules

- **No local-only patches.** Setting spmForKmp's `minIos` by hand got past the first error and
  failed on the next one, and it would have needed reverting once the plugin caught up. Fix the
  version, or wait for it.
- **You cannot always go back.** macOS 27 refuses to run Xcode 26. If the laptop moves first,
  local iOS builds stay broken until the tooling supports the new Xcode, and iOS is verified by
  CI and TestFlight in the meantime. Say so in the PR rather than implying a local run.
- **Pin the exact Xcode, not a range.** `setup-xcode` with `'~27'` picked `27.1` because the
  image also carries a `27.1` beta, so CI built with a beta. Use `'27.0'` and move it on purpose.
- **A preview runner is not a release runner.** Expect queueing and instability on a preview
  image; do not put the release pipeline on one.

## 4. Failure signatures

| You see | It is | Fix |
|---|---|---|
| `IPHONEOS_DEPLOYMENT_TARGET ... is set to 12.0, but the range of supported deployment target versions is 15.0 to 27.0.x` (only visible in the hand-run `swift build -v`) | spmForKmp building the bridges for iOS 12 | Bump spmForKmp to a release with support for that Xcode |
| `compiledBinary ... libaiTextBridge.a which doesn't exist` | A half-applied workaround, or a stale `build/spmKmpPlugin` | Remove the workaround, delete `core/*/build/spmKmpPlugin`, rebuild |
| Xcode 26 installer says a newer version is required | The laptop's macOS is ahead of that Xcode | Use the newest Xcode and wait for tooling (rule 2) |

See `docs/learning/2026-09-23-xcode-27-was-not-kotlin.md` for how this was found.
