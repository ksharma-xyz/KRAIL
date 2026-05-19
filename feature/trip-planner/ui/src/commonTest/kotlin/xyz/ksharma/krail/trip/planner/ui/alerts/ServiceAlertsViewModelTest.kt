package xyz.ksharma.krail.trip.planner.ui.alerts

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertsViewModel
import xyz.ksharma.krail.trip.planner.ui.alerts.toServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlertState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceAlertsViewModelTest {

    private val fakeAnalytics = FakeAnalytics()
    private val fakeSandook = FakeSandook()
    private lateinit var viewModel: ServiceAlertsViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ServiceAlertsViewModel(
            analytics = fakeAnalytics,
            sandook = fakeSandook
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN ServiceAlertsViewModel initial state WHEN uiState is collected THEN assert Initial State`() =
        runTest {
            viewModel.uiState.test {
                awaitItem().run {
                    assertEquals(ServiceAlertState(), this)
                }

                advanceUntilIdle()
                assertScreenViewEventTracked(
                    fakeAnalytics,
                    expectedScreenName = AnalyticsScreen.ServiceAlerts.name,
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN journeyId WHEN fetchAlerts is called THEN alerts are returned`() =
        runTest {
            val journeyId = "testJourneyId"
            val expectedAlerts = listOf(
                SelectServiceAlertsByJourneyId(
                    heading = "Alert 1",
                    message = "Message 1",
                    journeyId = "1",
                ),
                SelectServiceAlertsByJourneyId(
                    heading = "Alert 2",
                    message = "Message 2",
                    journeyId = "2",
                )
            )

            fakeSandook.insertAlerts(journeyId, expectedAlerts)

            val alerts = viewModel.fetchAlerts(journeyId)
            assertEquals(expectedAlerts.map { it.toServiceAlert() }, alerts)
        }
}
