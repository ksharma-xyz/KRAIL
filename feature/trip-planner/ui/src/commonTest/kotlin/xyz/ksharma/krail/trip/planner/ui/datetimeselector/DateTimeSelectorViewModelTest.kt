package xyz.ksharma.krail.trip.planner.ui.datetimeselector

import app.cash.turbine.test
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectorEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `DateTimeSelectorViewModel` holds no state — its whole job is telling analytics that the plan-trip
 * sheet was opened and that the rider reset the time. Both are `onStart` / event-driven, so neither
 * fires unless something actually subscribes or dispatches.
 */
class DateTimeSelectorViewModelTest {

    private val analytics = FakeAnalytics()

    @Test
    fun `GIVEN the sheet WHEN it is collected THEN the plan-trip screen view is tracked once`() =
        krailRunTest {
            val viewModel = DateTimeSelectorViewModel(analytics)
            assertFalse(analytics.isEventTracked("view_screen"))

            viewModel.uiState.test {
                awaitItem()
                runCurrent()

                assertScreenViewEventTracked(analytics, expectedScreenName = "PlanTrip")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN the sheet WHEN the time is reset THEN the reset is tracked`() = krailRunTest {
        val viewModel = DateTimeSelectorViewModel(analytics)

        viewModel.onEvent(DateTimeSelectorEvent.ResetDateTimeClick)

        assertTrue(analytics.isEventTracked("reset_time_click"))
    }
}
