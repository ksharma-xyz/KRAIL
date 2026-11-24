package xyz.ksharma.krail.core.testing.device

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import kotlin.math.abs
import kotlin.test.fail

private const val SCROLL_STEP_PX = 10_000f
private const val SCROLL_PASSES = 50
private const val WINDOW_TOLERANCE_DP = 1f
private const val DENSITY_TOLERANCE = 0.01f

/**
 * One profile's window, and what a test does on it: compose the screen, then ask whether the rider can use it. Node queries
 * (`onNodeWithTag` and the rest) work here directly, and [rule] is the Compose test rule for the clock and idling.
 */
class DeviceMatrixScope internal constructor(
    val profile: DeviceProfile,
    val rule: ComposeTestRule,
    private val scenario: ActivityScenario<ComponentActivity>,
) : SemanticsNodeInteractionsProvider by rule {

    private val blockers = BlockerLog()
    private var readback: SystemInsets? = null

    /**
     * Composes [content] edge to edge in this profile's window, with the profile's insets dispatched to it as the platform
     * does, and the system regions drawn over it as nodes that take the touch.
     *
     * [theme] wraps the content and must be the app's bare theme, never one that pads for the system bars (`PreviewTheme`
     * does): a screen handed insets already used up passes whatever it does with them. The system regions are outside it.
     * [inspectionMode] is `LocalInspectionMode`, true by default because a host test has no map, permission or camera to wait on.
     */
    fun setContent(
        inspectionMode: Boolean = true,
        theme: @Composable (@Composable () -> Unit) -> Unit = { it() },
        content: @Composable () -> Unit,
    ) {
        val insets = profile.windowInsets()
        scenario.onActivity { activity ->
            // The app draws edge to edge and handles its own insets. Left to the default the window pads its content for the
            // system bars, the screen is handed a window already shrunk, and the test cannot fail.
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            holdInsets(activity.window.decorView, insets)
            activity.setContent {
                CompositionLocalProvider(LocalInspectionMode provides inspectionMode) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        theme { content() }
                        SystemBarBlockers(profile.insets, blockers)
                        ReportSafeDrawing { readback = it }
                    }
                }
            }
        }
        rule.waitForIdle()
        // Handed again once the content stands, so the screen settles on the insets before anything is measured.
        scenario.onActivity { holdInsets(it.window.decorView, insets) }
        rule.waitForIdle()
        checkTheWindowIsTheProfile()
    }

    /**
     * Fails if any control, a node the rider can press, toggle, select or type into, stands wholly or partly in a region the
     * system owns. Bounds are the ones the rider can see. A node inside a scroller may run under the end edge along the scroll
     * axis, so a screen that scrolls must also call [assertScrollEndClearsBottomBar].
     *
     * Fails when there is no control at all, because then there is nothing for it to check and it would pass for any layout.
     */
    fun assertNothingInteractiveUnderSystemBars() {
        val root = rootNode()
        if (controlCount(root) == 0) {
            fail("${profile.name}: the screen has no control on it, so there is nothing to check against the bars")
        }
        val problems = controlsUnderSystemBars(root, profile)
        if (problems.isNotEmpty()) {
            fail(
                "${profile.name}: ${problems.size} control(s) stand in a region the system owns " +
                    "(safe area ${safeAreaOf(root, profile)}):\n  " + problems.joinToString("\n  "),
            )
        }
    }

    /**
     * A real press at the middle of [node], at the window's own coordinates, so whatever stands over that point takes it. Fails
     * if a system region took it instead. [then] runs after the press, for the test to check what it did.
     */
    fun assertTouchReaches(node: SemanticsNodeInteraction, then: () -> Unit = {}) {
        node.visibleBoundsInRoot()
        val target = node.fetchSemanticsNode()
        val point = target.boundsInRoot.center
        blockers.reset()
        rule.onAllNodes(isRoot())[0].performTouchInput { click(point) }
        rule.waitForIdle()
        if (blockers.presses > 0) {
            fail(
                "${profile.name}: a touch on ${target.describe()} at (${point.x}, ${point.y}) px landed on " +
                    "${blockers.lastRegion}, which the system takes, so the control cannot be pressed",
            )
        }
        then()
    }

    /**
     * Scrolls [list] to its end and fails if its last item stands in the bottom system region. A list that draws edge to edge
     * needs the bottom bar's height as content padding, or its last row is out of reach.
     */
    fun assertScrollEndClearsBottomBar(list: SemanticsNodeInteraction) {
        scrollToEnd(list)
        val scroller = list.fetchSemanticsNode()
        val root = rootNode()
        val lowest = lowestContentBottom(scroller)
            ?: fail("${profile.name}: ${scroller.describe()} shows nothing at the end of its scroll")
        val barTop = safeAreaOf(root, profile).bottom
        if (lowest > barTop + root.layoutInfo.density.density) {
            fail(
                "${profile.name}: at the end of its scroll the last item of ${scroller.describe()} " +
                    "ends at $lowest px, inside ${SystemRegion.BOTTOM}, which starts at $barTop px",
            )
        }
    }

    private fun scrollToEnd(list: SemanticsNodeInteraction) {
        var previous = Float.NaN
        var passes = 0
        while (passes < SCROLL_PASSES) {
            val range = list.fetchSemanticsNode().config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
            val position = (range ?: fail("${profile.name}: the node is not a vertical scroller")).value()
            if (position == previous) return
            previous = position
            list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, SCROLL_STEP_PX) }
            rule.waitForIdle()
            passes++
        }
        fail("${profile.name}: the list did not stop scrolling after $SCROLL_PASSES passes")
    }

    private fun rootNode(): SemanticsNode = rule.onAllNodes(isRoot())[0].fetchSemanticsNode()

    /**
     * The window the test got must be the profile's, and the insets the composition reads back must be the profile's. A runner
     * that set up the wrong window, or dispatched nothing, would make every check pass or fail against the wrong device.
     */
    private fun checkTheWindowIsTheProfile() {
        val root = rootNode()
        val density = root.layoutInfo.density
        val widthDp = root.size.width / density.density
        val heightDp = root.size.height / density.density
        val widthOff = abs(widthDp - profile.widthDp) > WINDOW_TOLERANCE_DP
        val heightOff = abs(heightDp - profile.heightDp) > WINDOW_TOLERANCE_DP
        if (widthOff || heightOff) {
            fail("${profile.name}: the window is $widthDp x $heightDp dp, not ${profile.widthDp} x ${profile.heightDp}")
        }
        if (abs(density.density - profile.density) > DENSITY_TOLERANCE ||
            abs(density.fontScale - profile.fontScale) > DENSITY_TOLERANCE
        ) {
            fail(
                "${profile.name}: density ${density.density} and font scale ${density.fontScale}, " +
                    "not ${profile.density} and ${profile.fontScale}",
            )
        }
        if (readback != profile.insets) {
            fail("${profile.name}: safeDrawing reads $readback, the profile's insets are ${profile.insets}")
        }
    }
}
