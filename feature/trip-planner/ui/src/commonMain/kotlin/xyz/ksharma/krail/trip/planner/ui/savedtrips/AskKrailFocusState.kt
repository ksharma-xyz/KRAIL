package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.ksharma.krail.trip.planner.ui.components.ai.isBackdropBlurSupported
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState

// How far the screen softens behind the Ask KRAIL dialog, and how long the focus pull takes.
private val ASK_KRAIL_FOCUS_BLUR_RADIUS = 12.dp
private const val ASK_KRAIL_FOCUS_BLUR_MILLIS = 300

// The dim that stands in for the blur where it cannot draw (Android below 12).
private const val ASK_KRAIL_FALLBACK_DIM_ALPHA = 0.35f

// How long the row's wheel spins after the Ask KRAIL dialog closes onto it. The spin's own
// deceleration (rememberAiSpinAngle) starts when this flips back, so the felt animation runs a
// little longer than this number.
private const val AI_HANDOFF_SPIN_MILLIS = 1_200L

/**
 * Everything the home screen does in response to the Ask KRAIL dialog, as three values: how
 * far the content behind it is blurred, how strong the stand-in dim is where the blur cannot
 * draw, and whether the row's wheel is in its one-shot handoff spin.
 */
@Immutable
internal data class AskKrailFocusState(
    val blurRadius: Dp,
    val fallbackDimAlpha: Float,
    val handoffSpin: Boolean,
)

/**
 * Owns the focus pull and the handoff spin so [SavedTripsScreen] only reads three values.
 *
 * The blur softens the whole screen while the dialog is up and releases as it leaves; where
 * `Modifier.blur` cannot draw (no RenderEffect below Android 12) a dim overlay takes over so
 * the dialog still owns the screen's attention. The handoff spin fires on the open-to-closed
 * transition with an answer present — keyed on the transition, not the closed state, so a
 * rotation recomposing into that same state cannot replay it.
 */
@Composable
internal fun rememberAskKrailFocus(aiState: AiSearchInputUiState): AskKrailFocusState {
    val blurSupported = remember { isBackdropBlurSupported() }
    val blurRadius by animateDpAsState(
        targetValue = if (aiState.isInputOpen && blurSupported) ASK_KRAIL_FOCUS_BLUR_RADIUS else 0.dp,
        animationSpec = tween(durationMillis = ASK_KRAIL_FOCUS_BLUR_MILLIS),
        label = "askKrailFocusBlur",
    )
    val fallbackDimAlpha by animateFloatAsState(
        targetValue = if (aiState.isInputOpen && !blurSupported) ASK_KRAIL_FALLBACK_DIM_ALPHA else 0f,
        animationSpec = tween(durationMillis = ASK_KRAIL_FOCUS_BLUR_MILLIS),
        label = "askKrailFallbackDim",
    )

    var aiWasOpen by rememberSaveable { mutableStateOf(false) }
    var handoffSpin by remember { mutableStateOf(false) }
    LaunchedEffect(aiState.isInputOpen) {
        if (aiState.isInputOpen) {
            aiWasOpen = true
        } else if (aiWasOpen) {
            aiWasOpen = false
            if (aiState.resolved != null) {
                handoffSpin = true
                delay(AI_HANDOFF_SPIN_MILLIS)
                handoffSpin = false
            }
        }
    }

    return AskKrailFocusState(
        blurRadius = blurRadius,
        fallbackDimAlpha = fallbackDimAlpha,
        handoffSpin = handoffSpin,
    )
}
