package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.CONFIRM
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.DESTINATION
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.ORIGIN
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.PARK_RIDE
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep.THEME
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IntroEmojiTest {

    private val approved = FestivalManager.commonEmojiList

    @Test
    fun `each screen has the emoji the table says, read from the approved list or the two exceptions`() {
        // The list is read by index and never typed: 5 is the alarm clock, 6 the auto rickshaw, 0 the scooter.
        assertEquals(approved[5], introEmojiFor(THEME))
        assertEquals(approved[6], introEmojiFor(PARK_RIDE))
        assertEquals(INTRO_PIN_EMOJI, introEmojiFor(DESTINATION))
        assertEquals(approved[0], introEmojiFor(ORIGIN))
        assertEquals(INTRO_STAR_EMOJI, introEmojiFor(CONFIRM))
    }

    @Test
    fun `the Park and Ride screen has a transport emoji from the list`() {
        assertTrue(introEmojiFor(PARK_RIDE) in approved)
    }

    @Test
    fun `every emoji the Intro uses is on the approved list or is the pin or the star`() {
        IntroStep.entries.forEach { step ->
            val emoji = introEmojiFor(step)

            assertTrue(emoji in approved || emoji == INTRO_PIN_EMOJI || emoji == INTRO_STAR_EMOJI, "$step")
        }
    }

    @Test
    fun `the pin and the star are not on the list, so they stay declared as the exceptions they are`() {
        assertTrue(INTRO_PIN_EMOJI !in approved)
        assertTrue(INTRO_STAR_EMOJI !in approved)
    }

    @Test
    fun `no two screens in a row share an emoji, for a list of any size from two up`() {
        (2..MAX_LIST).forEach { size ->
            val list = List(size) { "e$it" }
            IntroStep.entries.zipWithNext().forEach { (a, b) ->
                assertNotEquals(introEmojiFor(a, list), introEmojiFor(b, list), "size $size: $a then $b")
            }
        }
    }

    @Test
    fun `no two screens the rider meets in a row share an emoji, with the origin screen passed over or not`() {
        val orders = listOf(
            IntroState().copy(startFromParkRide = false),
            IntroState().copy(startFromParkRide = true),
        ).map { it.stepsInFlow(first = THEME) }

        assertTrue(ORIGIN in orders.first() && ORIGIN !in orders.last())
        orders.forEach { order ->
            order.zipWithNext().forEach { (a, b) ->
                assertNotEquals(introEmojiFor(a), introEmojiFor(b), "$a then $b")
            }
        }
    }

    @Test
    fun `a list of any size gives an entry of that list, wrapping round the end`() {
        (1..MAX_LIST).forEach { size ->
            val list = List(size) { "e$it" }
            listOf(THEME, PARK_RIDE, ORIGIN).forEach { step ->
                assertEquals(list[(step.ordinal + 5) % size], introEmojiFor(step, list), "size $size $step")
            }
        }
    }

    @Test
    fun `the same step and list always give the same emoji`() {
        IntroStep.entries.forEach { step -> assertEquals(introEmojiFor(step), introEmojiFor(step), "$step") }
    }

    @Test
    fun `an empty list gives nothing for the screens that use it and leaves the two exceptions alone`() {
        val none = emptyList<String>()

        assertEquals("", introEmojiFor(THEME, none))
        assertEquals("", introEmojiFor(PARK_RIDE, none))
        assertEquals("", introEmojiFor(ORIGIN, none))
        assertEquals(INTRO_PIN_EMOJI, introEmojiFor(DESTINATION, none))
        assertEquals(INTRO_STAR_EMOJI, introEmojiFor(CONFIRM, none))
    }

    private companion object {
        const val MAX_LIST = 12
    }
}
