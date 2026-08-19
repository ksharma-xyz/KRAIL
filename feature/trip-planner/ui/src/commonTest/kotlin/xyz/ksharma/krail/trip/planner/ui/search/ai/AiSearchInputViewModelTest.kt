package xyz.ksharma.krail.trip.planner.ui.search.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.TimeIntent
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.speechtotext.SpeechToTextAvailability
import xyz.ksharma.krail.core.speechtotext.SpeechToTextResult
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.ChainedStopTextResolver
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.RiderOriginLocator
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopLabelTextResolver
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopSearchTextResolver
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAiTextService
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsRepository
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeSpeechToTextService
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TEST_LOCATION = Location(latitude = -33.8688, longitude = 151.2093, timestamp = 0L)

@OptIn(ExperimentalCoroutinesApi::class)
class AiSearchInputViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val aiTextService = FakeAiTextService()
    private val speechToTextService = FakeSpeechToTextService()
    private val stopResultsManager = FakeStopResultsManager()
    private val sandook = FakeSandook()

    // The real chain, not a stub: these tests exercise resolution end to end, so a
    // change in capability order shows up here rather than only in production.
    private val stopTextResolver = ChainedStopTextResolver(
        listOf(
            StopLabelTextResolver(sandook),
            StopSearchTextResolver(stopResultsManager),
        ),
    )
    private val nearbyStopsRepository = FakeNearbyStopsRepository()
    private lateinit var viewModel: AiSearchInputViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AiSearchInputViewModel(
            aiTextService = aiTextService,
            speechToTextService = speechToTextService,
            stopTextResolver = stopTextResolver,
            riderOriginLocator = RiderOriginLocator(nearbyStopsRepository = nearbyStopsRepository),
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
            stopTextResolver = stopTextResolver,
            riderOriginLocator = RiderOriginLocator(nearbyStopsRepository = nearbyStopsRepository),
            isAiSearchInputEnabled = { false },
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("leaving home for central"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(AiSearchInputPhase.IDLE, viewModel.uiState.value.phase)
    }

    @Test
    fun `with the flag off there is no way in`() = runTest(testDispatcher) {
        viewModel = AiSearchInputViewModel(
            aiTextService = aiTextService,
            speechToTextService = speechToTextService,
            stopTextResolver = stopTextResolver,
            riderOriginLocator = RiderOriginLocator(nearbyStopsRepository = nearbyStopsRepository),
            isAiSearchInputEnabled = { false },
        )

        // The state the row reads to decide whether to draw the button at all.
        assertFalse(viewModel.uiState.value.isFeatureEnabled)

        // And if something opens it anyway, it does not open. A sheet whose only action is
        // inert is the defect this pair of guards exists for.
        viewModel.onEvent(AiSearchInputEvent.OpenInput)
        assertFalse(viewModel.uiState.value.isInputOpen)
    }

    @Test
    fun `with the flag on the way in is there and opens`() = runTest(testDispatcher) {
        assertTrue(viewModel.uiState.value.isFeatureEnabled)

        viewModel.onEvent(AiSearchInputEvent.OpenInput)

        assertTrue(viewModel.uiState.value.isInputOpen)
    }

    @Test
    fun `starting over keeps the way in`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.OpenInput)
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.StartOver)

        // The draft goes; what the app knows about itself does not, or the row would lose the
        // button the moment a rider started again.
        assertEquals("", viewModel.uiState.value.typedText)
        assertTrue(viewModel.uiState.value.isFeatureEnabled)
    }

    @Test
    fun `editing the sentence clears the problem it produced`() = runTest(testDispatcher) {
        aiTextService.extractionResult = null
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("take me to hogwarts"))
        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()
        assertEquals(AiSearchInputPhase.UNRESOLVED, viewModel.uiState.value.phase)

        // No dismiss control by design: a banner with an X asks the rider to tidy up after a
        // failure that was not theirs. Changing the sentence is already them moving on, and
        // leaving the old message up makes the new attempt look like it has failed too.
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("take me to central"))

        assertEquals(AiSearchInputPhase.IDLE, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.unresolvedReason)
        assertNull(viewModel.uiState.value.unmatchedPlace)
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
        // Runs the submit but not the settle beat: RESOLVED is the phase the handoff fires
        // on, and it steps down to IDLE once the close lands (covered by the settle-beat test).
        runCurrent()

        val resolved = viewModel.uiState.value.resolved
        assertEquals(AiSearchInputPhase.RESOLVED, viewModel.uiState.value.phase)
        assertEquals(StopItem(stopName = "Central Station", stopId = "10101"), resolved?.fromStopItem)
        assertEquals(StopItem(stopName = "Town Hall", stopId = "10102"), resolved?.toStopItem)
        assertEquals(listOf("train"), resolved?.modeHints)
        assertTrue(resolved?.hasAnyStop == true)
    }

    @Test
    fun `a stop label resolves as a destination`() = runTest(testDispatcher) {
        // "let's go to work" - the rider's own word for a place, which no stop is named.
        sandook.upsertStopLabel(
            label = "work",
            emoji = "💼",
            stopId = "10102",
            stopName = "Town Hall",
            sortOrder = 0L,
        )
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "work",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("let's go to work"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(
            StopItem(stopName = "Town Hall", stopId = "10102"),
            viewModel.uiState.value.resolved?.toStopItem,
        )
    }

    @Test
    fun `an unlabelled word does not become a stop that merely contains it`() =
        runTest(testDispatcher) {
            // Same sentence as the label test, on a phone with no "work" label. Rather than
            // filling the destination with "70 Powderworks Rd", it resolves to nothing and
            // leaves the field for the rider.
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Central Station",
                destinationText = "work",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("let's go to work"))

            viewModel.onEvent(AiSearchInputEvent.Submit)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.resolved?.toStopItem)
        }

    @Test
    fun `one stop resolving is still a result - the other field is left for manual entry`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Nonexistent Place",
                destinationText = "Town Hall",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.OpenInput)
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("from nowhere to town hall"))

            viewModel.onEvent(AiSearchInputEvent.Submit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Nonexistent Place", state.resolved?.fromText)
            assertNull(state.resolved?.fromStopItem)
            assertEquals(StopItem(stopName = "Town Hall", stopId = "10102"), state.resolved?.toStopItem)
            // One stop is still a handoff: the field it found is filled on the row, the other
            // stays for a normal tap, and the dialog has closed itself by now (advanceUntilIdle
            // runs the settle beat through) with the phase stepped down alongside.
            assertFalse(state.isInputOpen)
            assertEquals(AiSearchInputPhase.IDLE, state.phase)
        }

    @Test
    fun `neither stop resolving is a failure - box stays open with the text intact`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Nonexistent Place",
                destinationText = "Also Nonexistent",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.OpenInput)
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("from nowhere to nowhere"))

            viewModel.onEvent(AiSearchInputEvent.Submit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AiSearchInputPhase.UNRESOLVED, state.phase)
            assertNull(state.resolved)
            assertTrue(state.isInputOpen)
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
    fun `resolving stays up for the settle beat, then closes itself onto the row`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Central Station",
                destinationText = "Town Hall",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.OpenInput)
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))
            assertTrue(viewModel.uiState.value.isInputOpen)

            viewModel.onEvent(AiSearchInputEvent.Submit)
            // Runs the submit up to the delayed close without letting the delay elapse: the
            // beat where the border settles and the rider sees the send answered.
            runCurrent()

            val duringBeat = viewModel.uiState.value
            assertEquals(AiSearchInputPhase.RESOLVED, duringBeat.phase)
            assertTrue(duringBeat.isInputOpen)
            assertTrue(duringBeat.resolved?.hasWholeTrip == true)

            // The answer is not read here — it lands on the home row (SavedTripsEntry writes
            // it on the RESOLVED emission above). After the beat the dialog closes onto it.
            advanceUntilIdle()
            val afterBeat = viewModel.uiState.value
            assertFalse(afterBeat.isInputOpen)
            // The resolve is kept as a record, but the phase steps down WITH the close. The
            // row's writes fire only while phase == RESOLVED, and the writing effect re-runs
            // whenever the home entry recomposes — coming back from the stop-search screen
            // included. A state still reading RESOLVED here replayed the AI's stops over the
            // rider's own later manual picks (the bug this pins down).
            assertEquals(AiSearchInputPhase.IDLE, afterBeat.phase)
            assertTrue(afterBeat.resolved?.hasWholeTrip == true)
        }

    @Test
    fun `rewording during the settle beat keeps the dialog open`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.OpenInput)
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))
        viewModel.onEvent(AiSearchInputEvent.Submit)
        runCurrent()
        assertTrue(viewModel.uiState.value.isInputOpen)

        // Editing inside the beat is the rider opting out of the handoff. The timed close
        // must not pull the dialog out from under the sentence they are changing.
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall at 9"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isInputOpen)
        assertEquals(AiSearchInputPhase.IDLE, state.phase)
    }

    @Test
    fun `the row-write gate closes with the dialog`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.OpenInput)
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))
        viewModel.onEvent(AiSearchInputEvent.Submit)
        runCurrent()

        // Live during the settle beat: this is the emission the home row writes on.
        assertTrue(viewModel.uiState.value.isHandoffActionable)

        // Closed and consumed after it. The writer re-launches whenever the home entry
        // recomposes (coming back from the stop-search screen included); a gate still open
        // here is the bug where old AI stops replayed over the rider's later manual picks.
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isHandoffActionable)
    }

    @Test
    fun `reopening after a handoff is a fresh prompt`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.OpenInput)
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))
        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isInputOpen)

        // The wheel is tapped again. The last answer belongs to the row now; showing it back
        // inside the dialog would be a stale copy of a row the rider may have edited since.
        viewModel.onEvent(AiSearchInputEvent.OpenInput)

        val state = viewModel.uiState.value
        assertTrue(state.isInputOpen)
        assertEquals("", state.typedText)
        assertNull(state.resolved)
        assertEquals(AiSearchInputPhase.IDLE, state.phase)
    }

    @Test
    fun `closing the box throws the draft away rather than keeping it for next time`() =
        runTest(testDispatcher) {
            viewModel.onEvent(AiSearchInputEvent.OpenInput)
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("half a sentence"))

            viewModel.onEvent(AiSearchInputEvent.CloseInput)

            val state = viewModel.uiState.value
            assertEquals(false, state.isInputOpen)
            assertEquals("", state.typedText)
            assertEquals(AiSearchInputPhase.IDLE, state.phase)
        }

    @Test
    fun `closing the box while listening stops the mic`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.OpenInput)
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        // runCurrent, not advanceUntilIdle: the session has a 20s ceiling on it now, and
        // advancing until idle would run virtual time past that and stop the mic before the
        // test gets to. Same reason in every test below that asserts a live session.
        runCurrent()
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.onEvent(AiSearchInputEvent.CloseInput)

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
    fun `no time mentioned means no time, not a time we chose`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        // This used to fall back to "leave now", which was harmless while nothing displayed it
        // and became wrong the moment the home screen grew a chip: a rider who said nothing
        // about when saw "Leave Today 12:29 AM" and read it as a decision they had made. Null
        // already means now everywhere downstream, so the fallback was never carrying meaning,
        // only manufacturing it.
        assertNull(viewModel.uiState.value.resolved?.dateTimeSelectionItem)
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
            stopTextResolver = stopTextResolver,
            riderOriginLocator = RiderOriginLocator(
                resolveCurrentLocation = { TEST_LOCATION },
                nearbyStopsRepository = nearbyStopsRepository,
            ),
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
    fun `a sentence about nothing does not fill the origin with wherever the rider is`() =
        runTest(testDispatcher) {
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
                stopTextResolver = stopTextResolver,
                riderOriginLocator = RiderOriginLocator(
                    resolveCurrentLocation = { TEST_LOCATION },
                    nearbyStopsRepository = nearbyStopsRepository,
                ),
                isAiSearchInputEnabled = { true },
            )
            // "hey how are you" parses without error and mentions no place at all. The nearby
            // fallback used to run regardless, so the rider got their current stop written
            // into From and a screen that closed as though it had understood them.
            aiTextService.extractionResult = TripIntentExtraction(
                originText = null,
                destinationText = null,
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("hey how are you"))

            viewModel.onEvent(AiSearchInputEvent.Submit)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AiSearchInputPhase.UNRESOLVED, state.phase)
            assertNull(state.resolved)
        }

    @Test
    fun `a sentence with no place in it says so, rather than blaming the stop search`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = null,
                destinationText = null,
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("hey how are you"))

            viewModel.onEvent(AiSearchInputEvent.Submit)
            advanceUntilIdle()

            assertEquals(UnresolvedReason.NO_PLACE_MENTIONED, viewModel.uiState.value.unresolvedReason)
        }

    @Test
    fun `a named place that matches no stop is quoted back`() = runTest(testDispatcher) {
        aiTextService.extractionResult = TripIntentExtraction(
            originText = null,
            destinationText = "Hogwarts",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("take me to Hogwarts"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UnresolvedReason.STOP_NOT_FOUND, state.unresolvedReason)
        assertEquals("Hogwarts", state.unmatchedPlace)
    }

    @Test
    fun `a place the model invented is never quoted back at the rider`() = runTest(testDispatcher) {
        // The model is used to look stops up, never to produce anything the rider reads. Here
        // it returns a place that is nowhere in what they typed: a reworded or hallucinated
        // name, printed inside quote marks, would read as their own words.
        aiTextService.extractionResult = TripIntentExtraction(
            originText = null,
            destinationText = "Kings Cross Station",
            timeIntent = null,
        )
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("somewhere nice please"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UnresolvedReason.STOP_NOT_FOUND, state.unresolvedReason)
        assertNull(state.unmatchedPlace)
    }

    @Test
    fun `a model that gives nothing back is its own kind of failure`() = runTest(testDispatcher) {
        aiTextService.extractionResult = null
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("central to town hall"))

        viewModel.onEvent(AiSearchInputEvent.Submit)
        advanceUntilIdle()

        assertEquals(UnresolvedReason.COULD_NOT_READ, viewModel.uiState.value.unresolvedReason)
    }

    @Test
    fun `nearby-stop resolver failure leaves origin unresolved, not a crash`() = runTest(testDispatcher) {
        viewModel = AiSearchInputViewModel(
            aiTextService = aiTextService,
            speechToTextService = speechToTextService,
            stopTextResolver = stopTextResolver,
            riderOriginLocator = RiderOriginLocator(
                resolveCurrentLocation = { null },
                nearbyStopsRepository = nearbyStopsRepository,
            ),
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
        viewModel.onEvent(AiSearchInputEvent.OpenInput)
        viewModel.onEvent(AiSearchInputEvent.TypedTextChanged("some text"))

        viewModel.onEvent(AiSearchInputEvent.StartOver)

        val state = viewModel.uiState.value
        assertEquals(false, state.isInputOpen)
        assertEquals("", state.typedText)
        assertEquals(AiSearchInputPhase.IDLE, state.phase)
        assertNull(state.resolved)
    }

    @Test
    fun `start listening surfaces partial transcripts while speaking`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        runCurrent()

        speechToTextService.results.emit(SpeechToTextResult.Partial("central to"))
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isListening)
        assertEquals("central to", state.speechTranscript)
    }

    @Test
    fun `a final transcript fills the field and waits for the rider to send`() =
        runTest(testDispatcher) {
            // It used to submit here. The recogniser deciding it has heard a full sentence is
            // not the rider deciding they have finished saying one, and a mis-heard word was
            // already on its way to a search before they could look at it.
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
            assertEquals(AiSearchInputPhase.IDLE, state.phase)
        }

    @Test
    fun `pressing send after speaking runs the same submit typing does`() =
        runTest(testDispatcher) {
            aiTextService.extractionResult = TripIntentExtraction(
                originText = "Central Station",
                destinationText = "Town Hall",
                timeIntent = null,
            )
            viewModel.onEvent(AiSearchInputEvent.StartListening)
            advanceUntilIdle()
            speechToTextService.results.emit(SpeechToTextResult.Final("central to town hall"))
            advanceUntilIdle()

            viewModel.onEvent(AiSearchInputEvent.Submit)
            // Full idle runs the settle beat too, so the phase has already stepped down; the
            // resolve itself is what proves the spoken sentence went through the same submit.
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.resolved?.hasWholeTrip == true)
        }

    @Test
    fun `words appear in the field while the rider is still speaking`() =
        runTest(testDispatcher) {
            // Partials used to land in speechTranscript only, which nothing renders, so a rider
            // watched an empty box while they talked and the whole sentence appeared at once
            // when they stopped.
            // runCurrent, not advanceUntilIdle: advancing to idle runs virtual time past the
            // listening ceiling, and the session would have stopped itself before the
            // assertion.
            viewModel.onEvent(AiSearchInputEvent.StartListening)
            runCurrent()

            speechToTextService.results.emit(SpeechToTextResult.Partial("central to"))
            runCurrent()

            assertEquals("central to", viewModel.uiState.value.typedText)
            assertTrue(viewModel.uiState.value.isListening)
        }

    @Test
    fun `a recogniser error is reported as itself, not as a permission problem`() =
        runTest(testDispatcher) {
            // The rider tapped the mic while listening and again straight after; the platform
            // recogniser was still winding down and returned busy. Permission is granted, so
            // the reason must not be a permission one - the box words these differently.
            viewModel.onEvent(AiSearchInputEvent.StartListening)
            advanceUntilIdle()

            speechToTextService.results.emit(SpeechToTextResult.Error(reason = "recognizer_error_8"))
            advanceUntilIdle()

            assertEquals("recognizer_error_8", viewModel.uiState.value.speechUnavailableReason)
        }

    @Test
    fun `starting again tears the previous session down first`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        runCurrent()
        val stopsAfterFirstStart = speechToTextService.stopListeningCallCount

        viewModel.onEvent(AiSearchInputEvent.StartListening)
        runCurrent()

        // Without this the platform recogniser is still running when the new session starts,
        // which comes back as a busy error.
        assertEquals(stopsAfterFirstStart + 1, speechToTextService.stopListeningCallCount)
        assertNull(viewModel.uiState.value.speechUnavailableReason)
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
            runCurrent()
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

            // The late transcript still has to LAND. It no longer submits, so what proves it
            // was not dropped is that it reached the field.
            assertEquals("central to town hall", viewModel.uiState.value.typedText)
        }

    @Test
    fun `listening stops itself once it hits the ceiling`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        runCurrent()
        assertTrue(viewModel.uiState.value.isListening)

        // A recogniser that never reports an end would otherwise leave a live microphone and a
        // running waveform on screen with no way out but backing off the sheet.
        advanceTimeBy(LISTENING_CEILING_IN_TEST + 1)
        runCurrent()

        assertEquals(false, viewModel.uiState.value.isListening)
        assertEquals(1, speechToTextService.stopListeningCallCount)
    }

    @Test
    fun `a rider still speaking at the ceiling is given longer`() = runTest(testDispatcher) {
        viewModel.onEvent(AiSearchInputEvent.StartListening)
        runCurrent()

        // Words arriving inside the window just before the ceiling: this rider is mid
        // sentence, and cutting them off there is the thing the extension exists to prevent.
        // Three seconds before the ceiling is inside the four-second window, and is also a
        // pause the recogniser itself would still be waiting through.
        advanceTimeBy(LISTENING_CEILING_IN_TEST - 3_000)
        runCurrent()
        speechToTextService.results.emit(SpeechToTextResult.Partial("central to town"))
        advanceTimeBy(3_001)
        runCurrent()

        assertTrue(viewModel.uiState.value.isListening, "should not stop at the ordinary ceiling")

        advanceTimeBy(LISTENING_EXTENSION_IN_TEST)
        runCurrent()

        // Fifteen seconds is a hard stop, extension or not.
        assertEquals(false, viewModel.uiState.value.isListening)
    }

    @Test
    fun `words that stopped before the ceiling do not earn the extension`() =
        runTest(testDispatcher) {
            viewModel.onEvent(AiSearchInputEvent.StartListening)
            runCurrent()

            // Said early, then silence. Every session has words in it somewhere, so the test
            // is whether they were still arriving at the end, not whether there were any.
            speechToTextService.results.emit(SpeechToTextResult.Partial("central"))
            runCurrent()

            advanceTimeBy(LISTENING_CEILING_IN_TEST + 1)
            runCurrent()

            assertEquals(false, viewModel.uiState.value.isListening)
        }
}

private const val LISTENING_CEILING_IN_TEST = 10_000L
private const val LISTENING_EXTENSION_IN_TEST = 5_000L
