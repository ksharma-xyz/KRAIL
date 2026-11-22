package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.CONFIRM
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.DESTINATION
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.ORIGIN
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.PARK_RIDE
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.THEME
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroStepFlowTest {

    private val card = ParkRideCardPhase.Card(ParkRideCardStation(stationId = "1", stationName = "Bella Vista"))

    private val untouched = ON_PARK_RIDE
    private val declined = IntroState(parkRideAnswer = ParkRideAnswer.NO)
    private val cardShown = IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = card)
    private val noStationNear = IntroState(
        parkRideAnswer = ParkRideAnswer.YES,
        parkRideCard = ParkRideCardPhase.NoStationNear,
    )
    private val noLocation = IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.None)
    private val parkRideStart = IntroState(
        parkRideAnswer = ParkRideAnswer.YES,
        parkRideCard = card,
        addedParkRide = true,
        startStopId = "1",
        startName = "Bella Vista",
        startFromParkRide = true,
    )

    // The order of the screens the rider meets, for each state the flow can be in, and for each first screen. The
    // theme screen is last and never skipped, so a first screen after it leaves it alone.
    private val orders = listOf(
        row("nothing answered", untouched, PARK_RIDE, PARK_RIDE, DESTINATION, ORIGIN, CONFIRM, THEME),
        row("answered no", declined, PARK_RIDE, PARK_RIDE, DESTINATION, ORIGIN, CONFIRM, THEME),
        row("a card is showing", cardShown, PARK_RIDE, PARK_RIDE, DESTINATION, ORIGIN, CONFIRM, THEME),
        row("no car park near", noStationNear, PARK_RIDE, DESTINATION, ORIGIN, CONFIRM, THEME),
        row("no location", noLocation, PARK_RIDE, DESTINATION, ORIGIN, CONFIRM, THEME),
        row("a car park is the start", parkRideStart, PARK_RIDE, PARK_RIDE, DESTINATION, CONFIRM, THEME),
        row("opening on the destination screen", untouched, DESTINATION, DESTINATION, ORIGIN, CONFIRM, THEME),
        row("opening on the theme screen", untouched, THEME, THEME),
    )

    @Test
    fun `the screens are shown in the order the table says`() {
        orders.forEach { row ->
            // Standing on the last screen, which is never skipped, so the order shows only what the rules decide.
            assertEquals(row.order, row.state.copy(step = THEME).stepsInFlow(row.first), row.name)
        }
    }

    @Test
    fun `after is the next screen in that order, and null on the last`() {
        orders.forEach { row ->
            row.order.forEachIndexed { index, step ->
                val at = row.state.copy(step = step)
                assertEquals(row.order.getOrNull(index + 1), at.stepAfter(row.first), "${row.name}: after $step")
            }
        }
    }

    @Test
    fun `the default flow is the five screens in order, opening on the Park and Ride screen and ending on the theme`() {
        assertEquals(PARK_RIDE, IntroState().step)
        assertEquals(
            listOf(PARK_RIDE, DESTINATION, ORIGIN, CONFIRM, THEME),
            untouched.copy(step = THEME).stepsInFlow(),
        )
        assertEquals(DESTINATION, untouched.copy(step = PARK_RIDE).stepAfter())
        assertEquals(DESTINATION, untouched.movedOn().step)
        assertEquals(THEME, untouched.copy(step = CONFIRM).stepAfter())
    }

    @Test
    fun `from the Park and Ride screen the flow passes over nothing but a screen with nothing to decide`() {
        assertEquals(DESTINATION, noStationNear.copy(step = PARK_RIDE).stepAfter())
        assertEquals(DESTINATION, declined.copy(step = PARK_RIDE).stepAfter())
    }

    @Test
    fun `the last screen has nothing after it`() {
        assertNull(untouched.copy(step = THEME).stepAfter())
    }

    @Test
    fun `the screen the rider is on stays in the order even when it has just become skippable`() {
        val onIt = noStationNear.copy(step = PARK_RIDE)

        assertEquals(listOf(PARK_RIDE, DESTINATION, ORIGIN, CONFIRM, THEME), onIt.stepsInFlow(PARK_RIDE))
        assertEquals(DESTINATION, onIt.stepAfter(PARK_RIDE))
    }

    @Test
    fun `the Park and Ride screen has nothing to decide only after no car park or no location`() {
        assertFalse(untouched.parkRideHasNothingToDecide(), "unanswered")
        assertFalse(declined.parkRideHasNothingToDecide(), "answered no, the rider may change their mind")
        assertFalse(cardShown.parkRideHasNothingToDecide(), "a card to decide on")
        assertFalse(
            IntroState(parkRideAnswer = ParkRideAnswer.YES, parkRideCard = ParkRideCardPhase.Locating())
                .parkRideHasNothingToDecide(),
            "still looking",
        )
        assertTrue(noStationNear.parkRideHasNothingToDecide(), "no car park near")
        assertTrue(noLocation.parkRideHasNothingToDecide(), "denied or no fix")
    }

    @Test
    fun `leaving Park and Ride moves to the next screen only while the rider is still on it`() {
        assertEquals(DESTINATION, untouched.leavingParkRide(PARK_RIDE).step)
        assertEquals(DESTINATION, noStationNear.copy(step = PARK_RIDE).leavingParkRide().step)
        // A look that ends after the rider is on another screen never pulls them forward.
        assertEquals(THEME, untouched.copy(step = THEME).leavingParkRide().step)
        assertEquals(DESTINATION, untouched.copy(step = DESTINATION).leavingParkRide().step)
    }

    @Test
    fun `moving on from the last screen stays on it`() {
        assertEquals(THEME, untouched.copy(step = THEME).movedOn().step)
    }

    @Test
    fun `only the theme screen with a theme chosen can finish the Intro`() {
        val chosen = KrailThemeStyle.Train.id

        assertEquals(
            mapOf(
                PARK_RIDE to false,
                DESTINATION to false,
                ORIGIN to false,
                CONFIRM to false,
                THEME to true,
            ),
            IntroStep.entries.associateWith { untouched.copy(step = it, selectedThemeId = chosen).canFinish() },
        )
        assertFalse(untouched.copy(step = THEME, selectedThemeId = null).canFinish(), "no theme chosen")
    }

    @Test
    fun `when a car park is the start the origin screen is passed over`() {
        val onDestination = parkRideStart.copy(step = DESTINATION)

        assertEquals(CONFIRM, onDestination.stepAfter(), "forward from the destination")
        assertEquals(CONFIRM, onDestination.movedOn().step)
    }

    @Test
    fun `without a car park start the origin screen sits between the destination and the confirm screen`() {
        val onDestination = declined.copy(step = DESTINATION)

        assertEquals(ORIGIN, onDestination.stepAfter())
    }

    @Test
    fun `a destination that is the car park station puts the origin screen back, because the start is cleared`() {
        val onDestination = parkRideStart.copy(step = DESTINATION)

        val picked = onDestination.withPick(isStart = false, stopId = "1", stopName = "Bella Vista")

        assertFalse(picked.startFromParkRide)
        assertEquals(ORIGIN, picked.stepAfter())
    }
}

private class Row(
    val name: String,
    val state: IntroState,
    val first: IntroStep,
    val order: List<IntroStep>,
)

private fun row(name: String, state: IntroState, first: IntroStep, vararg order: IntroStep) =
    Row(name, state, first, order.toList())
