package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One location result for each yes, and never a second, through the turns of the screen the rider
 * can cause: a rotation before the fix, a rotation after it while the view model is still finding
 * the station, a second yes, and a no that arrives while the fix is awaited.
 *
 * The state that says the card awaits a location belongs to the view model, which survives a turn,
 * so here it is a field of the test, as it is in the app.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroLocationEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }
    private val awaiting = mutableStateOf(false)
    private val results = mutableListOf<Pair<LocationResult, LatLng?>>()

    private val fix = Location(latitude = -33.730592, longitude = 150.944024, timestamp = 0L)
    private val fixAsLatLng = LatLng(fix.latitude, fix.longitude)

    private fun show(manager: FakeUserLocationManager) {
        restorationTester.setContent {
            IntroLocationEffect(
                awaiting = awaiting.value,
                userLocationManager = manager,
                onResult = { result, location -> results += result to location },
            )
        }
        composeRule.waitForIdle()
    }

    private fun setAwaiting(value: Boolean) {
        composeRule.runOnIdle { awaiting.value = value }
        composeRule.waitForIdle()
    }

    private fun recreate() {
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
    }

    private fun granted() = FakeUserLocationManager(currentLocation = { Result.success(fix) })

    @Test
    fun `nothing is asked while the card is not awaiting a location`() {
        val manager = granted()

        show(manager)

        assertEquals(0, manager.currentLocationCalls)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `a yes asks once and sends one granted result with the fix`() {
        val manager = granted()
        show(manager)

        setAwaiting(true)

        assertEquals(1, manager.currentLocationCalls)
        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.GRANTED to fixAsLatLng), results)
    }

    @Test
    fun `staying on the card while the station is found asks and sends nothing more`() {
        val manager = granted()
        show(manager)
        setAwaiting(true)

        repeat(3) { composeRule.waitForIdle() }

        assertEquals(1, manager.currentLocationCalls)
        assertEquals(1, results.size)
    }

    @Test
    fun `a turn of the screen after the result, while the card still awaits, sends nothing again`() {
        val manager = granted()
        show(manager)
        setAwaiting(true)

        recreate()
        recreate()

        assertEquals(1, manager.currentLocationCalls, "a result already sent is not asked for again")
        assertEquals(1, results.size)
    }

    @Test
    fun `a turn of the screen before the fix lands asks again and still sends one result`() {
        val gate = CompletableDeferred<Result<Location>>()
        val manager = FakeUserLocationManager(currentLocation = { gate.await() })
        show(manager)
        setAwaiting(true)
        assertEquals(1, manager.currentLocationCalls)

        recreate()
        composeRule.runOnIdle { gate.complete(Result.success(fix)) }
        composeRule.waitForIdle()

        assertEquals(
            2,
            manager.currentLocationCalls,
            "the first ask went with the screen and the second took its place",
        )
        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.GRANTED to fixAsLatLng), results)
    }

    @Test
    fun `a second yes after the card left Locating sends its own result`() {
        val manager = granted()
        show(manager)
        setAwaiting(true)
        setAwaiting(false)

        setAwaiting(true)

        assertEquals(2, manager.currentLocationCalls)
        assertEquals(2, results.size)
    }

    @Test
    fun `a no while the fix is awaited sends nothing for that yes, and the next yes sends one`() {
        val gate = CompletableDeferred<Result<Location>>()
        val manager = FakeUserLocationManager(currentLocation = { gate.await() })
        show(manager)
        setAwaiting(true)

        setAwaiting(false)
        composeRule.runOnIdle { gate.complete(Result.success(fix)) }
        composeRule.waitForIdle()
        assertTrue(results.isEmpty(), "the cancelled ask must not answer: $results")

        setAwaiting(true)

        assertEquals(1, results.size)
        assertEquals(2, manager.currentLocationCalls)
    }

    @Test
    fun `a refused permission, such as a dismissed dialog, is one denied result and does not leave the card waiting`() {
        val manager = FakeUserLocationManager(currentLocation = { Result.failure(LocationError.PermissionDenied()) })
        show(manager)

        setAwaiting(true)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.DENIED to null), results)
    }

    @Test
    fun `a manager that fails is one unavailable result`() {
        val manager = FakeUserLocationManager(currentLocation = { error("no fix") })
        show(manager)

        setAwaiting(true)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.UNAVAILABLE to null), results)
    }
}
