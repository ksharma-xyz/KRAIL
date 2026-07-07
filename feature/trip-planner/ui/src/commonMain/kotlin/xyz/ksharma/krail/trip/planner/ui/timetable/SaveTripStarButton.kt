package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * The title-bar save star. When [celebrate] flips true (first save via the
 * "Save this trip?" tile), the star spins a full turn, flashes gold and pops
 * in size before settling back to its normal tint and size — connecting the
 * tile's Save action to where the save control permanently lives. Motion
 * mirrors the intro screen's save-trips star (360 degree spin, tween).
 */
@Composable
internal fun SaveTripStarButton(
    isTripSaved: Boolean,
    celebrate: Boolean,
    onCelebrateEnd: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val goldFraction = remember { Animatable(0f) }

    LaunchedEffect(celebrate) {
        if (!celebrate) return@LaunchedEffect
        val spin = launch {
            rotation.animateTo(
                targetValue = SPIN_DEGREES,
                animationSpec = tween(durationMillis = SPIN_DURATION_MS, easing = FastOutSlowInEasing),
            )
            rotation.snapTo(0f)
        }
        val pop = launch {
            scale.animateTo(
                targetValue = POP_SCALE,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            )
        }
        val gold = launch {
            goldFraction.animateTo(1f, animationSpec = tween(durationMillis = GOLD_IN_MS))
            delay(GOLD_HOLD_MS)
            goldFraction.animateTo(0f, animationSpec = tween(durationMillis = GOLD_OUT_MS))
        }
        joinAll(spin, pop, gold)
        onCelebrateEnd()
    }

    val tint = lerp(KrailTheme.colors.onSurface, StarGold, goldFraction.value)

    ActionButton(
        onClick = onClick,
        contentDescription = if (isTripSaved) "Remove Saved Trip" else "Save Trip",
        modifier = modifier,
    ) {
        Image(
            painter = if (isTripSaved) {
                painterResource(Res.drawable.ic_star_filled)
            } else {
                painterResource(Res.drawable.ic_star)
            },
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier
                .size(KrailTheme.dimensions.iconM)
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

private val StarGold = Color(color = 0xFFFFD700)
private const val SPIN_DEGREES = 360f
private const val SPIN_DURATION_MS = 700
private const val POP_SCALE = 1.5f
private const val GOLD_IN_MS = 250
private const val GOLD_HOLD_MS = 250L
private const val GOLD_OUT_MS = 300
