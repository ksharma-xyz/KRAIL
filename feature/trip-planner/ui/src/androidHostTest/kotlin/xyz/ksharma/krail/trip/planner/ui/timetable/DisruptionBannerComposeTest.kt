package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionReason
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent

/**
 * The banner as a rider meets it: what shows, what a tap does, and which events the screen sends.
 * Pixels are covered by the snapshot suite; this checks behaviour.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class DisruptionBannerComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<TimeTableUiEvent>()
    private val opened = mutableListOf<String>()

    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    private val lineAlert = ServiceAlert(
        heading = "Buses replace trains between Cronulla, Waterfall and Central",
        message = "<div>Saturday 21 and Sunday 22 November</div>",
        isLineAlert = true,
        url = "https://transportnsw.info/alerts/details#/ems-81168",
    )

    private fun show(banner: DisruptionBannerState) {
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    LazyColumn { disruptionBannerItem(banner = banner, onEvent = { events += it }) }
                }
            }
        }
    }

    @Test
    fun noBanner_showsNothingAndSendsNothing() {
        show(DisruptionBannerState.None)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(REPLACEMENT_BUSES_TITLE).assertDoesNotExist()
        assertTrue(events.isEmpty())
    }

    @Test
    fun replacementBuses_showTheFixedTitleAndReportDisplay() {
        show(DisruptionBannerState.ReplacementBuses())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(REPLACEMENT_BUSES_TITLE).assertIsDisplayed()
        assertTrue(TimeTableUiEvent.DisruptionBannerDisplayed in events)
    }

    @Test
    fun bareReplacementBanner_doesNothingWhenTapped() {
        show(DisruptionBannerState.ReplacementBuses())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(REPLACEMENT_BUSES_TITLE).performClick()
        composeRule.waitForIdle()

        assertFalse(TimeTableUiEvent.DisruptionBannerExpanded in events)
        composeRule.onNodeWithText("More info").assertDoesNotExist()
    }

    @Test
    fun replacementBannerWithLineAlert_expandsAndOpensItsLink() {
        show(DisruptionBannerState.ReplacementBuses(lineAlert = lineAlert))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(REPLACEMENT_BUSES_TITLE).performClick()
        composeRule.waitForIdle()
        assertTrue(TimeTableUiEvent.DisruptionBannerExpanded in events)

        composeRule.onNodeWithText("More info").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("https://transportnsw.info/alerts/details#/ems-81168"), opened)
        assertTrue(
            TimeTableUiEvent.DisruptionBannerCtaClicked("https://transportnsw.info/alerts/details#/ems-81168") in events,
        )
    }

    @Test
    fun replacementBannerWithMetroAlert_showsItsDetailAndLink() {
        show(DisruptionBannerState.ReplacementBuses(disruption = previewAlert))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(REPLACEMENT_BUSES_TITLE).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("More info").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("https://transportnsw.info/alerts"), opened)
    }

    @Test
    fun metroAlertBanner_keepsTheAlertHeadlineAndOpensItsLink() {
        show(DisruptionBannerState.Shown(previewAlert, DisruptionReason.MODE_MISSING))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(previewAlert.headline).assertIsDisplayed()
        composeRule.onNodeWithText(previewAlert.headline).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("More info").performClick()
        composeRule.waitForIdle()

        assertTrue(TimeTableUiEvent.DisruptionBannerExpanded in events)
        assertEquals(listOf("https://transportnsw.info/alerts"), opened)
    }
}
