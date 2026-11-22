package xyz.ksharma.krail.trip.planner.ui.flow

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Test
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The classic home's three ways out, through the real entry and the recording navigator: the search bar opens
 * Search Stop on the destination field, the microphone in it opens Ask KRAIL and not Search Stop, and an unset
 * Set Work pill opens Search Stop to set that label.
 */
class HomeBarFlowTest : FlowTest() {

    @Test
    fun aTapOnTheBarOpensSearchStopOnTheDestinationField() {
        launchHome()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(SearchStopFieldType.TO), navigator.searchStopRequests)
        assertEquals(listOf<Pair<StopItem?, StopItem?>>(null to null), navigator.searchStopPresets)
    }

    @Test
    fun aTapOnTheMicrophoneOpensAskKrailAndNotSearchStop() {
        launchHome()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertTrue(fakes.aiSearchInputViewModel.uiState.value.isInputOpen, "Ask KRAIL is open.")
        assertTrue(navigator.searchStopRequests.isEmpty(), "Search Stop did not open.")
    }

    @Test
    fun aTapOnAnUnsetPillOpensSearchStopToSetThatLabel() {
        launchHome()

        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Work"), useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(SearchStopFieldType.LABEL), navigator.searchStopRequests)
        assertEquals(listOf<String?>("Work"), navigator.searchStopLabelKeys)
    }
}
