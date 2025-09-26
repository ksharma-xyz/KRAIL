package xyz.ksharma.krail.taj.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.material3.ripple
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.unit.Dp

@Stable
fun krailRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
): IndicationNodeFactory {
    return ripple(bounded, radius, color)
}

@Stable
fun krailRipple(
    color: ColorProducer,
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
): IndicationNodeFactory {
    return ripple(color, bounded, radius)
}
