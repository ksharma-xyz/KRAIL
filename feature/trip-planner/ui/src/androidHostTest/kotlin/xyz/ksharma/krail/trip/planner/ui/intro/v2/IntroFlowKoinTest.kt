package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.trip.planner.ui.di.viewModelsModule
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The real Koin registration of [IntroFlowViewModel], with fakes at its edges, created the way the
 * Intro entry creates it: `koinViewModel` inside a composition on an Android host.
 *
 * It holds that Koin gives the view model a real `SavedStateHandle` through its view model
 * parameters, that what the view model writes lands in that handle, and that a key for each visit
 * gives a new view model and the same key gives the same one. This is Android under Robolectric. The
 * iOS path is the same Koin code, but it is not run here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class IntroFlowKoinTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun startKoinWithFakes() {
        startKoin {
            modules(
                viewModelsModule,
                introKoinFakes(),
            )
        }
    }

    @After
    fun stopKoinAgain() {
        stopKoin()
    }

    private fun handleOf(viewModel: IntroFlowViewModel): SavedStateHandle {
        val field = IntroFlowViewModel::class.java.getDeclaredField("savedStateHandle")
        field.isAccessible = true
        return field.get(viewModel) as SavedStateHandle
    }

    @Composable
    private fun Create(key: String?, onCreated: (IntroFlowViewModel) -> Unit) {
        val viewModel = if (key == null) koinViewModel<IntroFlowViewModel>() else koinViewModel(key = key)
        SideEffect { onCreated(viewModel) }
    }

    @Test
    fun `koinViewModel creates the Intro view model with a saved state handle`() {
        var created: IntroFlowViewModel? = null

        composeRule.setContent { Create(key = null) { created = it } }
        composeRule.waitForIdle()

        val viewModel = assertNotNull(created, "Koin did not create the view model")
        assertNotNull(handleOf(viewModel))
    }

    @Test
    fun `what the view model saves lands in the handle Koin gave it`() {
        var created: IntroFlowViewModel? = null
        composeRule.setContent { Create(key = null) { created = it } }
        composeRule.waitForIdle()
        val viewModel = assertNotNull(created)

        viewModel.onEvent(IntroUiEvent.AnswerParkRide(yes = false))
        composeRule.waitForIdle()

        val saved = handleOf(viewModel).get<String>(INTRO_SAVED_STATE_KEY)
        assertNotNull(saved, "Nothing was written to the saved state handle")
        assertEquals(ParkRideAnswer.NO, viewModel.state.value.parkRideAnswer)
        assertTrue(saved.contains("NO"), "The saved state does not carry the answer: $saved")
    }

    @Test
    fun `the same key gives the same view model and a new key gives a new one with its own handle`() {
        val created = mutableMapOf<String, IntroFlowViewModel>()
        composeRule.setContent {
            Create(key = "intro-a") { created["a"] = it }
            Create(key = "intro-a") { created["a-again"] = it }
            Create(key = "intro-b") { created["b"] = it }
        }
        composeRule.waitForIdle()

        assertSame(created.getValue("a"), created.getValue("a-again"))
        assertNotSame(created.getValue("a"), created.getValue("b"))
        assertNotSame(handleOf(created.getValue("a")), handleOf(created.getValue("b")))
    }
}
