package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import krail.feature.park_ride.ui.generated.resources.Res
import krail.feature.park_ride.ui.generated.resources.ic_car
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentColor

/**
 * The car glyph on its own, for a small mark beside a Park & Ride station's name. The same drawable
 * [ParkAndRideIcon] draws inside its badge, exposed because the resource class is internal to this
 * module. It is decoration, so it carries no description: the name beside it says what it is.
 *
 * Sized by the caller's [modifier] and tinted with [tint], the surrounding content colour by default.
 */
@Composable
fun CarIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Image(
        painter = painterResource(Res.drawable.ic_car),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}
