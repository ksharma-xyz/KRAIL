package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeSolidBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroFirstStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// The same width as a phone, so the Intro reads the same on a tablet and only gains margin.
private val IntroMaxContentWidth = DUAL_PANE_LIST_WIDTH

/**
 * The Intro: the screen and the decision bar. It holds no state of its own beyond scroll, so every decision is
 * [state], and everything the rider does leaves through [onEvent]. Nothing on it depends on whether the stops data is
 * loaded, and no button on it is ever disabled.
 *
 * The flow only moves forward. There is no back button, and system back and the back swipe do nothing on every
 * screen after the first, where they are consumed. On the first screen they follow the platform and leave the app.
 * Content is capped to a phone width and centred, and each screen scrolls, so nothing is ever clipped at a large
 * font or a narrow window. The decision bar is laid over the bottom of the screen and the content goes full height
 * beneath it: while the bar is shown each screen keeps its last row able to scroll clear of it (`IntroBarLayer`).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun IntroFlowScreen(
    state: IntroState,
    suggestedStops: SuggestedStops,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val reduceMotion = isReduceMotionEnabled()
    val send = rememberIntroSender(onEvent)

    // Consumed on every screen after the first, so the rider cannot go back. On the first it is not handled here.
    BackHandler(enabled = state.step != IntroFirstStep) {}

    val change = LocalIntroThemeChange.current
    // Once a theme is chosen the window's ground is a light tint of it, which the wash grows into when a badge is tapped.
    val tint = themeSolidBackgroundColor()
    val settledGround = if (state.selectedThemeId != null) tint else null
    // On the colour screen the button shows from the point in the sequence that says so, and at once on every other screen.
    val revealed = state.step != IntroStep.THEME || change?.buttonRevealed ?: true
    // The window's ground, which a sticky bar over scrolling rows is opaque in: the tint once it is drawn, else the surface.
    val pageGround = if (change != null && settledGround != null) settledGround else KrailTheme.colors.surface

    Box(
        modifier = modifier
            .testTag(TripPlannerTestTags.INTRO_SCREEN)
            .fillMaxSize()
            .background(KrailTheme.colors.surface)
            .then(
                if (change == null) {
                    Modifier
                } else {
                    Modifier
                        .onGloballyPositioned { change.attachRoot(it) }
                        .drawBehind { change.drawGround(this, settledGround) }
                },
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val bar = introBar(state)
        IntroBarLayer(
            barShown = bar?.shown == true && revealed,
            modifier = Modifier.widthIn(max = IntroMaxContentWidth).fillMaxHeight(),
            bar = bar?.let {
                {
                    IntroDecisionBar(
                        left = it.left,
                        right = it.right,
                        onEvent = send,
                        shown = it.shown && revealed,
                        rightShown = it.rightShown,
                        modifier = Modifier.padding(
                            horizontal = dim.pageHorizontalPadding,
                            vertical = dim.spacingL,
                        ),
                    )
                }
            },
        ) {
            AnimatedContent(
                targetState = state.step,
                // Clipped, so a screen sliding past the edge of a capped column never draws in the margin.
                modifier = Modifier.fillMaxSize().clipToBounds(),
                transitionSpec = { introScreenTransition(initialState, targetState, reduceMotion) },
                label = "intro-step",
            ) { step ->
                when (step) {
                    IntroStep.THEME -> IntroThemeStepContent(selectedThemeId = state.selectedThemeId, onEvent = send)
                    IntroStep.PARK_RIDE -> IntroParkRideStepContent(state = state)
                    IntroStep.DESTINATION, IntroStep.ORIGIN -> IntroStopStepContent(
                        state = state,
                        suggestedStops = suggestedStops,
                        onEvent = send,
                        ground = pageGround,
                    )
                    IntroStep.CONFIRM -> IntroConfirmStepContent(state = state)
                }
            }
        }
    }
}

/**
 * The way the screen sends an event: to [onEvent], after doing what only the screen can for the keyboard. A selection
 * and a confirmation hide it and let go of the focus, so it never stays up over the next screen. The search field
 * opens the keyboard itself when it is tapped.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun rememberIntroSender(onEvent: (IntroUiEvent) -> Unit): (IntroUiEvent) -> Unit {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val latest by rememberUpdatedState(onEvent)
    return remember(keyboard, focusManager) {
        {
                event ->
            when (event.keyboardAction()) {
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
