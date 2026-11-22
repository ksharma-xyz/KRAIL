package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The theme screen through the real view model, with the fakes at the edges: a tap or the left button selects
 * a theme and stores it, nothing is reported until the rider confirms, the confirm moves on, and the choice
 * survives going back and a new view model.
 */
class IntroFlowViewModelThemeTest {

    private val train = KrailThemeStyle.Train.id
    private val metro = KrailThemeStyle.Metro.id
    private val bus = KrailThemeStyle.Bus.id
    private val ferry = KrailThemeStyle.Ferry.id

    private val onTheme = IntroState(step = IntroStep.THEME)

    private fun IntroRig.stored(): Long? = sandook.getProductClass()

    private fun IntroRig.reported(): List<Any?> =
        analytics.getTrackedEvents("theme_selected").map { it.properties?.get("themeId") }

    @Test
    fun `a tap selects the theme and stores it, and stays on the screen with nothing reported`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onTheme))

        rig.send(IntroUiEvent.PickTheme(metro))

        assertEquals(metro, rig.state.selectedThemeId)
        assertEquals(IntroStep.THEME, rig.state.step)
        assertEquals(metro.toLong(), rig.stored())
        assertEquals(emptyList<Any?>(), rig.reported())
    }

    @Test
    fun `a tap on another badge replaces the selection and the stored theme`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onTheme))

        rig.send(IntroUiEvent.PickTheme(metro), IntroUiEvent.PickTheme(ferry))

        assertEquals(ferry, rig.state.selectedThemeId)
        assertEquals(ferry.toLong(), rig.stored())
    }

    @Test
    fun `a tap on the theme already selected changes nothing and writes nothing`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onTheme))
        rig.send(IntroUiEvent.PickTheme(metro))
        // Something else stores a different theme, so a second write of Metro would show.
        rig.sandook.insertOrReplaceTheme(bus.toLong())

        rig.send(IntroUiEvent.PickTheme(metro))

        assertEquals(metro, rig.state.selectedThemeId)
        assertEquals(bus.toLong(), rig.stored())
    }

    @Test
    fun `a theme the Intro does not offer is ignored, as is a tap on any other screen`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onTheme))

        rig.send(
            IntroUiEvent.PickTheme(KrailThemeStyle.PurpleDrip.id),
            IntroUiEvent.PickTheme(KrailThemeStyle.BarbiePink.id),
            IntroUiEvent.PickTheme(NOT_A_THEME),
        )

        assertNull(rig.state.selectedThemeId)
        assertNull(rig.stored())

        val elsewhere = IntroRig(this, handle = savedAt(IntroState(step = IntroStep.PARK_RIDE)))
        elsewhere.send(IntroUiEvent.PickTheme(metro), IntroUiEvent.ConfirmTheme)

        assertNull(elsewhere.state.selectedThemeId)
        assertNull(elsewhere.stored())
    }

    @Test
    fun `tapping each badge in turn selects it and stores it, and moves nowhere`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onTheme))
        val seen = mutableListOf<Int?>()
        val stored = mutableListOf<Long?>()

        listOf(train, metro, bus, ferry, train).forEach { id ->
            rig.send(IntroUiEvent.PickTheme(id))
            seen += rig.state.selectedThemeId
            stored += rig.stored()
        }

        assertEquals<List<Int?>>(listOf(train, metro, bus, ferry, train), seen)
        assertEquals(seen.map { it?.toLong() }, stored)
        assertEquals(IntroStep.THEME, rig.state.step)
        assertEquals(emptyList<Any?>(), rig.reported())
    }

    @Test
    fun `yes with a theme selected reports it once, with only its id, and moves on to Park and Ride`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onTheme))
        rig.send(IntroUiEvent.PickTheme(train), IntroUiEvent.PickTheme(metro), IntroUiEvent.PickTheme(bus))
        assertEquals(emptyList<Any?>(), rig.reported(), "three taps on badges report nothing")

        rig.send(IntroUiEvent.ConfirmTheme)

        assertEquals(listOf<Any?>(bus.toString()), rig.reported())
        assertEquals(setOf("themeId"), rig.analytics.getTrackedEvents("theme_selected").single().properties?.keys)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
        assertEquals(bus, rig.state.selectedThemeId)
    }

    @Test
    fun `yes with no theme selected does nothing`() = krailRunTest {
        val rig = IntroRig(this, handle = savedAt(onTheme))

        rig.send(IntroUiEvent.ConfirmTheme)

        assertEquals(IntroStep.THEME, rig.state.step)
        assertEquals(emptyList<Any?>(), rig.reported())
    }

    @Test
    fun `a confirm does nothing on any other screen`() = krailRunTest {
        val rig = IntroRig(
            this,
            handle = savedAt(IntroState(step = IntroStep.DESTINATION, selectedThemeId = metro)),
        )

        rig.send(IntroUiEvent.ConfirmTheme)

        assertEquals(IntroStep.DESTINATION, rig.state.step)
        assertEquals(emptyList<Any?>(), rig.reported())
    }

    @Test
    fun `the selected theme survives a new view model made from the saved state`() = krailRunTest {
        val first = IntroRig(this, handle = savedAt(onTheme))
        first.send(IntroUiEvent.PickTheme(ferry))

        val second = IntroRig(this, handle = first.handle)

        assertEquals(ferry, second.state.selectedThemeId)
        assertEquals(IntroStep.THEME, second.state.step)
    }

    private companion object {
        const val NOT_A_THEME = 12_345
    }
}
