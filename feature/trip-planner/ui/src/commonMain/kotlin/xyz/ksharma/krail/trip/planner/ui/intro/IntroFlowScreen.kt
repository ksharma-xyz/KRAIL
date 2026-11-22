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
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
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
 * The two step Intro: the bar with the progress dots, the step, and the one bottom button. It holds
 * no state of its own beyond scroll, so every decision is [state] and [readiness], and everything
 * the rider does leaves through [onEvent].
 *
 * System back on step 2 is [IntroUiEvent.Back], and on step 1 it follows the platform. Content is
 * capped to a phone width and centred, and each step scrolls, so nothing is ever clipped at a large
 * font or a narrow window. The bottom button stays where it is and the step scrolls above it.
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

    BackHandler(enabled = state.step == IntroStep.FIRST_TRIP) { onEvent(IntroUiEvent.Back) }

    Box(
        modifier = modifier
            .testTag(TripPlannerTestTags.INTRO_SCREEN)
            .fillMaxSize()
            .background(KrailTheme.colors.surface)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = IntroMaxContentWidth).fillMaxHeight()) {
            IntroTopBar(step = state.step, onBack = { onEvent(IntroUiEvent.Back) })

            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { introStepTransition(reduceMotion) },
                label = "intro-step",
            ) { step ->
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (step) {
                        IntroStep.PARK_RIDE -> IntroParkRideStepContent(state = state, onEvent = onEvent)
                        IntroStep.FIRST_TRIP -> firstTripStep(state, suggestedStops, onEvent)
                    }
                }
            }

            IntroPrimaryButton(
                action = introFinishAction(state, readiness),
                onClick = { onEvent(IntroUiEvent.FinishPressed) },
                modifier = Modifier.padding(
                    horizontal = dim.pageHorizontalPadding,
                    vertical = dim.spacingL,
                ),
            )
        }
    }
}

/**
 * The one place the theme colour is drawn on this screen. A taj button colours its own content, so
 * the label carries no colour of its own.
 */
@Composable
private fun IntroPrimaryButton(
    action: IntroFinishAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = action.isEnabled(),
        modifier = modifier.testTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION),
    ) {
        Text(text = action.label())
    }
}
