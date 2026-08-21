package xyz.ksharma.krail.trip.planner.ui.settings

import app.cash.turbine.test
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfo
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfoProvider
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * `SettingsViewModel` reads the app version once a subscriber arrives and fires four analytics
 * events. The version string is the visible half: a debug build shows the build number, a release
 * build must not, and the debug-only tile is gated on the same flag.
 */
class SettingsViewModelTest {

    private val analytics = FakeAnalytics()
    private val platformOps = FakePlatformOps()
    private val appInfoProvider = FakeAppInfoProvider()

    private fun viewModel() = SettingsViewModel(
        appInfoProvider = appInfoProvider,
        analytics = analytics,
        platformOps = platformOps,
    )

    @Test
    fun `GIVEN a release build WHEN settings opens THEN the version has no build number and no debug tile`() =
        krailRunTest {
            appInfoProvider.mockAppInfo = FakeAppInfo(
                appVersion = "1.25.0",
                appBuildNumber = "412",
                isDebug = false,
            )

            viewModel().uiState.test {
                awaitItem()
                runCurrent()

                val state = expectMostRecentItem()
                assertEquals("1.25.0", state.appVersion)
                assertFalse(state.isDebug)
                assertScreenViewEventTracked(analytics, expectedScreenName = "Settings")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a debug build WHEN settings opens THEN the build number shows and the debug tile unlocks`() =
        krailRunTest {
            appInfoProvider.mockAppInfo = FakeAppInfo(
                appVersion = "1.25.0",
                appBuildNumber = "412",
                isDebug = true,
            )

            viewModel().uiState.test {
                awaitItem()
                runCurrent()

                val state = expectMostRecentItem()
                assertEquals("1.25.0 (412)", state.appVersion)
                assertTrue(state.isDebug)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN settings WHEN a friend is referred THEN the share sheet opens and is sourced to settings`() =
        krailRunTest {
            viewModel().onReferFriendClick()

            assertEquals("Tell your mates about KRAIL App", platformOps.lastShareTitle)
            assertTrue(platformOps.lastSharedText.orEmpty().contains("Download KRAIL here"))
            val event = assertIs<AnalyticsEvent.ReferFriend>(analytics.getTrackedEvent("refer_friend"))
            assertEquals(AnalyticsEvent.ReferFriend.EntryPoint.SETTINGS, event.entryPoint)
        }

    @Test
    fun `GIVEN settings WHEN the how-to and our-story rows are tapped THEN each is tracked separately`() =
        krailRunTest {
            val viewModel = viewModel()

            viewModel.onIntroClick()
            viewModel.onOurStoryClick()

            assertTrue(analytics.isEventTracked("how_to_krail"))
            assertTrue(analytics.isEventTracked("our_story"))
        }
}
