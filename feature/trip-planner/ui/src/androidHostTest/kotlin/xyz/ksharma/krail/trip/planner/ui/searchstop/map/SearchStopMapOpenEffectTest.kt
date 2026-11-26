package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.LocationResult
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapDismissedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapViewedEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

/**
 * When a map open starts and ends, driven through the lifecycle, the cover and the composition the way the screen
 * is. The tracker lives outside the composition, as it does in the ViewModel that owns it in the app, so a
 * rotation here is the composition built again over the same tracker.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SearchStopMapOpenEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    private val analytics = FakeAnalytics()
    private val clock = TestTimeSource()
    private val tracker = MapOpenTracker(analytics, timeSource = clock)
    private val owner = TestLifecycleOwner()

    private val shown = mutableStateOf(true)
    private val covered = mutableStateOf(false)
    private val stopCount = mutableStateOf(0)
    private val hadUserLocation = mutableStateOf(false)
    private var changingConfigurations = false

    private fun show(
        entry: MapEntry = MapEntry.SEARCH_STOP,
        permission: PermissionStatus = PermissionStatus.Granted,
        counted: MapOpenTracker? = tracker,
        layout: MapViewedEvent.Layout = MapViewedEvent.Layout.FULL,
        reported: PermissionStatus = permission,
    ) {
        owner.moveTo(Lifecycle.State.RESUMED)
        // One manager for the whole test: a new one on each recomposition would be a new key and restart the effect.
        val manager = FakeUserLocationManager(permissionStatus = permission, reportedStatus = reported)
        restorationTester.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                if (shown.value) {
                    MapOpenEffect(
                        mapOpen = counted,
                        entry = entry,
                        layout = layout,
                        covered = covered.value,
                        hadUserLocation = hadUserLocation.value,
                        stopCount = stopCount.value,
                        userLocationManager = manager,
                        isChangingConfigurations = { changingConfigurations },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun lifecycle(state: Lifecycle.State) {
        owner.moveTo(state)
        composeRule.waitForIdle()
    }

    private fun <T> change(state: MutableState<T>, value: T) {
        composeRule.runOnIdle { state.value = value }
        composeRule.waitForIdle()
    }

    private fun rotate() {
        changingConfigurations = true
        owner.moveTo(Lifecycle.State.CREATED)
        restorationTester.emulateSavedInstanceStateRestore()
        owner.moveTo(Lifecycle.State.RESUMED)
        changingConfigurations = false
        composeRule.waitForIdle()
    }

    private fun viewed() = analytics.getTrackedEvents("map_viewed").filterIsInstance<MapViewedEvent>()

    private fun dismissed() = analytics.getTrackedEvents("map_dismissed").filterIsInstance<MapDismissedEvent>()

    // region A map that is shown

    @Test
    fun `a map that becomes visible reports one view with its entry and the permission`() {
        show(entry = MapEntry.HOME, permission = PermissionStatus.Granted)

        val event = viewed().single()
        assertEquals(MapViewedEvent.Entry.HOME, event.entry)
        assertEquals(LocationResult.GRANTED, event.locationPermission)
        assertEquals(0, dismissed().size)
    }

    @Test
    fun `the view says whether a fix was in hand as the map opened`() {
        hadUserLocation.value = true

        show()

        assertEquals(true, viewed().single().hadUserLocation)
    }

    @Test
    fun `the view waits for the lifecycle to start`() {
        owner.moveTo(Lifecycle.State.CREATED)
        val manager = FakeUserLocationManager()
        restorationTester.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MapOpenEffect(
                    mapOpen = tracker,
                    entry = MapEntry.SEARCH_STOP,
                    layout = MapViewedEvent.Layout.FULL,
                    covered = false,
                    hadUserLocation = false,
                    stopCount = 0,
                    userLocationManager = manager,
                    isChangingConfigurations = { false },
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(emptyList(), analytics.events)

        lifecycle(Lifecycle.State.STARTED)

        assertEquals(1, viewed().size)
    }

    @Test
    fun `a map the host does not count sends nothing`() {
        show(counted = null)

        lifecycle(Lifecycle.State.CREATED)
        lifecycle(Lifecycle.State.RESUMED)

        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `no map in the composition sends nothing`() {
        shown.value = false

        show()
        lifecycle(Lifecycle.State.CREATED)

        assertEquals(emptyList(), analytics.events)
    }

    // endregion

    // region How an open ends

    @Test
    fun `the app leaving the foreground ends the open, once`() {
        show()

        lifecycle(Lifecycle.State.CREATED)
        lifecycle(Lifecycle.State.DESTROYED)

        assertEquals(1, viewed().size)
        assertEquals(1, dismissed().size)
        assertEquals(viewed().single().mapSessionId, dismissed().single().mapSessionId)
    }

    @Test
    fun `a map shown again after a stop is a new open with a new id and the same entry`() {
        show(entry = MapEntry.HOME)
        lifecycle(Lifecycle.State.CREATED)

        lifecycle(Lifecycle.State.RESUMED)

        assertEquals(2, viewed().size)
        assertEquals(listOf(MapViewedEvent.Entry.HOME, MapViewedEvent.Entry.HOME), viewed().map { it.entry })
        assertNotEquals(viewed()[0].mapSessionId, viewed()[1].mapSessionId)
        assertEquals(1, dismissed().size)

        lifecycle(Lifecycle.State.CREATED)

        assertEquals(2, dismissed().size)
        assertEquals(viewed().map { it.mapSessionId }, dismissed().map { it.mapSessionId })
    }

    @Test
    fun `the cover going up ends the open and coming down starts a new one`() {
        show()

        change(covered, true)

        assertEquals(1, dismissed().size)

        change(covered, false)

        assertEquals(2, viewed().size)
        assertNotEquals(viewed()[0].mapSessionId, viewed()[1].mapSessionId)
        assertEquals(1, dismissed().size)
    }

    @Test
    fun `a map that is covered from the start is never viewed`() {
        covered.value = true

        show()
        lifecycle(Lifecycle.State.CREATED)
        lifecycle(Lifecycle.State.RESUMED)

        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `leaving the composition ends the open, and a stop after that sends no second end`() {
        show()

        change(shown, false)
        lifecycle(Lifecycle.State.CREATED)

        assertEquals(1, viewed().size)
        assertEquals(1, dismissed().size)
    }

    @Test
    fun `a stop and then leaving the composition ends the open once`() {
        show()

        lifecycle(Lifecycle.State.CREATED)
        change(shown, false)

        assertEquals(1, dismissed().size)
    }

    @Test
    fun `leaving the map and coming back is a new open with a new id`() {
        show()
        change(shown, false)

        change(shown, true)

        assertEquals(2, viewed().size)
        assertNotEquals(viewed()[0].mapSessionId, viewed()[1].mapSessionId)
        assertEquals(1, dismissed().size)
    }

    @Test
    fun `a pick ends the open and the stop that follows ends nothing more`() {
        show()

        composeRule.runOnIdle { tracker.stopPicked() }
        lifecycle(Lifecycle.State.CREATED)

        assertEquals(1, dismissed().size)
        assertEquals(MapDismissedEvent.EndReason.PICKED, dismissed().single().endReason)
    }

    @Test
    fun `an open the cover ends is ended for the list`() {
        show()

        change(covered, true)

        assertEquals(MapDismissedEvent.EndReason.LIST, dismissed().single().endReason)
    }

    @Test
    fun `an open ended by a stop or by leaving the composition is ended as left`() {
        show()
        lifecycle(Lifecycle.State.CREATED)
        lifecycle(Lifecycle.State.RESUMED)

        change(shown, false)

        assertEquals(
            listOf(MapDismissedEvent.EndReason.LEFT, MapDismissedEvent.EndReason.LEFT),
            dismissed().map { it.endReason },
        )
    }

    @Test
    fun `the view says the layout it was given`() {
        show(layout = MapViewedEvent.Layout.SIDE)

        assertEquals(MapViewedEvent.Layout.SIDE, viewed().single().layout)
    }

    @Test
    fun `the view reports a refusal the app remembers, which the system lookup has forgotten`() {
        show(permission = PermissionStatus.NotDetermined, reported = PermissionStatus.Denied(canAskAgain = false))

        assertEquals(LocationResult.DENIED, viewed().single().locationPermission)
    }

    @Test
    fun `the open ends with whether markers were on the map`() {
        show()
        change(stopCount, 4)

        lifecycle(Lifecycle.State.CREATED)

        assertEquals(true, dismissed().single().hadStops)
    }

    @Test
    fun `a map with no markers the whole open ends as had no stops`() {
        show()

        lifecycle(Lifecycle.State.CREATED)

        assertEquals(false, dismissed().single().hadStops)
    }

    @Test
    fun `the open's length is read off the clock as it ends`() {
        show()
        clock += 20_000.milliseconds

        lifecycle(Lifecycle.State.CREATED)

        assertEquals(MapDismissedEvent.DurationBucket.FIFTEEN_TO_60S, dismissed().single().durationBucket)
    }

    // endregion

    // region Rotation and a restored state

    @Test
    fun `a rotation keeps the same open, with the same id, and sends no end and no second view`() {
        show()
        val before = tracker.sessionId

        rotate()

        assertEquals(before, tracker.sessionId)
        assertEquals(1, viewed().size)
        assertEquals(0, dismissed().size)
    }

    @Test
    fun `turning the phone twice is still one open`() {
        show()

        rotate()
        rotate()

        assertEquals(1, viewed().size)
        assertEquals(0, dismissed().size)
    }

    @Test
    fun `after a rotation the open still ends once, with the id it began with`() {
        show()
        val before = tracker.sessionId

        rotate()
        lifecycle(Lifecycle.State.CREATED)

        assertEquals(1, dismissed().size)
        assertEquals(before, dismissed().single().mapSessionId)
    }

    @Test
    fun `a rotation while the map is covered starts nothing`() {
        covered.value = true
        show()

        rotate()

        assertEquals(emptyList(), analytics.events)
        assertNull(tracker.sessionId)
    }

    // endregion

    // region The permission words

    @Test
    fun `every permission status maps to one of the four Intro words`() {
        assertEquals(LocationResult.GRANTED, PermissionStatus.Granted.toMapLocationResult())
        assertEquals(LocationResult.NOT_ASKED, PermissionStatus.NotDetermined.toMapLocationResult())
        assertEquals(LocationResult.DENIED, PermissionStatus.Denied(canAskAgain = true).toMapLocationResult())
        assertEquals(LocationResult.DENIED, PermissionStatus.Denied(canAskAgain = false).toMapLocationResult())
        assertEquals(LocationResult.UNAVAILABLE, PermissionStatus.Restricted.toMapLocationResult())
    }

    @Test
    fun `a denied rider is reported as denied on the view`() {
        show(permission = PermissionStatus.Denied(canAskAgain = false))

        assertEquals(LocationResult.DENIED, viewed().single().locationPermission)
    }

    @Test
    fun `a rider who was never asked is reported as not asked on the view`() {
        show(permission = PermissionStatus.NotDetermined)

        assertEquals(LocationResult.NOT_ASKED, viewed().single().locationPermission)
    }

    // endregion

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry

        fun moveTo(state: Lifecycle.State) {
            registry.currentState = state
        }
    }
}
