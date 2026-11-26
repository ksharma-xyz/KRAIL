package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.flow.RecordingTripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelTapOutcome
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The screen's half of a Home or Work chip tap: take the one location fix, act on the decision
 * once, and keep a rotation from losing either (`docs/FEATURE_QUALITY_CHECKLIST.md` sections 3 and
 * 10). The ViewModel half is `SavedTripsLabelChipTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [EFFECT_TEST_SDK], manifest = Config.NONE)
class LabelChipTapEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = RecordingTripPlannerNavigator()
    private val events = mutableListOf<SavedTripUiEvent>()

    @Test
    fun `an idle tap does nothing`() {
        setEffect(LabelChipTap.Idle)

        assertTrue(events.isEmpty())
        assertTrue(navigator.timeTableRequests.isEmpty() && navigator.searchStopRequests.isEmpty())
    }

    @Test
    fun `locating takes one fix and reports it`() {
        val located = mutableListOf<Unit>()
        setEffect(LabelChipTap.Locating("Home"), locate = {
            located += Unit
            RIDER
        })

        assertEquals(1, located.size, "One tap is one location fix.")
        assertEquals(
            listOf<SavedTripUiEvent>(
                SavedTripUiEvent.LabelChipLocated("Home", RIDER.latitude, RIDER.longitude),
            ),
            events,
        )
    }

    @Test
    fun `no fix is reported as no fix`() {
        setEffect(LabelChipTap.Locating("Work"), locate = { null })

        assertEquals(
            listOf<SavedTripUiEvent>(
                SavedTripUiEvent.LabelChipLocated("Work", null, null),
            ),
            events,
        )
    }

    @Test
    fun `a plan opens the trip results the way a saved trip does, once, then reports handled`() {
        setEffect(LabelChipTap.Decided(LabelTapOutcome.Plan(from = CENTRAL, to = TOWN_HALL)))

        assertEquals(listOf("200060" to "200070"), navigator.timeTableRequests)
        assertEquals(listOf<SavedTripUiEvent>(SavedTripUiEvent.LabelChipTapHandled), events)
    }

    @Test
    fun `a search opens the To and From search holding the labelled stop as the destination`() {
        setEffect(LabelChipTap.Decided(LabelTapOutcome.Search(to = TOWN_HALL)))

        assertEquals(
            listOf<SavedTripUiEvent>(SavedTripUiEvent.LabelChipTapHandled),
            events,
            "Home holds no stop, so the destination is not written to it.",
        )
        assertEquals(listOf(SearchStopFieldType.FROM), navigator.searchStopRequests)
        assertEquals(listOf<Pair<StopItem?, StopItem?>>(null to TOWN_HALL), navigator.searchStopPresets)
        assertTrue(navigator.timeTableRequests.isEmpty(), "The fallback never opens trip results.")
    }

    @Test
    fun `setting a label opens the stop search for that label`() {
        setEffect(LabelChipTap.Decided(LabelTapOutcome.Assign("Home")))

        assertEquals(listOf(SearchStopFieldType.LABEL), navigator.searchStopRequests)
        assertEquals(listOf<String?>("Home"), navigator.searchStopLabelKeys)
        assertEquals(listOf<SavedTripUiEvent>(SavedTripUiEvent.LabelChipTapHandled), events)
    }

    @Test
    fun `a decision is acted on once however often the screen recomposes`() {
        val tap = mutableStateOf<LabelChipTap>(LabelChipTap.Decided(LabelTapOutcome.Plan(CENTRAL, TOWN_HALL)))
        composeRule.setContent {
            LabelChipTapEffect(tap.value, { null }, { events += it }, navigator)
        }
        composeRule.waitForIdle()

        // The ViewModel steps the tap down once it is handled, which is what a re-launch then sees.
        composeRule.runOnIdle { tap.value = LabelChipTap.Idle }
        composeRule.waitForIdle()

        assertEquals(1, navigator.timeTableRequests.size)
        assertEquals(1, events.count { it == SavedTripUiEvent.LabelChipTapHandled })
    }

    @Test
    fun `a rotation during the fix delivers the real fix once and never a missing one`() {
        val fix = CompletableDeferred<LatLng?>()
        var asked = 0
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            LabelChipTapEffect(
                tap = LabelChipTap.Locating("Home"),
                locate = {
                    asked++
                    fix.await()
                },
                onEvent = { events += it },
                tripPlannerNavigator = navigator,
            )
        }
        composeRule.waitForIdle()
        assertTrue(events.isEmpty(), "The fix has not arrived, so nothing is reported yet.")

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        assertTrue(events.isEmpty(), "Recreating the screen must not report the cancelled fix as no fix.")

        fix.complete(RIDER)
        composeRule.waitForIdle()

        assertEquals(2, asked, "The recreated screen asks again, from the same tap.")
        assertEquals(
            listOf<SavedTripUiEvent>(
                SavedTripUiEvent.LabelChipLocated("Home", RIDER.latitude, RIDER.longitude),
            ),
            events,
        )
    }

    @Test
    fun `a location manager with a fix gives its position, one without gives none`() {
        val manager = FakeUserLocationManager()

        manager.answerWith(Result.success(Location(latitude = -33.8845, longitude = 151.2065, timestamp = 0L)))
        assertEquals(RIDER, runBlocking { manager.currentLatLng() })

        manager.answerWith(Result.failure(IllegalStateException("permission denied")))
        assertNull(runBlocking { manager.currentLatLng() })
    }

    @Test
    fun `a chip's tap asks for location as a home chip`() {
        val manager = FakeUserLocationManager()

        runBlocking { manager.currentLatLng() }

        assertEquals(listOf(PermissionResultEvent.Source.HOME_LABEL_CHIP), manager.locationSources)
    }

    @Test
    fun `only a result that names a label becomes a label assignment`() {
        val named = StopSelectedResult(SearchStopFieldType.LABEL, "200070", "Town Hall Station", labelKey = "Home")
        val unnamed = StopSelectedResult(SearchStopFieldType.LABEL, "200070", "Town Hall Station")

        onLabelStopSelected(named) { events += it }
        onLabelStopSelected(unnamed) { events += it }

        assertEquals(
            listOf<SavedTripUiEvent>(
                SavedTripUiEvent.LabelStopAssigned("Home", "200070", "Town Hall Station"),
            ),
            events,
        )
    }

    private fun setEffect(tap: LabelChipTap, locate: suspend () -> LatLng? = { null }) {
        composeRule.setContent {
            LabelChipTapEffect(tap, locate, { events += it }, navigator)
        }
        composeRule.waitForIdle()
    }

    private companion object {
        val RIDER = LatLng(latitude = -33.8845, longitude = 151.2065)
        val CENTRAL = StopItem(stopId = "200060", stopName = "Central Station")
        val TOWN_HALL = StopItem(stopId = "200070", stopName = "Town Hall Station")
    }
}

private const val EFFECT_TEST_SDK = 34
