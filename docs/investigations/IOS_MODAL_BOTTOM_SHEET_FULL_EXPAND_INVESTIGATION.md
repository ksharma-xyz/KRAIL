# Investigation: iOS ModalBottomSheet expands full-screen, no top peek (Android unaffected)

## User report

On iOS, dragging any `ModalBottomSheet` in the app (e.g. `StopDetailsBottomSheet`,
`AddLabelBottomSheet`, `SaveStopAsLabelSheet`, `LabelConflictSheet`, `MapOptionsBottomSheet`)
to its expanded state fills the entire screen edge-to-edge, top to bottom. The user cannot
see any of the underlying screen above the sheet.

On Android, the same sheets, same content, same call sites, always leave a visible gap at
the top when expanded, so the background screen peeks through.

## Where the component lives

`taj/src/commonMain/.../components/ModalBottomSheet.kt` declares a single `expect fun
ModalBottomSheet(...)` with no `sheetState` parameter exposed to callers. Both platform
`actual` implementations call the framework's `androidx.compose.material3.ModalBottomSheet`
directly and let it create its own default `SheetState` internally:

- `ModalBottomSheet.android.kt` — calls Material3 `ModalBottomSheet` unconditionally.
- `ModalBottomSheet.ios.kt` — calls Material3 `ModalBottomSheet` **only when
  `UIAccessibilityIsReduceMotionEnabled()` is false**. When reduced motion is on, it falls back
  to a hand-rolled `SimpleBottomSheetOverlay` (a plain `Box` + `Column` anchored to
  `Alignment.BottomCenter`) that has no expand/peek behaviour at all — not relevant to this
  bug since it doesn't animate or expand, but worth knowing this is a second, separate code
  path on iOS.

Neither platform passes a custom `SheetState`, `skipPartiallyExpanded`, or a height-constrained
`modifier` — both rely entirely on Material3's default detent calculation. No KRAIL code
computes screen height, safe-area insets, or peek height itself.

## Ruled out: app-side content sizing

Checked every current call site (`StopDetailsBottomSheet.kt`, `AddLabelBottomSheet.kt`,
`SaveStopAsLabelSheet.kt`, `LabelConflictSheet.kt`, `MapOptionsBottomSheet.kt`): none use
`fillMaxSize()` / `fillMaxHeight()` inside the sheet content, and none pass a custom
`modifier` that would force full-height measurement. Content is a wrap-content `Column`,
same on both platforms. This rules out "our content is measuring tall on iOS" as the cause —
the same composable tree, given the same constraints, would size identically on both
platforms if the framework's height/detent math were platform-agnostic.

## Root cause: known Compose Multiplatform iOS bug, not KRAIL app code

This is a documented issue in JetBrains' Compose Multiplatform framework (`compose-multiplatform
= "1.10.3"`, `material3 = "1.9.0"` per `gradle/libs.versions.toml`) — Material3's
`ModalBottomSheet`/`SheetState` detent (anchor) calculation on iOS does not correctly account
for safe-area insets and available container height the way the Android target does. Related
upstream reports:

- [#3701 — iOS: Expanded ModalBottomSheet: semi-transparent black background doesn't occupy complete screen](https://github.com/JetBrains/compose-multiplatform/issues/3701)
- [#5089 — iOS: ModalBottomSheet can't ignore safeArea](https://github.com/JetBrains/compose-multiplatform/issues/5089)
- [#4032 — ModalBottomSheet does not stick to the bottom of the screen](https://github.com/JetBrains/compose-multiplatform/issues/4032)
- [#4990 — ModalBottomSheet no appearance animation in skiko targets](https://github.com/JetBrains/compose-multiplatform/issues/4990)

Android's Material3 `ModalBottomSheet` deliberately caps the "expanded" detent so a margin of
background always remains visible at the top (standard Material spec behaviour, computed from
`LocalConfiguration`/window metrics). The iOS Skiko/UIKit-interop target's port of the same
detent logic doesn't apply the equivalent cap correctly against the device's safe-area /
screen metrics — the reports above show the inverse symptom too (scrim not covering the full
screen, sheet not sticking to the bottom), which is consistent with the anchor/available-height
computation being unreliable on iOS across multiple axes, not just this one.

Given identical KRAIL-side content and modifiers on both platforms, and no custom `SheetState`
or height override anywhere in this codebase, the divergence is coming from the framework
layer, not from `taj` or feature call sites.

## What needs to happen next

1. **Confirm framework version behaviour first.** Check if a newer Compose Multiplatform /
   Material3 release (current: CMP `1.10.3`, `material3 1.9.0`) has picked up a fix for
   #3701/#5089/#4032 upstream before writing a workaround — check the CMP changelog /
   issue threads for a "fixed in" release tag.
2. **If no upstream fix is available yet**, the pragmatic workaround is to stop relying on
   Material3's default `SheetState` detent math on iOS and instead:
   - Pass an explicit `SheetState` with `skipPartiallyExpanded = false` and verify whether
     forcing a partial detent changes anything (per the Slack thread linked in the research,
     some report `skipPartiallyExpanded = true` is what causes immediate full-screen open —
     confirm KRAIL isn't hitting that parameter indirectly through a default).
   - If that doesn't help, constrain the iOS `Material3ModalBottomSheet` call in
     `ModalBottomSheet.ios.kt` with an explicit `modifier.fillMaxHeight(fraction)` or a
     `Modifier.windowInsetsPadding` for the top safe area, so the sheet's own container is
     capped below full screen height regardless of what the framework's internal detent
     calculation decides.
   - As a last resort, consider a maintained third-party bottom sheet library that already
     handles this correctly across CMP targets (e.g. `dokar3/sheets`,
     `skydoves/FlexibleBottomSheet`) — only if the above two options prove unworkable, since
     swapping the sheet implementation is a bigger, riskier change than constraining height.
3. **Verify on both a notched/Dynamic Island device and an older iPhone** (no notch) — since
   the bug is safe-area related, it may reproduce differently (or not at all) depending on how
   much safe-area inset exists at the top of the device.
4. **Add a manual QA check** to whatever pre-release checklist covers iOS: expand every
   `ModalBottomSheet` call site and confirm background peek is visible at the top, same as
   Android, before shipping a fix.

No code changes have been made in this branch — this is investigation + documentation only,
per request.
