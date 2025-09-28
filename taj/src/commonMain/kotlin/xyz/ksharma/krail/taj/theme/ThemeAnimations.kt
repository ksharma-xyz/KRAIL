package xyz.ksharma.krail.taj.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

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
            animationSpec = createTextAnimationSpec(delayMillis = 100),
            label = "onSurface",
        ).value,

        label = animateColorAsState(
            targetValue = targetColors.label,
            animationSpec = createTextAnimationSpec(delayMillis = 100),
            label = "label",
        ).value,

        softLabel = animateColorAsState(
            targetValue = targetColors.softLabel,
            animationSpec = createTextAnimationSpec(delayMillis = 150),
            label = "softLabel",
        ).value,

        secondaryLabel = animateColorAsState(
            targetValue = targetColors.secondaryLabel,
            animationSpec = createTextAnimationSpec(delayMillis = 200),
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
    var animationStage by remember { mutableStateOf(0) }

    LaunchedEffect(targetSurface) {
        // Stage 1: Brief glow effect using glow design token
        animationStage = 1
        kotlinx.coroutines.delay(80) // Quick glow

        // Stage 2: Main intermediate color using surface design token
        animationStage = 2
        kotlinx.coroutines.delay(100) // Pause at intermediate

        // Stage 3: Final target color
        animationStage = 3
    }

    return when (animationStage) {
        1 -> intermediateColors.glowVariant // Brief glow using design token
        2 -> intermediateColors.surface // Intermediate using design token
        3 -> targetSurface // Final target
        else -> targetSurface
    }
}

/**
 * Animation specs for different color types
 */
private fun createSurfaceAnimationSpec() = tween<Color>(
    durationMillis = 1500,
    easing = androidx.compose.animation.core.FastOutSlowInEasing,
)

private fun createTextAnimationSpec(delayMillis: Int) = tween<Color>(
    durationMillis = 350,
    delayMillis = delayMillis,
    easing = androidx.compose.animation.core.FastOutSlowInEasing,
)
