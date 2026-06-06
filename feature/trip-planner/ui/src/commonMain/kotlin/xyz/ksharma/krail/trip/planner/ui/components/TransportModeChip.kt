package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor

@Composable
fun TransportModeChip(
    transportMode: TransportMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            NswTransportConfig.colorFor(transportMode).hexToComposeColor()
        } else {
            KrailTheme.colors.surface
        },
        animationSpec = tween(200),
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            Color.Transparent
        } else {
            NswTransportConfig.colorFor(
                transportMode,
            ).hexToComposeColor()
        },
        animationSpec = tween(200),
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) {
            getForegroundColor(
                backgroundColor = NswTransportConfig.colorFor(transportMode).hexToComposeColor(),
            )
        } else {
            Color.Gray
        },
        animationSpec = tween(200),
    )

    CompositionLocalProvider(
        LocalTextStyle provides KrailTheme.typography.titleMedium,
    ) {
        val dim = KrailTheme.dimensions
        Row(
            modifier = modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(50),
                )
                .border(
                    width = dim.strokeMedium,
                    color = borderColor,
                    shape = RoundedCornerShape(50),
                )
                .clickable(
                    indication = null,
                    interactionSource = null,
                ) { onClick() }
                .padding(horizontal = dim.spacingXS, vertical = dim.spacingXS)
                .padding(end = dim.spacingM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            // Make this a separate component - TransportModeIcon
            TransportModeIcon(
                transportMode = transportMode,
                displayBorder = true,
            )

            Text(text = transportMode.displayName, color = textColor)
        }
    }
}
