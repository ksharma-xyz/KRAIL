package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/** The content of one step, given everything the screen knows and the way to tell the view model. */
internal typealias IntroStepContent = @Composable (IntroState, SuggestedStops, (IntroUiEvent) -> Unit) -> Unit

/** A step with nothing in it. The shell tests use it, and the app gives step 2 [introFirstTripStep]. */
internal val EmptyIntroStep: IntroStepContent = { _, _, _ -> }

// The same width as a phone, so the Intro reads the same on a tablet and only gains margin.
private val IntroMaxContentWidth = DUAL_PANE_LIST_WIDTH

/**
 * The Intro: the back button, the screen, and the decision bar. It holds no state of its own
 * beyond scroll, so every decision is [state] and [readiness], and everything the rider does leaves
 * through [onEvent].
 *
 * System back, like the back button, is [IntroUiEvent.Back] whenever there is a screen to go back
 * to, and on the first screen it follows the platform. Content is capped to a phone width and
 * centred, and each screen scrolls, so nothing is ever clipped at a large font or a narrow window.
 * The decision bar stays where it is and the screen scrolls above it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun IntroFlowScreen(
    state: IntroState,
    suggestedStops: SuggestedStops,
    readiness: StopsReadiness,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    firstTripStep: IntroStepContent = EmptyIntroStep,
) {
    val dim = KrailTheme.dimensions
    val reduceMotion = isReduceMotionEnabled()

    val canGoBack = state.stepBefore() != null
    BackHandler(enabled = canGoBack) { onEvent(IntroUiEvent.Back) }

    Box(
        modifier = modifier
            .testTag(TripPlannerTestTags.INTRO_SCREEN)
            .fillMaxSize()
            .background(KrailTheme.colors.surface)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = IntroMaxContentWidth).fillMaxHeight()) {
            IntroTopBar(showBack = canGoBack, onBack = { onEvent(IntroUiEvent.Back) })

            AnimatedContent(
                targetState = state.step,
                // Clipped, so a screen sliding past the edge of a capped column never draws in the margin.
                modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                transitionSpec = { introScreenTransition(initialState, targetState, reduceMotion) },
                label = "intro-step",
            ) { step ->
                when (step) {
                    IntroStep.THEME -> IntroThemeStepContent(selectedThemeId = state.selectedThemeId, onEvent = onEvent)
                    IntroStep.PARK_RIDE -> IntroParkRideStepContent(state = state)
                    // Temporary: the first trip screen has its own layout until the destination screen is built.
                    IntroStep.DESTINATION -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        firstTripStep(state, suggestedStops, onEvent)
                    }
                    // Temporary, until the origin and confirm screens are built.
                    IntroStep.ORIGIN, IntroStep.CONFIRM -> IntroPendingStepBody(step)
                }
            }

            introBar(state, readiness)?.let { bar ->
                IntroDecisionBar(
                    left = bar.left,
                    right = bar.right,
                    onEvent = onEvent,
                    rightEnabled = bar.rightEnabled,
                    modifier = Modifier.padding(
                        horizontal = dim.pageHorizontalPadding,
                        vertical = dim.spacingL,
                    ),
                )
            }
        }
    }
}
