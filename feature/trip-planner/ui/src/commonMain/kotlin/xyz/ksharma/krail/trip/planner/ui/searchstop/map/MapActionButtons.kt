package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_filter
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.maps.ui.components.UserLocationButton
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Action buttons for the map (Options and Location).
 * All business logic (location fetch, permission handling) is delegated to the caller via callbacks.
 *
 * @param onOptionsClick Callback for options button click
 * @param onLocationButtonClick Callback when location button is tapped — caller handles the rest
 * @param isLocationActive Whether location tracking is active. Controls button color.
 * @param modifier Modifier to be applied to the component
 */
@Composable
internal fun MapActionButtons(
    onOptionsClick: () -> Unit,
    onLocationButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocationActive: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onOptionsClick,
            dimensions = ButtonDefaults.mediumButtonSize(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_filter),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ButtonDefaults.buttonColors().contentColor),
                    modifier = Modifier.size(18.dp),
                )
                Text(text = "Options")
            }
        }

        UserLocationButton(onClick = onLocationButtonClick, isActive = isLocationActive)
    }
}

// region Previews

@PreviewComponent
@Composable
private fun MapActionButtonsActivePreview() {
    PreviewTheme {
        MapActionButtons(
            onOptionsClick = {},
            onLocationButtonClick = {},
            isLocationActive = true,
        )
    }
}

@PreviewComponent
@Composable
private fun MapActionButtonsInactivePreview() {
    PreviewTheme {
        MapActionButtons(
            onOptionsClick = {},
            onLocationButtonClick = {},
            isLocationActive = false,
        )
    }
}

// endregion
