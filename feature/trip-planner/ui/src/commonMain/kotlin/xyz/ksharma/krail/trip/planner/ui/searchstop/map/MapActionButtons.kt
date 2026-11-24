package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.core.maps.ui.components.UserLocationButton
import xyz.ksharma.krail.taj.modifier.sideSafeInsets
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.MapControlVisibility
import xyz.ksharma.krail.trip.planner.ui.components.mapVanishIn
import xyz.ksharma.krail.trip.planner.ui.components.mapVanishOut

/**
 * The location button at the bottom end of the map, one 48 dp touch target [bottomPadding] above the system bottom inset and
 * one edge in from the side safe area (so it stands clear of the status pill strip on the outer screen of a foldable iPhone),
 * one edge above it unless the host says otherwise. Search Stop stands it on the one line Options is on, above the map's
 * info icon. All business logic (location fetch, permission handling) is delegated to the caller via callbacks.
 *
 * @param onLocationButtonClick Callback when location button is tapped, the caller handles the rest
 * @param isLocationActive Whether location tracking is active. Controls button color.
 * @param bottomPadding How far above the system bottom inset the button stands.
 * @param visible False while a host hides the control (Search Stop, while a stop card is open): it shrinks to nothing
 * about its own centre while it fades, takes no tap on the way out, and leaves the tree. It comes back the same way in
 * reverse, to the same place, so nothing around it moves. With reduced motion it is simply there or not.
 * @param modifier Modifier to be applied to the component
 */
@Composable
internal fun MapActionButtons(
    onLocationButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocationActive: Boolean = false,
    bottomPadding: Dp = KrailTheme.dimensions.spacingXL,
    visible: Boolean = true,
    reduceMotion: Boolean = isReduceMotionEnabled(),
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sideSafeInsets()
            .navigationBarsPadding()
            .padding(start = dim.spacingXL, end = dim.spacingXL, bottom = bottomPadding),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MapControlVisibility(
            visible = visible,
            enter = mapVanishIn(reduceMotion),
            exit = mapVanishOut(reduceMotion),
        ) {
            UserLocationButton(
                onClick = onLocationButtonClick,
                isActive = isLocationActive,
                modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON),
            )
        }
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
