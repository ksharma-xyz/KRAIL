package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationStatusPill
import xyz.ksharma.krail.trip.planner.ui.searchstop.StatusPill

/** What the Map offline chip says. The same words the design names it by. */
internal const val MAP_OFFLINE_TEXT = "Map offline"

/**
 * The notices under the home top row, rule 24 of `docs/MAP_HOME_UX.md`: the location status pill and the
 * Map offline chip, centred. Each is one whole item in a wrapping row, so with both showing they sit side
 * by side while there is room and one under the other when there is not. Nothing is squeezed, clipped or
 * hidden to make the other fit, which is what a [FlowRow] of whole items gives at any width and font scale.
 *
 * The location pill is the same pill Search Stop draws, asking for the permission or opening Settings. The
 * Map offline chip is that pill's twin, a status and not a control, so it does nothing when it is tapped.
 */
@Composable
internal fun HomeNoticesRow(
    notices: ImmutableList<HomeNotice>,
    pill: LocationPill,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (notices.isEmpty()) return
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = modifier
            .testTag(TripPlannerTestTags.HOME_MAP_NOTICES)
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        notices.forEach { notice ->
            key(notice) {
                when (notice) {
                    HomeNotice.LocationPermission -> LocationStatusPill(pill = pill, onClick = onLocationClick)
                    HomeNotice.MapOffline -> StatusPill(
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
}

// region Previews

@PreviewComponent
@Composable
private fun HomeNoticesBothPreview() {
    PreviewTheme {
        HomeNoticesRow(
            notices = kotlinx.collections.immutable.persistentListOf(
                HomeNotice.LocationPermission,
                HomeNotice.MapOffline,
            ),
            pill = LocationPill.Ask,
            onLocationClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun HomeNoticesOfflineOnlyPreview() {
    PreviewTheme {
        HomeNoticesRow(
            notices = kotlinx.collections.immutable.persistentListOf(HomeNotice.MapOffline),
            pill = LocationPill.Hidden,
            onLocationClick = {},
        )
    }
}

// endregion
