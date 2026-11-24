package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.devicematrix.BareKrailTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The way back at the top of Search Stop. Whatever the box shows, the From and To card or the lone pill, one title bar stands
 * above it with the Timetable's bare back button, in the same place with the same padding, and the box has no arrow of its own.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopBackButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var backs = 0
    private var composeView: View? = null

    @Composable
    private fun TopBlock(shape: FormShape, onGradient: Boolean) {
        SearchStopTopBlock(
            form = SearchStopFormModel(
                shape = shape,
                editing = null,
                toPlaceholder = TO_PLACEHOLDER,
                fromPlaceholder = FROM_PLACEHOLDER,
                toStop = null,
                fromStop = null,
                fieldState = rememberTextFieldState(),
                focusRequester = remember { FocusRequester() },
                onFlowEvent = {},
                onGoClick = {},
                onTextChange = {},
            ),
            location = LocationPillState(
                pill = LocationPill.Hidden,
                granted = true,
                ask = LocationPermissionAsk(allowRequest = false, onStatus = {}, onRequest = {}),
                userLocationManager = rememberUserLocationManager(),
                onClick = {},
            ),
            onBack = { backs++ },
            onGradient = onGradient,
        )
    }

    private fun show(
        shape: FormShape,
        mode: ThemeMode = ThemeMode.LIGHT,
        onGradient: Boolean = false,
    ) {
        composeRule.setContent {
            BareKrailTheme(mode = mode) {
                composeView = LocalView.current
                Box(modifier = Modifier.fillMaxSize().background(GROUND)) { TopBlock(shape, onGradient) }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun backCount(): Int = composeRule
        .onAllNodes(hasTestTag(TripPlannerTestTags.SEARCH_STOP_BACK), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .size

    private fun assertNear(message: String, expected: Float, actual: Float) =
        assertTrue(abs(expected - actual) <= 0.5f, "$message, expected $expected and was $actual")

    /** Every pixel of the back button's square, as the window draws it. */
    private fun backPixels(): List<Color> {
        var bitmap: Bitmap? = null
        composeRule.runOnUiThread {
            val view = checkNotNull(composeView)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(checkNotNull(bitmap)))
        }
        val drawn = checkNotNull(bitmap)
        val back = boundsOf(TripPlannerTestTags.SEARCH_STOP_BACK)
        return with(composeRule.density) {
            (back.left.roundToPx() until back.right.roundToPx()).flatMap { x ->
                (back.top.roundToPx() until back.bottom.roundToPx()).map { y -> Color(drawn.getPixel(x, y)) }
            }
        }
    }

    private fun Color.isNear(other: Color): Boolean =
        abs(red - other.red) < CLOSE && abs(green - other.green) < CLOSE && abs(blue - other.blue) < CLOSE

    private fun assertBareArrowIn(ink: Color) {
        val pixels = backPixels()
        val inked = pixels.count { it.isNear(ink) }
        val ground = pixels.count { it.isNear(GROUND) }
        assertTrue(inked > MIN_INK_PIXELS, "the arrow is drawn in the expected ink, only $inked pixels are")
        // A circle behind the arrow would take most of the square from the ground.
        assertTrue(ground > pixels.size * MIN_GROUND_SHARE, "nothing is behind the arrow, $ground of ${pixels.size}")
    }

    @Test
    fun `the card's back button sits where the Timetable's does and the card starts one gap under the bar`() {
        show(FormShape.ToAndFrom)

        assertEquals(1, backCount())
        val back = boundsOf(TripPlannerTestTags.SEARCH_STOP_BACK)
        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear("width", 48f, (back.right - back.left).value)
        assertNear("height", 48f, (back.bottom - back.top).value)
        assertNear("from the start, the title bar's padding", BAR_START, back.left.value)
        assertNear("from the top, the title bar's padding", CORNER + BAR_TOP, back.top.value)
        assertNear(
            "the card starts one gap under the bar, as on the Timetable",
            CORNER + BAR_HEIGHT + UNDER_BAR,
            card.top.value,
        )
        assertNear("the card keeps its side edge", searchStopGaps().edge.value, card.left.value)
        assertTrue(back.top >= 0.dp)
    }

    @Test
    fun `a tap on the card's back button goes back once`() {
        show(FormShape.ToAndFrom)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).performClick()
        composeRule.waitForIdle()

        assertEquals(1, backs)
    }

    @Test
    fun `in light mode on the map the arrow is bare, in dark ink`() {
        show(FormShape.ToAndFrom)

        assertBareArrowIn(md_theme_light_onSurface)
    }

    @Test
    fun `in dark mode on the map, which stays light, the arrow is bare and still in dark ink`() {
        show(FormShape.ToAndFrom, mode = ThemeMode.DARK)

        assertBareArrowIn(md_theme_light_onSurface)
    }

    @Test
    fun `in dark mode on the gradient the arrow is bare, in the dark mode's own ink`() {
        show(FormShape.ToAndFrom, mode = ThemeMode.DARK, onGradient = true)

        assertBareArrowIn(md_theme_dark_onSurface)
    }

    @Test
    fun `over the lone pill the back button has the same place and padding, and the pill has no arrow of its own`() {
        show(FormShape.Single)

        assertEquals(1, backCount())
        val back = boundsOf(TripPlannerTestTags.SEARCH_STOP_BACK)
        val pill = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("from the start, as over the card", BAR_START, back.left.value)
        assertNear("from the top, as over the card", CORNER + BAR_TOP, back.top.value)
        assertNear("the pill starts where the card does", CORNER + BAR_HEIGHT + UNDER_BAR, pill.top.value)
        assertNear("the pill keeps the card's side edge", searchStopGaps().edge.value, pill.left.value)
        val glyph = boundsOf(TripPlannerTestTags.SEARCH_STOP_SEARCH_GLYPH)
        assertTrue(glyph.top > pill.top && glyph.bottom < pill.bottom, "the search glyph is inside the pill")
    }

    @Test
    fun `a tap on the back button over the lone pill goes back once`() {
        show(FormShape.Single)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).performClick()
        composeRule.waitForIdle()

        assertEquals(1, backs)
    }

    private companion object {
        /** A ground no ink or card colour is near, so what is behind the arrow can be told apart. */
        val GROUND = Color(0xFF3CB44B)

        // taj's `TitleBar`, whose tokens are its own: the button's padding at the start and above it, and the bar's height.
        // This window has no status bar, so the bar adds its corner clearance, as on the iPhone Duo outer screen.
        const val CORNER = 16f

        const val BAR_START = 8f
        const val BAR_TOP = 4f
        const val BAR_HEIGHT = 56f

        // What the Timetable leaves between its title bar and its From and To card.
        const val UNDER_BAR = 8f
        const val CLOSE = 0.08f
        const val MIN_INK_PIXELS = 40
        const val MIN_GROUND_SHARE = 0.75f
    }
}
