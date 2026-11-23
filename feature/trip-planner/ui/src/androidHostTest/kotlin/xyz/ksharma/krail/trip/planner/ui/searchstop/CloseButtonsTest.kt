package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.components.softFieldColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCard
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardPlacement
import xyz.ksharma.krail.trip.planner.ui.stopcard.stopCardPalette

/**
 * Every X on Search Stop and its stop card is a circle with a cross in it, never a bare glyph: taj's round icon button
 * 40 dp across inside a real 48 dp box that is itself the clickable, tagged for the screen reader as `Close` or `Clear`. The
 * 48 dp box takes the tap in the 4 dp around the circle, which the map under a control that floats over it would otherwise
 * win. The cross is 4.5 to 1 or better on its circle, and the circle shows on what it sits on.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class CloseButtonsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var closed = 0
    private var mapTaps = 0

    private fun showCard() {
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { mapTaps++ } })
                    StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                        StopCard(
                            stop = CENTRAL,
                            departures = DeparturesState(isLoading = false),
                            onEvent = {},
                            onClose = { closed++ },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun showScreen() {
        val state = SearchStopFixtures.recentWithDefaults().copy(
            isMapsAvailable = true,
            mapUiState = MapUiState.Ready(mapDisplay = MapDisplay(userLocation = LatLng(-33.8688, 151.2093))),
        )
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(
                    LocalInspectionMode provides true,
                    LocalSoftwareKeyboardController provides object : SoftwareKeyboardController {
                        override fun show() = Unit
                        override fun hide() = Unit
                    },
                ) {
                    SearchStopScreen(searchStopState = state)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun near(what: String, expected: Dp, actual: Dp) =
        assertTrue(abs(expected.value - actual.value) <= 1.5f, "$what should be $expected but is $actual")

    /** What a screen reader reads on the control: the merged node's description, whole. */
    private fun description(tag: String): String? = composeRule.onNodeWithTag(tag).fetchSemanticsNode().config
        .let { if (it.contains(SemanticsProperties.ContentDescription)) it[SemanticsProperties.ContentDescription].joinToString() else null }

    // region the stop card's X

    @Test
    fun `the stop card's X is a 40 dp circle in a real 48 dp target at the end of the header`() {
        showCard()

        val target = bounds(TripPlannerTestTags.StopCard.CLOSE)
        val circle = bounds(TripPlannerTestTags.StopCard.CLOSE_CIRCLE)
        near("target width", 48.dp, target.width)
        near("target height", 48.dp, target.height)
        near("circle width", 40.dp, circle.width)
        near("circle height", 40.dp, circle.height)
        near("the circle is centred in its target, across", target.left + 4.dp, circle.left)
        near("and down", target.top + 4.dp, circle.top)
        assertTrue(target.right <= bounds(TripPlannerTestTags.StopCard.HEADER).right, "It stays in the header")
    }

    @Test
    fun `the stop card's X reads Close and the stop's name, is a button, and a tap closes the card`() {
        showCard()

        val label = description(TripPlannerTestTags.StopCard.CLOSE)
        assertTrue(label != null && label.startsWith("Close") && "Central Station" in label, "The label is $label")
        val config = composeRule.onNodeWithTag(TripPlannerTestTags.StopCard.CLOSE).fetchSemanticsNode().config
        assertEquals(Role.Button, config[SemanticsProperties.Role])
        composeRule.onNodeWithTag(TripPlannerTestTags.StopCard.CLOSE).performClick()
        assertEquals(1, closed)
    }

    @Test
    fun `a tap in the 4 dp around the circle closes the card and never reaches the map under it`() {
        showCard()
        val target = composeRule.onNodeWithTag(TripPlannerTestTags.StopCard.CLOSE).fetchSemanticsNode().boundsInRoot
        val density = composeRule.density.density

        composeRule.onRoot().performTouchInput { click(Offset(target.left + 1.5f * density, target.top + 1.5f * density)) }
        composeRule.waitForIdle()

        assertEquals(1, closed, "The corner of the 48 dp box is the X's")
        assertEquals(0, mapTaps, "and the map did not get it")
    }

    @Test
    fun `the cross reads on its circle and the circle shows on the header, in the light`() {
        var glyph = Color.Unspecified
        var circle = Color.Unspecified
        var header = Color.Unspecified
        composeRule.setContent {
            PreviewTheme {
                val palette = stopCardPalette()
                glyph = palette.closeGlyph
                circle = palette.closeCircle
                header = mapCardColors().header
            }
        }
        composeRule.waitForIdle()

        println("CLOSE light glyphOnCircle=${glyph.contrastRatio(circle)} circleOnHeader=${circle.contrastRatio(header)}")
        assertTrue(glyph.contrastRatio(circle) >= 4.5f, "The cross is ${glyph.contrastRatio(circle)} to 1 on its circle")
        assertTrue(circle.contrastRatio(header) >= 1.15f, "The circle is ${circle.contrastRatio(header)} to 1 off the header")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `the cross reads on its circle and the circle shows on the header, in the dark`() {
        var glyph = Color.Unspecified
        var circle = Color.Unspecified
        var header = Color.Unspecified
        composeRule.setContent {
            PreviewTheme {
                val palette = stopCardPalette()
                glyph = palette.closeGlyph
                circle = palette.closeCircle
                header = mapCardColors().header
            }
        }
        composeRule.waitForIdle()

        println("CLOSE dark glyphOnCircle=${glyph.contrastRatio(circle)} circleOnHeader=${circle.contrastRatio(header)}")
        assertTrue(glyph.contrastRatio(circle) >= 4.5f, "The cross is ${glyph.contrastRatio(circle)} to 1 on its circle")
        assertTrue(circle.contrastRatio(header) >= 1.15f, "The circle is ${circle.contrastRatio(header)} to 1 off the header")
    }

    // endregion

    // region the search field's X

    @Test
    fun `the search field's X is a 40 dp circle in a 48 dp target, reads Clear, and clears the text`() {
        showScreen()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performTextInput("Cen")

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).assertIsDisplayed()
        val target = bounds(TripPlannerTestTags.SEARCH_STOP_CLEAR)
        val circle = bounds(TripPlannerTestTags.SEARCH_STOP_CLEAR_CIRCLE)
        near("target width", 48.dp, target.width)
        near("target height", 48.dp, target.height)
        near("circle", 40.dp, circle.width)
        assertEquals("Clear", description(TripPlannerTestTags.SEARCH_STOP_CLEAR))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).assertDoesNotExist()
    }

    @Test
    fun `the clear cross reads on its circle on the soft pill and on the lone pill's surface fill`() {
        var text = Color.Unspecified
        var soft = Color.Unspecified
        var surface = Color.Unspecified
        composeRule.setContent {
            PreviewTheme {
                text = softFieldColors().contentColor
                soft = softFieldColors().containerColor
                surface = KrailTheme.colors.surface
            }
        }
        composeRule.waitForIdle()

        // The circle is the text colour and the cross the pill's fill, whichever of the two fills the pill has.
        println("CLEAR light crossOnCircleSoft=${soft.contrastRatio(text)} crossOnCircleSurface=${surface.contrastRatio(text)}")
        assertTrue(soft.contrastRatio(text) >= 4.5f)
        assertTrue(surface.contrastRatio(text) >= 4.5f)
    }

    // endregion

    private companion object {
        val CENTRAL = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train),
        )
    }
}
