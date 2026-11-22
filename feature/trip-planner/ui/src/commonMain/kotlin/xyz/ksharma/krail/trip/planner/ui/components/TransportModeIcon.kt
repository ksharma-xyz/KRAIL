package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.toAdaptiveDecorativeIconSize
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens

@Composable
fun TransportModeIcon(
    transportMode: TransportMode,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.White,
    displayBorder: Boolean = false,
    size: TransportModeIconSize = TransportModeIconSize.Medium,
) {
    CompositionLocalProvider(
        LocalTextColor provides Color.White,
        LocalTextStyle provides size.letterStyle(),
        // Alpha is intentionally always 100% — the mode icon circle must remain fully opaque
        // even when used inside a parent that sets LocalContentAlpha to 0.5f (e.g. previous
        // departure rows or past journey cards). The icon's coloured circle is its primary
        // identity signal and should never be dimmed.
        LocalContentAlpha provides ContentAlphaTokens.EnabledContentAlpha,
    ) {
        Box(
            modifier = modifier
                .size(size.dpSize.toAdaptiveDecorativeIconSize())
                .heightIn(min = size.dpSize)
                .clip(CircleShape)
                .background(
                    color = NswTransportConfig.colorFor(transportMode).hexToComposeColor(),
                    shape = CircleShape,
                )
                .borderIfEnabled(
                    enabled = displayBorder,
                    color = borderColor,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = NswTransportConfig.nameFor(transportMode).first().toString().uppercase(),
            )
        }
    }
}

/**
 * The style of the letter for [this] size. Every size up to [TransportModeIconSize.Large] is the small title, the same as
 * StopsRow and TransportModeInfo. The extra large size has a bigger circle, so its letter takes the large headline, which
 * is the same share of the circle. The colour is not here: the letter is always white, whatever the size.
 */
@Composable
private fun TransportModeIconSize.letterStyle(): TextStyle = when (this) {
    TransportModeIconSize.XLarge -> KrailTheme.typography.headlineLarge
    else -> KrailTheme.typography.titleSmall
}

@Composable
private fun Modifier.borderIfEnabled(enabled: Boolean, color: Color): Modifier =
    if (enabled) {
        val dim = KrailTheme.dimensions
        this.then(
            border(
                width = dim.strokeMedium,
                color = color,
                shape = CircleShape,
            ),
        )
    } else {
        this
    }

private val ICON_SIZE_XSMALL = 20.dp
private val ICON_SIZE_SMALL = 22.dp
private val ICON_SIZE_MEDIUM = 28.dp
private val ICON_SIZE_LARGE = 32.dp
private val ICON_SIZE_XLARGE = 64.dp

enum class TransportModeIconSize(val dpSize: Dp) {
    XSmall(ICON_SIZE_XSMALL),
    Small(ICON_SIZE_SMALL),
    Medium(ICON_SIZE_MEDIUM),
    Large(ICON_SIZE_LARGE),

    /** Twice [Large], for a mode shown as the thing to choose, such as the Intro's theme badges. */
    XLarge(ICON_SIZE_XLARGE),
}

// region Previews

private const val previewGroupName = "Transport Mode Icons"

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@Preview(group = previewGroupName)
@Composable
private fun TrainPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Train,
            displayBorder = false,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun TrainPreviewLarge() {
    PreviewTheme(
        backgroundColor = Color.Transparent,
    ) {
        TransportModeIcon(
            transportMode = TransportMode.Train,
            displayBorder = false,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun BusPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Bus,
            displayBorder = false,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@Preview(group = previewGroupName)
@Composable
private fun MetroPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Metro,
            displayBorder = false,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@Preview(group = previewGroupName)
@Composable
private fun LightRailPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.LightRail,
            displayBorder = false,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@Preview(group = previewGroupName)
@Composable
private fun FerryPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Ferry,
            displayBorder = false,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun TrainWithBackgroundPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Train,
            displayBorder = true,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun BusWithBackgroundPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Bus,
            displayBorder = true,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun MetroWithBackgroundPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Metro,
            displayBorder = true,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun LightRailWithBackgroundPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.LightRail,
            displayBorder = true,
        )
    }
}

// @ScreenshotTest disabled: missing baseline (recording timed out, see README)
@PreviewComponent
@Composable
private fun FerryWithBackgroundPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.Ferry,
            displayBorder = true,
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun SchoolBusPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.SchoolBus,
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun SchoolBusWithBackgroundPreview() {
    PreviewTheme {
        TransportModeIcon(
            transportMode = TransportMode.SchoolBus,
            displayBorder = true,
        )
    }
}

// endregion
