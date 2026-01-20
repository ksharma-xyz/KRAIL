package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor

@Composable
internal fun ParkRideIcon(
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1f,
        ),
    ) {
        ParkRideIconContainer(
            backgroundColor = themeColor(),
            modifier = modifier,
        ) {
            Text(
                text = "P",
                style = KrailTheme.typography.displayLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center),
            )
        }
    }
}

@Composable
internal fun ParkRideIconContainer(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color = backgroundColor)
            .clearAndSetSemantics {},
    ) {
        content()
    }
}

// region Previews

@Preview
@Composable
private fun ParkRideIconPinkThemePreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink) {
        ParkRideIcon()
    }
}

@Preview
@Composable
private fun ParkRideIconMetroThemePreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        ParkRideIcon()
    }
}

@Preview
@Composable
private fun ParkRideIconBusThemePreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        ParkRideIcon()
    }
}

@Preview
@Composable
private fun ParkRideIconPurpleThemePreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        ParkRideIcon()
    }
}

@Preview
@Composable
private fun ParkRideIconTrainThemePreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        ParkRideIcon()
    }
}

// endregion
