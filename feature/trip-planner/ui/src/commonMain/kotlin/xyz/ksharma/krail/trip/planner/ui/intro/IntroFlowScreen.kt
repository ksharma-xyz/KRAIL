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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroFirstStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// The same width as a phone, so the Intro reads the same on a tablet and only gains margin.
private val IntroMaxContentWidth = DUAL_PANE_LIST_WIDTH

/**
 * The Intro: the screen and the decision bar. It holds no state of its own beyond scroll, so every decision is
 * [state] and [readiness], and everything the rider does leaves through [onEvent].
 *
 * The flow only moves forward. There is no back button, and system back and the back swipe do nothing on every
 * screen after the first, where they are consumed. On the first screen they follow the platform and leave the app.
 * Content is capped to a phone width and centred, and each screen scrolls, so nothing is ever clipped at a large
 * font or a narrow window. The decision bar stays where it is and the screen scrolls above it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun IntroFlowScreen(
    state: IntroState,
    suggestedStops: SuggestedStops,
    readiness: StopsReadiness,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val reduceMotion = isReduceMotionEnabled()
    val searchFocus = remember { FocusRequester() }
    val send = rememberIntroSender(searchFocus, onEvent)

    // Consumed on every screen after the first, so the rider cannot go back. On the first it is not handled here.
    BackHandler(enabled = state.step != IntroFirstStep) {}

    Box(
        modifier = modifier
            .testTag(TripPlannerTestTags.INTRO_SCREEN)
            .fillMaxSize()
            .background(KrailTheme.colors.surface)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = IntroMaxContentWidth).fillMaxHeight()) {
            AnimatedContent(
                targetState = state.step,
                // Clipped, so a screen sliding past the edge of a capped column never draws in the margin.
                modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                transitionSpec = { introScreenTransition(initialState, targetState, reduceMotion) },
                label = "intro-step",
            ) { step ->
                when (step) {
                    IntroStep.THEME -> IntroThemeStepContent(selectedThemeId = state.selectedThemeId, onEvent = send)
                    IntroStep.PARK_RIDE -> IntroParkRideStepContent(state = state)
                    IntroStep.DESTINATION, IntroStep.ORIGIN -> IntroStopStepContent(
                        state = state,
                        suggestedStops = suggestedStops,
                        focusRequester = searchFocus,
                        onEvent = send,
                    )
                    IntroStep.CONFIRM -> IntroConfirmStepContent(state = state)
                }
            }

            introBar(state, readiness)?.let { bar ->
                IntroDecisionBar(
                    left = bar.left,
                    right = bar.right,
                    onEvent = send,
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

/**
 * The way the screen sends an event: to [onEvent], after doing what only the screen can for the keyboard. No search
 * focuses [searchFocus] and shows the keyboard, and a choice hides it and lets go of the focus, so it never
 * stays up over the next screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun rememberIntroSender(
    searchFocus: FocusRequester,
    onEvent: (IntroUiEvent) -> Unit,
): (IntroUiEvent) -> Unit {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val latest by rememberUpdatedState(onEvent)
    return remember(searchFocus, keyboard, focusManager) {
        {
                event ->
            when (event.keyboardAction()) {
                IntroKeyboardAction.Open -> {
                    searchFocus.requestFocus()
                    keyboard?.show()
                }
                IntroKeyboardAction.Close -> {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
                IntroKeyboardAction.None -> Unit
            }
            latest(event)
        }
    }
}
