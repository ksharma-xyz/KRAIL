package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import krail.feature.park_ride.ui.generated.resources.Res
import krail.feature.park_ride.ui.generated.resources.ic_directions_turn_right
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeGroundColor

private val DIRECTIONS_GLYPH: DrawableResource = Res.drawable.ic_directions_turn_right

/**
 * Directions to one car park: taj's [RoundIconButton] as it ships, filled with the theme colour through its own
 * `color` parameter, with the white [DIRECTIONS_GLYPH] on it, like the directions button of a maps app. This file is
 * the one place the glyph is chosen, so every Directions button in the app draws the same drawable. It opens
 * the default maps app on the car park's own position, which is the caller's [onClick], and a screen reader hears
 * which car park it is for. The button is taj's 48 dp circle, so the touch target is the button itself.
 */
@Composable
fun DirectionsButton(
    carParkName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = "Directions to $carParkName"
    RoundIconButton(
        onClick = onClick,
        color = themeGroundColor(),
        onClickLabel = label,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(DIRECTIONS_GLYPH),
            contentDescription = label,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(KrailTheme.dimensions.iconM),
        )
    }
}

// region Previews

@PreviewScreen
@Composable
private fun PreviewDirectionsButtons() {
    PreviewTheme {
        val dim = KrailTheme.dimensions
        FlowRow(
            modifier = Modifier.padding(dim.spacingXL),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
        ) {
            DirectionsButton(carParkName = "Tallawong P1", onClick = {})
        }
    }
}

// endregion
