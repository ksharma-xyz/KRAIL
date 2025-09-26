package xyz.ksharma.krail.taj

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

const val XL_FONT_SCALE_THRESHOLD = 1.7f
const val L_FONT_SCALE_THRESHOLD = 1.3f

@Composable
fun getImageHeightRatio(): Float {
    val density = LocalDensity.current
    val fontScale = density.fontScale
    return when {
        fontScale >= XL_FONT_SCALE_THRESHOLD -> 0.45f // Extra large fonts need more text space
        fontScale >= L_FONT_SCALE_THRESHOLD -> 0.5f // Large fonts need some text space
        else -> 0.6f // Normal fonts can use standard image size
    }
}
