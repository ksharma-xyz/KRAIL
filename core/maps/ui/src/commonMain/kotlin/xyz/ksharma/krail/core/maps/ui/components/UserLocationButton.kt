package xyz.ksharma.krail.core.maps.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.modifier.debouncedKlickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.krailRipple
import kotlin.time.Duration.Companion.seconds

/**
 * Pure UI component displaying a circular location indicator button.
 *
 * Features:
 * - Outer circle (28dp) with theme color (or muted grey when inactive)
 * - Inner circle (20dp) with white border
 * - No business logic - just UI and click handling
 *
 * @param onClick Callback when button is clicked
 * @param isActive Whether location tracking is active (permission granted + has a fix).
 *                 When false the button renders in a muted grey to signal unavailability,
 *                 and the outer circle pulses to grab the user's attention.
 * @param modifier Modifier to be applied to the component
 */
@Composable
fun UserLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
) {
    val targetColor =
        if (isActive) KrailTheme.colors.userLocationDot else KrailTheme.colors.softLabel
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "UserLocationButtonColor",
    )

    // Pulse animation: outer circle grows and shrinks when permission is not granted,
    // drawing the user's attention to the button. graphicsLayer keeps the layout size
    // fixed at 28dp so the pulse is purely visual and doesn't affect surrounding layout.
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseScale",
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .debouncedKlickable(
                debounceMs = 1.seconds,
                indication = krailRipple(color = KrailTheme.colors.userLocationDot),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Outer circle — pulses when inactive to signal permission is needed.
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    val scale = if (!isActive) pulseScale else 1f
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(color),
        )

        // Inner solid circle with white border
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .border(width = 3.dp, color = Color.White, shape = CircleShape),
        )
    }
}

// region Preview

@PreviewComponent
@Composable
private fun UserLocationButtonPreview() {
    PreviewTheme {
        UserLocationButton(onClick = {})
    }
}

@PreviewComponent
@Composable
private fun UserLocationButtonPreview_isActive() {
    PreviewTheme {
        UserLocationButton(onClick = {}, isActive = false)
    }
}

// endregion
