package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 * A surface that does not reach the edge of the window, a centred sheet narrower than the window for one, already has some room
 * between its edge and the window's. Pass that room as [clearOnEachSide] and only the part of the inset beyond it is padded, so
 * the surface is not pushed in from an edge the system never touches (a phone in landscape with a camera cutout on one side
 * must not get a sheet that sits off centre). The inset is consumed in full either way, so nothing nested pads it again.
 *
 * @param clearOnEachSide Room the surface already has between its edge and the window's, at each side.
 * @param sides The insets to pad by. The default is read here, which is right wherever the screen's own insets are read.
 *
 * This is the only file allowed to name `displayCutout`: `scripts/check_layout_invariants.py` fails the build on any other
 * non test Kotlin that does, and `docs/testing/DEVICE_PROFILES.md` describes the matrix test that proves a screen stands
 * inside the side inset on every device.
 */
@Composable
fun Modifier.sideSafeInsets(
    clearOnEachSide: Dp = 0.dp,
    sides: WindowInsets = sideSafeWindowInsets(),
): Modifier {
    if (clearOnEachSide <= 0.dp) return windowInsetsPadding(sides)
    return windowInsetsPadding(sides.exclude(WindowInsets(left = clearOnEachSide, right = clearOnEachSide)))
        .consumeWindowInsets(sides)
}

/**
 * The inset [sideSafeInsets] pads by: the horizontal part of `safeDrawing`. Read it where the screen's own insets are read and
 * pass it as `sides` when the surface is drawn in another window, a sheet's, whose own composition may report less.
 */
@Composable
fun sideSafeWindowInsets(): WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
