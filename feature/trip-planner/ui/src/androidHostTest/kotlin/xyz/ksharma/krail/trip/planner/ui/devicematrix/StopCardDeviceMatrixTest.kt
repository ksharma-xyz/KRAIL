package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCard
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardAction
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardPlacement

/**
 * The stop card, with its departure board, on every device profile (`docs/testing/DEVICE_PROFILES.md`). It floats at the bottom of
 * the map pane, so its close button, its action and its board heading must stand inside the safe area above the bottom bar and
 * clear of a side strip, and both the closed and the open board are held to it. It is a component and not a screen, so the
 * adoption guard does not require it; it is here because a floating card at the bottom is exactly where a bar takes a control.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class StopCardDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private val stop = NearbyStopFeature(
        stopId = "200060",
        stopName = "Central Station",
        position = LatLng(-33.8830, 151.2070),
        transportModes = persistentListOf(TransportMode.Train, TransportMode.Bus),
    )

    private val board = DeparturesState(
        isLoading = false,
        departures = persistentListOf(row("T1"), row("T4"), row("333", "Bus")),
    )

    @Test
    fun `the stop card keeps its close button and its action inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "StopCard") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                        StopCard(
                            stop = stop,
                            departures = board,
                            onEvent = {},
                            onClose = {},
                            boardOpenAtStart = false,
                            actions = { StopCardAction(text = "Select stop", onClick = {}) },
                        )
                    }
                }
            }

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(StopCard.CLOSE))
        }

    @Test
    fun `the stop card with its board open keeps every control inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "StopCard") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                        StopCard(
                            stop = stop,
                            departures = board,
                            onEvent = {},
                            onClose = {},
                            boardOpenAtStart = true,
                            actions = { StopCardAction(text = "Select stop", onClick = {}) },
                        )
                    }
                }
            }

            assertNothingInteractiveUnderSystemBars()
        }

    private fun row(line: String, mode: String = "Train") = StopDeparture(
        lineNumber = line,
        lineColorCode = "#F99D1C",
        transportModeName = mode,
        destinationName = "Anywhere",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2030-01-01T01:30:00Z",
    )
}
