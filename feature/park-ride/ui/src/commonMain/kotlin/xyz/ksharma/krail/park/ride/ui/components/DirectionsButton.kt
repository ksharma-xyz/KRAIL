package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import krail.feature.park_ride.ui.generated.resources.Res
import krail.feature.park_ride.ui.generated.resources.ic_directions_turn_right
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.modifier.scalingKlickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeGroundColor

/** The two looks the Directions button can have, kept side by side so they can be compared on a device. */
enum class DirectionsButtonStyle {
    /** A filled circle in the theme colour, taj's round icon button. */
    Circle,

    /** A filled rounded square turned 45 degrees, the shape of a road sign, in the theme colour. */
    Diamond,
}

/** The look every Directions button in the app has. Change this one value to compare the two. */
val DIRECTIONS_BUTTON_STYLE: DirectionsButtonStyle = DirectionsButtonStyle.Diamond

/** The smallest a touch target may be, which the button's box always is. */
private val DIRECTIONS_TARGET = 48.dp

/**
 * The diamond's own side and corner radius. Turned 45 degrees, a rounded square reaches
 * `side * sqrt(2) - 2 * radius * (sqrt(2) - 1)` from tip to tip, which at 38 and 8 is a little under
 * 48, so the whole sign sits inside the target and never leans on a neighbour.
 */
private val DIAMOND_SIDE = 38.dp
private val DIAMOND_RADIUS = 8.dp

/**
 * Directions to one car park: a filled button in the theme colour with a white turn right arrow, like the
 * directions button of a maps app. It opens the default maps app on the car park's own position, which is
 * the caller's [onClick], and a screen reader hears which car park it is for.
 *
 * The box is always 48 dp square, the touch target, whatever the [style]. The diamond is drawn inside it and
 * its corners stay tappable. A press scales the diamond, because a ripple is a rectangle or a circle and
 * would show outside the sign.
 */
@Composable
fun DirectionsButton(
    carParkName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DirectionsButtonStyle = DIRECTIONS_BUTTON_STYLE,
) {
    val label = "Directions to $carParkName"
    when (style) {
        DirectionsButtonStyle.Circle -> RoundIconButton(
            onClick = onClick,
            color = themeGroundColor(),
            onClickLabel = label,
            modifier = modifier,
        ) {
            DirectionsGlyph(label = label)
        }

        DirectionsButtonStyle.Diamond -> Box(
            modifier = modifier.size(DIRECTIONS_TARGET).scalingKlickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // The sign is turned, the arrow is not, so it is a sibling over it and never rotated back.
            Box(
                modifier = Modifier
                    .size(DIAMOND_SIDE)
                    .rotate(DIAMOND_TURN_DEGREES)
                    .background(color = themeGroundColor(), shape = RoundedCornerShape(DIAMOND_RADIUS)),
            )
            DirectionsGlyph(label = label)
        }
    }
}

private const val DIAMOND_TURN_DEGREES = 45f

/** The white turn right arrow, in the app's icon size. It carries the spoken label for the whole button. */
@Composable
private fun DirectionsGlyph(label: String) {
    Image(
        painter = painterResource(Res.drawable.ic_directions_turn_right),
        contentDescription = label,
        colorFilter = ColorFilter.tint(Color.White),
        modifier = Modifier.size(KrailTheme.dimensions.iconM),
    )
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
            DirectionsButton(carParkName = "Tallawong P1", onClick = {}, style = DirectionsButtonStyle.Circle)
            DirectionsButton(carParkName = "Tallawong P1", onClick = {}, style = DirectionsButtonStyle.Diamond)
        }
    }
}

// endregion
