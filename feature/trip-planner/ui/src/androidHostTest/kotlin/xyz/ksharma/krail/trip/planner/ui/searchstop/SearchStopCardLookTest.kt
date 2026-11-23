package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface_placeholder
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The From and To box, the bare single pill and the location notice follow the app mode on the map and in the list state alike, so
 * the two states draw the very same fill, text and glyphs in light and in dark mode, and the notice draws exactly what the box
 * draws. Everything is read from the real components, built from the taj tokens and from the literal colours of the approved
 * picture (Train 7E4D1C and Bus 0F607A in dark mode), never from the code under test, and every value is printed.
 *
 * The hairline is the one thing that differs between the states, and only in dark mode: it is drawn on the gradient and not on
 * the map, where the dark box is already well clear of the light map. It fades on the card spec when the ground changes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopCardLookTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var composeView: View? = null
    private var themeColor = Color.Unspecified
    private var themeState by mutableStateOf(KrailThemeStyle.Train)
    private var onGradient by mutableStateOf(false)
    private var shape by mutableStateOf(FormShape.ToAndFrom)
    private var withNotice by mutableStateOf(false)
    private lateinit var typed: TextFieldState

    private val offered = listOf(
        KrailThemeStyle.Train,
        KrailThemeStyle.Metro,
        KrailThemeStyle.Bus,
        KrailThemeStyle.Ferry,
    )

    private fun setCard(reduceMotion: Boolean = false) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.setContent {
            // A new key a new theme, so every theme is drawn from the first frame as the real screen would draw it.
            key(themeState) {
                PreviewTheme(themeStyle = themeState) {
                    composeView = LocalView.current
                    themeColor = themeDecorColor()
                    typed = rememberTextFieldState()
                    Column(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.surface).padding(gaps.edge)) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = shape,
                                editing = ToFromField.To,
                                toPlaceholder = if (shape == FormShape.Single) SINGLE_PROMPT else TO_PLACEHOLDER,
                                fromPlaceholder = FROM_PLACEHOLDER,
                                toStop = null,
                                fromStop = CENTRAL.takeIf { shape == FormShape.ToAndFrom },
                                fieldState = typed,
                                focusRequester = remember { FocusRequester() },
                                onFlowEvent = {},
                                onGoClick = {},
                                onTextChange = {},
                                onSwap = if (shape == FormShape.ToAndFrom) ({}) else null,
                            ),
                            onBack = {},
                            onGradient = onGradient,
                        )
                        if (withNotice) {
                            LocationNotice(
                                pill = LocationPill.Ask,
                                dismissed = false,
                                onDismiss = {},
                                onCtaClick = {},
                                onGradient = onGradient,
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun use(style: KrailThemeStyle) {
        composeRule.runOnIdle { themeState = style }
        composeRule.waitForIdle()
    }

    private fun flipTo(gradient: Boolean) {
        composeRule.runOnUiThread {
            onGradient = gradient
            Snapshot.sendApplyNotifications()
        }
    }

    // region reading what is drawn

    private fun drawn(): Bitmap {
        var bitmap: Bitmap? = null
        composeRule.runOnUiThread {
            val view = checkNotNull(composeView)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(checkNotNull(bitmap)))
        }
        return checkNotNull(bitmap)
    }

    private fun Bitmap.at(x: Dp, y: Dp): Color =
        Color(getPixel(with(composeRule.density) { x.roundToPx() }, with(composeRule.density) { y.roundToPx() }))

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getUnclippedBoundsInRoot()

    private fun DpRect.cx() = left + width / 2f
    private fun DpRect.cy() = top + height / 2f

    private fun close(expected: Color, actual: Color, within: Float = 0.02f) =
        abs(expected.red - actual.red) <= within && abs(expected.green - actual.green) <= within &&
            abs(expected.blue - actual.blue) <= within

    private fun assertClose(expected: Color, actual: Color, what: String) =
        assertTrue(close(expected, actual), "$what: expected ${hex(expected)}, got ${hex(actual)}")

    private fun hex(c: Color) = "%06X".format(c.toArgb() and 0xFFFFFF)

    /** The fill of the card at [tag], 3 dp inside its left edge, clear of the hairline and of the marker column, at its middle. */
    private fun fillOf(tag: String, image: Bitmap = drawn()): Color {
        val box = boundsOf(tag)
        return image.at(box.left + 3.dp, box.cy())
    }

    /** The pixels across the left edge of [tag], where a 1 dp stroke is 2 or 3 pixels wide. */
    private fun edgeStrip(tag: String, image: Bitmap = drawn()): List<Color> {
        val box = boundsOf(tag)
        val density = composeRule.density
        val y = with(density) { box.cy().roundToPx() }
        return (with(density) { (box.left - 2.dp).roundToPx() }..with(density) { (box.left + 2.dp).roundToPx() })
            .map { Color(image.getPixel(it, y)) }
    }

    /** How present the hairline is: the lightest pixel across the edge, which the dark box and the dark page are both far below. */
    private fun edgeStrength(
        tag: String,
        image: Bitmap = drawn(),
    ): Float = edgeStrip(tag, image).maxOf { it.luminance() }

    /** The pixel across the edge nearest [expected]. */
    private fun edgeNearest(tag: String, expected: Color, image: Bitmap = drawn()): Color {
        fun distance(c: Color) = abs(c.red - expected.red) + abs(c.green - expected.green) + abs(c.blue - expected.blue)
        return edgeStrip(tag, image).minBy { distance(it) }
    }

    private fun SemanticsNode.textColors(): List<Color> {
        val own = mutableListOf<TextLayoutResult>().also {
            config.getOrNull(
                SemanticsActions.GetTextLayoutResult,
            )?.action?.invoke(it)
        }
        return own.map { it.layoutInput.style.color.copy(alpha = 1f) } + children.flatMap { it.textColors() }
    }

    private fun textColorsIn(tag: String): List<Color> =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().textColors()

    private fun textColorOf(text: String): Color =
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().textColors().first()

    /** The pixel of [bounds] nearest [expected], so a glyph that has holes and anti aliased edges is read at its ink. */
    private fun nearestInk(bounds: DpRect, expected: Color, image: Bitmap = drawn()): Color {
        val density = composeRule.density
        var best = Color.Unspecified
        var bestDistance = Float.MAX_VALUE
        for (x in with(density) { bounds.left.roundToPx() }..with(density) { bounds.right.roundToPx() }) {
            for (y in with(density) { bounds.top.roundToPx() }..with(density) { bounds.bottom.roundToPx() }) {
                val c = Color(image.getPixel(x, y))
                val d = abs(c.red - expected.red) + abs(c.green - expected.green) + abs(c.blue - expected.blue)
                if (d < bestDistance) {
                    bestDistance = d
                    best = c
                }
            }
        }
        return best
    }

    /** A square of the card's start edge corner, one dp outside it and [CORNER] dp in, as pixels, so two corners can be compared. */
    private fun corner(tag: String, top: Boolean, image: Bitmap = drawn()): List<Int> {
        val box = boundsOf(tag)
        val density = composeRule.density
        val x0 = with(density) { (box.left - 1.dp).roundToPx() }
        val y0 = with(density) { (if (top) box.top - 1.dp else box.bottom - CORNER.dp).roundToPx() }
        val size = with(density) { (CORNER + 1).dp.roundToPx() }
        return buildList { for (dy in 0 until size) for (dx in 0 until size) add(image.getPixel(x0 + dx, y0 + dy)) }
    }

    private fun type(text: String) {
        composeRule.runOnIdle { typed.setTextAndPlaceCursorAtEnd(text) }
        composeRule.waitForIdle()
    }

    // endregion

    // region the expected colours, from the tokens and from the picture

    private fun expectedFill(theme: Color, dark: Boolean): Color =
        theme.copy(alpha = if (dark) DARK_ALPHA else LIGHT_ALPHA)
            .compositeOver(if (dark) md_theme_dark_surface else md_theme_light_surface)

    private fun ink(dark: Boolean) = if (dark) md_theme_dark_onSurface else md_theme_light_onSurface

    private fun edge(dark: Boolean) = if (dark) md_theme_dark_softLabel else md_theme_light_softLabel

    /** The picture's literal colours, per offered theme: what the box is on the map and in the list state. */
    private val pictureDark = mapOf(
        KrailThemeStyle.Train to Color(0xFF7E4D1C),
        KrailThemeStyle.Metro to Color(0xFF0F5544),
        KrailThemeStyle.Bus to Color(0xFF0F607A),
        KrailThemeStyle.Ferry to Color(0xFF385E24),
    )

    /** And in light mode, the old constants as the app draws them: the theme at 15 percent over white, with the alpha kept in 8 bits. */
    private val pictureLight = mapOf(
        KrailThemeStyle.Train to Color(0xFFFEEDDE),
        KrailThemeStyle.Metro to Color(0xFFD9F0EB),
        KrailThemeStyle.Bus to Color(0xFFD9F4FD),
        KrailThemeStyle.Ferry to Color(0xFFE6F3E0),
    )

    // endregion

    // region the box and the notice, on the map and in the list state, on every offered theme

    private fun assertBoxAndNotice(mode: String, dark: Boolean) {
        withNotice = true
        setCard()
        val literal = if (dark) pictureDark else pictureLight
        val page = if (dark) md_theme_dark_surface else md_theme_light_surface
        println(
            "CARDLOOK table $mode: theme ground | box fill | notice fill | box text | notice title, words, Not now, pin | box edge, notice edge",
        )
        for (style in offered) {
            use(style)
            val fills = mutableMapOf<Boolean, Color>()
            for (gradient in listOf(false, true)) {
                flipTo(gradient)
                composeRule.waitForIdle()
                val ground = if (gradient) "list" else "map"
                val image = drawn()
                val boxFill = fillOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
                val noticeFill = fillOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, image)
                val boxText = textColorsIn(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
                val title = textColorOf(LOCATION_NOTICE_TITLE)
                val words = textColorOf(LOCATION_NOTICE_BODY)
                val notNow = textColorOf(LOCATION_NOTICE_DISMISS)
                val pin = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PIN)
                val pinInk = nearestInk(pin, ink(dark), image)
                val dot = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
                val dotInk = image.at(dot.cx(), dot.cy())
                val boxEdge = edgeStrength(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
                val noticeEdge = edgeStrength(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, image)
                println(
                    "CARDLOOK $mode ${style.name} $ground boxFill=${hex(boxFill)} noticeFill=${hex(noticeFill)} " +
                        "boxText=${boxText.map { hex(it) }.toSet()} noticeTitle=${hex(title)} noticeWords=${hex(words)} " +
                        "noticeNotNow=${hex(notNow)} pin=${hex(pinInk)} dot=${hex(dotInk)} boxEdge=$boxEdge noticeEdge=$noticeEdge " +
                        "textOnFill=${ink(dark).contrastRatio(boxFill)}",
                )

                // The fill is the picture's colour, built from the tokens, on both grounds.
                assertClose(
                    literal.getValue(style),
                    expectedFill(themeColor, dark),
                    "$mode ${style.name}: the tokens give the picture's colour",
                )
                assertClose(
                    literal.getValue(style),
                    boxFill,
                    "$mode ${style.name} $ground: the box is the picture's colour",
                )
                // The notice is exactly the box: the same fill, the same hairline, the same corners, pixel for pixel.
                assertEquals(
                    boxFill.toArgb(),
                    noticeFill.toArgb(),
                    "$mode ${style.name} $ground: the notice's fill is the box's fill",
                )
                assertEquals(
                    corner(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, top = true, image = image),
                    corner(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, top = true, image = image),
                    "$mode ${style.name} $ground: the notice's top corner and hairline are the box's",
                )
                assertEquals(
                    corner(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, top = false, image = image),
                    corner(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, top = false, image = image),
                    "$mode ${style.name} $ground: the notice's bottom corner and hairline are the box's",
                )
                assertEquals(boxEdge, noticeEdge, "$mode ${style.name} $ground: the notice's hairline is the box's")
                // Text, markers and glyphs: the one ink, the same in the box and the notice.
                assertTrue(boxText.isNotEmpty(), "the box holds text")
                for (color in boxText + listOf(title, words, notNow)) {
                    assertEquals(ink(dark), color, "$mode ${style.name} $ground: every text is the mode's ink")
                }
                assertClose(ink(dark), pinInk, "$mode ${style.name} $ground: the notice's pin is the ink")
                assertClose(ink(dark), dotInk, "$mode ${style.name} $ground: the box's dot is the ink")
                assertTrue(
                    ink(dark).contrastRatio(boxFill) >= TEXT,
                    "$mode ${style.name}: the text on the fill is ${ink(dark).contrastRatio(boxFill)} to 1",
                )
                // The hairline: light mode always, dark mode on the gradient only.
                val drawnEdge = !dark || gradient
                if (drawnEdge) {
                    assertClose(
                        edge(dark),
                        edgeNearest(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, edge(dark), image),
                        "$mode ${style.name} $ground: the box's hairline",
                    )
                    assertClose(
                        edge(dark),
                        edgeNearest(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, edge(dark), image),
                        "$mode ${style.name} $ground: the notice's hairline",
                    )
                } else {
                    for (tag in listOf(
                        TripPlannerTestTags.SEARCH_STOP_GROUP_CARD,
                        TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL,
                    )) {
                        assertTrue(
                            edgeStrip(
                                tag,
                                image,
                            ).all { close(page, it, 0.02f) || close(literal.getValue(style), it, 0.02f) || it.luminance() < 0.3f },
                            "$mode ${style.name} $ground: nothing but the page and the fill across $tag's edge on the map",
                        )
                        assertTrue(
                            edgeStrength(tag, image) < 0.3f,
                            "$mode ${style.name} $ground: no hairline on the map",
                        )
                    }
                }
                fills[gradient] = boxFill
            }
            assertEquals(
                fills.getValue(false).toArgb(),
                fills.getValue(true).toArgb(),
                "$mode ${style.name}: the map and the list state are one fill",
            )
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the box and the notice are the same dark tint with cream text on the map and in the list state, on all four themes`() =
        assertBoxAndNotice("dark", dark = true)

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the box and the notice are the same light tint with the old text, on the map and in the list state, on all four themes`() =
        assertBoxAndNotice("light", dark = false)

    // endregion

    // region the bare pill

    private fun assertPill(mode: String, dark: Boolean) {
        shape = FormShape.Single
        setCard()
        val literal = if (dark) pictureDark else pictureLight
        for (style in offered) {
            use(style)
            for (gradient in listOf(false, true)) {
                flipTo(gradient)
                composeRule.waitForIdle()
                val ground = if (gradient) "list" else "map"
                val pill = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
                val fill = drawn().at(pill.right - 24.dp, pill.cy())
                val prompt = textColorsIn(TripPlannerTestTags.SEARCH_ROW_TO)
                val back = nearestInk(boundsOf(TripPlannerTestTags.SEARCH_STOP_BACK), ink(dark))
                println(
                    "CARDLOOK pill $mode ${style.name} $ground fill=${hex(fill)} prompt=${prompt.map { hex(it) }.toSet()} back=${hex(back)} " +
                        "edge=${edgeStrength(TripPlannerTestTags.SEARCH_ROW_TO)}",
                )
                assertClose(
                    literal.getValue(style),
                    fill,
                    "$mode ${style.name} $ground: the bare pill is the picture's colour",
                )
                assertTrue(prompt.isNotEmpty(), "the pill's prompt can be read")
                for (color in prompt) {
                    val allowed = if (dark) {
                        setOf(
                            md_theme_dark_onSurface,
                        )
                    } else {
                        setOf(md_theme_light_onSurface, md_theme_light_onSurface_placeholder)
                    }
                    assertTrue(color in allowed, "$mode ${style.name} $ground: the prompt is ${hex(color)}")
                }
                assertClose(ink(dark), back, "$mode ${style.name} $ground: the back arrow is the box's ink")
                val drawnEdge = !dark || gradient
                val strength = edgeStrength(TripPlannerTestTags.SEARCH_ROW_TO)
                assertTrue(
                    (strength > 0.3f) == drawnEdge,
                    "$mode ${style.name} $ground: the pill's hairline is ${if (drawnEdge) "drawn" else "absent"}, strength $strength",
                )
            }
        }
        // The clear X follows the ink too, once a query is typed, on both grounds.
        type("seven hills")
        for (gradient in listOf(false, true)) {
            flipTo(gradient)
            composeRule.waitForIdle()
            assertClose(
                ink(dark),
                nearestInk(boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH), ink(dark)),
                "$mode, ${if (gradient) "list" else "map"}: the clear X is the box's ink",
            )
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the bare pill is the same dark tint with cream prompt, back arrow and clear X on the map and in the list state`() =
        assertPill("dark", dark = true)

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the bare pill is the same light tint with the old prompt, on the map and in the list state`() =
        assertPill("light", dark = false)

    // endregion

    // region the move between the two states

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode a trip from the map to the list state and back keeps the same fill, text and glyphs in every frame, and only the hairline fades`() {
        withNotice = true
        setCard()
        composeRule.mainClock.autoAdvance = false
        val box = TripPlannerTestTags.SEARCH_STOP_GROUP_CARD
        val notice = TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL
        val dark = expectedFill(themeColor, dark = true)
        val strengths = mutableListOf<Pair<String, Float>>()

        fun frame(label: String) {
            val image = drawn()
            assertClose(dark, fillOf(box, image), "$label: the box's fill")
            assertClose(dark, fillOf(notice, image), "$label: the notice's fill")
            val dot = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
            assertClose(md_theme_dark_onSurface, image.at(dot.cx(), dot.cy()), "$label: the dot")
            val swap = boundsOf(TripPlannerTestTags.SEARCH_ROW_SWAP_GLYPH)
            assertClose(
                md_theme_dark_onSurface,
                nearestInk(swap, md_theme_dark_onSurface, image),
                "$label: the swap glyph",
            )
            val pin = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PIN)
            assertClose(
                md_theme_dark_onSurface,
                nearestInk(pin, md_theme_dark_onSurface, image),
                "$label: the notice's pin",
            )
            for (color in textColorsIn(box) + textColorsIn(notice).filter { it != textColorOf(LOCATION_NOTICE_CTA_ALLOW) }) {
                assertEquals(md_theme_dark_onSurface, color, "$label: every text")
            }
            val strength = edgeStrength(box, image)
            assertEquals(strength, edgeStrength(notice, image), "$label: the notice's hairline is the box's")
            strengths += label to strength
        }

        frame("map, at rest")
        flipTo(true)
        for (ms in listOf(16L, 70L, 140L, 200L, 300L)) {
            composeRule.mainClock.advanceTimeBy(ms)
            frame("to the list state, +$ms ms")
        }
        flipTo(false)
        for (ms in listOf(16L, 70L, 140L, 200L, 300L)) {
            composeRule.mainClock.advanceTimeBy(ms)
            frame("back to the map, +$ms ms")
        }
        println("CARDLOOK roundtrip hairline strength by frame: $strengths")

        val atRest = strengths.first().second
        val onGradient = strengths[6].second
        assertTrue(atRest < 0.3f, "no hairline on the map at rest: $atRest")
        assertTrue(onGradient > 0.3f, "the hairline is drawn on the gradient: $onGradient")
        val going = strengths.subList(0, 7).map { it.second }
        assertTrue(
            going.zipWithNext().all { (a, b) -> b >= a - 0.001f },
            "the hairline only grows on the way to the list state: $going",
        )
        assertTrue(
            going.any { it > atRest + 0.01f && it < onGradient - 0.01f },
            "and it passes through a part way frame: $going",
        )
        val coming = strengths.subList(6, strengths.size).map { it.second }
        assertTrue(coming.zipWithNext().all { (a, b) -> b <= a + 0.001f }, "and only shrinks on the way back: $coming")
        assertTrue(strengths.last().second < 0.3f, "and is gone again on the map: ${strengths.last().second}")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `with reduced motion the hairline jumps in one frame, and the fill never moves`() {
        setCard(reduceMotion = true)
        composeRule.mainClock.autoAdvance = false
        val box = TripPlannerTestTags.SEARCH_STOP_GROUP_CARD
        val dark = expectedFill(themeColor, dark = true)
        assertTrue(edgeStrength(box) < 0.3f, "no hairline on the map")

        flipTo(true)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertClose(dark, fillOf(box), "the fill is the same one frame on")
        assertTrue(edgeStrength(box) > 0.3f, "and the hairline is fully drawn, with no step between")

        flipTo(false)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertClose(dark, fillOf(box), "the fill is the same one frame after going back")
        assertTrue(edgeStrength(box) < 0.3f, "and the hairline is gone in one frame")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `a screen that opens on the gradient in dark mode has its hairline from the first frame`() {
        onGradient = true
        setCard()

        assertTrue(edgeStrength(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD) > 0.3f)
    }

    // endregion

    private companion object {
        const val DARK_ALPHA = 0.45f
        const val LIGHT_ALPHA = 0.15f
        const val TEXT = 4.5f
        const val CORNER = 16
        const val SINGLE_PROMPT = "Choose a stop for Home"
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
    }
}
