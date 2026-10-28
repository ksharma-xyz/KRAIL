package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.Certainty
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardBody
import xyz.ksharma.krail.trip.planner.ui.components.JourneyCard
import xyz.ksharma.krail.trip.planner.ui.components.JourneyCardState
import xyz.ksharma.krail.trip.planner.ui.components.LegView
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

/**
 * Every scenario in docs/testing/CROWDING_SCENARIOS.md, drawn on each surface a rider sees it
 * on: the closed journey card, the opened leg card and the departure board. The words come from
 * CrowdingWording exactly as the mappers produce them, and the expected text is written out here,
 * so a wording change or a surface that stops drawing a line fails by scenario name.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class CrowdingScenarioUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Scenario(
        val name: String,
        val cars: String,
        val tag: String,
        val sentence: String?,
        val description: String,
    ) {
        val levels = CarriageLevels(cars.map { LETTERS.getValue(it) })
    }

    private val scenarios = listOf(
        Scenario(
            name = "Seats all along",
            cars = "SSSSSSSS",
            tag = "Seats available",
            sentence = "Seats expected all along the train.",
            description = "Carriages: Seats available",
        ),
        Scenario(
            name = "Few seats all along",
            cars = "FFFFFFFF",
            tag = "Few seats",
            sentence = "A few seats expected along the train.",
            description = "Carriages: Few seats",
        ),
        Scenario(
            name = "Standing all along",
            cars = "XXXXXXXX",
            tag = "Standing only",
            sentence = "Standing only expected all along the train.",
            description = "Carriages: Standing only",
        ),
        Scenario(
            name = "Seats at both ends",
            cars = "SSSFFSSS",
            tag = "Seats at both ends",
            sentence = "Seats expected at both ends.",
            description = "Carriages: Seats available, Few seats",
        ),
        Scenario(
            name = "Few seats at both ends",
            cars = "FXXXXXXF",
            tag = "Few seats at both ends",
            sentence = "A few seats expected at both ends.",
            description = "Carriages: Few seats, Standing only",
        ),
        Scenario(
            name = "Seats in the middle",
            cars = "XSSSSSSX",
            tag = "Seats in the middle",
            sentence = "Seats expected in the middle.",
            description = "Carriages: Seats available, Standing only",
        ),
        Scenario(
            name = "More seats at the back than the front",
            cars = "SSSXXXXS",
            tag = "Seats at the back",
            sentence = "Seats expected at the back.",
            description = "Carriages: Seats available, Standing only",
        ),
        Scenario(
            name = "Seats at the back only",
            cars = "SSFFFFFF",
            tag = "Seats at the back",
            sentence = "Seats expected at the back.",
            description = "Carriages: Seats available, Few seats",
        ),
        Scenario(
            name = "Seats at the front only",
            cars = "FFFFFFSS",
            tag = "Seats at the front",
            sentence = "Seats expected at the front.",
            description = "Carriages: Seats available, Few seats",
        ),
    )

    @Test
    fun closedJourneyCardShowsEachScenarioTag() {
        var scenario by mutableStateOf(scenarios.first())
        composeRule.setContent {
            PreviewTheme {
                JourneyCard(
                    timeToDeparture = "in 12 mins",
                    originTime = "8:23am",
                    destinationTime = "8:40am",
                    totalTravelTime = "17 mins",
                    platformNumber = "1",
                    platformText = "Platform 1",
                    transportModeLineList = persistentListOf(
                        TransportModeLine(transportMode = TransportMode.Train, lineName = "T9"),
                    ),
                    legList = persistentListOf(),
                    cardState = JourneyCardState.DEFAULT,
                    totalWalkTime = null,
                    totalUniqueServiceAlerts = 0,
                    onClick = {},
                    seatTag = CrowdingWording.carriageTag(scenario.levels, Certainty.FORECAST),
                    seatCars = scenario.levels,
                )
            }
        }
        scenarios.forEach { next ->
            scenario = next
            composeRule.waitForIdle()
            composeRule.onNodeWithText(next.tag).assertExists(next.name)
        }
    }

    @Test
    fun openedLegCardShowsEachScenarioSentenceAndCars() {
        var scenario by mutableStateOf(scenarios.first())
        composeRule.setContent {
            PreviewTheme {
                LegView(
                    routeText = "towards Central via Strathfield",
                    transportModeLine = TransportModeLine(transportMode = TransportMode.Train, lineName = "T9"),
                    stops = persistentListOf(
                        TimeTableState.JourneyCardInfo.Stop("8:23am", "Epping Station, Platform 1", true),
                        TimeTableState.JourneyCardInfo.Stop("8:40am", "Burwood Station, Platform 1", true),
                    ),
                    carriages = scenario.levels,
                    carriageSentence = CrowdingWording.forCarriages(scenario.levels, Certainty.FORECAST)?.sentence,
                )
            }
        }
        scenarios.forEach { next ->
            scenario = next
            composeRule.waitForIdle()
            if (next.sentence != null) {
                composeRule.onNodeWithText(next.sentence).assertExists(next.name)
            }
            composeRule.onNodeWithTag(CARRIAGE_ROW_TAG, useUnmergedTree = true)
                .assertContentDescriptionEquals(next.description)
        }
    }

    @Test
    fun departureRowShowsEachScenarioTagAndOpensToItsSentence() {
        var state by mutableStateOf(DeparturesState(isLoading = false))
        composeRule.setContent {
            PreviewTheme {
                DepartureBoardBody(state = state, onRetry = {}, onLoadPreviousDepartures = {})
            }
        }
        scenarios.forEachIndexed { index, scenario ->
            // A different run each time, so every scenario's row starts closed.
            state = DeparturesState(isLoading = false, departures = persistentListOf(departure(scenario, index)))
            composeRule.waitForIdle()
            composeRule.onNodeWithText(scenario.tag).assertExists(scenario.name)
            composeRule.onNodeWithText(DESTINATION).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(CARRIAGE_ROW_TAG, useUnmergedTree = true)
                .assertContentDescriptionEquals(scenario.description)
            if (scenario.sentence != null) {
                composeRule.onNodeWithText(scenario.sentence).assertExists(scenario.name)
            }
        }
    }

    private fun departure(scenario: Scenario, run: Int) = StopDeparture(
        lineNumber = "T9",
        lineColorCode = "#D11F2F",
        transportModeName = "Train",
        destinationName = DESTINATION,
        departureTimeText = "8:23 am",
        departureUtcDateTime = "2026-10-26T21:23:00Z",
        relativeTimeText = "in 7 mins",
        realtimeTripId = "191E.1324.165.12.A.8.9200214$run",
        carriages = scenario.levels,
        carriageSentence = CrowdingWording.forCarriages(scenario.levels, Certainty.FORECAST)?.sentence,
        carriageTag = CrowdingWording.carriageTag(scenario.levels, Certainty.FORECAST),
    )

    private companion object {
        const val DESTINATION = "Central via Strathfield"
        val LETTERS = mapOf(
            'S' to SeatLevel.SEATS_AVAILABLE,
            'F' to SeatLevel.FEW_SEATS,
            'X' to SeatLevel.STANDING_ONLY,
        )
    }
}
