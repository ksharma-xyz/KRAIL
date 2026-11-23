package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroEmojiTest {

    private val approved = FestivalManager.commonEmojiList

    private class Row(val name: String, val step: IntroStep, val themeId: Int?, val expected: String)

    @Test
    fun `each screen has the emoji the table says, read from the approved list or the declared exceptions`() {
        // The list is read by index and never typed: 0 is the scooter, a neutral ride that is no mode, and 2 is the rocket.
        val table = listOf(
            Row("theme, nothing chosen", THEME, null, approved[0]),
            Row("theme, Train", THEME, KrailThemeStyle.Train.id, INTRO_TRAIN_EMOJI),
            Row("theme, Metro", THEME, KrailThemeStyle.Metro.id, INTRO_METRO_EMOJI),
            Row("theme, Bus", THEME, KrailThemeStyle.Bus.id, INTRO_BUS_EMOJI),
            Row("theme, Ferry", THEME, KrailThemeStyle.Ferry.id, INTRO_FERRY_EMOJI),
            Row("theme, a fun theme the Intro does not offer", THEME, KrailThemeStyle.PurpleDrip.id, approved[0]),
            Row("theme, an id that is no theme", THEME, NOT_A_THEME, approved[0]),
            Row("park and ride", PARK_RIDE, null, INTRO_CAR_EMOJI),
            Row("destination", DESTINATION, null, INTRO_PIN_EMOJI),
            Row("origin", ORIGIN, null, approved[2]),
            Row("confirm", CONFIRM, null, INTRO_STAR_EMOJI),
        )

        table.forEach { row -> assertEquals(row.expected, introEmojiFor(row.step, row.themeId), row.name) }
    }

    @Test
    fun `the theme screen never shows the alarm clock, and a choice only changes the theme screen`() {
        val clock = approved.single { it.contains("\u23F0") }

        listOf(
            null,
            KrailThemeStyle.Train.id,
            KrailThemeStyle.Metro.id,
            KrailThemeStyle.Bus.id,
            KrailThemeStyle.Ferry.id,
        )
            .forEach { id -> assertNotEquals(clock, introEmojiFor(THEME, id), "theme $id") }
        IntroStep.entries.filter { it != THEME }.forEach { step ->
            assertEquals(introEmojiFor(step), introEmojiFor(step, KrailThemeStyle.Ferry.id), "$step")
            assertNotEquals(clock, introEmojiFor(step), "$step")
        }
    }

    @Test
    fun `each of the four themes has its own transport emoji, none of them on the festival list`() {
        val shown = listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry)
            .map { introEmojiFor(THEME, it.id) }

        assertEquals(shown.size, shown.toSet().size, "two themes share an emoji: $shown")
        shown.forEach { assertTrue(it !in approved, it) }
        assertNull(themeEmoji(null))
        assertNull(themeEmoji(NOT_A_THEME))
    }

    @Test
    fun `the transport emoji are the code points looked up from the Unicode names`() {
        val table = listOf(
            "TRAIN" to (INTRO_TRAIN_EMOJI to listOf(0x1F686)),
            "METRO" to (INTRO_METRO_EMOJI to listOf(0x1F687)),
            "BUS" to (INTRO_BUS_EMOJI to listOf(0x1F68C)),
            "FERRY with the emoji variation selector" to (INTRO_FERRY_EMOJI to listOf(0x26F4, 0xFE0F)),
            "AUTOMOBILE" to (INTRO_CAR_EMOJI to listOf(0x1F697)),
        )

        table.forEach { (name, pair) ->
            assertEquals(pair.second, pair.first.codePointList(), name)
        }
    }

    @Test
    fun `every emoji the Intro uses is on the approved list or is one of the declared exceptions`() {
        val allowed = approved + IntroDeclaredEmoji
        val choices = listOf<Int?>(null) + introThemeChoices().map { it.style.id }

        IntroStep.entries.forEach { step ->
            choices.forEach { id ->
                val emoji = introEmojiFor(step, id)

                assertTrue(emoji in allowed, "$step with $id gave $emoji")
            }
        }
    }

    @Test
    fun `the declared exceptions are not on the list, so they stay declared as the exceptions they are`() {
        IntroDeclaredEmoji.forEach { assertTrue(it !in approved, it) }
        assertEquals(IntroDeclaredEmoji.size, IntroDeclaredEmoji.toSet().size)
    }

    @Test
    fun `no two screens the rider meets in a row share an emoji, with the origin screen passed over or not`() {
        val orders = listOf(
            IntroState().copy(startFromParkRide = false),
            IntroState().copy(startFromParkRide = true),
        ).map { it.stepsInFlow() }

        assertTrue(ORIGIN in orders.first() && ORIGIN !in orders.last())
        orders.forEach { order ->
            order.zipWithNext().forEach { (a, b) ->
                assertNotEquals(introEmojiFor(a), introEmojiFor(b), "$a then $b")
            }
        }
    }

    @Test
    fun `the theme screen differs from the confirm screen before it for every choice, and Park and Ride from the next`() {
        (listOf<Int?>(null) + introThemeChoices().map { it.style.id }).forEach { id ->
            assertNotEquals(introEmojiFor(CONFIRM), introEmojiFor(THEME, id), "confirm then theme $id")
            assertNotEquals(introEmojiFor(PARK_RIDE), introEmojiFor(DESTINATION), "park and ride then destination")
        }
    }

    @Test
    fun `a list of any size gives an entry of that list for the origin and for an unchosen theme, wrapping round`() {
        (1..MAX_LIST).forEach { size ->
            val list = List(size) { "e$it" }

            assertEquals(list[0], introEmojiFor(THEME, null, list), "size $size theme")
            // The origin's entry is the third place, a number of its own, so a screen put before it moves nothing.
            assertEquals(list[2 % size], introEmojiFor(ORIGIN, null, list), "size $size origin")
        }
    }

    @Test
    fun `the five screens show five different emoji, against the real list, so the origin is not the scooter of the theme screen`() {
        val shown = listOf(
            "theme, nothing chosen" to introEmojiFor(THEME, null),
            "park and ride" to introEmojiFor(PARK_RIDE),
            "destination" to introEmojiFor(DESTINATION),
            "origin" to introEmojiFor(ORIGIN),
            "confirm" to introEmojiFor(CONFIRM),
        )

        assertEquals(5, shown.map { it.second }.toSet().size, "two screens share an emoji: $shown")
        shown.forEach { (name, emoji) -> assertTrue(emoji.isNotEmpty(), "$name has none") }
        assertEquals(approved[2], introEmojiFor(ORIGIN), "the origin is the third entry of the list")
        assertEquals(listOf(0x1F680), introEmojiFor(ORIGIN).codePointList(), "and the third entry is the rocket")
        assertNotEquals(introEmojiFor(THEME, null), introEmojiFor(ORIGIN))
    }

    @Test
    fun `the origin's entry is inside the real list and is not the entry of any other screen, so it cannot wrap onto one`() {
        // The one place the origin reads is the same for a list of any length from the real one up, and is not the first entry.
        assertTrue(approved.size > 2, "the real list has ${approved.size} entries, too few for the origin's place")
        val originIndex = approved.indexOf(introEmojiFor(ORIGIN))
        assertEquals(2, originIndex)
        assertTrue(originIndex in approved.indices)
        assertNotEquals(
            approved.indexOf(introEmojiFor(THEME, null)),
            originIndex,
            "the origin is the theme screen's entry",
        )
        // Every longer list gives the same entry, because the place is under the length and nothing wraps.
        (approved.size..MAX_LIST).forEach { size ->
            val list = List(size) { "e$it" }
            assertEquals(list[2], introEmojiFor(ORIGIN, null, list), "size $size")
            assertNotEquals(introEmojiFor(THEME, null, list), introEmojiFor(ORIGIN, null, list), "size $size")
        }
        // The declared exceptions of the other three screens are not the list's entry either.
        IntroDeclaredEmoji.forEach { assertNotEquals(it, introEmojiFor(ORIGIN), it) }
    }

    @Test
    fun `the same step, choice and list always give the same emoji`() {
        IntroStep.entries.forEach { step ->
            assertEquals(
                introEmojiFor(step, KrailThemeStyle.Bus.id),
                introEmojiFor(step, KrailThemeStyle.Bus.id),
                "$step",
            )
        }
    }

    @Test
    fun `an empty list gives nothing for the screens that use it and leaves the declared exceptions alone`() {
        val none = emptyList<String>()

        assertEquals("", introEmojiFor(THEME, null, none))
        assertEquals("", introEmojiFor(ORIGIN, null, none))
        assertEquals(INTRO_TRAIN_EMOJI, introEmojiFor(THEME, KrailThemeStyle.Train.id, none))
        assertEquals(INTRO_FERRY_EMOJI, introEmojiFor(THEME, KrailThemeStyle.Ferry.id, none))
        assertEquals(INTRO_CAR_EMOJI, introEmojiFor(PARK_RIDE, null, none))
        assertEquals(INTRO_PIN_EMOJI, introEmojiFor(DESTINATION, null, none))
        assertEquals(INTRO_STAR_EMOJI, introEmojiFor(CONFIRM, null, none))
    }

    private fun String.codePointList(): List<Int> {
        val points = mutableListOf<Int>()
        var index = 0
        while (index < length) {
            points += codePointAtCompat(index)
            index += if (points.last() > MAX_BMP) 2 else 1
        }
        return points
    }

    // Common code has no Character class, so the surrogate pair is joined by hand.
    private fun String.codePointAtCompat(index: Int): Int {
        val first = this[index]
        if (!first.isHighSurrogate() || index + 1 >= length) return first.code
        val second = this[index + 1]
        return ((first.code - HIGH_SURROGATE_BASE) shl SURROGATE_SHIFT) + (second.code - LOW_SURROGATE_BASE) + SUPPLEMENTARY_BASE
    }

    private companion object {
        const val MAX_LIST = 12
        const val NOT_A_THEME = 12_345
        const val MAX_BMP = 0xFFFF
        const val HIGH_SURROGATE_BASE = 0xD800
        const val LOW_SURROGATE_BASE = 0xDC00
        const val SURROGATE_SHIFT = 10
        const val SUPPLEMENTARY_BASE = 0x10000
    }
}
