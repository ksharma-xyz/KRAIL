package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.toAdaptiveDecorativeIconSize
import xyz.ksharma.krail.taj.toAdaptiveSize
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

@Composable
fun TransportModeIcon(
    transportMode: TransportMode,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.White,
    adaptiveSize: Boolean = false,
    displayBorder: Boolean = false,
    size: TransportModeIconSize = TransportModeIconSize.Medium,
) {
    CompositionLocalProvider(
        LocalTextColor provides Color.White,
        // should be same as StopsRow and TransportModeInfo
        LocalTextStyle provides KrailTheme.typography.titleSmall,
        // Alpha should always be 100%
        LocalContentAlpha provides ContentAlphaTokens.EnabledContentAlpha,
    ) {
        Box(
            modifier = modifier
                .size(
                    if (adaptiveSize) size.dpSize.toAdaptiveSize()
                    else size.dpSize.toAdaptiveDecorativeIconSize()
                )
                .clip(CircleShape)
                .background(
                    color = transportMode.colorCode.hexToComposeColor(),
                    shape = CircleShape,
                )
                .borderIfEnabled(
                    enabled = displayBorder,
                    color = borderColor,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = transportMode.name.first().toString().uppercase(),
            )
        }
    }
}

@Composable
private fun Modifier.borderIfEnabled(enabled: Boolean, color: Color): Modifier =
    if (enabled) {
        this.then(
            border(
                width = 3.dp,
                color = color,
                shape = CircleShape,
            )
        )
    } else this

enum class TransportModeIconSize(val dpSize: Dp) {
    XSmall(20.dp), Small(22.dp), Medium(28.dp), Large(32.dp)
}

// region Previews
@Preview
@Composable
private fun TrainPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Train(),
            displayBorder = false
        )
    }
}

@Preview
@Composable
private fun BusPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Bus(),
            displayBorder = false
        )
    }
}

@Preview
@Composable
private fun MetroPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Metro(),
            displayBorder = false
        )
    }
}

@Preview
@Composable
private fun LightRailPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.LightRail(),
            displayBorder = false
        )
    }
}

@Preview
@Composable
private fun FerryPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Ferry(),
            displayBorder = false
        )
    }
}

@Preview
@Composable
private fun TrainWithBackgroundPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Train(),
            displayBorder = true
        )
    }
}

@Preview
@Composable
private fun BusWithBackgroundPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Bus(),
            displayBorder = true
        )
    }
}

@Preview
@Composable
private fun MetroWithBackgroundPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Metro(),
            displayBorder = true
        )
    }
}

@Preview
@Composable
private fun LightRailWithBackgroundPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.LightRail(),
            displayBorder = true
        )
    }
}

@Preview
@Composable
private fun FerryWithBackgroundPreview() {
    KrailTheme {
        TransportModeIcon(
            transportMode = TransportMode.Ferry(),
            displayBorder = true,
        )
    }
}

// endregion
