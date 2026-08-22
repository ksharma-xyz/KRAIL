package xyz.ksharma.krail.trip.planner.ui.alerts.summary

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons
import xyz.ksharma.krail.taj.components.AlertFeedbackVoteChoice
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAiTextService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val ALERT_ONE = ServiceAlert(heading = "Trains delayed", message = "Signal fault at Central.")
private val ALERT_TWO = ServiceAlert(heading = "Buses replace trains", message = "T1 line between Redfern and Central.")

@OptIn(ExperimentalCoroutinesApi::class)
class AlertSummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val aiTextService = FakeAiTextService()
    private lateinit var viewModel: AlertSummaryViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiTextService.summarizeResult = "Trains delayed due to a signal fault."
        viewModel = AlertSummaryViewModel(
            aiTextService = aiTextService,
            isAlertSummaryEnabled = { true },
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `flag off never requests a summary`() = runTest(testDispatcher) {
        viewModel = AlertSummaryViewModel(
            aiTextService = aiTextService,
            isAlertSummaryEnabled = { false },
        )

        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value)
    }

    @Test
    fun `empty alert set never requests a summary`() = runTest(testDispatcher) {
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf()))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value)
    }

    @Test
    fun `single alert resolves to a summary`() = runTest(testDispatcher) {
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlertSummaryUiState.Resolved)
        assertEquals("Trains delayed due to a signal fault.", state.text)
    }

    @Test
    fun `same alert set requested twice only summarizes once`() = runTest(testDispatcher) {
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        assertEquals(1, aiTextService.summarizeCallCount)
    }

    @Test
    fun `a newer request cancels the older one before it can overwrite the result`() = runTest(testDispatcher) {
        // Neither onEvent call is followed by advanceUntilIdle, so the first request's
        // coroutine is still queued (not yet started) when the second one cancels it via
        // activeJob - the exact guard this test exists to lock in.
        aiTextService.summarizeResult = "Trains delayed due to a signal fault."
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))

        aiTextService.summarizeResult = "Buses replace trains between Redfern and Central."
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_TWO)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlertSummaryUiState.Resolved)
        assertEquals("Buses replace trains between Redfern and Central.", state.text)
    }

    @Test
    fun `availability failure clears any previously resolved summary`() = runTest(testDispatcher) {
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AlertSummaryUiState.Resolved)

        aiTextService.availability = AiAvailability.Unavailable(reason = AiUnavailableReasons.DEVICE_UNSUPPORTED)
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_TWO)))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value)
    }

    @Test
    fun `downloading reason resets the dedupe key so a later retry actually runs`() = runTest(testDispatcher) {
        aiTextService.availability = AiAvailability.Unavailable(reason = AiUnavailableReasons.MODEL_DOWNLOADING)
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value)

        aiTextService.availability = AiAvailability.Available
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AlertSummaryUiState.Resolved)
    }

    @Test
    fun `unsupported_device reason does not reset the dedupe key`() = runTest(testDispatcher) {
        aiTextService.availability = AiAvailability.Unavailable(reason = AiUnavailableReasons.DEVICE_UNSUPPORTED)
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value)

        aiTextService.availability = AiAvailability.Available
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        // Same set already marked requested - a permanently-unsupported device must not
        // retry forever just because the sheet reopened with the same alerts.
        assertNull(viewModel.uiState.value)
    }

    @Test
    fun `multi-alert set joins per-alert summaries with bullets`() = runTest(testDispatcher) {
        aiTextService.summarizeResult = "A summarized line"
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(setOf(ALERT_ONE, ALERT_TWO).toPersistentSet()))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AlertSummaryUiState.Resolved)
        assertEquals("• A summarized line\n• A summarized line", state.text)
    }

    @Test
    fun `null summary result never resolves`() = runTest(testDispatcher) {
        aiTextService.summarizeResult = null
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value)
    }

    @Test
    fun `first vote is recorded and tracked`() = runTest(testDispatcher) {
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        viewModel.onEvent(AlertSummaryEvent.VoteClicked(AlertFeedbackVoteChoice.UP))

        val state = viewModel.uiState.value
        assertTrue(state is AlertSummaryUiState.Resolved)
        assertEquals(AlertFeedbackVoteChoice.UP, state.vote)
    }

    @Test
    fun `second vote is ignored - one vote per summary`() = runTest(testDispatcher) {
        viewModel.onEvent(AlertSummaryEvent.SummaryRequested(persistentSetOf(ALERT_ONE)))
        advanceUntilIdle()

        viewModel.onEvent(AlertSummaryEvent.VoteClicked(AlertFeedbackVoteChoice.UP))
        viewModel.onEvent(AlertSummaryEvent.VoteClicked(AlertFeedbackVoteChoice.DOWN))

        val state = viewModel.uiState.value
        assertTrue(state is AlertSummaryUiState.Resolved)
        assertEquals(AlertFeedbackVoteChoice.UP, state.vote)
    }

    @Test
    fun `vote before any summary resolved does nothing`() = runTest(testDispatcher) {
        viewModel.onEvent(AlertSummaryEvent.VoteClicked(AlertFeedbackVoteChoice.UP))

        assertNull(viewModel.uiState.value)
    }
}
