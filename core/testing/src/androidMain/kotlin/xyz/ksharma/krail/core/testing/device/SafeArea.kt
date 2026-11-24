package xyz.ksharma.krail.core.testing.device

import androidx.compose.ui.semantics.SemanticsNode
import kotlin.math.roundToInt

/** The part of the window the system leaves to the app, in px from the top left of the root. */
internal data class SafeArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    override fun toString(): String = "L$left T$top R$right B$bottom px"
}

/** The safe area of [root] under [profile], rounded the way the insets were dispatched so the two agree to the pixel. */
internal fun safeAreaOf(root: SemanticsNode, profile: DeviceProfile): SafeArea {
    val density = root.layoutInfo.density.density
    fun px(dp: Int): Float = (dp * density).roundToInt().toFloat()
    return SafeArea(
        left = px(profile.insets.left),
        top = px(profile.insets.top),
        right = root.size.width - px(profile.insets.right),
        bottom = root.size.height - px(profile.insets.bottom),
    )
}
