package xyz.ksharma.krail.splash

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.appstart.AppStart
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfoProvider
import xyz.ksharma.krail.core.testing.fakes.FakeAppVersionManager
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_HAS_SEEN_INTRO
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The logo is the plain ink on the very first launch and the rider's own colour on every later one.
 *
 * First run is read from what is really stored, by the real [SplashViewModel] over the real fakes of its stores:
 * - **Fresh install.** Nothing stored. The Intro flag is unset and no colour is stored.
 * - **Killed part way through the Intro.** The Intro keeps its progress in one preference, which is not the flag and not a colour,
 *   because the colour screen is the last and the flag is written when its button is pressed. Still a first run, so neutral again.
 * - **A colour tapped, then killed.** The Intro stores the colour the moment it is tapped, before it writes the flag. The rider has
 *   chosen, so the logo is that colour, and the Intro still opens next as it did.
 * - **Finished.** The flag and the colour. The rider's colour.
 * - **An upgrade from the earlier Intro.** The earlier Intro wrote the same flag, so such a rider has it, and is shown what they were
 *   shown before, their colour, or Train orange if they never chose one.
 *
 * And the frame before the load: the state a launch starts in has the defaults of a rider who has been here before, a flag that is
 * set and Train, and the logo must not read them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashFirstRunTest {

    private val sandook = FakeSandook()
    private val preferences = FakeSandookPreferences()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel() = SplashViewModel(
        sandook = sandook,
        ioDispatcher = StandardTestDispatcher(testScheduler),
        appStart = object : AppStart {
            override fun start() = Unit
        },
        preferences = preferences,
        appVersionManager = FakeAppVersionManager(),
        appStartTracker = AppStartTracker(
            analytics = FakeAnalytics(),
            appInfoProvider = FakeAppInfoProvider(),
            peekAiAvailability = { AiAvailability.Available },
            isAiSearchEnabled = { true },
            ioDispatcher = StandardTestDispatcher(testScheduler),
        ),
    )

    /** Starts the load the way the screen does, by collecting `isLoading`, and runs it to the end. */
    private fun TestScope.loaded(): SplashViewModel = viewModel().also {
        backgroundScope.launch { it.isLoading.collect { } }
        advanceUntilIdle()
    }

    private fun colourOf(style: KrailThemeStyle): Color = style.hexColorCode.hexToComposeColor()

    private fun SplashViewModel.logo(): Color? = splashLogoColor(uiState.value)

    private fun TestScope.destination(model: SplashViewModel): Any? {
        model.onUiEvent(SplashUiEvent.SplashAnimationComplete)
        advanceUntilIdle()
        return model.uiState.value.navigationDestination
    }

    @Test
    fun `a fresh install draws the logo in the ink and goes on to the Intro`() = runTest {
        val model = loaded()

        assertTrue(model.uiState.value.loaded)
        assertTrue(model.uiState.value.isFirstRun)
        assertNull(model.logo(), "a fresh install is drawn in the ink, never the default orange")
        assertEquals(IntroRoute, destination(model))
    }

    @Test
    fun `killed part way through the Intro is still a first run, with the ink again`() = runTest {
        // What the Intro leaves behind before its last screen: its progress, and neither the flag nor a colour.
        preferences.setString("KEY_INTRO_PROGRESS", "DESTINATION")
        val model = loaded()

        assertTrue(model.uiState.value.isFirstRun)
        assertNull(model.logo())
        assertEquals(IntroRoute, destination(model))
    }

    @Test
    fun `a colour tapped on the last screen then killed shows that colour and still opens the Intro`() = runTest {
        sandook.insertOrReplaceTheme(KrailThemeStyle.Metro.id.toLong())
        val model = loaded()

        assertFalse(model.uiState.value.isFirstRun, "a colour that was chosen is never hidden")
        assertEquals(colourOf(KrailThemeStyle.Metro), model.logo())
        assertEquals(IntroRoute, destination(model), "the flag was not written, so the Intro opens as it did")
    }

    @Test
    fun `a finished Intro with a chosen colour draws the logo in that colour and goes to Saved Trips`() = runTest {
        sandook.insertOrReplaceTheme(KrailThemeStyle.Ferry.id.toLong())
        preferences.setBoolean(KEY_HAS_SEEN_INTRO, true)
        val model = loaded()

        assertFalse(model.uiState.value.isFirstRun)
        assertEquals(colourOf(KrailThemeStyle.Ferry), model.logo())
        assertEquals(SavedTripsRoute, destination(model))
    }

    @Test
    fun `a rider upgrading with a saved theme and the Intro already seen sees exactly what they saw before`() = runTest {
        sandook.insertOrReplaceTheme(KrailThemeStyle.Bus.id.toLong())
        preferences.setBoolean(KEY_HAS_SEEN_INTRO, true)
        val model = loaded()

        assertEquals(colourOf(KrailThemeStyle.Bus), model.logo(), "their saved colour, as the logo has always been")
        assertEquals(SavedTripsRoute, destination(model))
    }

    @Test
    fun `a rider who saw the Intro and never chose a colour still gets the Train orange, as today`() = runTest {
        preferences.setBoolean(KEY_HAS_SEEN_INTRO, true)
        val model = loaded()

        assertFalse(model.uiState.value.isFirstRun)
        assertEquals(colourOf(KrailThemeStyle.Train), model.logo())
    }

    @Test
    fun `a rider from before the Intro existed, with a colour and no flag, keeps their colour`() = runTest {
        sandook.insertOrReplaceTheme(KrailThemeStyle.BarbiePink.id.toLong())
        val model = loaded()

        assertEquals(colourOf(KrailThemeStyle.BarbiePink), model.logo())
        assertEquals(IntroRoute, destination(model), "no flag, so they get the Intro, as they do today")
    }

    @Test
    fun `before the load finishes the logo is never the theme colour, for a first run or a returning rider`() = runTest {
        // The state a launch starts in is a returning rider's: the flag set and the default theme.
        val start = SplashState()
        assertTrue(start.hasSeenIntro)
        assertEquals(KrailThemeStyle.Train, start.themeStyle)
        assertNull(splashLogoColor(start), "the frame before the load reads no default")

        listOf(false, true).forEach { returning ->
            if (returning) {
                sandook.insertOrReplaceTheme(KrailThemeStyle.Metro.id.toLong())
                preferences.setBoolean(KEY_HAS_SEEN_INTRO, true)
            }
            val model = viewModel()
            val seen = mutableListOf<Pair<Boolean, Color?>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                model.uiState.collect { seen += it.loaded to splashLogoColor(it) }
            }
            backgroundScope.launch { model.isLoading.collect { } }
            advanceUntilIdle()

            val before = seen.takeWhile { !it.first }
            assertTrue(before.isNotEmpty(), "the unloaded frame is seen")
            assertTrue(
                before.all { it.second == null },
                "returning $returning: a frame drew the theme colour before the load: $before",
            )
            val last = seen.last()
            assertTrue(last.first, "the load finished")
            if (returning) assertEquals(colourOf(KrailThemeStyle.Metro), last.second) else assertNull(last.second)
        }
    }

    @Test
    fun `the logo waits for the whole load, so a flag read before the colour cannot show the default`() = runTest {
        // Every state between the start and the end is checked: none is loaded until all three values are read.
        val model = viewModel()
        val states = mutableListOf<SplashState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect { states += it } }
        backgroundScope.launch { model.isLoading.collect { } }
        advanceUntilIdle()

        val firstLoaded = states.indexOfFirst { it.loaded }
        assertTrue(firstLoaded > 0, "the load was seen part way")
        states.take(firstLoaded).forEach { assertNull(splashLogoColor(it), "$it") }
        assertNotNull(states[firstLoaded].themeStyle)
        assertFalse(states[firstLoaded].hasSeenIntro, "the flag is read before the load is declared done")
    }

    @Test
    fun `the decision is the same as what the state says, for every combination`() {
        listOf(false, true).forEach { seen ->
            listOf(false, true).forEach { saved ->
                listOf(false, true).forEach { loaded -> assertDecision(seen = seen, saved = saved, loaded = loaded) }
            }
        }
    }

    private fun assertDecision(seen: Boolean, saved: Boolean, loaded: Boolean) {
        val state = SplashState(hasSeenIntro = seen, hasSavedTheme = saved, loaded = loaded)
        val firstRun = !seen && !saved
        val expected = if (loaded && !firstRun) colourOf(state.themeStyle) else null

        assertEquals(firstRun, state.isFirstRun, "seen $seen saved $saved")
        assertEquals(expected, splashLogoColor(state), "seen $seen saved $saved loaded $loaded")
    }
}
