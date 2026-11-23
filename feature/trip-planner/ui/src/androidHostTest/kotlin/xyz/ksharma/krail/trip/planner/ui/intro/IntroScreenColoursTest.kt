package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.themeGroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every surface on an Intro screen is that screen's own colour family and never the app's theme. The app's theme on a first run is
 * the default Train orange, and the card, the selected row and the trip card used to be that orange beside a green, blue or pink
 * hero. The test draws the real screens with Train as the app's theme, in light and dark mode, and reads the pixels of the card, the
 * `P` tile, the selected row and the trip card out of the drawn screen, so what is held is what the rider sees. The same
 * components drawn outside the Intro still read the app's theme, which is the half that keeps Saved Trips as it was.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroScreenColoursTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf<IntroState>(PreviewCardContentState)
    private var surface = Color.Unspecified
    private var dim: KrailDimensions? = null
    private var appThemeSeen = Color.Unspecified
    private var wrappedThemeSeen = Color.Unspecified
    private var colourScreenThemeSeen = Color.Unspecified

    private val train = KrailThemeStyle.Train.hexColorCode.hexToComposeColor()

    private fun startIntro(mode: ThemeMode, appTheme: KrailThemeStyle = KrailThemeStyle.Train) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(appTheme.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        surface = KrailTheme.colors.surface
                        dim = KrailTheme.dimensions
                        IntroFlowScreen(
                            state = shown.value,
                            suggestedStops = DEFAULT_SUGGESTED_STOPS,
                            onEvent = { _, _ -> },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun dp(value: Float) = with(composeRule.density) { value.dp.toPx() }

    private fun dp(value: Dp) = with(composeRule.density) { value.toPx() }

    private fun pixel(x: Float, y: Float, rootTag: String = TripPlannerTestTags.INTRO_SCREEN): Color =
        IntroPixels(composeRule).of(rootTag).colourAt(x, y)

    private fun boundsOf(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun close(a: Color, b: Color, tolerance: Float = 0.015f) =
        abs(a.red - b.red) <= tolerance && abs(a.green - b.green) <= tolerance && abs(a.blue - b.blue) <= tolerance

    private fun assertClose(expected: Color, actual: Color, what: String) =
        assertTrue(close(expected, actual), "$what: expected $expected, drew $actual")

    /** The tint taj makes of [theme] over the page, which is what a card or a selected row draws: 0.45 in dark and 0.15 in light. */
    private fun tintOf(theme: KrailThemeStyle, dark: Boolean) =
        introGroundColour(theme.hexColorCode.hexToComposeColor(), surface, dark)

    private fun cardPixel(): Color {
        val card = boundsOf(INTRO_CARD_SLOT_TAG)
        val pad = dp(requireNotNull(dim).pageHorizontalPadding)
        return pixel(card.left + pad + dp(4f), card.center.y)
    }

    private fun tilePixel(): Color {
        val card = boundsOf(INTRO_CARD_SLOT_TAG)
        val d = requireNotNull(dim)
        val tileLeft = card.left + dp(d.pageHorizontalPadding) * 2
        val tileTop = card.top + dp(20f) + dp(d.spacingXXS)
        return pixel(tileLeft + dp(3f), tileTop + dp(d.savedTripIconButtonSize) / 2f)
    }

    private fun selectedRowPixel(state: IntroState): Color {
        val row = boundsOf(TripPlannerTestTags.introStop(requireNotNull(state.selectedStop).stopId))
        return pixel(row.left - dp(4f), row.center.y)
    }

    private fun confirmTilePixel(): Color {
        val tile = boundsOf(TripPlannerTestTags.INTRO_CONFIRM_TILE)
        return pixel(tile.left + dp(4f), tile.center.y)
    }

    private fun assertScreens(dark: Boolean) {
        val mode = if (dark) "dark" else "light"
        val trainTint = tintOf(KrailThemeStyle.Train, dark)

        // Park and Ride: the card tint is green, and the P tile is the green button's deep fill, the colour the white letter reads on.
        shown.value = PreviewCardContentState
        composeRule.waitForIdle()
        assertClose(tintOf(KrailThemeStyle.Ferry, dark), cardPixel(), "$mode Park and Ride card")
        assertClose(introButtonFill(introAccentTheme(IntroStep.PARK_RIDE)), tilePixel(), "$mode P tile")
        assertTrue(!close(trainTint, cardPixel()), "$mode: the Park and Ride card is the Train tint")
        assertTrue(!close(train, tilePixel()), "$mode: the P tile is the raw Train orange")

        // Destination: the selected row is blue.
        shown.value = PreviewDestinationSelected
        composeRule.waitForIdle()
        assertClose(tintOf(KrailThemeStyle.Bus, dark), selectedRowPixel(PreviewDestinationSelected), "$mode Destination row")
        assertTrue(!close(trainTint, selectedRowPixel(PreviewDestinationSelected)), "$mode: the selected row is the Train tint")

        // Origin: the selected row is teal.
        val origin = PreviewOriginNearby.copy(selectedStop = PreviewOriginNearby.nearbyStops.first())
        shown.value = origin
        composeRule.waitForIdle()
        assertClose(tintOf(KrailThemeStyle.Metro, dark), selectedRowPixel(origin), "$mode Origin row")

        // Confirm: the trip card is pink.
        shown.value = PreviewConfirm
        composeRule.waitForIdle()
        assertClose(tintOf(KrailThemeStyle.BarbiePink, dark), confirmTilePixel(), "$mode trip card")
        assertTrue(!close(trainTint, confirmTilePixel()), "$mode: the trip card is the Train tint")
    }

    @Test
    fun `in light mode the card, tile, selected rows and trip card are each screen's own colour, never the Train tint`() {
        startIntro(ThemeMode.LIGHT)

        assertScreens(dark = false)
    }

    @Test
    fun `in dark mode the card, tile, selected rows and trip card are each screen's own colour, never the Train tint`() {
        startIntro(ThemeMode.DARK)

        assertScreens(dark = true)
    }

    @Test
    fun `the same screens are the same colours whatever theme the app is in`() {
        // The saved theme is Ferry here, and the Destination row is still Bus blue and the Park and Ride card still Ferry green.
        startIntro(ThemeMode.LIGHT, appTheme = KrailThemeStyle.BarbiePink)

        shown.value = PreviewDestinationSelected
        composeRule.waitForIdle()
        assertClose(tintOf(KrailThemeStyle.Bus, dark = false), selectedRowPixel(PreviewDestinationSelected), "Destination row")
        shown.value = PreviewConfirm
        composeRule.waitForIdle()
        assertClose(tintOf(KrailThemeStyle.BarbiePink, dark = false), confirmTilePixel(), "trip card")
    }

    private fun startOutside(mode: ThemeMode) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        surface = KrailTheme.colors.surface
                        dim = KrailTheme.dimensions
                        Outside()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    // The two components the Intro borrows, drawn as Saved Trips and home draw them: no screen colours around them.
    @Composable
    private fun Outside() {
        val horizontal = KrailTheme.dimensions.pageHorizontalPadding
        val card = (PreviewCardContentState.parkRideCard as ParkRideCardPhase.Card).station.toParkRideUiState()
        Column(modifier = Modifier.testTag(OUTSIDE).fillMaxWidth().background(KrailTheme.colors.surface)) {
            Box(modifier = Modifier.testTag(OUTSIDE_PARK).fillMaxWidth().padding(horizontal = horizontal)) {
                ParkRideCard(parkRideUiState = card, isExpanded = true, onClick = null)
            }
            Box(modifier = Modifier.testTag(OUTSIDE_TRIP).fillMaxWidth().padding(horizontal = horizontal)) {
                SavedTripCard(
                    fromDisplay = StopDisplay(stopId = "2155384", name = "Tallawong Station"),
                    toDisplay = StopDisplay(stopId = "200060", name = "Central Station"),
                    onCardClick = {},
                )
            }
        }
    }

    private fun assertOutsideIsTheAppTheme(dark: Boolean) {
        val mode = if (dark) "dark" else "light"
        val d = requireNotNull(dim)
        val park = boundsOf(OUTSIDE_PARK)
        val trip = boundsOf(OUTSIDE_TRIP)
        // The boxes are padded by a page margin, which is outside the card, and the tile is a page margin further in.
        val margin = dp(d.pageHorizontalPadding)

        // The card is the Train tint and the tile is the raw Train colour, as they are on the home card.
        assertClose(tintOf(KrailThemeStyle.Train, dark), pixel(park.left + margin + dp(4f), park.center.y, OUTSIDE), "$mode Park and Ride card")
        val tileLeft = park.left + margin * 2
        val tileTop = park.top + dp(20f) + dp(d.spacingXXS)
        assertClose(train, pixel(tileLeft + dp(3f), tileTop + dp(d.savedTripIconButtonSize) / 2f, OUTSIDE), "$mode P tile")
        // The saved trip card, which the Intro's trip card is, is the Train tint.
        assertClose(tintOf(KrailThemeStyle.Train, dark), pixel(trip.left + margin + dp(4f), trip.center.y, OUTSIDE), "$mode saved trip card")
    }

    @Test
    fun `in light mode the same cards outside the Intro are still the app's theme, so Saved Trips and home keep Train`() {
        startOutside(ThemeMode.LIGHT)

        assertOutsideIsTheAppTheme(dark = false)
    }

    @Test
    fun `in dark mode the same cards outside the Intro are still the app's theme, so Saved Trips and home keep Train`() {
        startOutside(ThemeMode.DARK)

        assertOutsideIsTheAppTheme(dark = true)
    }

    @Test
    fun `a screen of the Intro gives its content its colour and what is outside it the app's, and the colour screen is not wrapped`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        Probe { appThemeSeen = it }
                        IntroScreenColours(IntroStep.DESTINATION) { Probe { wrappedThemeSeen = it } }
                        IntroScreenColours(IntroStep.THEME) { Probe { colourScreenThemeSeen = it } }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(train, appThemeSeen, "outside the screen the app's theme")
        assertEquals(KrailThemeStyle.Bus.hexColorCode.hexToComposeColor(), wrappedThemeSeen, "inside Destination it is Bus blue")
        assertEquals(train, colourScreenThemeSeen, "the colour screen is not wrapped, so it reads the app's theme as it did")
    }

    @Composable
    private fun Probe(seen: (Color) -> Unit) {
        val colour = themeGroundColor()
        SideEffect { seen(colour) }
    }

    private companion object {
        const val OUTSIDE = "outside.root"
        const val OUTSIDE_PARK = "outside.park"
        const val OUTSIDE_TRIP = "outside.trip"
    }
}
