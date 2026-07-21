package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI state for the Park & Ride picker — the searchable list of stations the app supports.
 */
@Stable
data class AddParkRideState(
    val stations: ImmutableList<ParkRideStationPickerItem> = persistentListOf(),
    val isLoading: Boolean = true,
    /**
     * Emoji and greeting for the loading state, from the same festival source the timetable
     * uses — so a loading screen looks like the app, not like this one screen.
     */
    val loadingEmoji: LoadingEmoji? = null,
    val error: ErrorKind? = null,
    val query: String = "",
    /** The station whose detail sheet is open, if any. */
    val selectedStation: ParkRideStationPickerItem? = null,
    /**
     * Availability for [selectedStation]'s car parks, read from the local store.
     *
     * Empty until a fetch lands. Cached rows show immediately, so re-opening a station that
     * was loaded recently is instant and costs no API call.
     */
    val selectedStationDetails: ImmutableList<ParkRideUiState.ParkRideFacilityDetail> =
        persistentListOf(),
    /** True only while a network fetch for [selectedStation] is in flight. */
    val isLoadingSelectedStation: Boolean = false,
) {

    /**
     * The rows the picker should render for the current [query].
     *
     * Matching is a plain case-insensitive contains over the station name, its car park names
     * and the stop name — a rider searching "Tallawong" should find it whether the catalogue
     * calls the car park "Tallawong P1" or names the station.
     */
    val visibleStations: List<ParkRideStationPickerItem>
        get() = if (query.isBlank()) {
            stations
        } else {
            stations.filter { it.matches(query) }
        }

    /**
     * [visibleStations] split into address-book style alphabetical sections.
     *
     * Sections are derived rather than stored so they always match the current [query] —
     * searching narrows the letters shown instead of leaving empty headers behind. Stations
     * whose name does not start with a letter are collected under "#", so nothing is dropped.
     */
    val sections: List<StationSection>
        get() = visibleStations
            .groupBy { station -> station.sectionLetter() }
            .map { (letter, stations) -> StationSection(letter = letter, stations = stations) }
            // "#" sorts after Z, the way a contacts list orders it.
            .sortedWith(compareBy({ it.letter == NON_ALPHA_SECTION }, { it.letter }))

    @Stable
    data class StationSection(
        val letter: String,
        val stations: List<ParkRideStationPickerItem>,
    )

    enum class ErrorKind {
        /** Remote Config returned no facilities — nothing to offer, and retrying may help. */
        NoFacilities,
    }

    /**
     * One station in the picker, which may cover several car parks.
     *
     * A station is not the same thing as a stop ID or a facility ID: NSW data collides in both
     * directions. Tallawong is one stop (`2155384`) with three car parks (P1/P2/P3), while
     * Mona Vale is one car park (facility `12`) reachable from two stop IDs. Either shape
     * would produce duplicate rows if the list were keyed on one ID alone, so entries are
     * grouped into connected components over the (stopId, facilityId) pairs and shown once.
     *
     * @param stationId stable identity for the group, used as the list key.
     * @param mappings every (stopId, facilityId) pair the group covers. Adding the station
     * stores all of them, so the home card lists all of its car parks together.
     * @param isUserAdded the rider added this station themselves (source `UserAdded`), so it
     * is theirs to remove.
     * @param isFromSavedTrip a saved trip passes through this station, so its card is on the
     * home screen whether or not the rider added it. Shown as added, because it is, but it
     * cannot be removed here — the trip owns it.
     */
    @Stable
    data class ParkRideStationPickerItem(
        val stationId: String,
        val stationName: String,
        val stopName: String,
        val carParkNames: List<String>,
        val mappings: List<ParkRideMapping>,
        val isUserAdded: Boolean = false,
        val isFromSavedTrip: Boolean = false,
        /**
         * Position of the station, read from the local GTFS stops table. Null when the stop ID
         * is not in the local database, in which case the station is simply not plotted — the
         * list row still works.
         */
        val position: StationPosition? = null,
    ) {
        /**
         * Ticked when the station's card is on the home screen for any reason. Showing only
         * user-added stations as ticked would claim "not added" for a station the rider can
         * plainly see on their home screen.
         */
        val added: Boolean get() = isUserAdded || isFromSavedTrip

        /** A saved trip is holding this station, so the picker cannot remove it. */
        val isLockedBySavedTrip: Boolean get() = isFromSavedTrip && !isUserAdded

        val carParkCount: Int get() = mappings.map { it.facilityId }.distinct().size

        fun matches(query: String): Boolean =
            stationName.contains(query, ignoreCase = true) ||
                stopName.contains(query, ignoreCase = true) ||
                carParkNames.any { it.contains(query, ignoreCase = true) }
    }

    @Stable
    data class ParkRideMapping(
        val stopId: String,
        val facilityId: String,
        val facilityName: String,
    )

    @Stable
    data class StationPosition(val latitude: Double, val longitude: Double)

    @Stable
    data class LoadingEmoji(val emoji: String, val greeting: String)

    companion object {
        const val NON_ALPHA_SECTION = "#"
    }
}

private fun AddParkRideState.ParkRideStationPickerItem.sectionLetter(): String {
    val first = stationName.firstOrNull() ?: return AddParkRideState.NON_ALPHA_SECTION
    return if (first.isLetter()) first.uppercase() else AddParkRideState.NON_ALPHA_SECTION
}

/**
 * Events raised by the Park & Ride picker.
 */
sealed interface AddParkRideUiEvent {

    data class SearchQueryChanged(val query: String) : AddParkRideUiEvent

    /** Adds every car park at the station when not yet added, removes them all when added. */
    data class ToggleStation(
        val item: AddParkRideState.ParkRideStationPickerItem,
    ) : AddParkRideUiEvent

    /** A map pin was tapped: open its sheet and load availability if it is off cooldown. */
    data class StationSelected(
        val item: AddParkRideState.ParkRideStationPickerItem,
    ) : AddParkRideUiEvent

    data object StationDismissed : AddParkRideUiEvent

    /** Hand off to the device's default maps app for driving directions. */
    data class DirectionsClicked(
        val position: AddParkRideState.StationPosition,
        val stationName: String,
    ) : AddParkRideUiEvent

    data object Retry : AddParkRideUiEvent
}
