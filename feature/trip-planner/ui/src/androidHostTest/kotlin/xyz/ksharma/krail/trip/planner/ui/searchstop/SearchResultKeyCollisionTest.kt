package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * The results list mixes three kinds of result, each carrying an id from an unrelated source:
 * stop ids from the GTFS stop table, trip ids raw from the routes proto, address ids from the
 * geocoder's `stop_finder`. Nothing makes those id spaces disjoint, and the list was keyed on
 * the bare id, so one value appearing in two of them was enough to throw
 * "Key ... was already used" and take the screen down.
 *
 * Route results only appear when the query is exactly a bus route short name, which is also
 * when a short numeric id is most likely to be echoed by both sides. Rendering is the
 * assertion here: these fail by throwing, not by comparing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SearchResultKeyCollisionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val collidingId = "700"

    private fun stateWith(vararg results: SearchStopState.SearchResult) = SearchStopState(
        // The list renders listState.results, not searchResults.
        listState = ListState.Results(results = persistentListOf(*results)),
        searchQuery = "700",
    )

    @Test
    fun `a trip and a stop sharing an id both render`() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = stateWith(
                        SearchStopState.SearchResult.Trip(
                            tripId = collidingId,
                            routeShortName = "700",
                            headsign = "Parramatta",
                            transportMode = TransportMode.Bus,
                        ),
                        SearchStopState.SearchResult.Stop(
                            stopName = "Seven Hills Station",
                            stopId = collidingId,
                        ),
                    ),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText("Seven Hills Station").assertIsDisplayed()
    }

    @Test
    fun `an address and a stop sharing an id both render`() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = stateWith(
                        SearchStopState.SearchResult.Stop(
                            stopName = "Seven Hills Station",
                            stopId = collidingId,
                        ),
                        SearchStopState.SearchResult.Address(
                            addressId = collidingId,
                            displayName = "700 George St",
                            addressType = "street",
                        ),
                    ),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText("Seven Hills Station").assertIsDisplayed()
    }

    @Test
    fun `distinct stops still render side by side`() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = stateWith(
                        SearchStopState.SearchResult.Stop(
                            stopName = "Seven Hills Station",
                            stopId = "2147",
                        ),
                        SearchStopState.SearchResult.Stop(
                            stopName = "Blacktown Station",
                            stopId = "2148",
                        ),
                    ),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText("Seven Hills Station").assertIsDisplayed()
        composeRule.onNodeWithText("Blacktown Station").assertIsDisplayed()
    }
}
