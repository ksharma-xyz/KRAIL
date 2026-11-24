package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Keeps a surface out of the sides of the display that belong to the system: a camera cutout, a navigation bar on the side
 * in landscape, and the status pill down the right edge of the outer screen of a foldable iPhone. A full bleed surface under
 * it, a map for example, is not padded and still draws edge to edge; only the cards, controls and notices that float over it
 * stand inside.
 *
 * It reads `safeDrawing`, never `displayCutout`. Compose on iOS puts the side safe area in the system bars and reports the
 * cutout only for the top edge in portrait, so on that screen a cutout only inset is zero and a card runs under the pill, where
 * the system takes the touch and opens Control Center instead of the control. `safeDrawing` is the one inset that carries the
 * side on both platforms. The keyboard has no side, so it adds nothing here and the screen's root `imePadding()` stays the only
 * keyboard authority (`docs/LAYOUT_AND_INSETS.md` 1.1).
 *
 * This is the only file allowed to name `displayCutout`: `scripts/check_layout_invariants.py` fails the build on any other
 * non test Kotlin that does, and `docs/testing/DEVICE_PROFILES.md` describes the matrix test that proves a screen stands
 * inside the side inset on every device.
 */
@Composable
fun Modifier.sideSafeInsets(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
