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
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

@Composable
fun TransportModeChip(
    transportMode: TransportMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            transportMode.colorCode.hexToComposeColor()
        } else {
            KrailTheme.colors.surface
        },
        animationSpec = tween(200),
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else transportMode.colorCode.hexToComposeColor(),
        animationSpec = tween(200),
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) {
            getForegroundColor(
                backgroundColor = transportMode.colorCode.hexToComposeColor(),
            )
        } else {
            Color.Gray
        },
        animationSpec = tween(200),
    )

    CompositionLocalProvider(
        LocalTextStyle provides KrailTheme.typography.titleMedium,
    ) {
        Row(
            modifier = modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(50),
                )
                .border(
                    width = 3.dp, // is token , should be 1.dp less than the horizontal padding.
                    color = borderColor,
                    shape = RoundedCornerShape(50),
                )
                .clickable(
                    indication = null,
                    interactionSource = null,
                ) { onClick() }
                // horizontal padding value should be same as border width of
                // TransportModeIcon
                .padding(horizontal = 4.dp, vertical = 4.dp) // is token
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Make this a separate component - TransportModeIcon
            TransportModeIcon(
                transportMode = transportMode,
                displayBorder = true,
                adaptiveSize = true,
            )

            Text(text = transportMode.name, color = textColor)
        }
    }
}
