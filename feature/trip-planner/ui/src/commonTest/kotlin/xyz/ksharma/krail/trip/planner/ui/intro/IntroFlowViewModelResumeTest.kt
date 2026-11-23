package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.flow.onEach
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.helpers.CallLog
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_HAS_SEEN_INTRO
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A rider who swipes the app away or force stops it comes back to the screen they were on, through the real view
 * model with the fakes at the edges. The saved progress is in a store of its own in the rig, so a test can put one
 * there before the view model opens, and read what it kept.
 */
class IntroFlowViewModelResumeTest {

    private val train = KrailThemeStyle.Train.id

    private val tripPicked = IntroState(
        parkRideAnswer = ParkRideAnswer.NO,
        startStopId = TALLAWONG.stopId,
        startName = TALLAWONG.name,
        endStopId = CENTRAL.stopId,
        endName = CENTRAL.name,
    )

    private val bellaVistaAdded = IntroState(
        parkRideAnswer = ParkRideAnswer.YES,
        locationResult = LocationResult.GRANTED,
        parkRideCard = ParkRideCardPhase.Card(ParkRideCardStation(stationId = "2153478", stationName = "Bella Vista")),
        addedParkRide = true,
        startStopId = "2153478",
        startName = "Bella Vista Station",
        startFromParkRide = true,
    )

    // A fresh process: the system has nothing saved with the screen, only the device has the progress.
    private fun freshProcess(
        scope: KrailTestScope,
        saved: IntroState? = null,
        seenBefore: Boolean = false,
        progressPreferences: SandookPreferences = FakeSandookPreferences(),
    ): IntroRig {
        saved?.let { progressPreferences.setString(INTRO_PROGRESS_KEY, it.toProgress().toText()) }
        return IntroRig(scope, handle = SavedStateHandle(), progressPreferences = progressPreferences).also { rig ->
            if (seenBefore) rig.preferences.setBoolean(KEY_HAS_SEEN_INTRO, true)
        }
    }

    private fun IntroRig.saved(): IntroProgress? =
        introProgressFromText(progressPreferences.getString(INTRO_PROGRESS_KEY))

    private fun IntroRig.screenViews(): List<Any?> =
        analytics.getTrackedEvents("view_screen").map { it.properties?.get("name") }

    @Test
    fun `a first run with nothing saved opens on the first screen and writes nothing`() = krailRunTest {
        val rig = freshProcess(this)

        rig.settle()

        assertEquals(IntroState(), rig.state)
        assertNull(rig.progressPreferences.getString(INTRO_PROGRESS_KEY))
    }

    @Test
    fun `the Intro opens on the screen the rider was on, with their answers, after the app was killed`() = krailRunTest {
        val screens = listOf(
            tripPicked.copy(step = IntroStep.DESTINATION),
            tripPicked.copy(step = IntroStep.ORIGIN, startStopId = null, startName = null),
            tripPicked.copy(step = IntroStep.CONFIRM),
            tripPicked.copy(step = IntroStep.THEME, tripDecision = TripDecision.SAVED, selectedThemeId = train),
        )

        screens.forEach { saved ->
            val rig = freshProcess(this, saved)

            rig.settle()

            assertEquals(saved, rig.state, "saved at ${saved.step}")
        }
    }

    @Test
    fun `the first screen comes back as the first screen, with the question unanswered`() = krailRunTest {
        val rig = freshProcess(this, IntroState(step = IntroStep.PARK_RIDE))

        rig.settle()

        assertEquals(IntroState(step = IntroStep.PARK_RIDE), rig.state)
    }

    @Test
    fun `a station added before the kill keeps the origin screen skipped, and the start is that station`() = krailRunTest {
        val rig = freshProcess(this, bellaVistaAdded.copy(step = IntroStep.DESTINATION))
        rig.settle()
        assertEquals(IntroStep.DESTINATION, rig.state.step)
        assertTrue(rig.state.addedParkRide)

        rig.send(IntroUiEvent.SelectStop(CENTRAL.toIntroStop()), IntroUiEvent.ConfirmStop)

        assertEquals(IntroStep.CONFIRM, rig.state.step, "the origin screen is passed over")
        assertEquals("Bella Vista Station", rig.state.startName)
        assertEquals(CENTRAL.stopId, rig.state.endStopId)
    }

    @Test
    fun `the rider carries on from the screen they were on, to the finish and the trip they said yes to`() = krailRunTest {
        val rig = freshProcess(this, tripPicked.copy(step = IntroStep.CONFIRM))
        rig.settle()

        rig.withEffects {
            rig.answerAndFinish(yes = true)

            awaitFinish()
        }

        assertEquals(1, rig.sandook.selectAllTrips().size)
        assertEquals(1, rig.log.count("pref:$KEY_HAS_SEEN_INTRO=true"))
    }

    @Test
    fun `killed on the theme screen after saying yes, the finish writes the trip`() = krailRunTest {
        val rig = freshProcess(
            this,
            tripPicked.copy(step = IntroStep.THEME, tripDecision = TripDecision.SAVED, selectedThemeId = train),
        )
        rig.settle()

        rig.withEffects {
            rig.send(IntroUiEvent.ConfirmTheme)

            awaitFinish()
        }

        val trip = rig.sandook.selectAllTrips().single()
        assertEquals(listOf(TALLAWONG.stopId, CENTRAL.stopId), listOf(trip.fromStopId, trip.toStopId))
    }

    @Test
    fun `killed on the theme screen after not now, the finish writes no trip`() = krailRunTest {
        val rig = freshProcess(
            this,
            tripPicked.copy(step = IntroStep.THEME, tripDecision = TripDecision.DECLINED, selectedThemeId = train),
        )
        rig.settle()

        rig.withEffects {
            rig.send(IntroUiEvent.ConfirmTheme)

            awaitFinish()
        }

        assertTrue(rig.sandook.selectAllTrips().isEmpty())
    }

    @Test
    fun `a saved theme screen with no answer recorded writes no trip and is not skipped`() = krailRunTest {
        val rig = freshProcess(this, tripPicked.copy(step = IntroStep.THEME, selectedThemeId = train))
        rig.settle()

        rig.withEffects {
            rig.send(IntroUiEvent.ConfirmTheme)

            awaitFinish()
        }

        assertTrue(rig.sandook.selectAllTrips().isEmpty())
        assertEquals("false", rig.outcomeEvent().properties?.get("skipped").toString())
    }

    @Test
    fun `the finish after a kill reports the answer, the permission, the station and the search as before`() = krailRunTest {
        val rig = freshProcess(
            this,
            bellaVistaAdded.copy(
                step = IntroStep.THEME,
                endStopId = CENTRAL.stopId,
                endName = CENTRAL.name,
                usedSearch = true,
                tripDecision = TripDecision.SAVED,
                selectedThemeId = train,
            ),
        )
        rig.settle()

        rig.withEffects {
            rig.send(IntroUiEvent.ConfirmTheme)

            awaitFinish()
        }

        val reported = rig.outcomeEvent().properties.orEmpty()
        assertEquals("yes", reported["parkRideAnswer"])
        assertEquals("granted", reported["locationResult"])
        assertEquals(true, reported["addedParkRide"])
        assertEquals(true, reported["usedSearch"])
        assertEquals(true, reported["savedTrip"])
    }

    @Test
    fun `the first screen view is reported once for the screen resumed on, not once for each screen before it`() = krailRunTest {
        val rig = freshProcess(this, tripPicked.copy(step = IntroStep.CONFIRM))
        rig.settle()

        rig.viewModel.onStepShown(rig.state.step)
        rig.viewModel.onStepShown(rig.state.step)

        assertEquals(listOf<Any?>("IntroConfirm"), rig.screenViews())

        rig.send(IntroUiEvent.AnswerSaveTrip(yes = true))
        rig.viewModel.onStepShown(rig.state.step)

        assertEquals(listOf<Any?>("IntroConfirm", "IntroTheme"), rig.screenViews())
    }

    @Test
    fun `a resume on the theme screen reports the theme screen once`() = krailRunTest {
        val rig = freshProcess(this, tripPicked.copy(step = IntroStep.THEME, tripDecision = TripDecision.SAVED))
        rig.settle()

        repeat(2) { rig.viewModel.onStepShown(rig.state.step) }

        assertEquals(listOf<Any?>("IntroTheme"), rig.screenViews())
    }

    @Test
    fun `progress saved by a rig that has seen the Intro is not used and is cleared, a replay starts clean`() = krailRunTest {
        val rig = freshProcess(this, tripPicked.copy(step = IntroStep.CONFIRM), seenBefore = true)

        rig.settle()

        assertEquals(IntroState(), rig.state)
        assertNull(rig.progressPreferences.getString(INTRO_PROGRESS_KEY), "the old progress is cleared")
    }

    @Test
    fun `a replay writes no progress as the rider goes through it`() = krailRunTest {
        val rig = freshProcess(this, seenBefore = true)
        rig.settle()

        rig.send(IntroUiEvent.AnswerParkRide(yes = false))
        rig.send(IntroUiEvent.SelectStop(CENTRAL.toIntroStop()), IntroUiEvent.ConfirmStop)

        assertEquals(IntroStep.ORIGIN, rig.state.step)
        assertNull(rig.progressPreferences.getString(INTRO_PROGRESS_KEY))
    }

    @Test
    fun `progress that cannot be read, or is from another version, opens the first screen`() = krailRunTest {
        val good = tripPicked.copy(step = IntroStep.CONFIRM).toProgress().toText()
        listOf(
            "{not json",
            "",
            good.replace("\"version\":$INTRO_PROGRESS_VERSION", "\"version\":${INTRO_PROGRESS_VERSION + 1}"),
            good.replace("CONFIRM", "NOT_A_STEP"),
        ).forEach { text ->
            val rig = freshProcess(this)
            rig.progressPreferences.setString(INTRO_PROGRESS_KEY, text)

            rig.settle()

            assertEquals(IntroState(), rig.state, "text $text")
        }
    }

    @Test
    fun `what the system saved with the screen wins over the progress on the device`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(tripPicked.copy(step = IntroStep.ORIGIN)))
        rig.progressPreferences.setString(INTRO_PROGRESS_KEY, tripPicked.copy(step = IntroStep.CONFIRM).toProgress().toText())

        rig.settle()

        assertEquals(IntroStep.ORIGIN, rig.state.step)
    }

    @Test
    fun `each answer and each change of screen saves the progress`() = krailRunTest {
        val rig = freshProcess(this)
        rig.settle()

        rig.send(IntroUiEvent.AnswerParkRide(yes = false))
        assertEquals(IntroStep.DESTINATION, rig.saved()?.step)
        assertEquals(ParkRideAnswer.NO, rig.saved()?.parkRide)

        rig.send(IntroUiEvent.SelectStop(CENTRAL.toIntroStop()), IntroUiEvent.ConfirmStop)
        assertEquals(IntroStep.ORIGIN, rig.saved()?.step)
        assertEquals(ProgressStop(CENTRAL.stopId, CENTRAL.name), rig.saved()?.end)

        rig.send(IntroUiEvent.SelectStop(TALLAWONG.toIntroStop()), IntroUiEvent.ConfirmStop)
        assertEquals(IntroStep.CONFIRM, rig.saved()?.step)
        assertEquals(ProgressStop(TALLAWONG.stopId, TALLAWONG.name), rig.saved()?.start)

        rig.send(IntroUiEvent.AnswerSaveTrip(yes = false))
        assertEquals(IntroStep.THEME, rig.saved()?.step)
        assertEquals(TripDecision.DECLINED, rig.saved()?.decision)

        rig.send(IntroUiEvent.PickTheme(train))
        assertEquals(train, rig.saved()?.themeId)
    }

    @Test
    fun `a station added is saved with its id and name, and the answer and the permission with it`() = krailRunTest {
        val rig = freshProcess(this)
        rig.settle()

        rig.showBellaVista()
        rig.send(IntroUiEvent.AddParkRideAndContinue)

        val saved = assertNotNull(rig.saved())
        assertEquals(IntroStep.DESTINATION, saved.step)
        assertEquals(ParkRideAnswer.YES, saved.parkRide)
        assertEquals(LocationResult.GRANTED, saved.locationResult)
        assertTrue(saved.added)
        assertEquals("Bella Vista", saved.station?.name)
        assertEquals(rig.state.startStopId, saved.start?.id)
    }

    @Test
    fun `the Park and Ride question still open saves nothing, however far the answer got`() = krailRunTest {
        val rig = freshProcess(this)
        rig.settle()

        rig.showBellaVista()

        assertIs<ParkRideCardPhase.Card>(rig.state.parkRideCard)
        assertNull(rig.progressPreferences.getString(INTRO_PROGRESS_KEY), "the question is still open")
    }

    @Test
    fun `typing and the lists that load write nothing`() = krailRunTest {
        val prefs = RecordingProgressPreferences()
        val rig = freshProcess(this, progressPreferences = prefs)
        rig.settle()
        rig.send(IntroUiEvent.AnswerParkRide(yes = false))
        val writes = prefs.calls.count("write")

        rig.send(IntroUiEvent.QueryChanged("cen"), IntroUiEvent.SelectStop(CENTRAL.toIntroStop()))
        rig.send(IntroUiEvent.QueryChanged("cent"))

        assertEquals(writes, prefs.calls.count("write"))
        assertFalse("cent" in rig.progressPreferences.getString(INTRO_PROGRESS_KEY).orEmpty())
    }

    @Test
    fun `a screen that changes writes one value, and one that does not change writes none`() = krailRunTest {
        val prefs = RecordingProgressPreferences()
        val rig = freshProcess(this, progressPreferences = prefs)
        rig.settle()

        rig.send(IntroUiEvent.AnswerParkRide(yes = false))
        assertEquals(1, prefs.calls.count("write"))

        rig.send(IntroUiEvent.SelectStop(CENTRAL.toIntroStop()))
        assertEquals(1, prefs.calls.count("write"), "a selection is not part of the progress")

        rig.send(IntroUiEvent.ConfirmStop)
        assertEquals(2, prefs.calls.count("write"))
    }

    @Test
    fun `finishing clears the progress after the finale is handed on, and nothing writes it again`() = krailRunTest {
        val order = CallLog()
        val prefs = RecordingProgressPreferences(calls = order)
        val rig = freshProcess(this, tripPicked.copy(step = IntroStep.CONFIRM), progressPreferences = prefs)
        rig.settle()
        assertNotNull(rig.saved())

        rig.viewModel.effects.onEach { order.record("finale") }.test {
            rig.answerAndFinish(yes = true)
            awaitFinish()
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(rig.saved())
        assertEquals("clear", order.entries.last())
        assertTrue(order.entries.indexOf("finale") < order.entries.lastIndexOf("clear"), "${order.entries}")
        assertEquals(1, order.count("clear"))

        // Something that arrives after the finish, such as a late location result, does not bring it back.
        rig.send(IntroUiEvent.QueryChanged("late"))
        rig.send(IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null))
        assertNull(rig.saved())
        assertIs<IntroState>(rig.state)
    }

    @Test
    fun `a finished Intro is never resumed, even if the clear did not run`() = krailRunTest {
        val rig = freshProcess(this, tripPicked.copy(step = IntroStep.THEME, selectedThemeId = train), seenBefore = true)

        rig.settle()

        assertEquals(IntroState(), rig.state)
    }
}
