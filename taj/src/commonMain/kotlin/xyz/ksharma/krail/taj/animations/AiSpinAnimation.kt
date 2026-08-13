package xyz.ksharma.krail.taj.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

internal const val AI_SPIN_DURATION_MS = 4500
internal const val AI_SETTLE_DURATION_MS = 1000
private const val SETTLE_LINEAR_PORTION_MS = 550

// Travel during the linear portion roughly matches the spin's own angular speed
// (360deg / AI_SPIN_DURATION_MS), so the handoff from spinning to settling has no visible
// speed jump — it just starts decelerating from wherever it already was.
private const val SETTLE_EASE_OUT_DEGREES = 50f
private const val SETTLE_TOTAL_DEGREES = 140f
private const val FULL_TURN_DEGREES = 360f

private val SettleEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * Shared rotation driver for every place [xyz.ksharma.krail.taj.tokens.AiGradientTokens]
 * animates (the wheel mark, the card border). Both call this instead of hand-rolling their
 * own spec, so a wheel and a border started from the same `spinning` flip stay in the same
 * rhythm — same duration, same easing, not two copies that can drift apart.
 *
 * @param spinning While `true`, rotates at a slow constant speed. The instant it flips to
 * `false`, the current rotation decelerates to a full stop rather than cutting: linear for
 * the first ~55% of [AI_SETTLE_DURATION_MS], then an ease-out curve for the rest — reads as
 * physically slowing down, not switching off.
 */
@Composable
internal fun rememberAiSpinAngle(spinning: Boolean): Float {
    val angle = remember { Animatable(0f) }

    LaunchedEffect(spinning) {
        if (spinning) {
            angle.animateTo(
                targetValue = angle.value + FULL_TURN_DEGREES,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = AI_SPIN_DURATION_MS, easing = LinearEasing),
                ),
            )
        } else {
            val start = angle.value
            angle.animateTo(
                targetValue = start + SETTLE_TOTAL_DEGREES,
                animationSpec = keyframes {
                    durationMillis = AI_SETTLE_DURATION_MS
                    start at 0 using LinearEasing
                    (start + SETTLE_EASE_OUT_DEGREES) at SETTLE_LINEAR_PORTION_MS using SettleEasing
                    (start + SETTLE_TOTAL_DEGREES) at AI_SETTLE_DURATION_MS
                },
            )
        }
    }

    return angle.value
}
