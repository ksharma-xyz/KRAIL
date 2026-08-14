package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.taj.components.CloudGradientBackground
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

private val DialogWidth = 420.dp
private val DialogCornerRadius = 28.dp
private const val ENTER_SCALE = 0.94f
private const val EXIT_SCALE = 0.96f
private const val ENTER_MILLIS = 280
private const val EXIT_MILLIS = 220

/**
 * Where the AI input lives, decided by how much room there is.
 *
 * A phone in portrait gets the whole screen. That is what both platform guides say a modal
 * holding a keyboard should be at this width, and it also ends the keyboard problem rather
 * than managing it: nothing floats, so nothing has to lift out of the keyboard's way. A tablet
 * or a landscape phone gets a centred dialog, where a full screen for one sentence would be
 * absurd.
 *
 * Both wrap the same [AiInputContent]. Neither knows anything about resolving a trip.
 */
@Composable
fun AiInputSurface(
    state: AiSearchInputUiState,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptiveLayoutInfo = rememberAdaptiveLayoutInfo()
    val fontScale = LocalDensity.current.fontScale

    // Past the stacking scale the content is a tall single column, and a dialog that tall on a
    // tablet is a worse answer than the screen it would be floating over.
    val asDialog = (adaptiveLayoutInfo.isTablet || adaptiveLayoutInfo.isPhoneLandscape) &&
        fontScale < ACTIONS_STACK_SCALE

    val textFieldState = rememberTextFieldState()
    LaunchedEffect(state.typedText) {
        if (state.typedText != textFieldState.text.toString()) {
            textFieldState.setTextAndPlaceCursorAtEnd(state.typedText)
        }
    }

    if (asDialog) {
        AiInputDialog(
            state = state,
            textFieldState = textFieldState,
            onEvent = onEvent,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    } else {
        AiInputFullScreen(
            state = state,
            textFieldState = textFieldState,
            onEvent = onEvent,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

/**
 * Full screen, and deliberately NOT a Dialog.
 *
 * A Compose dialog opens its own window, and that window insets itself for the system bars,
 * so the gradient stopped short of the top and bottom edges and the system bar areas showed
 * whatever was behind. Drawing into the host window instead means this surface inherits the
 * app's own edge to edge setup, and `safeDrawingPadding` keeps the content clear of the bars
 * while the background runs to the edges. It also puts the keyboard inset on the same footing
 * as every other screen, rather than a second window's version of it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AiInputFullScreen(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    // At a large font scale the content is a tall column that will exceed the screen, so it
    // scrolls and the actions travel with it. Below that it fits, and the actions are pinned to
    // the bottom edge instead. A column cannot do both: weighting a child and scrolling are
    // mutually exclusive, since a scrolling column has no bounded height to distribute.
    val stacked = LocalDensity.current.fontScale >= ACTIONS_STACK_SCALE

    // Without a dialog window there is no automatic back handling, and this surface covers the
    // screen it was opened from.
    BackHandler(enabled = true) { onDismiss() }

    CloudGradientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            TitleBar(
                title = { Text(text = "KRAIL AI") },
                onNavActionClick = onDismiss,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dim.pageHorizontalPadding)
                    .padding(bottom = dim.spacingXL)
                    .then(if (stacked) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
            ) {
                AiInputContent(
                    state = state,
                    textFieldState = textFieldState,
                    onEvent = onEvent,
                    onDismiss = onDismiss,
                    modifier = if (stacked) Modifier else Modifier.weight(1f),
                    actionsAtBottom = !stacked,
                )
            }
        }
    }
}

/**
 * The dialog keeps one size from open to close. `decorFitsSystemWindows = false` stops the
 * dialog's own window from resizing itself for the keyboard, leaving `imePadding` as the single
 * authority; without it the two mechanisms run at once, which is the jumping this pattern is
 * known for.
 */
@Composable
private fun AiInputDialog(
    state: AiSearchInputUiState,
    textFieldState: TextFieldState,
    onEvent: (AiSearchInputEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Dialog(
        onDismissRequest = onDismiss,
        // usePlatformDefaultWidth is the only inset-adjacent flag in the common
        // DialogProperties: decorFitsSystemWindows is Android-only and usePlatformInsets is
        // not in this version's common surface, so the content applies its own insets and the
        // dialog window is left with its defaults.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Enter and exit animate scale and alpha only. Both are draw properties, so the card is
        // measured once at its final size and nothing reflows mid animation.
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

        Box(modifier = Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(ENTER_MILLIS)) + scaleIn(
                    initialScale = ENTER_SCALE,
                    animationSpec = tween(ENTER_MILLIS, easing = FastOutSlowInEasing),
                ),
                exit = fadeOut(tween(EXIT_MILLIS)) + scaleOut(
                    targetScale = EXIT_SCALE,
                    animationSpec = tween(EXIT_MILLIS),
                ),
            ) {
                Column(
                    modifier = modifier
                        .width(DialogWidth)
                        .clip(RoundedCornerShape(DialogCornerRadius))
                        .background(KrailTheme.colors.surface)
                        .padding(dim.spacingXXL),
                    verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
                ) {
                    // Cancel is right there in the actions, so the dialog does not repeat the
                    // way out in a bar of its own.
                    AiInputContent(
                        state = state,
                        textFieldState = textFieldState,
                        onEvent = onEvent,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}
