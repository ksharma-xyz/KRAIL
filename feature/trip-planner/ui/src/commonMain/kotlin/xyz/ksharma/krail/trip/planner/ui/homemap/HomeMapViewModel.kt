package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * What the home map needs that the saved trips screen does not already hold: whether there is a map at
 * all, whether it is offline, and the Home, Work and `P` pins with their positions.
 */
@Stable
internal data class HomeMapState(
    val isMapsAvailable: Boolean = false,
    val mapOffline: Boolean = false,
    val labelPins: ImmutableList<LabelPin> = persistentListOf(),
    val parkRidePins: ImmutableList<ParkRidePin> = persistentListOf(),
)

/** The saved labels and the saved stations' tiles the screen already holds, which the pins are drawn from. */
@Stable
internal data class HomeMapInputs(
    val labels: ImmutableList<StopLabel> = persistentListOf(),
    val tiles: ImmutableList<ParkRideTile> = persistentListOf(),
)

internal sealed interface HomeMapEvent {
    /** The labels or the saved stations changed, so the pins are worked out again. */
    data class InputsChanged(val inputs: HomeMapInputs) : HomeMapEvent
}

/**
 * Works out the home map's pins and its offline notice, from state the saved trips screen already has.
 *
 * **Cost.** Nothing here is a network call. The pins cost one local `selectStopCoordinatesBatch` per
 * change of the labels or the saved stations, on the I/O dispatcher, and nothing when neither has a
 * stop. The offline notice reads the app-wide connectivity flow, which already exists and is free to
 * read. With the maps flag off the ViewModel does no work at all, so today's home costs what it did.
 *
 * It holds nothing the rider typed: the inputs are sent again by the screen each time it is built,
 * so a recreated ViewModel is told what to draw.
 */
internal class HomeMapViewModel(
    private val sandook: Sandook,
    connectivity: ConnectivityObserver,
    flag: Flag,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val mapsAvailable: Boolean =
        flag.getFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key).asBoolean(fallback = false)

    private val inputs = MutableStateFlow(HomeMapInputs())

    /**
     * Started only while the screen is in front and for a short grace after, so nothing is worked out
     * for a home that is behind another screen.
     */
    val uiState: StateFlow<HomeMapState> = combine(
        connectivity.state,
        inputs.map(::resolvePins).flowOn(ioDispatcher),
    ) { transport, pins ->
        HomeMapState(
            isMapsAvailable = mapsAvailable,
            mapOffline = mapsAvailable && mapOfflineFor(transport),
            labelPins = pins.labels,
            parkRidePins = pins.parkRide,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeMapState(isMapsAvailable = mapsAvailable),
    )

    fun onEvent(event: HomeMapEvent) {
        when (event) {
            is HomeMapEvent.InputsChanged -> if (mapsAvailable) inputs.value = event.inputs
        }
    }

    private fun resolvePins(inputs: HomeMapInputs): HomePins {
        if (!mapsAvailable) return HomePins()
        val stopIds = pinStopIdsFor(inputs.labels, inputs.tiles)
        val coordinates = if (stopIds.isEmpty()) emptyMap() else sandook.selectStopCoordinatesBatch(stopIds)
        return HomePins(
            parkRide = parkRidePinsFor(inputs.tiles, coordinates),
            labels = labelPinsFor(inputs.labels, coordinates),
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
