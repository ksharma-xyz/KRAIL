package xyz.ksharma.krail.feature.track.network.crowding

import com.google.transit.realtime.FeedMessage
import xyz.ksharma.krail.feature.crowding.Certainty
import xyz.ksharma.krail.feature.crowding.CrowdingWording
import xyz.ksharma.krail.feature.crowding.SeatLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Every captured scenario through the real chain: feed bytes, decoded by Wire, indexed, then
 * worded. A change that alters what a rider reads for real TfNSW data fails here by name.
 */
class CrowdingScenarioTest {

    private val index = FeedMessage.ADAPTER.decode(CrowdingScenarioFeedFixture.bytes()).toCarriageIndex()

    @Test
    fun `every captured scenario decodes to its cars`() {
        CrowdingScenarioFeedFixture.scenarios.forEach { scenario ->
            val cars = assertNotNull(index.at(scenario.tripId, scenario.stopId), scenario.name)
            assertEquals(scenario.cars, cars.levels.joinToString("") { it.letter() }, scenario.name)
        }
    }

    @Test
    fun `every captured scenario shows its tag and sentence`() {
        CrowdingScenarioFeedFixture.scenarios.forEach { scenario ->
            val cars = index.at(scenario.tripId, scenario.stopId)!!
            assertEquals(scenario.tag, CrowdingWording.carriageTag(cars, Certainty.FORECAST), scenario.name)
            assertEquals(
                scenario.sentence,
                CrowdingWording.forCarriages(cars, Certainty.FORECAST)?.sentence,
                scenario.name,
            )
        }
    }

    @Test
    fun `at night every captured scenario says the Nightsafe line`() {
        CrowdingScenarioFeedFixture.scenarios.forEach { scenario ->
            val cars = index.at(scenario.tripId, scenario.stopId)!!
            assertEquals(
                CrowdingWording.NIGHT_LINE,
                CrowdingWording.forCarriages(cars, Certainty.FORECAST, isNight = true)?.sentence,
                scenario.name,
            )
        }
    }

    private fun SeatLevel.letter() = when (this) {
        SeatLevel.SEATS_AVAILABLE -> "S"
        SeatLevel.FEW_SEATS -> "F"
        SeatLevel.STANDING_ONLY -> "X"
    }
}
