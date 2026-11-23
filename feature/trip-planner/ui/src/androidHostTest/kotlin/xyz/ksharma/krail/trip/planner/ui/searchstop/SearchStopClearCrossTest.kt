package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The clear X of a search field is a bare cross: no circle, no fill, no ring, the size of the field's back arrow (24 dp) and the
 * field's own text colour, inside an invisible 48 dp box that is the touch target and never lets the tap through to the map
 * (`docs/MAP_HOME_UX.md`, rule 48). It is read from the real single pill and from the real From and To card, which both have one.
 * It shows only while the field holds text, clears it, and a screen reader hears Clear. The other X buttons keep their circle
 * (`CloseButtonsTest`).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopClearCrossTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var composeView: View? = null
    private var tint = Color.Unspecified
    private var ink = Color.Unspecified
    private var mapTaps = 0
    private lateinit var typed: TextFieldState

    private var shape by mutableStateOf(FormShape.ToAndFrom)
    private var editing by mutableStateOf<ToFromField?>(ToFromField.To)

    /** Shows the form once, as the single pill. [showShape] moves it between the pill and the card. */
    private fun setForm(first: FormShape) {
        shape = first
        composeRule.setContent {
            PreviewTheme {
                composeView = LocalView.current
                tint = mapCardColors().tint
                ink = mapCardColors().onTint
                typed = rememberTextFieldState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KrailTheme.colors.surface)
                        // The map under the box: a tap that reaches it is counted.
                        .pointerInput(Unit) { detectTapGestures { mapTaps++ } }
                        .padding(gaps.edge),
                ) {
                    SearchStopGroupCard(
                        form = SearchStopFormModel(
                            shape = shape,
                            editing = editing,
                            toPlaceholder = TO_PLACEHOLDER,
                            fromPlaceholder = FROM_PLACEHOLDER,
                            toStop = null,
                            fromStop = CENTRAL.takeIf { shape == FormShape.ToAndFrom },
                            fieldState = typed,
                            focusRequester = remember { FocusRequester() },
                            onFlowEvent = {},
                            onGoClick = {},
                            onTextChange = {},
                        ),
                        onBack = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The form as [next], with the field empty. */
    private fun showShape(next: FormShape) {
        composeRule.runOnIdle {
            shape = next
            typed.setTextAndPlaceCursorAtEnd("")
        }
        composeRule.waitForIdle()
    }

    private fun type(text: String) {
        composeRule.runOnIdle { typed.setTextAndPlaceCursorAtEnd(text) }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getUnclippedBoundsInRoot()

    private fun exists(tag: String) = composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    private fun DpRect.cx() = left + width / 2f
    private fun DpRect.cy() = top + height / 2f

    private fun assertNear(what: String, expected: Dp, actual: Dp, within: Float = 0.75f) {
        assertTrue(abs((expected - actual).value) <= within, "$what: expected $expected, got $actual")
    }

    private fun isClose(expected: Color, actual: Color, within: Float = 0.03f): Boolean =
        abs(expected.red - actual.red) <= within && abs(expected.green - actual.green) <= within &&
            abs(expected.blue - actual.blue) <= within

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

    // region the cross, in the pill and in the card

    /**
     * The one rule of the clear X, asserted on whatever field it is in: a bare 24 dp cross the size of a field's back arrow in the
     * text colour, centred in a 48 dp box, with nothing drawn in the box round the cross (so no circle, no fill, no ring) and no
     * circle node. [behind] is the colour the box sits on.
     */
    private fun assertBareCross(label: String, behind: Color) {
        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR)
        val glyph = boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH)
        assertNear("$label, the touch box is 48 dp wide", 48.dp, box.width)
        assertNear("$label, and 48 dp tall", 48.dp, box.height)
        assertNear("$label, the cross is 24 dp wide", 24.dp, glyph.width)
        assertNear("$label, and 24 dp tall", 24.dp, glyph.height)
        assertNear("$label, the cross is centred in its box, across", box.cx(), glyph.cx())
        assertNear("$label, and down", box.cy(), glyph.cy())

        val image = drawn()
        // Inside the 40 dp circle the X used to have, clear of the cross and of its 24 dp box: 8 dp from the box's left edge, on
        // its centre line. A circle, a fill or a ring would be drawn there; a bare cross leaves what the field is made of.
        assertTrue(
            isClose(behind, image.at(box.left + 8.dp, box.cy())),
            "$label, nothing is drawn round the cross: ${image.at(box.left + 8.dp, box.cy())} against $behind",
        )
        assertTrue(
            isClose(behind, image.at(box.cx(), box.top + 8.dp)),
            "$label, nothing is drawn above the cross either",
        )
        // And inside the cross's own 24 dp box, off its strokes: a tight circle or a fill behind the cross would show there.
        assertTrue(
            isClose(behind, image.at(glyph.left + 2.dp, glyph.cy())),
            "$label, no tight circle behind the cross: ${image.at(glyph.left + 2.dp, glyph.cy())} against $behind",
        )
        assertTrue(
            isClose(behind, image.at(glyph.cx(), glyph.top + 2.dp)),
            "$label, no fill above the cross's strokes",
        )
        // The centre of a cross is where its two strokes cross, so it is the text colour.
        assertTrue(isClose(ink, image.at(glyph.cx(), glyph.cy())), "$label, the cross is the field's text colour $ink")
    }

    @Test
    fun `the pill's clear X is a bare 24 dp cross in a 48 dp box with no circle, in the field's text colour`() {
        setForm(FormShape.Single)
        type("seven hills")

        assertBareCross("the pill", behind = tint)
    }

    @Test
    fun `the card's clear X is a bare 24 dp cross in a 48 dp box with no circle, 36 dp from the card's right edge`() {
        setForm(FormShape.ToAndFrom)
        type("seven hills")

        assertBareCross("the card", behind = tint)
        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear(
            "the cross stands on the controls' axis",
            36.dp,
            card.right - boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH).cx(),
        )
    }

    @Test
    fun `the cross is the size of the field's back arrow`() {
        setForm(FormShape.Single)
        type("seven hills")

        val arrow = composeRule.onNode(hasContentDescription("Back"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val cross = boundsOf(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH)
        assertNear("the arrow's glyph is 24 dp", 24.dp, arrow.width)
        assertNear("the cross is as wide as the arrow", arrow.width, cross.width)
        assertNear("and as tall", arrow.height, cross.height)
    }

    @Test
    fun `no circle node is left on the clear X`() {
        setForm(FormShape.Single)
        for (shape in listOf(FormShape.Single, FormShape.ToAndFrom)) {
            showShape(shape)
            type("a")
            assertTrue(exists(TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH), "$shape draws the bare cross")
            assertTrue(!exists("searchstop.clear.circle"), "$shape draws no circle round it")
        }
    }

    // endregion

    // region when it shows and what it does

    @Test
    fun `the X shows only while the field holds text, in the pill and in the card`() {
        setForm(FormShape.Single)
        for (shape in listOf(FormShape.Single, FormShape.ToAndFrom)) {
            showShape(shape)
            assertTrue(!exists(TripPlannerTestTags.SEARCH_STOP_CLEAR), "$shape, empty: no X")

            type("Cen")
            assertTrue(exists(TripPlannerTestTags.SEARCH_STOP_CLEAR), "$shape, with text: the X")

            type("")
            assertTrue(!exists(TripPlannerTestTags.SEARCH_STOP_CLEAR), "$shape, cleared: no X")
        }
    }

    @Test
    fun `a tap on the X clears the field, and a screen reader hears one button named Clear`() {
        setForm(FormShape.Single)
        for (shape in listOf(FormShape.Single, FormShape.ToAndFrom)) {
            showShape(shape)
            type("Cen")

            val config = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).fetchSemanticsNode().config
            assertEquals(
                "Clear",
                config[SemanticsProperties.ContentDescription].joinToString(),
                "$shape, the name is unchanged",
            )
            assertEquals(Role.Button, config[SemanticsProperties.Role], "$shape, it is a button")

            composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).performClick()
            composeRule.waitForIdle()
            assertEquals("", typed.text.toString(), "$shape, the tap cleared the field")
            assertTrue(!exists(TripPlannerTestTags.SEARCH_STOP_CLEAR), "$shape, and the X went")
        }
    }

    @Test
    fun `a tap in the corner of the 48 dp box clears the field and never reaches the map under it`() {
        setForm(FormShape.Single)
        for (shape in listOf(FormShape.Single, FormShape.ToAndFrom)) {
            showShape(shape)
            type("Cen")
            mapTaps = 0
            val box = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).fetchSemanticsNode().boundsInRoot
            val density = composeRule.density.density

            composeRule.onRoot().performTouchInput { click(Offset(box.left + 2f * density, box.top + 2f * density)) }
            composeRule.waitForIdle()

            assertEquals("", typed.text.toString(), "$shape, the corner of the box is the X's")
            assertEquals(0, mapTaps, "$shape, and the map did not get it")
        }
    }

    // endregion

    private companion object {
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
    }
}
