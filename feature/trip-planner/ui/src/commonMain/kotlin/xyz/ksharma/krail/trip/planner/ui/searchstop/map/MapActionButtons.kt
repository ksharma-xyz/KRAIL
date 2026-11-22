package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.core.maps.ui.components.UserLocationButton
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * The location button at the bottom end of the map. The mode filter that used to sit beside it as
 * Options is a row of chips under the form now, so this holds the one button.
 * All business logic (location fetch, permission handling) is delegated to the caller via callbacks.
 *
 * @param onLocationButtonClick Callback when location button is tapped, the caller handles the rest
 * @param isLocationActive Whether location tracking is active. Controls button color.
 * @param modifier Modifier to be applied to the component
 */
@Composable
internal fun MapActionButtons(
    onLocationButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocationActive: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = dim.spacingXL, vertical = dim.spacingXL),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserLocationButton(onClick = onLocationButtonClick, isActive = isLocationActive)
    }
}

// region Previews

@PreviewComponent
@Composable
private fun MapActionButtonsActivePreview() {
    PreviewTheme {
        MapActionButtons(
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
            onLocationButtonClick = {},
            isLocationActive = false,
        )
    }
}

// endregion
