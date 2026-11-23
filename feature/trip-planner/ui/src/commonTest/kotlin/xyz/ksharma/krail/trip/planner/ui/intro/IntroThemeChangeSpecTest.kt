package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.intro.finale.floodRadiusPx
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * How a second tap carries on from the first. A run is plain values, so the state at any time of one run can be set against the
 * state at the very start of the run that follows it, and they must be the same: colours, the ground, the words, the emoji, the
 * press and the button, at every time and for every pair of picks.
 */
class IntroThemeChangeSpecTest {

    private val surface = Color(0xFFFFFFFF)
    private val onSurface = Color(0xFF101010)
    private val windowWidth = 1000f
    private val windowHeight = 2000f
    private val tolerance = 1e-4f
    private val questionRequest = request(null, -1, Offset.Unspecified)
    private val probes = listOf(
        0f,
        100f,
        200f,
        450f,
        700f,
        900f,
    ).flatMap { x -> listOf(0f, 300f, 800f, 1500f, 1999f).map { y -> x to y } }
    private val interruptTimes = listOf(0f, 60f, 120f, 233f, 300f, 450f, 520f, 600f, 699f, 700f, 900f)

    private fun KrailThemeStyle.colour(): Color = hexColorCode.hexToComposeColor()

    private fun request(
        style: KrailThemeStyle?,
        tapped: Int,
        origin: Offset,
        reduceMotion: Boolean = false,
    ): ThemeChangeRequest {
        val id = style?.id
        val colour = style?.colour() ?: Color(0xFFF6891F)
        val headline = introThemeHeadline(id)
        return ThemeChangeRequest(
            reduceMotion = reduceMotion,
            target = ThemeChangeTarget(id, style?.hexColorCode ?: "#FFF6891F", colour),
            toText = if (style == null) onSurface else colour.copy(alpha = 1f),
            toGround = if (style == null) surface else introGroundColour(colour, surface, dark = false),
            headline = HeadlineContent(headline.lines, headline.emphasisIndex, introEmojiFor(IntroStep.THEME, id)),
            tapped = tapped,
            origin = origin,
        )
    }

    // The screen as it opens, with no theme, and the run for the first pick after it.
    private fun first(reduceMotion: Boolean = false, style: KrailThemeStyle = KrailThemeStyle.Train): ThemeChangeSpec {
        val settled = settledThemeChangeSpec(questionRequest, ground = surface, text = onSurface)
        return themeChangeSpecAfter(settled, settled.durationMs, request(style, 0, Offset(200f, 300f), reduceMotion))
    }

    private fun second(previous: ThemeChangeSpec, timeMs: Float, reduceMotion: Boolean = false): ThemeChangeSpec =
        themeChangeSpecAfter(previous, timeMs, request(KrailThemeStyle.Bus, 2, Offset(700f, 1500f), reduceMotion))

    // The colour the ground has at one point of the window: the top layer that reaches it.
    private fun groundAt(spec: ThemeChangeSpec, timeMs: Float, x: Float, y: Float): Color {
        var seen = Color.Unspecified
        spec.forEachGroundLayer(timeMs) { colour, origin, share ->
            val centre = if (origin == Offset.Unspecified) Offset(windowWidth / 2, windowHeight / 2) else origin
            val radius = floodRadiusPx(share, windowWidth, windowHeight, centre.x, centre.y)
            val reaches = share >= 1f || (share > 0f && hypot(x - centre.x, y - centre.y) <= radius)
            if (reaches) seen = colour
        }
        return seen
    }

    private fun assertSameColour(expected: Color, actual: Color, what: String) {
        assertEquals(expected.red, actual.red, tolerance, "$what red")
        assertEquals(expected.green, actual.green, tolerance, "$what green")
        assertEquals(expected.blue, actual.blue, tolerance, "$what blue")
    }

    @Test
    fun `a run that follows another starts in the state the other had reached, at every time and for every colour and the ground`() {
        val one = first()
        interruptTimes.forEach { time ->
            val two = second(one, time)
            val at = time.coerceAtMost(one.durationMs)

            assertSameColour(one.colourAt(at), two.colourAt(0f), "theme colour at $time")
            assertSameColour(one.textAt(at), two.textAt(0f), "text colour at $time")
            probes.forEach { (x, y) ->
                assertSameColour(groundAt(one, at, x, y), groundAt(two, 0f, x, y), "ground at ($x, $y) at $time")
            }
        }
    }

    @Test
    fun `the circle that was growing is left where it was under the new one, and a circle that had covered everything leaves none`() {
        val one = first()

        val early = second(one, 200f)
        assertEquals(1, early.wash.frozen.size, "the circle of the first tap is kept")
        assertEquals(Offset(200f, 300f), early.wash.frozen.single().origin)
        assertEquals(themeWash(200f), early.wash.frozen.single().share, tolerance)
        assertEquals(Offset(700f, 1500f), early.wash.origin, "and the new one grows from the new badge")

        val late = second(one, 560f)
        assertTrue(late.wash.frozen.isEmpty(), "a covered window has nothing under the new circle")
        assertEquals(one.colours.toGround, late.colours.fromGround, "and the ground it grows over is the old theme's")
    }

    @Test
    fun `no more than four circles are kept, so a fast run of taps stays small`() {
        var spec = first()
        repeat(8) { spec = second(spec, 40f) }

        assertEquals(THEME_WASH_FROZEN_MAX, spec.wash.frozen.size)
    }

    @Test
    fun `the headline that is most to be seen leaves from the opacity and the place it had`() {
        val one = first()

        // At 100 ms the question is half way out and the tagline has not started: the question leaves from there.
        val early = second(one, 100f)
        assertEquals(introThemeHeadline(null).lines, early.headline.outgoing?.lines)
        assertEquals(
            themeOutgoingAlpha(1f, themeOutgoing(100f, false)),
            early.headline.outgoingLooks.alphas.first(),
            tolerance,
        )
        assertEquals(
            themeOutgoingRiseDp(0f, themeOutgoing(100f, false), false),
            early.headline.outgoingLooks.risesDp.first(),
            tolerance,
        )

        // At 400 ms the tagline is on its way in, and it is the one that leaves.
        val late = second(one, 400f)
        assertEquals(introThemeHeadline(KrailThemeStyle.Train.id).lines, late.headline.outgoing?.lines)
        val looks = incomingLooks(2, 400f, false)
        assertEquals(looks.alphas, late.headline.outgoingLooks.alphas)
        assertEquals(looks.risesDp, late.headline.outgoingLooks.risesDp)
        assertEquals(introThemeHeadline(KrailThemeStyle.Bus.id).lines, late.headline.incoming.lines)
    }

    @Test
    fun `the emoji starts at the scale and the glyph it had`() {
        val one = first()

        interruptTimes.forEach { time ->
            val two = second(one, time)
            val at = time.coerceAtMost(one.durationMs)

            assertEquals(
                themeEmojiScale(at, one.headline.emojiScaleAtStart, false),
                two.headline.emojiScaleAtStart,
                tolerance,
                "scale at $time",
            )
            assertEquals(one.emojiAt(at), two.emojiAt(0f), "glyph at $time")
            assertEquals(
                themeEmojiScale(at, 1f, false),
                themeEmojiScale(0f, two.headline.emojiScaleAtStart, false),
                tolerance,
                "scale at $time",
            )
        }
    }

    @Test
    fun `the badge that was pressed carries on from how far it had got, and the one tapped now starts its press`() {
        val one = first()
        val two = second(one, 100f)

        assertEquals(one.pressAt(0, 100f), two.pressAt(0, 0f), tolerance, "the first badge goes on from where it was")
        assertEquals(1f, two.pressAt(2, 0f), tolerance, "the new badge starts at rest")
        assertTrue(two.pressAt(2, 90f) < 0.93f, "and dips")
        assertEquals(1f, two.pressAt(1, 50f), "a badge that was not tapped never moves")
    }

    @Test
    fun `the button shows at 450 ms on the first pick, and is already there on any run that follows once it was`() {
        val one = first()
        assertEquals(450f, one.buttonAtMs)

        assertEquals(450f, second(one, 100f).buttonAtMs, "not shown yet, so it still waits")
        assertEquals(0f, second(one, 450f).buttonAtMs, "shown, so it stays")
        assertEquals(0f, second(one, 700f).buttonAtMs)
        val settled = settledThemeChangeSpec(request(KrailThemeStyle.Metro, -1, Offset.Unspecified), surface, onSurface)
        assertEquals(0f, settled.buttonAtMs, "a screen that opens with a theme already has its button")
        assertEquals(
            Float.MAX_VALUE,
            settledThemeChangeSpec(questionRequest, surface, onSurface).buttonAtMs,
            "and with none it has not",
        )
    }

    @Test
    fun `the colours settle on exactly the theme's own colour and the ground's own`() {
        val one = first()

        assertEquals(KrailThemeStyle.Train.colour(), one.colourAt(one.durationMs))
        assertEquals(KrailThemeStyle.Train.colour(), one.colourAt(5000f))
        assertEquals(one.colours.fromColour, one.colourAt(0f))
        assertEquals(one.colours.toText, one.textAt(one.durationMs))
        var last = Color.Unspecified
        one.forEachGroundLayer(one.durationMs) { colour, _, _ -> last = colour }
        assertEquals(one.colours.toGround, last)
        assertNotEquals(surface, last, "a chosen theme tints the ground")
    }

    @Test
    fun `the first pick grows the wash from the badge, over the old ground, and nothing is drawn after the run`() {
        val one = first()
        val layers = mutableListOf<Triple<Color, Offset, Float>>()
        one.forEachGroundLayer(300f) { colour, origin, share -> layers += Triple(colour, origin, share) }

        assertEquals(listOf(surface, one.colours.toGround), layers.map { it.first })
        assertEquals(1f, layers[0].third, "the old ground is the whole window")
        assertEquals(Offset(200f, 300f), layers[1].second)
        assertEquals(themeWash(300f), layers[1].third, tolerance)
    }

    @Test
    fun `reduced motion has no circle, only the two grounds cross-fading, and a later run carries on from the blend`() {
        val one = first(reduceMotion = true, style = KrailThemeStyle.Metro)
        val layers = mutableListOf<Float>()
        one.forEachGroundLayer(100f) { _, _, share -> layers += share }

        assertEquals(listOf(1f), layers, "one whole-window layer and no circle")
        assertEquals(200f, one.durationMs)
        assertEquals(1f, one.pressAt(0, 50f), "no press")
        val mid = one.colourAt(100f)
        assertNotEquals(one.colours.fromColour, mid)
        assertNotEquals(one.colours.toColour, mid)

        val two = second(one, 100f, reduceMotion = true)
        assertSameColour(mid, two.colourAt(0f), "colour carried on")
        assertSameColour(groundAt(one, 100f, 10f, 10f), groundAt(two, 0f, 10f, 10f), "ground carried on")
        assertTrue(two.wash.frozen.isEmpty(), "a cross-fade leaves no circle")
    }
}
