package xyz.ksharma.krail.trip.planner.ui.flow

import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.navigation.ResultEventBus
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.entries.SavedTripsEntry
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import java.util.concurrent.TimeUnit

/**
 * Base for **flow tests**: real ViewModels, real composition, a navigation-shaped lifecycle,
 * fakes only at the module edges. The layer between a ViewModel unit test (one collector, no
 * composition) and a screen interaction test (one composable, handed-in state) — the layer
 * where state observed in one place and written in another crosses a navigation boundary.
 *
 * See `docs/INTEGRATION_TESTING_PLAN.md` for the plan this implements and for what composes
 * cleanly under Robolectric and what does not.
 *
 * ## What gets composed
 *
 * The real [SavedTripsEntry], built through the real `entryProvider` DSL and rendered by
 * `NavEntry.Content()`. That is the whole reason this harness exists: the entry owns the
 * `LaunchedEffect` that writes the Ask KRAIL resolve into the home row, and the `ResultEffect`
 * that receives stop picks from the search screen. Neither is reachable from a screen test.
 *
 * `NavDisplay` and its decorators are deliberately not in the picture, so ViewModels are
 * scoped to the Activity's store rather than to a nav entry. Both survive the two lifecycle
 * events below, which is the property the flow tests rely on.
 *
 * ## Usage
 *
 * ```kotlin
 * class MyFlowTest : FlowTest() {
 *     @Test
 *     fun theRowKeepsTheRidersOwnPick() {
 *         fakes.aiTextService.extractionResult = TripIntentExtraction(...)
 *         launchHome()
 *
 *         askKrail("central to town hall")           // resolve through the real pipeline
 *         letTimePass(SETTLE_BEAT_MILLIS)            // the dialog closes itself
 *         pickStop(SearchStopFieldType.FROM, "10103", "Parramatta Station")
 *
 *         leaveAndReturn()                           // the effects re-launch here
 *
 *         composeRule.onNodeWithText("Parramatta Station").assertIsDisplayed()
 *     }
 * }
 * ```
 *
 * ## The two lifecycle events
 *
 * - [leaveAndReturn] disposes the entry and composes it again, which is what going to the
 *   stop-search screen and coming back does. Every `LaunchedEffect` in the entry is cancelled
 *   and re-launched; ViewModels are not.
 * - [recreate] is Activity recreation (rotation, theme switch): the composition is saved,
 *   thrown away and restored from its `rememberSaveable` values. ViewModels are not recreated,
 *   matching a real configuration change.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [FLOW_TEST_SDK],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
abstract class FlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    internal val fakes = HomeFlowFakes()

    internal val navigator = RecordingTripPlannerNavigator()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    /** Whether the entry is currently on screen. Flipped by [leaveAndReturn]. */
    private val hosted = mutableStateOf(true)

    @Before
    fun startFlowKoin() {
        startKoin { modules(fakes.koinModule) }
    }

    @After
    fun stopFlowKoin() {
        stopKoin()
        // The bus is a process singleton. Left alone, a result one test sent but never
        // collected is delivered to the next test's entry.
        ResultEventBus.getInstance().removeResult<StopSelectedResult>()
    }

    /**
     * Composes the home entry and waits for its first load to settle.
     *
     * The wait is not optional. The search row is held off screen until the saved-trips load
     * has emitted once and then slides in, so a test that asserts straight after composition
     * finds its node in the tree but not yet displayed.
     */
    protected fun launchHome() {
        restorationTester.setContent {
            PreviewTheme {
                if (hosted.value) {
                    HomeEntry(navigator = navigator)
                }
            }
        }
        composeRule.waitForIdle()
        letTimePass(ENTRY_REVEAL_MILLIS)
    }

    /**
     * Leave the entry and come back to it, the way a rider does when they open the stop-search
     * screen and pick something. The entry leaves composition entirely, so its effects are
     * cancelled and re-launched on return against whatever state the ViewModels now hold.
     */
    protected fun leaveAndReturn() {
        composeRule.runOnIdle { hosted.value = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { hosted.value = true }
        composeRule.waitForIdle()
        letTimePass(ENTRY_REVEAL_MILLIS)
    }

    /** Activity recreation: save, discard, restore. ViewModels survive; composition does not. */
    protected fun recreate() {
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        letTimePass(ENTRY_REVEAL_MILLIS)
    }

    /**
     * Move both clocks forward. Compose animations run on the test frame clock, while
     * `delay()` inside a ViewModel runs on the main looper's queue, and a flow test usually
     * needs both: the settle beat that closes the Ask KRAIL dialog is a `delay`, the reveal of
     * the row it closes onto is an animation.
     */
    protected fun letTimePass(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS)
        composeRule.waitForIdle()
    }

    /**
     * Ask KRAIL, from opening the surface to sending the sentence. These are the three events
     * the Ask KRAIL surface itself sends. The open matters because the phase only steps down
     * again on a surface that was open (`closeAfterHandoff`).
     *
     * Each event is composed through before the next one, because the real surface answers
     * back: `TextField` reports its contents from a `LaunchedEffect` keyed on them, so opening
     * the dialog echoes an empty `TypedTextChanged` and filling it echoes the sentence. Sent as
     * one batch, those echoes land *during* the resolve and step the phase back to IDLE, which
     * silently disables any test that depends on the resolve staying live.
     */
    protected fun askKrail(sentence: String) {
        sendAiEvent(AiSearchInputEvent.OpenInput)
        sendAiEvent(AiSearchInputEvent.TypedTextChanged(sentence))
        sendAiEvent(AiSearchInputEvent.Submit)
    }

    private fun sendAiEvent(event: AiSearchInputEvent) {
        composeRule.runOnIdle { fakes.aiSearchInputViewModel.onEvent(event) }
        composeRule.waitForIdle()
    }

    /**
     * Play the stop-search screen's part: put a picked stop on the real result bus, which is
     * exactly what `SearchStopScreen` does before it navigates back. The entry's own
     * `ResultEffect` is what receives it.
     */
    protected fun pickStop(result: StopSelectedResult) {
        ResultEventBus.getInstance().sendResult(result = result)
        composeRule.waitForIdle()
    }
}

/** Robolectric SDK level shared by every flow test. */
internal const val FLOW_TEST_SDK = 34

/** Long enough for the bottom search row's slide-in to finish after the first load. */
private const val ENTRY_REVEAL_MILLIS = 1_000L

/**
 * The real entry, composed without `NavDisplay`. `entryProvider` is the same builder the app
 * uses (`collectEntryProviders`), and `Content()` is the same call `NavDisplay` makes for the
 * top of the back stack.
 */
@Composable
private fun HomeEntry(navigator: TripPlannerNavigator) {
    val provider = entryProvider<NavKey> { SavedTripsEntry(navigator) }
    provider(SavedTripsRoute).Content()
}
