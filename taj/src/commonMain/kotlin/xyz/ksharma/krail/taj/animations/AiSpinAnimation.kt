package xyz.ksharma.krail.taj.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

internal const val AI_SPIN_DURATION_MS = AiSpinDefaults.SPIN_DURATION_MILLIS
internal const val AI_SETTLE_DURATION_MS = 1000
private const val SETTLE_LINEAR_PORTION_MS = 550

// Travel during the linear portion matches the spin's own angular speed (360deg over the
// spin duration), so the handoff from spinning to settling has no visible speed jump — it
// just starts decelerating from wherever it already was. Scaled from the spin duration for
// the same reason: a faster spin has to enter its settle at its own speed, not the default's.
private const val SETTLE_EASE_OUT_DEGREES = 50f
private const val SETTLE_TAIL_DEGREES = 90f
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
 * @param spinDurationMillis One full turn while spinning. The default is the shared rhythm;
 * a surface where the spin is the main event (the Ask KRAIL dialog frame) passes a shorter
 * one. The settle's travel scales with it, so a faster spin also decelerates from its own
 * speed instead of snapping down to the default's.
 */
@Composable
internal fun rememberAiSpinAngle(
    spinning: Boolean,
    spinDurationMillis: Int = AI_SPIN_DURATION_MS,
): Float {
    val angle = remember { Animatable(0f) }
    // The settle is a deceleration FROM a spin, so it only ever runs after one. Without this
    // guard the effect's first composition (spinning = false, nothing to decelerate) ran the
    // settle anyway, and a surface that shows its border at rest — the Ask KRAIL dialog —
    // opened with a 140-degree twirl it had not earned, timed exactly with the keyboard
    // arriving. A border at rest holds still until real work starts it.
    val hasSpun = remember { mutableStateOf(false) }

    LaunchedEffect(spinning, spinDurationMillis) {
        if (spinning) {
            hasSpun.value = true
            angle.animateTo(
                targetValue = angle.value + FULL_TURN_DEGREES,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = spinDurationMillis, easing = LinearEasing),
                ),
            )
        } else if (hasSpun.value) {
            hasSpun.value = false
            val start = angle.value
            val speedRatio = AI_SPIN_DURATION_MS.toFloat() / spinDurationMillis
            val easeOutDegrees = SETTLE_EASE_OUT_DEGREES * speedRatio
            val totalDegrees = easeOutDegrees + SETTLE_TAIL_DEGREES
            angle.animateTo(
                targetValue = start + totalDegrees,
                animationSpec = keyframes {
                    durationMillis = AI_SETTLE_DURATION_MS
                    start at 0 using LinearEasing
                    (start + easeOutDegrees) at SETTLE_LINEAR_PORTION_MS using SettleEasing
                    (start + totalDegrees) at AI_SETTLE_DURATION_MS
                },
            )
        }
    }

    return angle.value
}
