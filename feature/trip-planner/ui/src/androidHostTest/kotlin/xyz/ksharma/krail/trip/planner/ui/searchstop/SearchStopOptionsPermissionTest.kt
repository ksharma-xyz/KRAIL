package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Options button is on the map state only while the location permission is granted. Not granted, in either of the
 * two states the location pill names (asks, or sends the rider to Settings), it is not drawn at all, with the same short
 * fade and scale as the other transitions, and it returns the same way when the permission is granted. The modes the rider
 * has switched off are the map's, held outside the button, so they survive the button being hidden.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SearchStopOptionsPermissionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var permission: PermissionStatus? by mutableStateOf(PermissionStatus.Granted)
    private var blockOpen by mutableStateOf(false)
    private val modes = NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS)
        .map { NswTransportConfig.productClassFor(it) }
    private val on = modes.first()
    private val off = modes.last()

    /** The screen's own rule: one gate decides where Options is, from the permission as the pill reads it. */
    private fun setControls(reduceMotion: Boolean = false) {
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val controls = modeControlsFor(
                        mode = SearchStopMode.Map,
                        mapReady = true,
                        locationGranted = locationGrantedFor(permission, hasUserLocation = false),
                    )
                    LaunchedEffect(controls) {
                        if (controls == ModeControls.Hidden) blockOpen = reduceModeBlock(blockOpen, ModeBlockEvent.ControlsHidden)
                    }
                    if (controls != ModeControls.Absent) {
                        SearchStopModeControls(
                            open = blockOpen,
                            // Only the first mode is on: the rider has switched the rest off.
                            selectedModes = persistentSetOf(on),
                            onButtonClick = { blockOpen = reduceModeBlock(blockOpen, ModeBlockEvent.ButtonTapped) },
                            onToggleMode = {},
                            pillHeight = MAP_OPTIONS_PILL_REST_HEIGHT,
                            onPillHeight = {},
                            visible = controls == ModeControls.Shown,
                            reduceMotion = reduceMotion,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    private fun givePermission(value: PermissionStatus?) = composeRule.runOnUiThread {
        permission = value
        Snapshot.sendApplyNotifications()
    }

    private fun optionsCount() =
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).fetchSemanticsNodes().size

    private fun optionsWidth() =
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).fetchSemanticsNode().boundsInRoot.width

    private fun chipState(productClass: Int): ToggleableState? = composeRule
        .onNodeWithTag(TripPlannerTestTags.searchStopModeChip(productClass))
        .fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState)

    @Test
    fun `with the permission granted Options is drawn`() {
        setControls()
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)

        assertEquals(1, optionsCount())
    }

    @Test
    fun `with the permission not asked yet, or refused, Options is not drawn at all`() {
        setControls()
        for (notGranted in listOf(
            PermissionStatus.NotDetermined,
            PermissionStatus.Denied(canAskAgain = true),
            PermissionStatus.Denied(canAskAgain = false),
            PermissionStatus.Restricted,
        )) {
            givePermission(PermissionStatus.Granted)
            composeRule.mainClock.advanceTimeBy(SETTLED_MS)
            assertEquals(1, optionsCount(), "Granted first")

            givePermission(notGranted)
            composeRule.mainClock.advanceTimeBy(SETTLED_MS)
            assertEquals(0, optionsCount(), "$notGranted has no Options")
        }
    }

    @Test
    fun `granting the permission brings Options back to its full size`() {
        setControls()
        val full = optionsWidth()
        givePermission(PermissionStatus.Denied(canAskAgain = false))
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, optionsCount())

        givePermission(PermissionStatus.Granted)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        val mid = optionsWidth()
        assertTrue(mid < full - 0.5f && mid > full * MapMotion.POP_START_SCALE - 1f, "It grows back, $mid of $full")
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(full, optionsWidth(), "to the size it had")
    }

    @Test
    fun `losing the permission fades Options away rather than snapping it off`() {
        setControls()
        val full = optionsWidth()

        givePermission(PermissionStatus.Denied(canAskAgain = false))
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)

        assertEquals(1, optionsCount(), "Still there part way through")
        assertTrue(optionsWidth() < full - 0.5f, "and already shrinking")
    }

    @Test
    fun `the modes the rider switched off survive Options being hidden, and its card returns shut`() {
        setControls()
        composeRule.runOnUiThread { blockOpen = true; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(ToggleableState.On, chipState(on))
        assertEquals(ToggleableState.Off, chipState(off))

        givePermission(PermissionStatus.Denied(canAskAgain = false))
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).fetchSemanticsNodes().size)

        givePermission(PermissionStatus.Granted)
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, optionsCount())
        assertEquals(0, composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).fetchSemanticsNodes().size, "Shut")
        composeRule.runOnUiThread { blockOpen = true; Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(ToggleableState.On, chipState(on), "The mode that was on is still on")
        assertEquals(ToggleableState.Off, chipState(off), "and the one that was off is still off")
    }

    @Test
    fun `with reduced motion Options is gone and back within a frame`() {
        setControls(reduceMotion = true)
        givePermission(PermissionStatus.Denied(canAskAgain = false))
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertEquals(0, optionsCount())

        givePermission(PermissionStatus.Granted)
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertEquals(1, optionsCount())
    }

    private companion object {
        const val FRAME_MS = 16L
        const val SETTLED_MS = 1_000L
    }
}
