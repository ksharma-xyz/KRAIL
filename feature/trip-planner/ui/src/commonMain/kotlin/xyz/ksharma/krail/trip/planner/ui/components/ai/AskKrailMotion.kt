package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment

/*
 * One set of timings for every state change on the Ask KRAIL surface. Each piece used to pick its
 * own, or none: the banner folded in over 350ms while the button under it popped in a single
 * frame, and the sentence turned into the field with no transition at all. A surface whose parts
 * change on different clocks reads as flicker even when each part is individually animated.
 */

// A block arriving or leaving: the problem banner, its action, the mic row.
internal const val FOLD_ENTER_MILLIS = 350
internal const val FOLD_EXIT_MILLIS = 250

// One thing replaced by another in the same place. The outgoing content leaves quickly and the
// incoming one waits for it, so the two are never both legible at once.
private const val SWAP_OUT_MILLIS = 120
private const val SWAP_IN_MILLIS = 240
private const val SWAP_IN_DELAY_MILLIS = 90

private const val SEND_ENTER_SCALE = 0.5f
private const val SEND_EXIT_SCALE = 0.35f
private const val SEND_FADE_MILLIS = 140

// Longer than the enter fade. The exit spring runs at MediumStiffness and a 140ms fade let the
// button vanish while the spring was still pulling it in.
private const val SEND_EXIT_FADE_MILLIS = 200

internal fun foldIn(): EnterTransition =
    expandVertically(animationSpec = tween(FOLD_ENTER_MILLIS, easing = FastOutSlowInEasing)) +
        fadeIn(tween(FOLD_ENTER_MILLIS))

internal fun foldOut(): ExitTransition =
    shrinkVertically(animationSpec = tween(FOLD_EXIT_MILLIS, easing = FastOutSlowInEasing)) +
        fadeOut(tween(FOLD_EXIT_MILLIS))

/**
 * A crossfade for content that swaps in place. Size snaps rather than animating, because every
 * swap on this surface sits inside the dialog core's own animateContentSize: two size animations
 * nested inside each other chase one another and the core wobbles.
 */
internal fun swapInPlace(): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(tween(SWAP_IN_MILLIS, delayMillis = SWAP_IN_DELAY_MILLIS)),
    initialContentExit = fadeOut(tween(SWAP_OUT_MILLIS)),
    sizeTransform = SizeTransform(clip = false) { _, _ -> snap() },
)

/**
 * Springs in from half size and settles, rather than fading. A send button appearing is the app
 * answering the rider, and a fade reads as something that was always there and is only now
 * catching up.
 *
 * [expandFrom] also opens the button's width, so a neighbour sharing its row slides aside rather
 * than jumping when the button arrives. Null leaves the width alone, for a row whose spacer
 * already absorbs it.
 */
internal fun sendButtonEnter(expandFrom: Alignment.Horizontal? = null): EnterTransition {
    val scale = scaleIn(
        initialScale = SEND_ENTER_SCALE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    ) + fadeIn(animationSpec = tween(durationMillis = SEND_FADE_MILLIS))
    return if (expandFrom == null) {
        scale
    } else {
        expandHorizontally(
            expandFrom = expandFrom,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        ) + scale
    }
}

/**
 * Leaves on the same spring it arrived on, rather than a flat tween. An exit that only fades reads
 * as the button having been switched off; the spring makes it pull back as deliberately as it
 * appeared, and shrinking further (0.35 rather than 0.5) means it reads as leaving rather than as
 * dimming.
 */
internal fun sendButtonExit(shrinkTowards: Alignment.Horizontal? = null): ExitTransition {
    val scale = scaleOut(
        targetScale = SEND_EXIT_SCALE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
    ) + fadeOut(animationSpec = tween(durationMillis = SEND_EXIT_FADE_MILLIS))
    return if (shrinkTowards == null) {
        scale
    } else {
        shrinkHorizontally(
            shrinkTowards = shrinkTowards,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
        ) + scale
    }
}
