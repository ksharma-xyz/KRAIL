package xyz.ksharma.krail.trip.planner.ui.search.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.TimeIntent
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.speechtotext.SpeechToTextAvailability
import xyz.ksharma.krail.core.speechtotext.SpeechToTextResult
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAiTextService
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsRepository
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeSpeechToTextService
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TEST_LOCATION = Location(latitude = -33.8688, longitude = 151.2093, timestamp = 0L)

@OptIn(ExperimentalCoroutinesApi::class)
class AiSearchInputViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val aiTextService = FakeAiTextService()
    private val speechToTextService = FakeSpeechToTextService()
    private val stopResultsManager = FakeStopResultsManager()
    private val nearbyStopsRepository = FakeNearbyStopsRepository()
    private lateinit var viewModel: AiSearchInputViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AiSearchInputViewModel(
            aiTextService = aiTextService,
            speechToTextService = speechToTextService,
            stopResultsManager = stopResultsManager,
            nearbyStopsRepository = nearbyStopsRepository,
            isAiSearchInputEnabled = { true },
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit while flag is off does nothing`() = runTest(testDispatcher) {
        viewModel = AiSearchInputViewModel(
            aiTextService = aiTextService,
            speechToTextService = speechToTextService,
            stopResultsManager = stopResultsManager,
            nearbyStopsRepository = nearbyStopsRepository,
            isAiSearchInputEnabled = { false },
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("leaving home for central"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(AiSearchInputPhase.IDLE, viewModel.uiState.value.phase)
    }

    @Test
    fun `submit with blank text does nothing`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("   "))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(AiSearchInputPhase.IDLE, viewModel.uiState.value.phase)
    }

    @Test
    fun `failed extraction resolves to UNRESOLVED, not silently nothing`() = runTest(testDispatcher) {
        aiTextService.extractionResult = null
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("gibberish"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(AiSearchInputPhase.UNRESOLVED, viewModel.uiState.value.phase)
    }

    @Test
    fun `resolves origin and destination against the real stop search results`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
            modeHints = listOf("train"),
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("from central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        val resolved = viewModel.uiState.value.resolved
        assertEquals(AiSearchInputPhase.RESOLVED, viewModel.uiState.value.phase)
        assertEquals(StopItem(stopName = "Central Station", stopId = "10101"), resolved?.fromStopItem)
        assertEquals(StopItem(stopName = "Town Hall", stopId = "10102"), resolved?.toStopItem)
        assertEquals(listOf("train"), resolved?.modeHints)
        assertTrue(resolved?.hasAnyStop == true)
    }

    @Test
    fun `one stop resolving is still a result - the other field is left for manual entry`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Nonexistent Place",
                destinationText = "Town Hall",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.OpenBox)
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("from nowhere to town hall"))

            viewModel.onEvent(AiSearchInputEvent.Submit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AiSearchInputPhase.RESOLVED, state.phase)
            assertEquals("Nonexistent Place", state.resolved?.fromText)
            assertNull(state.resolved?.fromStopItem)
            assertEquals(StopItem(stopName = "Town Hall", stopId = "10102"), state.resolved?.toStopItem)
            // The row can show the destination it did find, so the box gets out of the way.
            assertEquals(false, state.isBoxOpen)
        }

    @Test
    fun `neither stop resolving is a failure - box stays open with the text intact`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Nonexistent Place",
                destinationText = "Also Nonexistent",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.OpenBox)
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("from nowhere to nowhere"))

            viewModel.onEvent(AiSearchInputEvent.Submit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AiSearchInputPhase.UNRESOLVED, state.phase)
            assertNull(state.resolved)
            assertTrue(state.isBoxOpen)
            // Rewording means editing what's there, not retyping it.
            assertEquals("from nowhere to nowhere", state.typedText)
        }

    @Test
    fun `resolves a time intent into the confirm state`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = TimeIntent(isArrival = true, timeText = "6:30pm"),
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("get to town hall by 6:30pm"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        val dateTimeSelectionItem = viewModel.uiState.value.resolved?.dateTimeSelectionItem
        assertEquals(18, dateTimeSelectionItem?.hour)
        assertEquals(30, dateTimeSelectionItem?.minute)
    }

    @Test
    fun `resolving closes the box so the row shows its own filled fields`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.OpenBox)
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))
        assertTrue(viewModel.uiState.value.isBoxOpen)

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isBoxOpen)
    }

    @Test
    fun `closing the box throws the draft away rather than keeping it for next time`() =
        runTest(testDispatcher) {
            viewModel.onEvent(AiSearchInputEvent.OpenBox)
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("half a sentence"))

            viewModel.onEvent(AiSearchInputEvent.CloseBox)

            val state = viewModel.uiState.value
            assertEquals(false, state.isBoxOpen)
            assertEquals("", state.typedText)
            assertEquals(AiSearchInputPhase.IDLE, state.phase)
        }

    @Test
    fun `closing the box while listening stops the mic`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.OpenBox)
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.onEvent(AiSearchInputEvent.CloseBox)

        assertEquals(false, viewModel.uiState.value.isListening)
        assertEquals(1, speechToTextService.stopListeningCallCount)
    }

    @Test
    fun `model downloadable lands in DOWNLOADING, not the generic unresolved state`() = runTest(testDispatcher) {
        aiTextService.extractionAvailability = AiAvailability.Unavailable(reason = "downloadable")
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(AiSearchInputPhase.DOWNLOADING, viewModel.uiState.value.phase)
    }

    @Test
    fun `model downloading lands in DOWNLOADING too`() = runTest(testDispatcher) {
        aiTextService.extractionAvailability = AiAvailability.Unavailable(reason = "downloading")
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(AiSearchInputPhase.DOWNLOADING, viewModel.uiState.value.phase)
    }

    @Test
    fun `unsupported device lands in UNRESOLVED, not DOWNLOADING`() = runTest(testDispatcher) {
        aiTextService.extractionAvailability = AiAvailability.Unavailable(reason = "unsupported_device")
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(AiSearchInputPhase.UNRESOLVED, viewModel.uiState.value.phase)
    }

    @Test
    fun `no time mentioned defaults to leave-now, not left unset`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        val resolved = viewModel.uiState.value.resolved
        assertEquals(JourneyTimeOptions.LEAVE, resolved?.dateTimeSelectionItem?.option)
    }

    @Test
    fun `no origin mentioned falls back to the nearby-stop resolver`() = runTest(testDispatcher) {
        nearbyStopsRepository.nearbyStops = listOf(
            NearbyStop(
                stopId = "99999",
                stopName = "Nearby Station",
                latitude = TEST_LOCATION.latitude,
                longitude = TEST_LOCATION.longitude,
                transportModes = emptyList(),
            ),
        )
        viewModel = AiSearchInputViewModel(
            aiTextService = aiTextService,
            speechToTextService = speechToTextService,
            stopResultsManager = stopResultsManager,
            nearbyStopsRepository = nearbyStopsRepository,
            resolveCurrentLocation = { TEST_LOCATION },
            isAiSearchInputEnabled = { true },
        )
        aiTextService.extractionResult = TripIntentExtraction(
            originText = null,
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("need to get to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        val resolved = viewModel.uiState.value.resolved
        assertEquals(StopItem(stopName = "Nearby Station", stopId = "99999"), resolved?.fromStopItem)
        assertEquals("Nearby Station", resolved?.fromText)
    }

    @Test
    fun `nearby-stop resolver failure leaves origin unresolved, not a crash`() = runTest(testDispatcher) {
        viewModel = AiSearchInputViewModel(
            aiTextService = aiTextService,
            speechToTextService = speechToTextService,
            stopResultsManager = stopResultsManager,
            nearbyStopsRepository = nearbyStopsRepository,
            resolveCurrentLocation = { null },
            isAiSearchInputEnabled = { true },
        )
        aiTextService.extractionResult = TripIntentExtraction(
            originText = null,
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("need to get to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        val resolved = viewModel.uiState.value.resolved
        assertNull(resolved?.fromStopItem)
        assertNull(resolved?.fromText)
    }

    @Test
    fun `startOver resets the whole flow`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.OpenBox)
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("some text"))

        viewModel.onEvent(AiSearchInputEvent.StartOver)

        val state = viewModel.uiState.value
        assertEquals(false, state.isBoxOpen)
        assertEquals("", state.typedText)
        assertEquals(AiSearchInputPhase.IDLE, state.phase)
        assertNull(state.resolved)
    }

    @Test
    fun `start listening surfaces partial transcripts while speaking`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        advanceUntilIdle()

        speechToTextService.results.emit(SpeechToTextResult.Partial("central to"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isListening)
        assertEquals("central to", state.speechTranscript)
    }

    @Test
    fun `final transcript stops listening and submits automatically`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        advanceUntilIdle()

        speechToTextService.results.emit(SpeechToTextResult.Final("central to town hall"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isListening)
        assertEquals("central to town hall", state.typedText)
        assertEquals(AiSearchInputPhase.RESOLVED, state.phase)
    }

    @Test
    fun `speech unavailable surfaces the reason instead of listening`() = runTest(testDispatcher) {
        speechToTextService.availability = SpeechToTextAvailability.Unavailable(reason = "no_permission")

        viewModel.onEvent(AiSearchInputEvent.StartListening)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isListening)
        assertEquals("no_permission", state.speechUnavailableReason)
    }

    @Test
    fun `stopListening flips isListening immediately but keeps collecting for the final transcript`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Central Station",
                destinationText = "Town Hall",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.StartListening)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isListening)

            viewModel.onEvent(AiSearchInputEvent.StopListening)
            advanceUntilIdle()

            // stopListening only requests a graceful stop - isListening flips right away, but
            // the underlying flow collection must NOT be cancelled, or a Final result the
            // platform delivers a moment later would be silently dropped (the regression this
            // test guards against).
            assertEquals(false, viewModel.uiState.value.isListening)
            assertEquals(1, speechToTextService.stopListeningCallCount)

            speechToTextService.results.emit(SpeechToTextResult.Final("central to town hall"))
            advanceUntilIdle()

            assertEquals(AiSearchInputPhase.RESOLVED, viewModel.uiState.value.phase)
        }
}
