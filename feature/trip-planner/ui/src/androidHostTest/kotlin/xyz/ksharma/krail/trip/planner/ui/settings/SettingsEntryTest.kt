package xyz.ksharma.krail.trip.planner.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppInfoProvider
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.di.viewModelsModule
import xyz.ksharma.krail.trip.planner.ui.flow.RecordingTripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.entries.SettingsEntry
import kotlin.test.assertEquals

/**
 * The real Settings entry, built by the same `entryProvider` the app uses, with the real Koin registration of its view model and
 * fakes at the edges. Its Manage stop labels row asks the navigator for the Manage stop labels screen, once, and no other row does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SettingsEntryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = RecordingTripPlannerNavigator()

    @Before
    fun startKoinWithFakes() {
        startKoin {
            modules(
                viewModelsModule,
                module {
                    single<AppInfoProvider> { FakeAppInfoProvider() }
                    single<Analytics> { FakeAnalytics() }
                    single<PlatformOps> { FakePlatformOps() }
                },
            )
        }
    }

    @After
    fun stopKoinAfterwards() {
        stopKoin()
    }

    private fun launch() {
        composeRule.setContent { PreviewTheme { SettingsEntryContent(navigator) } }
        composeRule.waitForIdle()
    }

    @Test
    fun `a tap on Manage stop labels opens Manage stop labels once and does not leave Settings any other way`() {
        launch()

        composeRule.onNodeWithTag(TripPlannerTestTags.SETTINGS_ITEM_MANAGE_LABELS).performClick()
        composeRule.waitForIdle()

        assertEquals(1, navigator.manageStopLabelsRequests)
        assertEquals(0, navigator.goBackRequests)
    }

    @Test
    fun `a tap on Change Theme does not open Manage stop labels`() {
        launch()

        composeRule.onNodeWithTag(TripPlannerTestTags.SETTINGS_ITEM_THEME).performClick()
        composeRule.waitForIdle()

        assertEquals(0, navigator.manageStopLabelsRequests)
    }
}

/** The real entry, composed without `NavDisplay`, the way the Intro and home flow tests compose theirs. */
@Composable
private fun SettingsEntryContent(navigator: TripPlannerNavigator) {
    val provider = entryProvider<NavKey> { SettingsEntry(navigator) }
    provider(SettingsRoute).Content()
}
