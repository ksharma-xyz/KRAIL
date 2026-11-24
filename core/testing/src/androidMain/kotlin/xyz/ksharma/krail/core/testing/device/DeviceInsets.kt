package xyz.ksharma.krail.core.testing.device

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import kotlin.math.roundToInt

private const val DPI_PER_DENSITY = 160

/**
 * The Robolectric qualifiers that give the window this profile's size and density, in the order Android reads them. They are
 * set before the activity starts: changing them under a running activity does not resize its window.
 */
internal fun DeviceProfile.qualifiers(): String {
    val orientation = if (widthDp > heightDp) "land" else "port"
    val dpi = (density * DPI_PER_DENSITY).roundToInt()
    return "w${widthDp}dp-h${heightDp}dp-$orientation-${dpi}dpi"
}

/**
 * The window insets the platform of this profile's [DeviceProfile.shape] hands a window, one inset type at a time.
 * Which type carries which number is the whole point of a shape, see [PlatformInsetShape].
 */
internal fun DeviceProfile.windowInsets(): WindowInsetsCompat {
    fun px(dp: Int): Int = (dp * density).roundToInt()
    fun insets(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): Insets =
        Insets.of(px(left), px(top), px(right), px(bottom))

    val builder = WindowInsetsCompat.Builder()
    builder.setInsets(Type.statusBars(), insets(top = this.insets.top))
    when (shape) {
        PlatformInsetShape.ANDROID -> {
            builder.setInsets(Type.navigationBars(), insets(bottom = this.insets.bottom))
            builder.setInsets(Type.displayCutout(), insets(left = this.insets.left, right = this.insets.right))
        }
        PlatformInsetShape.ANDROID_SIDE_NAVIGATION_BAR ->
            builder.setInsets(
                Type.navigationBars(),
                insets(left = this.insets.left, right = this.insets.right, bottom = this.insets.bottom),
            )
        PlatformInsetShape.IOS -> {
            builder.setInsets(Type.navigationBars(), insets(bottom = this.insets.bottom))
            builder.setInsets(Type.captionBar(), insets(left = this.insets.left, right = this.insets.right))
            // Measured on the Duo: the gesture insets carry the side strip and the bottom, so `safeGestures` and
            // `safeContent` read it too, as they do on the iPhone.
            builder.setInsets(
                Type.systemGestures(),
                insets(left = this.insets.left, right = this.insets.right, bottom = this.insets.bottom),
            )
            builder.setInsets(Type.mandatorySystemGestures(), insets(bottom = this.insets.bottom))
        }
    }
    return builder.build()
}

/**
 * Holds [insets] on the decor view so every dispatch carries them. The system dispatches again whenever the layout or the focus
 * changes, so an inset handed over once is lost after the first tap and the screen is left on zero.
 */
internal fun holdInsets(decor: View, insets: WindowInsetsCompat) {
    ViewCompat.setOnApplyWindowInsetsListener(decor) { _, _ -> insets }
    ViewCompat.dispatchApplyWindowInsets(decor, insets)
}
