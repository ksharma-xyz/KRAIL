package xyz.ksharma.krail.trip.planner.ui.intro

import app.cash.turbine.test
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `IntroViewModel` does the one irreversible thing in onboarding: finishing the intro seeds the
 * local stops database and flips the "has seen intro" preference. If the flip happened without the
 * seed, or before it, a rider would land on a search screen with no stops in it and no way back to
 * the intro.
 */
class IntroViewModelTest {

    private val analytics = FakeAnalytics()
    private val platformOps = FakePlatformOps()
    private val preferences = FakeSandookPreferences()
    private val stopsManager = FakeStopsManager()
    private val busRoutesManager = FakeStopsManager()

    private fun viewModel() = IntroViewModel(
        analytics = analytics,
        platformOps = platformOps,
        preferences = preferences,
        nswStopsManager = stopsManager,
        nswBusRoutesManager = busRoutesManager,
    )

    @Test
    fun `GIVEN the intro screen WHEN it is first collected THEN the pages render and the view is tracked`() =
        krailRunTest {
            viewModel().uiState.test {
                val state = awaitItem()
                runCurrent()

                assertEquals(IntroState.default().pages, state.pages)
                assertScreenViewEventTracked(analytics, expectedScreenName = "Intro")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN the invite page WHEN a friend is referred THEN the share sheet opens and is attributed`() =
        krailRunTest {
            viewModel().onEvent(
                IntroUiEvent.ReferFriend(
                    analyticsEntryPoint = AnalyticsEvent.ReferFriend.EntryPoint.INTRO_CONTENT_BUTTON,
                ),
            )

            assertEquals("Tell your mates about KRAIL App", platformOps.lastShareTitle)
            // The copy is picked at random, so pin the part every variant carries.
            assertTrue(platformOps.lastSharedText.orEmpty().contains("Download KRAIL here"))
            val event = assertIs<AnalyticsEvent.ReferFriend>(analytics.getTrackedEvent("refer_friend"))
            assertEquals(AnalyticsEvent.ReferFriend.EntryPoint.INTRO_CONTENT_BUTTON, event.entryPoint)
        }

    @Test
    fun `GIVEN the intro is finished WHEN completing THEN stops are seeded before the flag is set`() =
        krailRunTest {
            val viewModel = viewModel()

            viewModel.onEvent(
                IntroUiEvent.Complete(pageType = IntroState.IntroPageType.PARK_RIDE, pageNumber = 6),
            )
            // Nothing may happen before the coroutine runs: the flag must never be set eagerly on
            // the event, or a failed seed would leave the rider past the intro with no stops.
            assertNull(preferences.getBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO))

            runCurrent()

            assertEquals(1, stopsManager.insertStopsCallCount)
            assertEquals(1, busRoutesManager.insertStopsCallCount)
            assertEquals(true, preferences.getBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO))
        }

    @Test
    fun `GIVEN the intro is finished WHEN completing THEN the page it was completed on is recorded`() =
        krailRunTest {
            viewModel().onEvent(
                IntroUiEvent.Complete(pageType = IntroState.IntroPageType.ALERTS, pageNumber = 3),
            )
            runCurrent()

            val event = assertIs<AnalyticsEvent.IntroLetsKrailClickEvent>(
                analytics.getTrackedEvent("intro_lets_krail"),
            )
            assertEquals(AnalyticsEvent.IntroLetsKrailClickEvent.InteractionPage.ALERTS, event.pageType)
            assertEquals(3, event.pageNumber)
        }

    @Test
    fun `GIVEN every intro page WHEN completed from it THEN each maps to its own analytics page`() =
        krailRunTest {
            val viewModel = viewModel()

            IntroState.IntroPageType.entries.forEachIndexed { index, pageType ->
                viewModel.onEvent(IntroUiEvent.Complete(pageType = pageType, pageNumber = index))
            }
            runCurrent()

            val recorded = analytics.getTrackedEvents("intro_lets_krail")
                .filterIsInstance<AnalyticsEvent.IntroLetsKrailClickEvent>()
                .map { it.pageType.name }

            assertEquals(IntroState.IntroPageType.entries.map { it.name }, recorded)
        }
}
