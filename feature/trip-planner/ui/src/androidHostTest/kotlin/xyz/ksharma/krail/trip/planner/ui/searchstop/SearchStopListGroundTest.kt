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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_outline_subtle
import xyz.ksharma.krail.taj.theme.md_theme_dark_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface_placeholder
import xyz.ksharma.krail.taj.theme.md_theme_light_outline_subtle
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
 * The Search Stop box and the location notice follow the app mode on the map and on the cloud gradient of the list state alike, read
 * from the real components: light mode is the Timetable's light tint with the light palette's text, hairline and divider, and dark
 * mode is the Timetable's own dark tint (45 percent of the theme colour over the dark surface) with the dark palette's cream text,
 * its `softLabel` hairline and its divider. The clear X, the swap glyph, the markers, the back arrow and the prompts take the box's
 * text colour, so they read in dark mode. The one thing that differs between the grounds is the dark mode hairline, which is not
 * drawn on the map. `SearchStopCardLookTest` holds the map against the list state theme by theme and frame by frame.
 *
 * The expected colours are built here from the taj tokens and the 45 and 15 percent of the Timetable's tint, not from the code
 * under test, so a drift in either shows.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopListGroundTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var composeView: View? = null
    private var theme = Color.Unspecified
    private var onGradient by mutableStateOf(false)
    private var shape by mutableStateOf(FormShape.ToAndFrom)
    private var editing by mutableStateOf<ToFromField?>(null)
    private var fromStop by mutableStateOf<StopItem?>(CENTRAL)
    private var toStop by mutableStateOf<StopItem?>(null)
    private lateinit var typed: TextFieldState

    private val darkTint get() = theme.copy(alpha = DARK_ALPHA).compositeOver(md_theme_dark_surface)
    private val lightTint get() = theme.copy(alpha = LIGHT_ALPHA).compositeOver(md_theme_light_surface)

    private fun setCard(
        gradient: Boolean,
        withNotice: Boolean = false,
        reduceMotion: Boolean = false,
        themeStyle: KrailThemeStyle = KrailThemeStyle.Train,
    ) {
        onGradient = gradient
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.setContent {
            PreviewTheme(themeStyle = themeStyle) {
                composeView = LocalView.current
                theme = themeDecorColor()
                typed = rememberTextFieldState()
                Column(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.surface).padding(gaps.edge)) {
                    SearchStopGroupCard(
                        form = SearchStopFormModel(
                            shape = shape,
                            editing = editing,
                            toPlaceholder = if (shape == FormShape.Single) SINGLE_PROMPT else TO_PLACEHOLDER,
                            fromPlaceholder = FROM_PLACEHOLDER,
                            toStop = toStop,
                            fromStop = fromStop.takeIf { shape == FormShape.ToAndFrom },
                            fieldState = typed,
                            focusRequester = remember { FocusRequester() },
                            onFlowEvent = {},
                            onGoClick = {},
                            onTextChange = {},
                            onSwap = if (shape == FormShape.ToAndFrom && (fromStop != null || toStop != null)) ({}) else null,
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
        composeRule.waitForIdle()
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

    private fun exists(tag: String) = composeRule.onAllNodes(
        androidx.compose.ui.test.hasTestTag(tag),
        useUnmergedTree = true,
    )
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()

    private fun DpRect.cx() = left + width / 2f
    private fun DpRect.cy() = top + height / 2f

    private fun close(expected: Color, actual: Color, within: Float = 0.03f) =
        abs(expected.red - actual.red) <= within && abs(expected.green - actual.green) <= within &&
            abs(expected.blue - actual.blue) <= within

    private fun assertClose(expected: Color, actual: Color, what: String) =
        assertTrue(close(expected, actual), "$what: expected $expected, got $actual")

    /** The card's own fill, 3 dp inside its left edge (clear of the hairline and of the marker column), at its middle. */
    private fun cardFill(image: Bitmap = drawn()): Color {
        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        return image.at(card.left + 3.dp, card.cy())
    }

    private fun SemanticsNode.textColors(): List<Color> {
        val own = mutableListOf<TextLayoutResult>().also {
            config.getOrNull(
                SemanticsActions.GetTextLayoutResult,
            )?.action?.invoke(it)
        }
        return own.map { it.layoutInput.style.color } + children.flatMap { it.textColors() }
    }

    private fun textColorsIn(tag: String): List<Color> =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().textColors()

    /** The pixel nearest [expected] across the left edge of [tag], where a 1 dp stroke is 2 or 3 pixels wide. */
    private fun edgePixel(tag: String, expected: Color): Color {
        val image = drawn()
        val box = boundsOf(tag)
        val density = composeRule.density
        val y = with(density) { box.cy().roundToPx() }
        val firstX = with(density) { (box.left - 2.dp).roundToPx() }
        val lastX = with(density) { (box.left + 2.dp).roundToPx() }
        val across = (firstX..lastX).map { Color(image.getPixel(it, y)) }
        fun distance(c: Color) = abs(c.red - expected.red) + abs(c.green - expected.green) + abs(c.blue - expected.blue)
        return across.minBy { distance(it) }
    }

    /** The pixel of [bounds] nearest [expected], so a glyph that has holes and anti aliased edges is read at its ink. */
    private fun nearestInk(bounds: DpRect, expected: Color, image: Bitmap = drawn()): Color {
        val density = composeRule.density
        val left = with(density) { bounds.left.roundToPx() }
        val right = with(density) { bounds.right.roundToPx() }
        val top = with(density) { bounds.top.roundToPx() }
        val bottom = with(density) { bounds.bottom.roundToPx() }
        val xs = left..right
        val ys = top..bottom
        var best = Color.Unspecified
        var bestDistance = Float.MAX_VALUE
        for (x in xs) {
            for (y in ys) {
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

    private fun type(text: String) {
        composeRule.runOnIdle { typed.setTextAndPlaceCursorAtEnd(text) }
        composeRule.waitForIdle()
    }

    // endregion

    // region the list state follows the app mode

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `list state in dark mode, the box is the Timetable's dark tint with cream text, the softLabel hairline and the dark divider`() {
        editing = ToFromField.To
        setCard(gradient = true)

        assertClose(
            darkTint,
            cardFill(),
            "the box on the gradient in dark mode is the theme at 45 percent over the dark surface",
        )
        assertTrue(!close(lightTint, cardFill()), "and it is not the light tint it was on the dark ground")
        val colors = textColorsIn(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertTrue(colors.isNotEmpty(), "the box holds text")
        for (color in colors) assertEquals(
            md_theme_dark_onSurface,
            color.copy(alpha = 1f),
            "every text in the box is the dark palette's cream",
        )
        assertClose(
            md_theme_dark_softLabel,
            edgePixel(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, md_theme_dark_softLabel),
            "the hairline is the dark palette's softLabel",
        )
        val divider = boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER)
        assertClose(
            md_theme_dark_outline_subtle.compositeOver(darkTint),
            drawn().at(divider.cx(), divider.cy()),
            "the divider is the dark outlineSubtle",
        )
        val dot = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertClose(md_theme_dark_onSurface, drawn().at(dot.cx(), dot.cy()), "the From dot is the cream text colour")
    }

    @Test
    fun `list state in light mode, the box is exactly the light look it was`() {
        editing = ToFromField.To
        setCard(gradient = true)

        assertClose(
            lightTint,
            cardFill(),
            "the box on the gradient in light mode is the theme at 15 percent over white",
        )
        for (color in textColorsIn(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)) {
            assertEquals(md_theme_light_onSurface, color.copy(alpha = 1f), "the text is the light palette's onSurface")
        }
        assertClose(
            md_theme_light_softLabel,
            edgePixel(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, md_theme_light_softLabel),
            "the hairline is the light softLabel",
        )
        val divider = boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER)
        assertClose(
            md_theme_light_outline_subtle.compositeOver(lightTint),
            drawn().at(divider.cx(), divider.cy()),
            "the divider is the light outlineSubtle",
        )
    }

    @Test
    fun `in light mode the box has the very same pixels on the map and on the gradient, and at every frame between`() {
        editing = ToFromField.To
        setCard(gradient = false)
        composeRule.mainClock.autoAdvance = false
        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val onMap = drawn()

        composeRule.runOnUiThread {
            onGradient = true
            Snapshot.sendApplyNotifications()
        }
        for (step in listOf(16L, 70L, 140L)) {
            composeRule.mainClock.advanceTimeBy(step)
            assertSameBox("$step ms after the change", onMap, drawn(), card)
        }
    }

    private fun assertSameBox(what: String, a: Bitmap, b: Bitmap, box: DpRect) {
        val density = composeRule.density
        var differing = 0
        for (x in with(density) { box.left.roundToPx() } until with(density) { box.right.roundToPx() }) {
            for (y in with(density) { box.top.roundToPx() } until with(density) { box.bottom.roundToPx() }) {
                if (a.getPixel(x, y) != b.getPixel(x, y)) differing++
            }
        }
        assertEquals(0, differing, "$what: $differing pixels of the box changed in light mode")
    }

    // endregion

    // region the map state follows the app mode too

    @Test
    fun `map state in light mode is the light look it was`() {
        editing = ToFromField.To
        setCard(gradient = false, withNotice = true)

        assertClose(lightTint, cardFill(), "light: the box on the map is the theme at 15 percent over white")
        for (color in textColorsIn(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)) {
            assertEquals(
                md_theme_light_onSurface,
                color.copy(alpha = 1f),
                "light: the text on the map is the light palette's onSurface",
            )
        }
        assertClose(
            md_theme_light_softLabel,
            edgePixel(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, md_theme_light_softLabel),
            "light: the hairline on the map is the light softLabel",
        )
        val divider = boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER)
        assertClose(
            md_theme_light_outline_subtle.compositeOver(lightTint),
            drawn().at(divider.cx(), divider.cy()),
            "light: the divider on the map is the light one",
        )
        val dot = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertClose(
            md_theme_light_onSurface,
            drawn().at(dot.cx(), dot.cy()),
            "light: the dot on the map is the light text colour",
        )
        val notice = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertClose(
            lightTint,
            drawn().at(notice.left + 3.dp, notice.cy()),
            "light: the notice on the map is the very same light tint",
        )
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `map state in dark mode is the very same dark look as the list state, with cream text and the dark divider and no hairline`() {
        editing = ToFromField.To
        setCard(gradient = false, withNotice = true)

        assertClose(darkTint, cardFill(), "dark: the box on the map is the theme at 45 percent over the dark surface")
        assertTrue(!close(lightTint, cardFill()), "dark: and it is not the light tint it was")
        val colors = textColorsIn(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertTrue(colors.isNotEmpty(), "the box holds text")
        for (color in colors) assertEquals(
            md_theme_dark_onSurface,
            color.copy(alpha = 1f),
            "dark: the text on the map is the dark palette's cream",
        )
        val divider = boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER)
        assertClose(
            md_theme_dark_outline_subtle.compositeOver(darkTint),
            drawn().at(divider.cx(), divider.cy()),
            "dark: the divider on the map is the dark outlineSubtle",
        )
        val dot = boundsOf(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT)
        assertClose(
            md_theme_dark_onSurface,
            drawn().at(dot.cx(), dot.cy()),
            "dark: the dot on the map is the cream text colour",
        )
        val notice = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertClose(
            darkTint,
            drawn().at(notice.left + 3.dp, notice.cy()),
            "dark: the notice on the map is the very same dark tint",
        )
        assertTrue(
            !close(
                md_theme_dark_softLabel,
                edgePixel(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, md_theme_dark_softLabel),
                0.05f,
            ),
            "dark: the box wears no hairline on the map",
        )
    }

    // endregion

    // region the box on the gradient, the other shapes and the notice

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `list state in dark mode, the lone pill is the dark tint with cream prompt, back arrow and clear X`() {
        shape = FormShape.Single
        editing = ToFromField.To
        setCard(gradient = true)

        val pill = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        assertClose(
            darkTint,
            drawn().at(pill.right - 24.dp, pill.cy()),
            "the lone pill on the gradient in dark mode is the dark tint",
        )
        val colors = textColorsIn(TripPlannerTestTags.SEARCH_ROW_TO)
        assertTrue(colors.isNotEmpty(), "the pill's prompt can be read")
        for (color in colors) assertEquals(
            md_theme_dark_onSurface,
            color.copy(alpha = 1f),
            "the prompt reads in the box's own cream text colour",
        )
        assertClose(
            md_theme_dark_onSurface,
            nearestInk(boundsOf(TripPlannerTestTags.SEARCH_STOP_BACK), md_theme_dark_onSurface),
            "the back arrow is the box's text colour",
        )

        type("seven hills")
        assertClose(
            md_theme_dark_onSurface,
            nearestInk(boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH), md_theme_dark_onSurface),
            "the clear X is the box's text colour",
        )
        assertClose(
            md_theme_dark_softLabel,
            edgePixel(TripPlannerTestTags.SEARCH_ROW_TO, md_theme_dark_softLabel),
            "the pill's hairline is the dark softLabel",
        )
    }

    @Test
    fun `list state in light mode, the lone pill is the light tint with the light placeholder`() {
        shape = FormShape.Single
        editing = ToFromField.To
        setCard(gradient = true)

        val pill = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        assertClose(
            lightTint,
            drawn().at(pill.right - 24.dp, pill.cy()),
            "the lone pill on the gradient in light mode is the light tint",
        )
        for (color in textColorsIn(TripPlannerTestTags.SEARCH_ROW_TO)) {
            assertTrue(
                color.copy(alpha = 1f) in setOf(md_theme_light_onSurface, md_theme_light_onSurface_placeholder),
                "the prompt is the light palette's, not the mode's: $color",
            )
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the clear X, the swap glyph and the markers follow the box's text colour on the gradient and on the map`() {
        editing = ToFromField.To
        setCard(gradient = true)

        val swap = boundsOf(TripPlannerTestTags.SEARCH_ROW_SWAP_GLYPH)
        assertClose(
            md_theme_dark_onSurface,
            nearestInk(swap, md_theme_dark_onSurface),
            "the swap glyph in the dark list state is cream",
        )
        val pin = boundsOf(TripPlannerTestTags.SEARCH_ROW_DESTINATION_PIN)
        assertClose(
            md_theme_dark_onSurface,
            nearestInk(pin, md_theme_dark_onSurface),
            "the destination pin in the dark list state is cream",
        )

        type("Cen")
        assertClose(
            md_theme_dark_onSurface,
            nearestInk(boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH), md_theme_dark_onSurface),
            "the clear X in the dark list state is cream",
        )

        composeRule.runOnUiThread {
            onGradient = false
            Snapshot.sendApplyNotifications()
        }
        composeRule.waitForIdle()
        assertClose(
            md_theme_dark_onSurface,
            nearestInk(boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH), md_theme_dark_onSurface),
            "back on the map the clear X is still cream, the same ink",
        )
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `list state in dark mode, the location notice is the dark tint too, and one flag moves the box and the notice together`() {
        editing = ToFromField.To
        setCard(gradient = true, withNotice = true)

        val notice = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertClose(
            darkTint,
            drawn().at(notice.left + 3.dp, notice.cy()),
            "the notice on the gradient in dark mode is the dark tint",
        )
        assertClose(darkTint, cardFill(), "and so is the box beside it")
        assertTrue(exists(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD))
    }

    // endregion

    // region a screen that opens on the gradient

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `a screen that opens on the gradient in dark mode is dark from its first frame`() {
        editing = ToFromField.To
        setCard(gradient = true)

        assertClose(darkTint, cardFill(), "dark on the gradient from the first frame")
    }

    // endregion

    // endregion

    private companion object {
        const val DARK_ALPHA = 0.45f
        const val LIGHT_ALPHA = 0.15f
        const val SINGLE_PROMPT = "Choose a stop for Home"
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
    }
}
