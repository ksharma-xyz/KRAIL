package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.components.softFieldColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The group box at the top of Search Stop, read from the real box: one rounded box on taj's own surface with
 * two soft pills, 48 tall and one gap apart, whose text ends in an ellipsis at large text, wearing the info
 * tile's glow and not a plain outline.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class SearchStopGroupCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var surface = Color.Unspecified
    private var pillFill = Color.Unspecified
    private var glow = Color.Unspecified
    private var composeView: View? = null

    private fun setBox(
        fontScale: Float? = null,
        toStop: StopItem? = null,
        fromStop: StopItem? = null,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides if (fontScale != null) Density(density.density, fontScale) else density,
            ) {
                PreviewTheme {
                    composeView = LocalView.current
                    surface = KrailTheme.colors.surface
                    pillFill = softFieldColors().containerColor
                    glow = themeBackgroundColor()
                    Box(modifier = Modifier.fillMaxSize().padding(gaps.edge)) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = FormShape.ToAndFrom,
                                editing = ToFromField.To,
                                toPlaceholder = toStop?.stopName ?: TO_PLACEHOLDER,
                                fromPlaceholder = fromStop?.stopName ?: FROM_PLACEHOLDER,
                                toStop = toStop,
                                fromStop = fromStop,
                                fieldState = rememberTextFieldState(),
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
        }
        composeRule.waitForIdle()
    }

    /** The pixels the window draws, read by drawing the compose view into a bitmap. */
    private fun drawnPixels(): Bitmap {
        var bitmap: Bitmap? = null
        composeRule.runOnUiThread {
            val view = checkNotNull(composeView)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(checkNotNull(bitmap)))
        }
        return checkNotNull(bitmap)
    }

    private fun Bitmap.at(x: Int, y: Int): Color = Color(getPixel(x, y))

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    // region shape

    @Test
    fun `the box is two pills, From above To, 48 tall and one gap apart, inside one padding on all four sides`() {
        setBox()

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        assertNear("To is 48 tall", 48.dp, to.height)
        assertNear("From is 48 tall", 48.dp, from.height)
        assertTrue(from.top < to.top, "From is the top pill, To is below it")
        assertNear("8 dp between the pills, no divider", 8.dp, to.top - from.bottom)
        assertNear("padding above From", gaps.cardPadding, from.top - box.top)
        assertNear("padding under To", gaps.cardPadding, box.bottom - to.bottom)
        assertNear("padding at the start", gaps.cardPadding, from.left - box.left)
        assertNear("padding at the end", gaps.cardPadding, box.right - from.right)
        assertNear("the box is 136 tall", 136.dp, box.height)
        assertEquals(16.dp, gaps.cardCorner, "The box keeps the card's 16 dp corner.")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `the box is 136 tall at 100 percent text`() = assertBoxHeightAt(1f)

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `the box is 136 tall at 150 percent text`() = assertBoxHeightAt(1.5f)

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `the box is 136 tall at 200 percent text`() = assertBoxHeightAt(2f)

    private fun assertBoxHeightAt(scale: Float) {
        setBox(fontScale = scale)

        assertNear("the box height", 136.dp, boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height)
        assertNear("the To pill", 48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height)
        assertNear("the From pill", 48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).height)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `a long stop name ends in an ellipsis on one line and never grows the pill at 200 percent text`() {
        setBox(fontScale = 2f, toStop = LONG, fromStop = LONG)

        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("the pill stays 48 tall", 48.dp, to.height)
        val layouts = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, useUnmergedTree = true)
            .fetchSemanticsNode()
            .collectTextLayouts()
        assertTrue(layouts.isNotEmpty(), "The pills hold text that can be measured.")
        for (layout in layouts) {
            assertEquals(1, layout.lineCount, "A pill is one line, never two.")
        }
        assertTrue(layouts.any { it.isLineEllipsized(0) }, "A name that does not fit ends in an ellipsis.")
    }

    private fun androidx.compose.ui.semantics.SemanticsNode.collectTextLayouts(): List<TextLayoutResult> {
        val own = buildList {
            val results = mutableListOf<TextLayoutResult>()
            config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
            addAll(results)
        }
        return own + children.flatMap { it.collectTextLayouts() }
    }

    // endregion

    // region fills and glow

    @Test
    fun `the box is filled with taj's surface and each pill with the soft fill`() {
        setBox()

        val image = drawnPixels()
        val density = composeRule.density
        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        // A point in the box's padding, and a point in the pill clear of its text and icons.
        val inPadding = image.at(with(density) { (box.left + 6.dp).roundToPx() }, with(density) { (box.top + 24.dp).roundToPx() })
        val inPill = image.at(with(density) { (to.left + to.width() * 0.9f).roundToPx() }, with(density) { (to.top + 4.dp).roundToPx() })

        assertClose(surface, inPadding, "the box")
        assertClose(pillFill, inPill, "the pill")
        assertNotEquals(surface, pillFill)
    }

    @Test
    fun `the box edge is the glow's themed border and not a plain neutral outline`() {
        setBox()

        val image = drawnPixels()
        val density = composeRule.density
        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val midY = with(density) { (box.top + box.height / 2f).roundToPx() }
        val edgeX = with(density) { box.left.roundToPx() }
        // The border is InfoTileDefaults.borderWidth wide on the inside of the box's edge, painted over the fill.
        val border = image.at(edgeX, midY)
        val expected = glow.compositeOver(surface)

        assertEquals(1.dp, InfoTileDefaults.borderWidth, "The glow's border is the info tile's 1 dp.")
        assertClose(expected, border, "the border pixel, the theme wash over the box fill")
        assertNotEquals(surface, border, "The edge is not the box's own fill.")
    }

    @Test
    fun `the glow spreads outside the box in the theme colour`() {
        setBox()

        val image = drawnPixels()
        val density = composeRule.density
        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val midY = with(density) { (box.top + box.height / 2f).roundToPx() }
        // Just outside the left edge, where the shadow is strongest: its radius is 12 dp and it spreads 2 dp.
        val outside = image.at(with(density) { (box.left - 3.dp).roundToPx() }, midY)
        // The far edge of the root, well past the glow, is the page colour.
        val page = image.at(with(density) { 2.dp.roundToPx() }, midY)

        assertEquals(12.dp, InfoTileDefaults.shadowRadius)
        assertEquals(2.dp, InfoTileDefaults.shadowSpread)
        assertNotEquals(page, outside, "The glow tints the page just outside the box: $outside against $page.")
    }

    // endregion

    private fun assertNear(what: String, expected: androidx.compose.ui.unit.Dp, actual: androidx.compose.ui.unit.Dp) {
        assertTrue(abs((expected - actual).value) <= 1f, "$what: expected $expected, got $actual")
    }

    private fun assertClose(expected: Color, actual: Color, what: String) {
        val within = 0.03f
        assertTrue(
            abs(expected.red - actual.red) <= within && abs(expected.green - actual.green) <= within &&
                abs(expected.blue - actual.blue) <= within,
            "$what: expected $expected, got $actual",
        )
    }

    private fun DpRect.width() = right - left

    private companion object {
        val LONG = StopItem(
            stopName = "Wollongong Station Platform Two Via Sydenham And Central",
            stopId = "stop_long",
        )
    }
}
