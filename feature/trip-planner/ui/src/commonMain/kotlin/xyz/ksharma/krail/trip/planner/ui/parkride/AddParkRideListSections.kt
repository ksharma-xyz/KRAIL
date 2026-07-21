package xyz.ksharma.krail.trip.planner.ui.parkride

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.park.ride.ui.components.ParkRideFacilityRow
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.ActionData
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideIcon
import xyz.ksharma.krail.trip.planner.ui.components.loading.LoadingEmojiAnim
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ErrorKind
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideUiEvent

/**
 * The LazyColumn body of the Park & Ride picker: loading, error and the sectioned station
 * list.
 *
 * Kept out of [AddParkRideScreen] so neither file grows past detekt's per-file function
 * limit, and so the list content can be read without the surrounding scaffold.
 */

internal fun LazyListScope.loadingRows(loadingEmoji: AddParkRideState.LoadingEmoji?) {
    val emoji = loadingEmoji ?: return
    item(key = "loading") {
        // Same spinning-emoji treatment the timetable uses while it loads, so waiting looks
        // like the rest of the app rather than a bespoke placeholder.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LoadingEmojiAnim(
                emoji = emoji.emoji,
                modifier = Modifier.padding(vertical = LOADING_VERTICAL_PADDING),
            )

            Text(
                text = emoji.greeting,
                style = KrailTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KrailTheme.dimensions.spacingXL),
            )
        }
    }
}

internal fun LazyListScope.errorRow(
    error: ErrorKind,
    onEvent: (AddParkRideUiEvent) -> Unit,
) {
    item(key = "error") {
        when (error) {
            ErrorKind.NoFacilities -> ErrorMessage(
                emoji = "🅿️",
                title = "No stations yet",
                message = "We couldn't load the list of Park & Ride stations. Check your " +
                    "connection and try again.",
                actionData = ActionData(
                    actionText = "Try again",
                    onActionClick = { onEvent(AddParkRideUiEvent.Retry) },
                ),
                modifier = Modifier.padding(vertical = KrailTheme.dimensions.spacingXL),
            )
        }
    }
}

internal fun LazyListScope.stationRows(
    state: AddParkRideState,
    onEvent: (AddParkRideUiEvent) -> Unit,
) {
    item(key = "picker-description") {
        Text(
            // No em dash in rider-facing copy.
            text = "Pick any Sydney car park and its live parking shows on your home screen.",
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.softLabel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
                .padding(top = KrailTheme.dimensions.spacingXS, bottom = KrailTheme.dimensions.spacingL),
        )
    }

    if (state.visibleStations.isEmpty()) {
        item(key = "no-results") {
            // Same component and wording SearchStop uses for an empty result, so a failed
            // search looks the same wherever the rider is in the app.
            ErrorMessage(
                title = "No match found!",
                message = "Try something else. \uD83D\uDD0D✨",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = KrailTheme.dimensions.spacingXL),
            )
        }
        return
    }

    // Address-book style: one sticky letter header per group, so a long list can be scanned
    // and scrolled by initial rather than read end to end.
    state.sections.forEach { section ->
        stickyHeader(key = "section-${section.letter}") {
            SectionHeader(letter = section.letter)
        }

        items(
            items = section.stations,
            key = { station -> station.stationId },
        ) { station ->
            ParkRideFacilityRow(
                facilityName = station.stationName,
                stopName = station.subtitle(),
                added = station.added,
                onClick = { onEvent(AddParkRideUiEvent.ToggleStation(station)) },
                // Same badge the home Park & Ride card renders, so a station reads identically
                // in the picker and on the home screen.
                icon = { ParkRideIcon() },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun SectionHeader(letter: String, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Opaque so rows scrolling underneath the pinned header do not show through.
            // Background is applied before the padding so the whole pinned strip, including
            // the leading gap, stays opaque as rows pass behind it.
            .background(KrailTheme.colors.surface)
            // Generous space above each letter and little below: the header belongs to the
            // group under it, so the gap should read as a break from the previous group.
            .padding(top = dim.spacingXXL, bottom = dim.spacingXS)
            .padding(horizontal = dim.pageHorizontalPadding)
            .height(SectionHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = letter,
            style = KrailTheme.typography.titleSmall,
            color = KrailTheme.colors.softLabel,
        )
    }
}

/**
 * Explains the row's state in place of a transient message.
 *
 * A station held by a saved trip is ticked but cannot be un-ticked here, which would look
 * broken without a reason. Saying so on the row means the rider knows before they tap, and
 * the explanation stays on screen instead of vanishing like a snackbar.
 */
private fun AddParkRideState.ParkRideStationPickerItem.subtitle(): String = when {
    isLockedBySavedTrip -> "Added by your saved trip"
    carParkCount > 1 -> "$carParkCount car parks"
    else -> stopName
}

// Matches TimeTableScreen so the loading state sits at the same height across screens.
private val LOADING_VERTICAL_PADDING = 60.dp
private val SectionHeaderHeight = 24.dp
