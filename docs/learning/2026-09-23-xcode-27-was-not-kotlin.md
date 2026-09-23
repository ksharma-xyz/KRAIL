# Xcode 27 broke every iOS build, and the cause was a Gradle plugin, not Kotlin

**2026-09-23** · **Build / iOS toolchain** · **Cost:** four failed iOS builds, one wrong patch, one
wrong upgrade theory, most of an afternoon of local iOS QA lost

## Symptom

After the laptop moved to macOS 27 with Xcode 27, every `xcodebuild` of `iosApp` failed within
seconds with a wall of `ERROR FOUND WHEN EXEC`. Gradle's own message was only
`Execution failed for task ':core:ai-text:SwiftPackageConfigAppleAiTextBridgeCompileSwiftPackageIosSimulatorArm64'. > spmForKmp failed when running buildPackage`.
Android and CI were green.

## Root cause

spmForKmp 1.6.1 builds our Swift bridges (`aiTextBridge`, `speechBridge`) with
`--triple arm64-apple-ios12.0-simulator`. Xcode 27's simulator SDK only accepts 15.0 and later.
The error saying so is printed by `swift build`, which spmForKmp runs with `-q`, so it never
reached the Gradle log. spmForKmp 1.9.7 adds Xcode 27 support and fixes it with no other change.

## Why it took so long

1. **The real error was hidden.** Gradle reported that the plugin failed, not why. Only running
   the plugin's own `swift build` command by hand with `-v` showed the deployment-target line.
2. **The first fix was a patch.** Setting `minIos = "15.0"` on both `swiftPackageConfig` blocks
   got past the first error, then failed on a missing `libaiTextBridge.a`. It looked close to
   working, which is what made it tempting. It was never pushed.
3. **Downgrading was not possible.** Xcode 26 would not run on macOS 27, so "use the Xcode CI
   uses" was not available.
4. **The next theory was Kotlin.** A summary of the Kotlin 2.4.20 release claimed Xcode 27
   support. The release page says nothing of the kind, and the compatibility guide pairs 2.4.20
   with Xcode 26.4. Upgrading Kotlin two minor versions would not have touched the failing
   step.

## What would have caught it sooner

- Run the failing plugin's command by hand with `-v` before theorising about which layer broke.
- Check the release notes of **each** tool in the build (Kotlin, spmForKmp, CMP) for the new
  Xcode, and read the official compatibility table rather than a summary of it.

## Actions taken

- [x] spmForKmp bumped to 1.9.7.
- [x] `docs/ci_cd/XCODE_AND_IOS_TOOLCHAIN.md`: what pins the toolchain, the order of work, the
  failure signatures.
- [x] `scripts/ios_toolchain_report.sh`: prints every pin and warns when local Xcode and CI
  disagree.
- [x] CI moved to the `xcode-27` runner image, in its own PR. TestFlight deliberately stays on Xcode 26 until that image is out of preview.
