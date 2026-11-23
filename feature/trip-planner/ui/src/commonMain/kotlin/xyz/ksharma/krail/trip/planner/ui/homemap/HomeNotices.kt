package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingStatusButton
import xyz.ksharma.krail.trip.planner.ui.searchstop.LOCATION_PILL_TEXT
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill

/** What the Map offline notice says. The same words the design names it by. */
internal const val MAP_OFFLINE_TEXT = "Map offline"

/**
 * The notices of the map home, rule 24 of `docs/MAP_HOME_UX.md`: the location status and the Map offline
 * status. Each is one [FloatingStatusButton], taj's own [xyz.ksharma.krail.taj.components.Button] at its extra small
 * size (the normal weight text) in the soft theme tint of the app mode, the same colours as the Search Stop box. They are whole
 * items emitted into the flow of the chips, so they sit at the same left edge as the chips and the search pill and wrap onto the next line
 * with the chips when the line is full. Nothing is squeezed, clipped or hidden to make the other fit.
 *
 * The location notice asks for the permission or opens Settings, whichever [pill] names. The Map offline
 * notice is a status and not a control, so it is a disabled button and does nothing when it is tapped.
 */
@Composable
internal fun FlowRowScope.HomeNotices(
    notices: ImmutableList<HomeNotice>,
    pill: LocationPill,
    onLocationClick: () -> Unit,
) {
    notices.forEach { notice ->
        key(notice) {
            when (notice) {
                HomeNotice.LocationPermission -> if (pill != LocationPill.Hidden) {
                    FloatingStatusButton(
                        text = LOCATION_PILL_TEXT,
                        tag = TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL,
                        onClick = onLocationClick,
                    )
                }
                HomeNotice.MapOffline -> FloatingStatusButton(
                    text = MAP_OFFLINE_TEXT,
                    tag = TripPlannerTestTags.HOME_MAP_OFFLINE_CHIP,
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.semantics { contentDescription = MAP_OFFLINE_TEXT },
                )
            }
        }
    }
}

// region Previews

@PreviewComponent
@Composable
private fun HomeNoticesBothPreview() {
    PreviewTheme {
        FlowRow {
            HomeNotices(
                notices = persistentListOf(HomeNotice.LocationPermission, HomeNotice.MapOffline),
                pill = LocationPill.Ask,
                onLocationClick = {},
            )
        }
    }
}

@PreviewComponent
@Composable
private fun HomeNoticesOfflineOnlyPreview() {
    PreviewTheme {
        FlowRow {
            HomeNotices(
                notices = persistentListOf(HomeNotice.MapOffline),
                pill = LocationPill.Hidden,
                onLocationClick = {},
            )
        }
    }
}

// endregion
