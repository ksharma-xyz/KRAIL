package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

// The Home, Work and Park & Ride chips (docs/MAP_HOME_UX.md rule 41) in the three states a rider
// sees, one above the other: both unset, both set, and Home set with Work not. The narrow and wide
// previews sit in the `timetable.narrow` and `timetable.wide` packages, which their own snapshot
// tests scan, and draw the same frame. The 320 dp one at 2x font is the frame where the row is one
// sideways line with a fade at the right edge and the first chip fully visible.

private val previewHome = StopLabel(emoji = "🏠", label = "Home")
private val previewWork = StopLabel(emoji = "💼", label = "Work")

private val previewHomeSet = previewHome.copy(stopId = "200060", stopName = "Central Station")
private val previewWorkSet = previewWork.copy(stopId = "200070", stopName = "Town Hall Station")

/** The frame every canvas draws. */
@Composable
internal fun HomeChipRowStates() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = SAVED_TRIPS_COLUMN_MAX_WIDTH),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PreviewChips(previewHome, previewWork)
                PreviewChips(previewHomeSet, previewWorkSet)
                PreviewChips(previewHomeSet, previewWork)
            }
        }
    }
}

@Composable
private fun PreviewChips(home: StopLabel, work: StopLabel) {
    HomeChipRow(
        chips = homeChipsFor(persistentListOf(home, work)),
        onLabelClick = {},
        onLabelLongClick = {},
        onParkRideClick = {},
    )
}

/** A phone: the chips wrap whole at 100 percent text and scroll sideways at 2x. */
@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewHomeChipRowPhone() {
    HomeChipRowStates()
}
