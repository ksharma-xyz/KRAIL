package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
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
 * [state], and everything the rider does leaves through [onEvent], with the step it was pressed on. Nothing on it depends on
 * whether the stops data is loaded, and no button on it is ever disabled.
 *
 * Nothing takes a press while a screen is moving. From the moment the step changes until the slide, which is the same
 * transition that draws it, has finished, the screens and the decision bar take no touch (`consumeTouches`), so a quick
 * second tap cannot land on the next screen's button or row, and a screen that is still arriving, at half opacity, is not a
 * target. The screen that is leaving takes none at all, and is hidden from a screen reader. Whatever still gets through, such
 * as a press from the accessibility services, carries its step, and the view model drops it when the Intro has moved on.
 *
 * The flow only moves forward. There is no back button, and system back and the back swipe do nothing on every
 * screen after the first, where they are consumed. On the first screen they follow the platform and leave the app.
 * Content is capped to a phone width and centred, and each screen scrolls, so nothing is ever clipped at a large
 * font or a narrow window. The decision bar is laid over the bottom of the screen and the content goes full height
 * beneath it: while the bar is shown each screen keeps its last row able to scroll clear of it (`IntroBarLayer`).
 *
 * A bar that is going away keeps what it was. The colour screen and the two stop screens open with their button hidden, so when the
 * Intro moves to one of them the old bar fades out over the bottom of the screen in its own words, layout and fill, takes no touch
 * and has no semantics, and the new bar is hidden from the first frame (`IntroBarHold`). It is not part of the layout, so the room
 * the screen reserves for its bar is the new step's at once.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun IntroFlowScreen(
    state: IntroState,
    suggestedStops: SuggestedStops,
    onEvent: (event: IntroUiEvent, pressedOn: IntroStep) -> Unit,
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
        val primaryFill = introAnimatedPrimaryFill(state.step)
        val barShown = bar?.shown == true && revealed
        // A bar that is fading out, because the Intro moved on to a screen whose bar is not shown yet, keeps its own clothes.
        val leavingBar = rememberLeavingIntroBar(state.step, bar, barShown, primaryFill)
        // The step on screen is moving to the step in the state until the one transition that draws it has finished.
        val transition = updateTransition(targetState = state.step, label = "intro-step")
        val settled = transition.currentState == transition.targetState && !transition.isRunning
        val sendFromBar = remember(send, state.step) { { event: IntroUiEvent -> send(event, state.step) } }
        IntroBarLayer(
            barShown = barShown,
            modifier = Modifier.widthIn(max = IntroMaxContentWidth).fillMaxHeight(),
            bar = bar?.let {
                {
                    // One bar for each step: a bar never carries its fade from the step before, so a hidden bar starts hidden.
                    key(state.step) {
                        IntroDecisionBar(
                            left = it.left,
                            right = it.right,
                            onEvent = sendFromBar,
                            shown = it.shown && revealed,
                            rightShown = it.rightShown,
                            acceptsPresses = settled,
                            labelPairs = it.labelPairs,
                            primaryFill = primaryFill,
                            modifier = Modifier.padding(
                                horizontal = dim.pageHorizontalPadding,
                                vertical = dim.spacingL,
                            ),
                        )
                    }
                }
            },
        ) {
            transition.AnimatedContent(
                // Clipped, so a screen sliding past the edge of a capped column never draws in the margin.
                modifier = Modifier.fillMaxSize().clipToBounds().consumeTouches(!settled),
                transitionSpec = { introScreenTransition(initialState, targetState, reduceMotion) },
            ) { step ->
                IntroStepContent(
                    step = step,
                    state = state,
                    suggestedStops = suggestedStops,
                    send = send,
                    ground = pageGround,
                )
            }
        }
        leavingBar?.let {
            // Over the bottom of the screen where the bar was, and no part of its layout.
            IntroLeavingBar(
                held = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = IntroMaxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingL),
            )
        }
    }
}

/**
 * The screen of [step]. When [step] is not the step the Intro is on, this is the screen that is sliding out: it takes no touch
 * and a screen reader does not find it, and whatever it sends carries the step it was drawn for.
 */
@Composable
private fun IntroStepContent(
    step: IntroStep,
    state: IntroState,
    suggestedStops: SuggestedStops,
    send: (IntroUiEvent, IntroStep) -> Unit,
    ground: Color,
) {
    val sendOnStep = remember(send, step) { { event: IntroUiEvent -> send(event, step) } }
    val leaving = step != state.step
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (leaving) Modifier.consumeTouches(true).clearAndSetSemantics {} else Modifier),
    ) {
        // Each screen is drawn in its own colour family, so a screen that is sliding out keeps its colours as it goes.
        IntroScreenColours(step = step) {
            when (step) {
                IntroStep.THEME -> IntroThemeStepContent(selectedThemeId = state.selectedThemeId, onEvent = sendOnStep)
                IntroStep.PARK_RIDE -> IntroParkRideStepContent(state = state)
                IntroStep.DESTINATION, IntroStep.ORIGIN -> IntroStopStepContent(
                    state = state,
                    suggestedStops = suggestedStops,
                    onEvent = sendOnStep,
                    ground = ground,
                )
                IntroStep.CONFIRM -> IntroConfirmStepContent(state = state)
            }
        }
    }
}

/**
 * The way the screen sends an event: to [onEvent], with the step it was pressed on, after doing what only the screen can for
 * the keyboard. A selection and a confirmation hide it and let go of the focus, so it never stays up over the next screen. The
 * search field opens the keyboard itself when it is tapped.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun rememberIntroSender(
    onEvent: (event: IntroUiEvent, pressedOn: IntroStep) -> Unit,
): (IntroUiEvent, IntroStep) -> Unit {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val latest by rememberUpdatedState(onEvent)
    return remember(keyboard, focusManager) {
        { event, pressedOn ->
            when (event.keyboardAction()) {
                IntroKeyboardAction.Close -> {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
                IntroKeyboardAction.None -> Unit
            }
            latest(event, pressedOn)
        }
    }
}
