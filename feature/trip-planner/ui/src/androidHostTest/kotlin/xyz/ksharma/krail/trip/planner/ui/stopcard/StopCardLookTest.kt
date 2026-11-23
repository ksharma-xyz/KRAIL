package xyz.ksharma.krail.trip.planner.ui.stopcard

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.MapCardColors
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The look of the stop card: a header band with the stop's name as the card's title, the mode badges and the close button,
 * and a body under it for the Departure Board and the action, in the soft theme tint of every card on the map and never
 * the near black slab it was. There is no divider, the band is the separation, and the same 16 dp edge inset is in both
 * parts. The title is a larger and heavier style than the board's section label at every text size, and a very long stop
 * name wraps whole inside the header without pushing the close button out.
 *
 * Each test runs in a window of its own size at density 1, so a dp is a pixel.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class StopCardLookTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var colors: MapCardColors? = null
    private var surface = Color.Unspecified
    private var darkSlab = Color.Unspecified

    private fun show(
        stop: NearbyStopFeature = CENTRAL,
        fontScale: Float = 1f,
        boardOpen: Boolean = false,
        board: DeparturesState = DeparturesState(isLoading = false),
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PreviewTheme {
                    colors = mapCardColors()
                    surface = KrailTheme.colors.surface
                    darkSlab = FloatingDark.container
                    Card(stop, board, boardOpen)
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Card(stop: NearbyStopFeature, board: DeparturesState, boardOpen: Boolean) {
        Box(modifier = Modifier.fillMaxSize()) {
            StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                StopCard(
                    stop = stop,
                    departures = board,
                    onEvent = {},
                    onClose = {},
                    boardOpenAtStart = boardOpen,
                    actions = {
                        StopCardAction(
                            text = "Set as destination",
                            onClick = {},
                            modifier = Modifier.testTag(StopCard.PICK),
                        )
                    },
                )
            }
        }
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun layoutOf(text: String): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(out)
        return out.first()
    }

    private fun near(what: String, expected: Dp, actual: Dp) =
        assertTrue(abs(expected.value - actual.value) <= TOLERANCE.value, "$what should be $expected but is $actual")

    // region the bands

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `the header band holds the title, the badges and the close button at the top, and the body is under it with no gap`() {
        show()

        val card = bounds(StopCard.CARD)
        val header = bounds(StopCard.HEADER)
        val body = bounds(StopCard.BODY)
        near("the band starts at the card's top", card.top, header.top)
        near("and is as wide as the card", card.width, header.width)
        for (tag in listOf(StopCard.TITLE, StopCard.MODES, StopCard.CLOSE)) {
            val part = bounds(tag)
            assertTrue(
                part.top >= header.top - TOLERANCE && part.bottom <= header.bottom + TOLERANCE,
                "$tag is in the band",
            )
        }
        assertTrue(
            bounds(StopCard.MODES).top >= bounds(StopCard.TITLE).bottom - TOLERANCE,
            "The badges sit under the title",
        )
        near("the body starts where the band ends, with nothing between", header.bottom, body.top)
        assertTrue(bounds(StopCard.BOARD_HEADING).top >= body.top - TOLERANCE, "The Departure Board row is in the body")
        assertTrue(bounds(StopCard.ACTIONS).top >= body.top - TOLERANCE, "and so is the action")
        assertTrue(bounds(StopCard.CLOSE).right <= header.right, "The close button stays in the band")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `the same 16 dp inset is on both parts, so the title and the action start on one edge`() {
        show()

        val card = bounds(StopCard.CARD)
        near("the title", card.left + 16.dp, bounds(StopCard.TITLE).left)
        near("the action", card.left + 16.dp, bounds(StopCard.PICK).left)
        near("the close button's right", card.right - 16.dp, bounds(StopCard.CLOSE).right)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the band and the body are the tint family, not the near black slab and not the surface`() {
        show()
        val card = checkNotNull(colors)

        val headerPixel = pixel(StopCard.HEADER, x = 6, y = 6)
        val bodyPixel = pixel(StopCard.BODY, x = 6, y = 6)
        assertEquals(card.header.opaque(), headerPixel.opaque(), "The band is the header tint")
        assertEquals(card.body.opaque(), bodyPixel.opaque(), "The body is the body tint")
        assertNotEquals(darkSlab.opaque(), bodyPixel.opaque(), "not the fixed dark container")
        assertNotEquals(surface.opaque(), bodyPixel.opaque(), "and not the page's own surface")
        assertNotEquals(headerPixel.opaque(), bodyPixel.opaque(), "The band is visibly its own colour")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the band and the body are the tint family, not near black and not the page surface`() {
        show()
        val card = checkNotNull(colors)

        val headerPixel = pixel(StopCard.HEADER, x = 6, y = 6)
        val bodyPixel = pixel(StopCard.BODY, x = 6, y = 6)
        assertEquals(card.header.opaque(), headerPixel.opaque())
        assertEquals(card.body.opaque(), bodyPixel.opaque())
        assertNotEquals(darkSlab.opaque(), bodyPixel.opaque(), "The card is not the fixed dark container")
        assertNotEquals(surface.opaque(), bodyPixel.opaque(), "and not the page's own surface")
        assertNotEquals(headerPixel.opaque(), bodyPixel.opaque())
    }

    /** The colour drawn [x] and [y] pixels in from the top left of the node tagged [tag], read from the drawn window. */
    private fun pixel(tag: String, x: Int, y: Int): Color {
        val node = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val decor = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            decor.width.coerceAtLeast(1),
            decor.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        decor.draw(Canvas(bitmap))
        val argb = bitmap.getPixel((node.left + x).toInt(), (node.top + y).toInt())
        return Color(argb)
    }

    private fun Color.opaque() = copy(alpha = 1f)

    // endregion

    // region the type hierarchy

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `the title style is larger and heavier than the board's section label`() {
        var title: androidx.compose.ui.text.TextStyle? = null
        var label: androidx.compose.ui.text.TextStyle? = null
        composeRule.setContent {
            PreviewTheme {
                title = StopCardType.title
                label = StopCardType.sectionLabel
            }
        }
        composeRule.waitForIdle()

        val t = checkNotNull(title)
        val l = checkNotNull(label)
        assertTrue(t.fontSize.value > l.fontSize.value, "Title ${t.fontSize} against label ${l.fontSize}")
        assertTrue(
            (t.fontWeight ?: FontWeight.Normal).weight > (l.fontWeight ?: FontWeight.Normal).weight,
            "Title weight ${t.fontWeight} against label weight ${l.fontWeight}",
        )
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `drawn at 100 percent the stop name is bigger than Departure Board`() = assertTitleOverLabel(fontScale = 1f)

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `drawn at 200 percent the stop name is still bigger than Departure Board`() = assertTitleOverLabel(
        fontScale = 2f,
    )

    private fun assertTitleOverLabel(fontScale: Float) {
        show(fontScale = fontScale)

        val title = layoutOf("Central Station").layoutInput.style
        val label = layoutOf("Departure Board").layoutInput.style
        val density = composeRule.density
        assertTrue(
            title.fontSize.value * fontScale * density.density > label.fontSize.value * fontScale * density.density,
            "The title is ${title.fontSize} and the section label ${label.fontSize}",
        )
        assertTrue((title.fontWeight ?: FontWeight.Normal).weight > (label.fontWeight ?: FontWeight.Normal).weight)
        assertTrue(
            layoutOf("Central Station").size.height >= layoutOf("Departure Board").size.height,
            "and a line taller",
        )
    }

    // endregion

    // region a long name

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `a very long stop name wraps whole inside the header at 100 percent and the close button stays in`() =
        assertLongNameWraps(fontScale = 1f, windowWidth = 411.dp)

    @Test
    @Config(qualifiers = "w320dp-h800dp-notnight")
    fun `at 320 dp and 200 percent a very long stop name wraps whole inside the header and the close button stays in`() =
        assertLongNameWraps(fontScale = 2f, windowWidth = 320.dp)

    private fun assertLongNameWraps(fontScale: Float, windowWidth: Dp) {
        show(stop = LONG_NAME, fontScale = fontScale)

        val layout = layoutOf(LONG_NAME.stopName)
        assertTrue(layout.lineCount >= 2, "The name wraps, on ${layout.lineCount} lines")
        assertTrue(!layout.isLineEllipsized(layout.lineCount - 1), "and is never cut short with an ellipsis")
        assertTrue(!layout.didOverflowHeight, "or clipped")
        val header = bounds(StopCard.HEADER)
        val title = bounds(StopCard.TITLE)
        val close = bounds(StopCard.CLOSE)
        assertTrue(title.right <= close.left + TOLERANCE, "The title $title ends before the close button $close")
        assertTrue(close.right <= header.right && close.right <= windowWidth, "The close button stays on the card")
        assertTrue(title.bottom <= header.bottom, "The whole name is inside the band")
        assertTrue(
            close.width >= 48.dp - TOLERANCE && close.height >= 48.dp - TOLERANCE,
            "and the button is still 48 dp",
        )
        assertEquals(
            1,
            composeRule.onAllNodesWithText(LONG_NAME.stopName, useUnmergedTree = true).fetchSemanticsNodes().size,
        )
    }

    // endregion

    // region short windows

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `at 320 dp and 200 percent in a short window with the board open the action is on screen and the board scrolls`() {
        show(stop = CENTRAL, fontScale = 2f, boardOpen = true, board = TALL_BOARD)

        val action = bounds(StopCard.PICK)
        val card = bounds(StopCard.CARD)
        assertTrue(action.bottom <= 640.dp + TOLERANCE, "The action ends at ${action.bottom}, inside the 640 dp window")
        assertTrue(action.top >= bounds(StopCard.HEADER).bottom - TOLERANCE, "under the header")
        assertTrue(card.top >= 0.dp - TOLERANCE, "and the card does not leave the top: ${card.top}")
        assertTrue(
            bounds(StopCard.BODY).height > 0.dp,
            "The body keeps room, ${bounds(StopCard.BODY).height}, to scroll in",
        )
        assertTrue(
            bounds(StopCard.BOARD_HEADING).top >= bounds(StopCard.HEADER).bottom - TOLERANCE,
            "with the board's heading in view",
        )
    }

    // endregion

    private companion object {
        val TOLERANCE = 1.5.dp

        val CENTRAL = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train, TransportMode.Bus),
        )

        val LONG_NAME = NearbyStopFeature(
            stopId = "99",
            stopName = "Parramatta Road Opposite Camperdown Memorial Rest Park Stand Bravo",
            position = LatLng(-33.88, 151.18),
            transportModes = persistentListOf(TransportMode.Bus, TransportMode.Train, TransportMode.Ferry),
        )

        val TALL_BOARD = DeparturesState(
            isLoading = false,
            departures = kotlinx.collections.immutable.persistentListOf<xyz.ksharma.krail.departures.ui.state.model.StopDeparture>()
                .addAll(
                    (1..14).map {
                        xyz.ksharma.krail.departures.ui.state.model.StopDeparture(
                            lineNumber = "T$it",
                            lineColorCode = "#F99D1C",
                            transportModeName = "Train",
                            destinationName = "Anywhere",
                            departureTimeText = "11:${30 + it} AM",
                            departureUtcDateTime = "2026-11-22T01:30:00Z",
                        )
                    },
                ),
        )
    }
}
