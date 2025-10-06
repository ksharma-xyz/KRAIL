package xyz.ksharma.core.test.viewmodels

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.core.test.fakes.FakeSandookPreferences
import xyz.ksharma.core.test.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.trip.planner.ui.state.usualride.ThemeSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSelectionViewModelTest {

    private val fakeAnalytics: Analytics = FakeAnalytics()
    private val fakeSandook = FakeSandook()
    private lateinit var viewModel: ThemeSelectionViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val fakePreferences = FakeSandookPreferences()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ThemeSelectionViewModel(
            sandook = fakeSandook,
            analytics = fakeAnalytics,
            ioDispatcher = testDispatcher,
            preferences = fakePreferences,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN ThemeSelectionViewModel initial state WHEN uiState is collected THEN assert Initial State`() =
        runTest {
            viewModel.uiState.test {
                awaitItem().run {
                    assertNull(selectedThemeStyle)
                    assertFalse(themeSelected)
                }

                advanceUntilIdle()
                assertScreenViewEventTracked(
                    fakeAnalytics,
                    expectedScreenName = AnalyticsScreen.ThemeSelection.name,
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN product class is null in Sandook WHEN uiState is collected THEN selectedTransportMode is Train`() =
        runTest {
            viewModel.uiState.test {
                awaitItem().run {
                    assertNull(selectedThemeStyle)
                    assertFalse(themeSelected)
                }

                // WHEN
                assertNull(fakeSandook.getProductClass())

                // THEN
                awaitItem().run {
                    assertFalse(themeSelected)
                    assertEquals(KrailThemeStyle.Train, selectedThemeStyle)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN product class is not null in Sandook WHEN uiState is collected THEN selectedTransportMode is product class value`() =
        runTest {
            // Set up the product class in FakeSandook
            val productClass = 2L
            fakeSandook.insertOrReplaceTheme(productClass)

            viewModel.uiState.test {
                skipItems(1) // initial state

                // WHEN
                assertEquals(productClass, fakeSandook.getProductClass())

                // THEN
                awaitItem().run {
                    assertFalse(themeSelected)
                    assertEquals(KrailThemeStyle.Metro, selectedThemeStyle)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN transport mode WHEN TransportModeSelected is triggered THEN uiState is updated and analytics event is tracked`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1) // initial state

                // WHEN
                viewModel.onEvent(ThemeSelectionEvent.ThemeSelected(9))
                advanceUntilIdle()

                // THEN
                assertEquals(9, fakeSandook.getProductClass())
                awaitItem().run {
                    assertTrue(themeSelected)
                }
                assertIs<FakeAnalytics>(fakeAnalytics)
                assertTrue(fakeAnalytics.isEventTracked("theme_selected"))
                val event = fakeAnalytics.getTrackedEvent("theme_selected")
                assertIs<AnalyticsEvent.ThemeSelectedEvent>(event)
                assertEquals("9", event.themeId)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN transport mode WHEN TransportModeSelected is triggered THEN uiState is updated with themeSelected as true`() =
        runTest {
            viewModel.uiState.test {
                awaitItem().run {
                    assertNull(selectedThemeStyle)
                    assertFalse(themeSelected)
                }

                viewModel.onEvent(ThemeSelectionEvent.ThemeSelected(1))

                awaitItem().run {
                    assertTrue(themeSelected)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN different theme modes WHEN ThemeModeSelected is triggered multiple times THEN preferences updates with latest theme mode code`() =
        runTest {
            // WHEN - Set to dark mode first
            viewModel.onEvent(ThemeSelectionEvent.ThemeModeSelected(ThemeMode.DARK.code))
            advanceUntilIdle()

            // THEN
            assertEquals(ThemeMode.DARK.code, fakePreferences.getLong(SandookPreferences.KEY_THEME_MODE))

            // WHEN - Change to system mode
            viewModel.onEvent(ThemeSelectionEvent.ThemeModeSelected(ThemeMode.SYSTEM.code))
            advanceUntilIdle()

            // THEN
            assertEquals(ThemeMode.SYSTEM.code, fakePreferences.getLong(SandookPreferences.KEY_THEME_MODE))
        }
}
