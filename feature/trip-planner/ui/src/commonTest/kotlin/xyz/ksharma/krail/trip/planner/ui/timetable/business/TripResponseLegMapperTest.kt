package xyz.ksharma.krail.trip.planner.ui.timetable.business

import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [TripResponse.Leg.toWalkingLegUiModel].
 *
 * Background: this leg only sets a pin-row name (icon + label shown above/below the "Walk"
 * text) for the true first-mile/last-mile walk of a journey, using [isFirstLeg]/[isLastLeg] to
 * identify that position. Two behaviours were changed together and both need locking down:
 *
 * 1. The pin name used to require the endpoint be a searched address (`origin/destination.type`
 *    in `poi`/`singlehouse`/`street`). A rider whose destination is a named transit stop (e.g.
 *    "Town Hall Station") got no pin row at all for the final walk - exactly the case where a
 *    rider unfamiliar with the city most needs to know where they're walking to. The gate was
 *    removed: any first/last walking leg gets a pin row, regardless of endpoint type.
 * 2. NSW flags some first/last-mile footpath legs `footPathInfoRedundant = true` (e.g. Town
 *    Hall Station -> QVB, Stand C), which previously dropped the leg from the journey card
 *    entirely - silently omitting the only text telling the rider they need to walk before
 *    boarding. First/last legs now ignore that flag; interior/interchange legs still respect
 *    it (see TripResponseLegMapper.kt's kdoc for the revert note if this proves too noisy).
 */
class TripResponseLegMapperTest {

    //region pin row name - first/last leg, any endpoint type

    @Test
    fun `Given first leg with address origin When toWalkingLegUiModel Then originPinName is set`() {
        val leg = walkingLeg(originType = "street", originName = "123 Example St", originDisassembledName = null)

        val result = leg.toWalkingLegUiModel(isFirstLeg = true) as? TimeTableState.JourneyCardInfo.Leg.WalkingLeg

        assertEquals("123 Example St", result?.originPinName)
    }

    @Test
    fun `Given first leg with named-stop origin When toWalkingLegUiModel Then originPinName is still set`() {
        // Regression: previously gated on origin.isAddressLocation(), so a plain transit stop
        // (type "stop", not an address) got no pin row at all for the first-mile walk.
        val leg = walkingLeg(originType = "stop", originName = "Town Hall Station", originDisassembledName = null)

        val result = leg.toWalkingLegUiModel(isFirstLeg = true) as? TimeTableState.JourneyCardInfo.Leg.WalkingLeg

        assertEquals("Town Hall Station", result?.originPinName)
    }

    @Test
    fun `Given last leg with named-stop destination When toWalkingLegUiModel Then destinationPinName is set`() {
        // The exact reported case: destination is "Town Hall Station" (type "stop"), reached by
        // a final walk from QVB. Without this, a rider unfamiliar with the city had no on-card
        // indication of where the last walk actually leads.
        val leg = walkingLeg(
            destinationType = "stop",
            destinationName = "Town Hall Station",
            destinationDisassembledName = null,
        )

        val result = leg.toWalkingLegUiModel(isLastLeg = true) as? TimeTableState.JourneyCardInfo.Leg.WalkingLeg

        assertEquals("Town Hall Station", result?.destinationPinName)
    }

    @Test
    fun `Given interior leg When toWalkingLegUiModel Then neither pin name is set`() {
        // A walking leg between two transit legs (interchange), not the journey's true first
        // or last mile - must never get a pin row, regardless of endpoint type.
        val leg = walkingLeg(
            originType = "street",
            originName = "Some Address",
            destinationType = "street",
            destinationName = "Another Address",
        )

        val result = leg.toWalkingLegUiModel(isFirstLeg = false, isLastLeg = false)
            as? TimeTableState.JourneyCardInfo.Leg.WalkingLeg

        assertNull(result?.originPinName)
        assertNull(result?.destinationPinName)
    }

    @Test
    fun `Given first leg When toWalkingLegUiModel Then destinationPinName is not set`() {
        // Only the endpoint matching this leg's own position should get a name - the first
        // leg's destination is an interchange point, not the journey's final destination.
        val leg = walkingLeg(destinationType = "stop", destinationName = "Interchange Stop")

        val result = leg.toWalkingLegUiModel(isFirstLeg = true) as? TimeTableState.JourneyCardInfo.Leg.WalkingLeg

        assertNull(result?.destinationPinName)
    }

    @Test
    fun `Given origin with no disassembledName When toWalkingLegUiModel Then falls back to name`() {
        val leg = walkingLeg(originName = "Long Form Origin Name", originDisassembledName = null)

        val result = leg.toWalkingLegUiModel(isFirstLeg = true) as? TimeTableState.JourneyCardInfo.Leg.WalkingLeg

        assertEquals("Long Form Origin Name", result?.originPinName)
    }

    //endregion

    //region footPathInfoRedundant override - first/last leg only

    @Test
    fun `Given interior leg with footPathInfoRedundant true When toWalkingLegUiModel Then leg is dropped`() {
        // Unchanged existing behaviour: an interior redundant walk is genuinely covered
        // elsewhere (e.g. platform-transfer context), so it must still be dropped.
        val leg = walkingLeg(footPathInfoRedundant = true)

        val result = leg.toWalkingLegUiModel(isFirstLeg = false, isLastLeg = false)

        assertNull(result)
    }

    @Test
    fun `Given first leg with footPathInfoRedundant true When toWalkingLegUiModel Then leg is still shown`() {
        // The core experiment: NSW's redundant flag on a first-mile walk (e.g. Town Hall
        // Station -> QVB, Stand C) must no longer drop the only text telling the rider they
        // need to walk before boarding.
        val leg = walkingLeg(footPathInfoRedundant = true)

        val result = leg.toWalkingLegUiModel(isFirstLeg = true)

        assertEquals(true, result is TimeTableState.JourneyCardInfo.Leg.WalkingLeg)
    }

    @Test
    fun `Given last leg with footPathInfoRedundant true When toWalkingLegUiModel Then leg is still shown`() {
        val leg = walkingLeg(footPathInfoRedundant = true)

        val result = leg.toWalkingLegUiModel(isLastLeg = true)

        assertEquals(true, result is TimeTableState.JourneyCardInfo.Leg.WalkingLeg)
    }

    @Test
    fun `Given first leg with footPathInfoRedundant false When toWalkingLegUiModel Then leg is shown`() {
        // Baseline: the common case (flag absent/false) must be unaffected by the override.
        val leg = walkingLeg(footPathInfoRedundant = false)

        val result = leg.toWalkingLegUiModel(isFirstLeg = true)

        assertEquals(true, result is TimeTableState.JourneyCardInfo.Leg.WalkingLeg)
    }

    //endregion

    //region existing invariants preserved

    @Test
    fun `Given leg with no resolvable duration When toWalkingLegUiModel Then returns null regardless of leg position`() {
        val leg = TripResponse.Leg(
            duration = null,
            origin = null,
            destination = null,
        )

        val result = leg.toWalkingLegUiModel(isFirstLeg = true, isLastLeg = true)

        assertNull(result, "No displayDuration means nothing to render, even for a first/last leg")
    }

    //endregion

    //region helpers

    private fun walkingLeg(
        durationSeconds: Long = 120L,
        originType: String? = "stop",
        originName: String? = "Origin Stop",
        originDisassembledName: String? = "Origin Stop, Platform 1",
        destinationType: String? = "stop",
        destinationName: String? = "Destination Stop",
        destinationDisassembledName: String? = "Destination Stop, Platform 2",
        footPathInfoRedundant: Boolean? = null,
    ) = TripResponse.Leg(
        duration = durationSeconds,
        footPathInfoRedundant = footPathInfoRedundant,
        origin = TripResponse.StopSequence(
            name = originName,
            disassembledName = originDisassembledName,
            type = originType,
        ),
        destination = TripResponse.StopSequence(
            name = destinationName,
            disassembledName = destinationDisassembledName,
            type = destinationType,
        ),
    )

    //endregion
}
