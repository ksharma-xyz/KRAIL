package xyz.ksharma.krail.taj.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

// Animation stage enum for exhaustive checking
private enum class AnimationStage {
    INITIAL,
    GLOW,
    INTERMEDIATE,
    FINAL
}

// Animation timing constants
private object AnimationTiming {
    const val GLOW_DELAY_MS = 80L
    const val INTERMEDIATE_DELAY_MS = 100L
    const val SURFACE_DURATION_MS = 1500
    const val TEXT_DURATION_MS = 350
    const val ON_SURFACE_DELAY_MS = 100
    const val LABEL_DELAY_MS = 100
    const val SOFT_LABEL_DELAY_MS = 150
    const val SECONDARY_LABEL_DELAY_MS = 200
}

/**
 * Handles smooth theme color transitions with intermediate color stages
 * when switching between light and dark modes.
 */
@Composable
internal fun createAnimatedColors(
    targetColors: KrailColors,
    isDarkMode: Boolean,
): KrailColors {
    val intermediateColors = getIntermediateColors(isDarkMode)
    val surfaceTarget = createSurfaceTransition(targetColors.surface, intermediateColors)

    return KrailColors(
        // Animated surface with intermediate transition
        surface = animateColorAsState(
            targetValue = surfaceTarget,
            animationSpec = createSurfaceAnimationSpec(),
            label = "surface",
        ).value,

        // Animated text colors
        onSurface = animateColorAsState(
            targetValue = targetColors.onSurface,
            animationSpec = createTextAnimationSpec(delayMillis = AnimationTiming.ON_SURFACE_DELAY_MS),
            label = "onSurface",
        ).value,

        label = animateColorAsState(
            targetValue = targetColors.label,
            animationSpec = createTextAnimationSpec(delayMillis = AnimationTiming.LABEL_DELAY_MS),
            label = "label",
        ).value,

        softLabel = animateColorAsState(
            targetValue = targetColors.softLabel,
            animationSpec = createTextAnimationSpec(delayMillis = AnimationTiming.SOFT_LABEL_DELAY_MS),
            label = "softLabel",
        ).value,

        secondaryLabel = animateColorAsState(
            targetValue = targetColors.secondaryLabel,
            animationSpec = createTextAnimationSpec(delayMillis = AnimationTiming.SECONDARY_LABEL_DELAY_MS),
            label = "secondaryLabel",
        ).value,

        // Static colors - no animation for better performance
        error = targetColors.error,
        errorContainer = targetColors.errorContainer,
        onError = targetColors.onError,
        onErrorContainer = targetColors.onErrorContainer,
        labelPlaceholder = targetColors.labelPlaceholder,
        scrim = targetColors.scrim,
        alert = targetColors.alert,
        badge = targetColors.badge,
        discoverChipBackground = targetColors.discoverChipBackground,
        discoverCardBackground = targetColors.discoverCardBackground,
        magicYellow = targetColors.magicYellow,
        deviationOnTime = targetColors.deviationOnTime,
        deviationEarly = targetColors.deviationEarly,
        deviationLate = targetColors.deviationLate,
        pastJourney = targetColors.pastJourney,
        futureJourney = targetColors.futureJourney,
    )
}

/**
 * Creates intermediate colors for smooth transitions
 */
private data class IntermediateColors(
    val surface: Color,
    val glowVariant: Color,
)

private fun getIntermediateColors(isDarkMode: Boolean): IntermediateColors {
    return if (isDarkMode) {
        // Going to dark: use design tokens for warm, slightly purple-tinted intermediates
        IntermediateColors(
            surface = md_theme_intermediate_to_dark_surface,
            glowVariant = md_theme_intermediate_to_dark_glow,
        )
    } else {
        // Going to light: use design tokens for cool, slightly blue-tinted intermediates
        IntermediateColors(
            surface = md_theme_intermediate_to_light_surface,
            glowVariant = md_theme_intermediate_to_light_glow,
        )
    }
}

/**
 * Creates multi-stage surface transition with glow effect using design tokens
 */
@Composable
private fun createSurfaceTransition(
    targetSurface: Color,
    intermediateColors: IntermediateColors,
): Color {
    var animationStage by remember { mutableStateOf(AnimationStage.INITIAL) }

    LaunchedEffect(targetSurface) {
        // Stage 1: Brief glow effect using glow design token
        animationStage = AnimationStage.GLOW
        delay(AnimationTiming.GLOW_DELAY_MS)

        // Stage 2: Main intermediate color using surface design token
        animationStage = AnimationStage.INTERMEDIATE
        delay(AnimationTiming.INTERMEDIATE_DELAY_MS)

        // Stage 3: Final target color
        animationStage = AnimationStage.FINAL
    }

    return when (animationStage) {
        AnimationStage.INITIAL -> targetSurface // Default state
        AnimationStage.GLOW -> intermediateColors.glowVariant // Brief glow using design token
        AnimationStage.INTERMEDIATE -> intermediateColors.surface // Intermediate using design token
        AnimationStage.FINAL -> targetSurface // Final target
    }
}

/**
 * Animation specs for different color types
 */
private fun createSurfaceAnimationSpec() = tween<Color>(
    durationMillis = AnimationTiming.SURFACE_DURATION_MS,
    easing = FastOutSlowInEasing,
)

private fun createTextAnimationSpec(delayMillis: Int) = tween<Color>(
    durationMillis = AnimationTiming.TEXT_DURATION_MS,
    delayMillis = delayMillis,
    easing = FastOutSlowInEasing,
)
