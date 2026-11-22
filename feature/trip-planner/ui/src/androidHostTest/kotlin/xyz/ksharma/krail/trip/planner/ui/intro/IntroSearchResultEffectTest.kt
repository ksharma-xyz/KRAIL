package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.navigation.ResultEventBus
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import kotlin.test.assertEquals

/**
 * The stop that comes back from the app's own stop search reaches the Intro as one event for each
 * pick: not lost while the search is on screen, not repeated by a turn of the screen, and not sent
 * for a pick that is not for a trip end.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class IntroSearchResultEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }
    private val events = mutableListOf<IntroUiEvent>()
    private val bus = ResultEventBus.getInstance()

    @Before
    fun clearTheBus() {
        bus.removeResult<StopSelectedResult>()
    }

    @After
    fun leaveTheBusClean() {
        // The bus is a process singleton. A result nobody collected would reach the next test.
        bus.removeResult<StopSelectedResult>()
    }

    private fun pick(fieldType: SearchStopFieldType, id: String = "10101", name: String = "Parramatta Station") {
        bus.sendResult(result = StopSelectedResult(fieldType = fieldType, stopId = id, stopName = name))
    }

    private fun show() {
        restorationTester.setContent { IntroSearchResultEffect(onEvent = { events += it }) }
        composeRule.waitForIdle()
    }

    @Test
    fun `a pick of the start arrives once as a start pick`() {
        show()

        pick(SearchStopFieldType.FROM)
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(
            listOf(
                IntroUiEvent.StopPickedFromSearch(isStart = true, stopId = "10101", stopName = "Parramatta Station"),
            ),
            events,
        )
    }

    @Test
    fun `a pick of the end arrives once as an end pick`() {
        show()

        pick(SearchStopFieldType.TO, id = "200080", name = "Wynyard Station")
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(
            listOf(IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = "200080", stopName = "Wynyard Station")),
            events,
        )
    }

    @Test
    fun `a pick made while the Intro was not composed arrives once when it is back`() {
        pick(SearchStopFieldType.FROM)

        show()

        assertEquals(1, events.size)
        assertEquals(true, (events.single() as IntroUiEvent.StopPickedFromSearch).isStart)
    }

    @Test
    fun `a turn of the screen after the pick sends nothing more`() {
        show()
        pick(SearchStopFieldType.TO)
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(1, events.size)
    }

    @Test
    fun `two picks arrive in the order they were made`() {
        show()

        pick(SearchStopFieldType.FROM, id = "1", name = "First")
        pick(SearchStopFieldType.TO, id = "2", name = "Second")
        composeRule.waitForIdle()

        assertEquals<List<IntroUiEvent>>(
            listOf(
                IntroUiEvent.StopPickedFromSearch(isStart = true, stopId = "1", stopName = "First"),
                IntroUiEvent.StopPickedFromSearch(isStart = false, stopId = "2", stopName = "Second"),
            ),
            events,
        )
    }

    @Test
    fun `a stop label pick sends nothing`() {
        show()

        pick(SearchStopFieldType.LABEL)
        composeRule.waitForIdle()

        assertEquals(emptyList<IntroUiEvent>(), events)
    }

    @Test
    fun `the latest callback receives the pick when the callback changes`() {
        val received = mutableListOf<String>()
        val which = mutableStateOf("first")
        restorationTester.setContent {
            val current = which.value
            IntroSearchResultEffect(onEvent = { received += current })
        }
        composeRule.waitForIdle()

        which.value = "second"
        composeRule.waitForIdle()
        pick(SearchStopFieldType.FROM)
        composeRule.waitForIdle()

        assertEquals(listOf("second"), received)
    }
}
