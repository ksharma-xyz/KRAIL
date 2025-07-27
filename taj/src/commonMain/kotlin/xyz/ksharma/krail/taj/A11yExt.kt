package xyz.ksharma.krail.taj

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

const val LARGE_FONT_SCALE_THRESHOLD = 1.3f

@Composable
fun isLargeFontScale(): Boolean {
    val density = LocalDensity.current
    val fontScale = density.fontScale
    return fontScale > LARGE_FONT_SCALE_THRESHOLD
}
