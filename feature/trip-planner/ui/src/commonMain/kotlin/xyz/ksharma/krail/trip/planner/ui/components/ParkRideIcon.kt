package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.car_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor

@Composable
internal fun ParkRideIcon(
    modifier: Modifier = Modifier,
) {
    ParkRideIconContainer(
        foregroundColor = getForegroundColor(themeColor()),
        modifier = modifier,
    ) {
        Text(
            text = "P",
            style = KrailTheme.typography.displayLarge,
            color = themeColor(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp, bottom = 4.dp),
        )

        Image(
            painter = painterResource(Res.drawable.car_icon),
            colorFilter = ColorFilter.tint(color = themeColor()),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 3.dp),
        )
    }
}

@Composable
internal fun ParkRideIconContainer(
    foregroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color = foregroundColor)
            .clearAndSetSemantics {}
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


// region ParkRideIconContainer Previews



//endregion