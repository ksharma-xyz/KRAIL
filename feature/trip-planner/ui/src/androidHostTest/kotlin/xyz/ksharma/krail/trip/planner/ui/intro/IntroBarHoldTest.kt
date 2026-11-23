package xyz.ksharma.krail.trip.planner.ui.intro

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A bar that is fading out wears its own clothes. The colour screen and the two stop screens open with their button hidden, and the
 * bar used to be built from the new step at once, so the old bar faded out carrying the new screen's words, layout and fill: a full
 * width orange Let's KRAIL on the pink screen's bar, a blue Yes, this one on the Park & Ride screen. This draws the real
 * `IntroFlowScreen` for every ordered pair of steps on a clock that is stepped by hand, and reads what is drawn, from the semantics
 * for the words and from the pixels for the fill, at the frames of the fade. The pure rule is held by `IntroBarLeavingTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroBarHoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf<IntroState>(PreviewAskState)
    private val events = mutableListOf<IntroUiEvent>()

    // Every step with its bar shown, and the state the Intro opens that step in.
    private val shownStates = mapOf(
        IntroStep.PARK_RIDE to PreviewAskState,
        IntroStep.DESTINATION to PreviewDestinationSelected,
        IntroStep.ORIGIN to PreviewOriginNearby.copy(selectedStop = IntroStop("201510", "Redfern Station")),
        IntroStep.CONFIRM to PreviewConfirm,
        IntroStep.THEME to IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Bus.id),
    )
    private val openStates = mapOf(
        IntroStep.PARK_RIDE to PreviewAskState,
        IntroStep.DESTINATION to PreviewDestinationSuggestions,
        IntroStep.ORIGIN to PreviewOriginNearby,
        IntroStep.CONFIRM to PreviewConfirm,
        IntroStep.THEME to PreviewThemeNoneState,
    )

    private val steps = IntroStep.entries.toList()

    private fun start(reduceMotion: Boolean = false, widthDp: Int? = null, fontScale: Float = 1f) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalDensity provides Density(density.density, fontScale = fontScale),
            ) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        Box(
                            modifier = if (widthDp == null) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.width(
                                    widthDp.dp,
                                ).fillMaxHeight()
                            },
                        ) {
                            IntroFlowScreen(
                                state = shown.value,
                                suggestedStops = DEFAULT_SUGGESTED_STOPS,
                                onEvent = { event, _ -> events += event },
                            )
                        }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(SETTLE_MS)
    }

    @After
    fun restoreMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    // A write from the test is applied at once, so the next frame already draws it and a frame is a frame of the change.
    private fun set(state: IntroState) {
        shown.value = state
        Snapshot.sendApplyNotifications()
    }

    private fun goTo(state: IntroState) {
        set(state)
        composeRule.mainClock.advanceTimeBy(SETTLE_MS)
    }

    private fun frames(ms: Long) = composeRule.mainClock.advanceTimeBy(ms)

    private fun primaryNodes() =
        composeRule.onAllNodes(hasTestTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION), useUnmergedTree = true)

    /** Every node with the text [label] in the unmerged tree and in the bar's rows, whatever its opacity. */
    private fun labelNodes(label: String, fromY: Float) =
        composeRule.onAllNodes(
            hasText(label),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().filter { it.boundsInRoot.top >= fromY }

    private fun image(): BufferedImage = IntroPixels(composeRule).of(TripPlannerTestTags.INTRO_SCREEN)

    private fun close(a: Color, b: Color, tolerance: Float) =
        abs(a.red - b.red) <= tolerance && abs(a.green - b.green) <= tolerance && abs(a.blue - b.blue) <= tolerance

    /**
     * The share of [region], inside its edges, that is within [tolerance] of [colour], read from the points of a grid on the drawn
     * picture. A bar drawn there fills most of it, and the odd mode badge of a list behind it does not.
     */
    private fun share(picture: BufferedImage, region: Rect, colour: Color, tolerance: Float): Float {
        val inset = with(composeRule.density) { 12.dp.toPx() }
        val box = Rect(region.left + inset, region.top + inset, region.right - inset, region.bottom - inset)
        val step = with(composeRule.density) { 4.dp.toPx() }
        var hits = 0
        var points = 0
        var y = box.top
        while (y < box.bottom) {
            var x = box.left
            while (x < box.right) {
                points++
                if (close(
                        Color(
                            picture.getRGB(
                                x.toInt().coerceIn(0, picture.width - 1),
                                y.toInt().coerceIn(0, picture.height - 1),
                            ),
                        ),
                        colour,
                        tolerance,
                    )
                ) {
                    hits++
                }
                x += step
            }
            y += step
        }
        return if (points == 0) 0f else hits.toFloat() / points
    }

    /** The primary button of each step with its bar shown: the fill it wears at rest and where it is. */
    private class Measured(val fill: Color, val shownRegion: Rect, val openRegion: Rect)

    private fun measure(): Map<IntroStep, Measured> = steps.associateWith { step ->
        goTo(openStates.getValue(step))
        val open = primaryNodes().fetchSemanticsNodes().single().boundsInRoot
        goTo(shownStates.getValue(step))
        val region = primaryNodes().fetchSemanticsNodes().single().boundsInRoot
        val inset = with(composeRule.density) { 14.dp.toPx() }
        val fill = Color(image().getRGB((region.left + inset).toInt(), region.center.y.toInt()))
        Measured(fill, region, open)
    }

    private fun labelsOf(step: IntroStep): List<String> =
        requireNotNull(introBar(shownStates.getValue(step))).let { listOfNotNull(it.left?.label, it.right.label) }

    private fun pairs() = steps.flatMap { from -> steps.filter { it != from }.map { from to it } }

    private fun newBarIsShown(to: IntroStep) = requireNotNull(introBar(openStates.getValue(to))).shown

    private fun barTop(): Float = composeRule.onAllNodes(
        hasTestTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION) or hasTestTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().minOf { it.boundsInRoot.top }

    @Test
    fun `at the frames of the fade the old bar draws its own words and fill, and the new bar's fill is never drawn`() {
        start()
        val measured = measure()
        pairs().filter { (_, to) -> !newBarIsShown(to) }.forEach { (from, to) ->
            goTo(shownStates.getValue(from))
            val where = "$from to $to"
            set(openStates.getValue(to))
            frames(FRAME_MS)
            val old = measured.getValue(from)
            val new = measured.getValue(to)

            // Straight away the old bar is what it was: its fill is on the screen, its words are drawn, and the new fill is not.
            var picture = image()
            assertTrue(
                share(picture, old.shownRegion, old.fill, 0.05f) > 0.5f,
                "the old fill is not drawn at the first frame of $where",
            )
            assertTrue(
                share(picture, new.openRegion, new.fill, 0.02f) < 0.05f,
                "the new fill is drawn at the first frame of $where",
            )
            val top = barTop()
            labelsOf(
                from,
            ).forEach {
                assertTrue(
                    labelNodes(it, top).isNotEmpty(),
                    "\"$it\" is not drawn at the first frame of $where",
                )
            }

            // Half way through the fade it is still the old bar and still never the new bar's fill.
            frames(BAR_REVEAL_MS / 2L)
            picture = image()
            labelsOf(
                from,
            ).forEach { assertTrue(labelNodes(it, top).isNotEmpty(), "\"$it\" is gone half way through $where") }
            assertTrue(
                share(picture, new.openRegion, new.fill, 0.02f) < 0.05f,
                "the new fill is drawn half way through $where",
            )

            // When the fade is over the old bar is gone, and the new one is still not shown.
            frames(BAR_REVEAL_MS.toLong())
            picture = image()
            assertEquals(
                1,
                primaryNodes().fetchSemanticsNodes().size,
                "the old bar is still there after the fade of $where",
            )
            assertTrue(
                share(picture, new.openRegion, new.fill, 0.02f) < 0.05f,
                "the new fill is drawn after the fade of $where",
            )
        }
    }

    @Test
    fun `a new bar that is shown takes over at once, and no old bar is held behind it`() {
        start()
        pairs().filter { (_, to) -> newBarIsShown(to) }.forEach { (from, to) ->
            goTo(shownStates.getValue(from))
            val where = "$from to $to"
            set(openStates.getValue(to))
            frames(FRAME_MS)

            val top = barTop()
            val old = labelsOf(from) - labelsOf(to).toSet()
            old.forEach {
                assertTrue(
                    labelNodes(it, top).isEmpty(),
                    "\"$it\" of the old bar is held behind the new bar in $where",
                )
            }
            labelsOf(
                to,
            ).forEach { assertTrue(labelNodes(it, top).isNotEmpty(), "\"$it\" of the new bar is not drawn in $where") }
            assertEquals(1, primaryNodes().fetchSemanticsNodes().size, where)
        }
    }

    @Test
    fun `while a bar fades out it takes no touch, has nothing for a screen reader and sends no event`() {
        start()
        pairs().filter { (_, to) -> !newBarIsShown(to) }.forEach { (from, to) ->
            goTo(shownStates.getValue(from))
            events.clear()
            set(openStates.getValue(to))
            frames(FRAME_MS * 3)
            val where = "$from to $to"

            // Nothing of either bar is in the tree a screen reader reads, or a keyboard or a switch reaches.
            composeRule.onAllNodesWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertCountEquals(0)
            composeRule.onAllNodesWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertCountEquals(0)
            // The unmerged tree has two: the old bar, held and fading, and the new one, hidden.
            assertEquals(
                2,
                primaryNodes().fetchSemanticsNodes().size,
                "the old bar is not held while it fades in $where",
            )
            val buttons = composeRule.onAllNodes(
                hasTestTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION) or hasTestTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION),
                useUnmergedTree = true,
            ).fetchSemanticsNodes()
            buttons.forEach { node ->
                composeRule.onRoot().performTouchInput {
                    click(
                        Offset(node.boundsInRoot.center.x, node.boundsInRoot.center.y),
                    )
                }
            }
            frames(FRAME_MS)
            assertEquals(emptyList(), events, "a tap on the fading bar was answered in $where")
        }
    }

    @Test
    fun `the held bar keeps its own layout, a stack at 320 dp and 200 percent, and not the new bar's single button`() {
        start(widthDp = 320, fontScale = 2f)
        goTo(shownStates.getValue(IntroStep.PARK_RIDE))
        val stackedNo = labelNodes(PARK_RIDE_NO, 0f).single().boundsInRoot
        val stackedYes = labelNodes(PARK_RIDE_YES, 0f).single().boundsInRoot
        assertTrue(stackedNo.bottom <= stackedYes.top, "the question's bar is not stacked at 320 dp and 200 percent")

        set(openStates.getValue(IntroStep.DESTINATION))
        frames(FRAME_MS * 2)

        val no = labelNodes(PARK_RIDE_NO, 0f).single().boundsInRoot
        val yes = labelNodes(PARK_RIDE_YES, 0f).single().boundsInRoot
        assertEquals(stackedNo, no, "the held bar moved")
        assertEquals(stackedYes, yes, "the held bar moved")
        assertEquals(1, labelNodes(STOP_CHOOSE, 0f).size, "the new bar's words are in the tree once, hidden")
    }

    @Test
    fun `with reduced motion the old bar cuts out in one frame and nothing of the new bar is drawn`() {
        start(reduceMotion = true)
        val measured = measure()
        pairs().filter { (_, to) -> !newBarIsShown(to) }.forEach { (from, to) ->
            goTo(shownStates.getValue(from))
            set(openStates.getValue(to))
            frames(FRAME_MS)
            val where = "$from to $to"
            val old = measured.getValue(from)
            val new = measured.getValue(to)

            val picture = image()
            assertEquals(1, primaryNodes().fetchSemanticsNodes().size, "an old bar is held in $where")
            assertTrue(
                share(picture, old.shownRegion, old.fill, 0.05f) < 0.05f,
                "the old fill is still drawn in $where",
            )
            assertTrue(share(picture, new.openRegion, new.fill, 0.02f) < 0.05f, "the new fill is drawn in $where")
        }
    }

    @Test
    fun `the colour screen's bar appears with the pick, in its own fill, once the old bar has gone`() {
        start()
        goTo(shownStates.getValue(IntroStep.CONFIRM))
        set(openStates.getValue(IntroStep.THEME))
        frames(BAR_REVEAL_MS + SETTLE_MS)
        assertEquals(1, primaryNodes().fetchSemanticsNodes().size)

        set(shownStates.getValue(IntroStep.THEME))
        frames(BAR_REVEAL_MS + FRAME_MS * 2)

        composeRule.onAllNodesWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertCountEquals(1)
        assertTrue(labelNodes(THEME_CONFIRM, barTop()).isNotEmpty())
        assertEquals(emptyList(), events)
    }

    private companion object {
        const val FRAME_MS = 16L
        const val SETTLE_MS = 700L
    }
}
