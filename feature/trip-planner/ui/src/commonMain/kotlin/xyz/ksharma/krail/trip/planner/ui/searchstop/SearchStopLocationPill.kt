package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingStatusButton

/**
 * The location status as its own small button on the saved trips map pane at the map's top left corner (rule 24 of
 * `docs/MAP_HOME_UX.md`), drawn only while [pill] is not [LocationPill.Hidden]. It is the [FloatingStatusButton] the home
 * notices use, so the two are one style: taj's filled button at its extra small size, the normal weight text, the soft tint
 * through its colours parameter and a 48 dp touch target. Its words wrap inside the button at large text and nothing beside it
 * is squeezed.
 *
 * Search Stop no longer draws it: its notice is the [LocationNotice], the app's own info tile. This one is for the screens that
 * have no room for a tile.
 *
 * It says what is wrong with the permission and nothing else. Tapping asks, or opens Settings when the rider
 * refused, and [onClick] does whichever [pill] names. The tag is the one a screen reader and a test find the
 * status by on either screen.
 */
@Composable
internal fun LocationStatusChip(
    pill: LocationPill,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!locationChipShownFor(pill)) return
    FloatingStatusButton(
        text = LOCATION_PILL_TEXT,
        tag = TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL,
        onClick = onClick,
        modifier = modifier,
    )
}

/** The pill's one line. The wording the location banner on this map always used. */
internal const val LOCATION_PILL_TEXT = "Location permission required"

@PreviewComponent
@Composable
private fun LocationStatusChipAskPreview() {
    PreviewTheme {
        LocationStatusChip(pill = LocationPill.Ask, onClick = {})
    }
}

@PreviewComponent
@Composable
private fun LocationStatusChipSettingsPreview() {
    PreviewTheme {
        LocationStatusChip(pill = LocationPill.OpenSettings, onClick = {})
    }
}
