package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.themeselection.themeIdParam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The colour the rider chose is on `intro_lets_krail` as `themeId`, in the form `theme_selected` sends it, and the
 * version is on every firing of the event, through every way the Intro can finish.
 */
class IntroLetsKrailThemeIdTest {

    private val tripPicked = IntroState(
        step = IntroStep.CONFIRM,
        parkRideAnswer = ParkRideAnswer.NO,
        startStopId = TALLAWONG.stopId,
        startName = TALLAWONG.name,
        endStopId = CENTRAL.stopId,
        endName = CENTRAL.name,
    )

    private fun KrailTestScope.rigAt(
        state: IntroState = tripPicked,
        readiness: StopsReadiness = StopsReadiness.Ready(stopCount = 100),
    ) = IntroRig(this, readiness, savedAt(state))

    @Test
    fun `the chosen colour is on the event for every theme the Intro offers`() = krailRunTest {
        introThemeChoices().forEach { choice ->
            val rig = rigAt()

            rig.answerAndFinish(yes = true, themeId = choice.style.id)

            assertEquals(choice.style.id.toString(), rig.outcomeEvent().properties?.get("themeId"), choice.name)
        }
    }

    @Test
    fun `the colour is in exactly the form theme_selected sends it`() = krailRunTest {
        introThemeChoices().forEach { choice ->
            val rig = rigAt()

            rig.answerAndFinish(yes = true, themeId = choice.style.id)

            val reported = rig.analytics.getTrackedEvent("theme_selected")
            assertEquals(
                reported?.properties?.get("themeId"),
                rig.outcomeEvent().properties?.get("themeId"),
                "${choice.name}: the two events say the colour in the same form",
            )
            assertEquals(
                AnalyticsEvent.ThemeSelectedEvent(themeId = themeIdParam(choice.style.id)).properties?.get("themeId"),
                rig.outcomeEvent().properties?.get("themeId"),
            )
        }
    }

    @Test
    fun `a not now carries the colour too`() = krailRunTest {
        val rig = rigAt()

        rig.answerAndFinish(yes = false, themeId = KrailThemeStyle.Ferry.id)

        assertEquals(true, rig.outcomeEvent().properties?.get("skipped"))
        assertEquals(KrailThemeStyle.Ferry.id.toString(), rig.outcomeEvent().properties?.get("themeId"))
    }

    @Test
    fun `the colour picked last is the one reported`() = krailRunTest {
        val rig = rigAt()

        rig.send(
            IntroUiEvent.AnswerSaveTrip(yes = true),
            IntroUiEvent.PickTheme(KrailThemeStyle.Train.id),
            IntroUiEvent.PickTheme(KrailThemeStyle.Bus.id),
            IntroUiEvent.ConfirmTheme,
        )

        assertEquals(KrailThemeStyle.Bus.id.toString(), rig.outcomeEvent().properties?.get("themeId"))
    }

    @Test
    fun `no colour chosen means no finish and no event, so no firing is without a colour it should have`() =
        krailRunTest {
            val rig = rigAt()

            rig.send(IntroUiEvent.AnswerSaveTrip(yes = true), IntroUiEvent.ConfirmTheme)

            assertNull(rig.analytics.getTrackedEvent("intro_lets_krail"))
        }

    @Test
    fun `the version is on every firing, whichever way the Intro finishes`() = krailRunTest {
        val finishes = listOf(
            "saved" to { rigAt().also { it.answerAndFinish(yes = true) } },
            "not now, skipped" to { rigAt().also { it.answerAndFinish(yes = false) } },
            "question never answered, skipped" to {
                rigAt(tripPicked.copy(parkRideAnswer = ParkRideAnswer.ASK)).also { it.answerAndFinish(yes = false) }
            },
            "no pair picked" to {
                rigAt(IntroState(step = IntroStep.CONFIRM, parkRideAnswer = ParkRideAnswer.NO))
                    .also { it.answerAndFinish(yes = true) }
            },
            "stops failed to load" to {
                rigAt(readiness = StopsReadiness.Failed(attempt = 4, reason = "disk"))
                    .also { it.answerAndFinish(yes = true) }
            },
        )

        finishes.forEach { (name, finish) ->
            val rig = finish()

            assertEquals(2, rig.outcomeEvent().properties?.get("introVersion"), name)
        }
    }

    @Test
    fun `the skipped outcome carries the version`() = krailRunTest {
        val rig = rigAt()

        rig.answerAndFinish(yes = false)

        assertEquals(true, rig.outcomeEvent().properties?.get("skipped"))
        assertEquals(2, rig.outcomeEvent().properties?.get("introVersion"))
    }
}
