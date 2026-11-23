package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically

/**
 * The transition between two headlines on one screen, as [spec] says: the new one fades in and rises [risePx] pixels
 * to its place while the old one fades out, in one tween with no spring. Only transforms and alpha are animated, and
 * there is no size animation, because both headlines keep the same height. The plain swap when [spec] is not animated.
 */
internal fun headlineChangeTransition(spec: HeadlineChangeSpec, risePx: Int): ContentTransform {
    if (!spec.animated) {
        return ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None,
            sizeTransform = null,
        )
    }
    return ContentTransform(
        targetContentEnter = fadeIn(tween(durationMillis = spec.enterMs, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(durationMillis = spec.enterMs, easing = FastOutSlowInEasing),
                initialOffsetY = { risePx },
            ),
        initialContentExit = fadeOut(tween(durationMillis = spec.exitMs, easing = FastOutSlowInEasing)),
        sizeTransform = null,
    )
}
