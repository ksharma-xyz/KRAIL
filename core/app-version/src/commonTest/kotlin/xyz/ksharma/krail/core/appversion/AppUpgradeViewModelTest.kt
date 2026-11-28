package xyz.ksharma.krail.core.appversion

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.AppUpgradePromptEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfoProvider
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import kotlin.test.Test
import kotlin.test.assertEquals

class AppUpgradeViewModelTest {

    private val analytics = FakeAnalytics()
    private val platformOps = FakePlatformOps()
    private val appInfoProvider = FakeAppInfoProvider()
    private val viewModel = AppUpgradeViewModel(
        appInfoProvider = appInfoProvider,
        platformOps = platformOps,
        analytics = analytics,
    )

    @Test
    fun `onShown tracks the shown action`() {
        viewModel.onShown()

        assertEquals(
            listOf(AppUpgradePromptEvent(action = AppUpgradePromptEvent.Action.SHOWN)),
            analytics.events,
        )
    }

    @Test
    fun `onShown tracks once when the screen is recreated`() {
        viewModel.onShown()
        viewModel.onShown()
        viewModel.onShown()

        assertEquals(1, analytics.getTrackedEvents("app_upgrade_prompt").size)
    }

    @Test
    fun `onUpdateClick tracks the update click and opens the store listing`() {
        viewModel.onUpdateClick()

        assertEquals(
            listOf(AppUpgradePromptEvent(action = AppUpgradePromptEvent.Action.UPDATE_CLICK)),
            analytics.events,
        )
        assertEquals(appInfoProvider.getAppInfo().appStoreUrl, platformOps.lastOpenedUrl)
    }

    @Test
    fun `every update click is tracked`() {
        viewModel.onUpdateClick()
        viewModel.onUpdateClick()

        assertEquals(2, analytics.getTrackedEvents("app_upgrade_prompt").size)
    }

    @Test
    fun `the event carries only the action param`() {
        assertEquals(
            mapOf("action" to "shown"),
            AppUpgradePromptEvent(action = AppUpgradePromptEvent.Action.SHOWN).properties,
        )
        assertEquals(
            mapOf("action" to "update_click"),
            AppUpgradePromptEvent(action = AppUpgradePromptEvent.Action.UPDATE_CLICK).properties,
        )
    }
}
