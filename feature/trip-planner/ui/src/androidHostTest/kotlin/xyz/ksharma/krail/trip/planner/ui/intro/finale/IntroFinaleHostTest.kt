package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The finale's host with a frame clock the test drives by hand, and the Intro's part played by the test: it says it has
 * navigated when it is asked to. It holds that an idle host draws nothing, that a run covers the app and hides it from a
 * screen reader, asks the Intro to navigate once and only at full cover, lifts only after the Intro has navigated,
 * can never hang, skips on a tap, takes the reduced motion path, and on a turn of the screen jumps to its end, still
 * navigating if it had not.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroFinaleHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val hostShown = mutableStateOf(true)
    private val signal = IntroFinaleSignal()
    private val collector = CoroutineScope(Dispatchers.Unconfined)
    private var collecting: Job? = null
    private var asks = 0

    @Before
    fun countTheAsksToNavigate() {
        collecting = collector.launch { signal.navigateRequests.collect { asks++ } }
    }

    @After
    fun stopCounting() {
        collecting?.cancel()
    }

    @Composable
    private fun Host(reduceMotion: Boolean) {
        PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
            IntroFinaleHost(signal = signal, reduceMotion = reduceMotion) {
                Box(modifier = Modifier.fillMaxSize().testTag(APP)) { Text(text = "The app") }
            }
        }
    }

    private fun show(reduceMotion: Boolean = false) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { if (hostShown.value) Host(reduceMotion) }
        advance(FRAME_MS)
    }

    // What a turn of the screen does to the host: the composition is disposed and made again, and the signal, which
    // is the app's and not the screen's, is the same one.
    private fun turnTheScreen() {
        setHostShown(false)
        advance(FRAME_MS * 2)
        setHostShown(true)
        advance(FRAME_MS)
    }

    // The clock is held, so a state written here has to be applied by hand for the composition to see it.
    private fun setHostShown(shown: Boolean) {
        composeRule.runOnUiThread {
            hostShown.value = shown
            Snapshot.sendApplyNotifications()
        }
    }

    private fun advance(ms: Long) = composeRule.mainClock.advanceTimeBy(ms)

    private fun start() {
        composeRule.runOnUiThread { assertEquals(true, signal.start(KrailThemeStyle.Metro.hexColorCode)) }
        advance(FRAME_MS * 2)
    }

    private fun theFinaleIsUp() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_FINALE).assertIsDisplayed()

    private fun theFinaleIsGone() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_FINALE).assertDoesNotExist()

    @Test
    fun `idle, the host shows the app and draws nothing`() {
        show()

        composeRule.onNodeWithTag(APP).assertIsDisplayed()
        theFinaleIsGone()
        assertNull(signal.request.value)
        assertEquals(0, asks)
    }

    @Test
    fun `a run covers the app, hides it from a screen reader, and does not ask to navigate before full cover`() {
        show()
        start()

        theFinaleIsUp()
        composeRule.onNodeWithTag(APP).assertDoesNotExist()
        advance(2300)
        assertEquals(0, asks, "asked before the colour covers the screen")
        theFinaleIsUp()
    }

    @Test
    fun `it asks the Intro to navigate once, at full cover, and keeps the colour up until the Intro has navigated`() {
        show()
        start()

        advance(2700)
        assertEquals(1, asks)
        advance(1000)
        assertEquals(1, asks, "asked a second time")
        theFinaleIsUp()
    }

    @Test
    fun `once the Intro has navigated the colour lifts and the app is back in view`() {
        show()
        start()
        advance(2700)
        composeRule.runOnUiThread { signal.onNavigated() }

        advance(1500)

        theFinaleIsGone()
        composeRule.onNodeWithTag(APP).assertIsDisplayed()
        assertNull(signal.request.value)
    }

    @Test
    fun `the colour lifts even if the Intro never says it navigated, so it can never hang`() {
        show()
        start()

        advance(FINALE_COVER_MS + SETTLE_MAX_MS + FINALE_FADE_MS + 400)

        theFinaleIsGone()
        composeRule.onNodeWithTag(APP).assertIsDisplayed()
        assertEquals(1, asks)
    }

    @Test
    fun `a tap anywhere skips to full cover, asks to navigate, and the colour is gone in under a second`() {
        show()
        start()
        advance(500)

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_FINALE).performTouchInput { click(center) }
        advance(100)
        assertEquals(1, asks)
        composeRule.runOnUiThread { signal.onNavigated() }
        advance(700)

        theFinaleIsGone()
        composeRule.onNodeWithTag(APP).assertIsDisplayed()
    }

    @Test
    fun `with reduced motion it asks on the first frames and is a short cross fade, with no long wait`() {
        show(reduceMotion = true)
        start()
        advance(100)

        assertEquals(1, asks)
        composeRule.runOnUiThread { signal.onNavigated() }
        advance(900)

        theFinaleIsGone()
        composeRule.onNodeWithTag(APP).assertIsDisplayed()
    }

    @Test
    fun `a turn of the screen before the cover jumps to the end, still navigating, and never plays it again`() {
        show()
        start()
        advance(1000)
        assertEquals(0, asks)

        turnTheScreen()
        advance(200)

        assertEquals(1, asks, "the navigation that had not happened was not asked for")
        composeRule.runOnUiThread { signal.onNavigated() }
        advance(200)
        theFinaleIsGone()
        assertNull(signal.request.value)
        composeRule.onNodeWithTag(APP).assertIsDisplayed()
    }

    @Test
    fun `a turn of the screen after the Intro navigated ends at once and does not ask again`() {
        show()
        start()
        advance(2700)
        composeRule.runOnUiThread { signal.onNavigated() }
        advance(100)
        assertEquals(1, asks)

        turnTheScreen()
        advance(200)

        theFinaleIsGone()
        assertNull(signal.request.value)
        assertEquals(1, asks)
    }

    @Test
    fun `a new run after one has ended plays again from the start`() {
        show()
        start()
        advance(2700)
        composeRule.runOnUiThread { signal.onNavigated() }
        advance(1500)
        assertNull(signal.request.value)

        start()
        theFinaleIsUp()
        val second = assertNotNull(signal.request.value)

        assertEquals(2L, second.id)
    }

    private companion object {
        const val APP = "app"
        const val FRAME_MS = 16L
    }
}
