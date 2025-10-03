package xyz.ksharma.krail.taj.animations

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
import xyz.ksharma.krail.taj.theme.KrailColors
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_dark_glow
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_light_glow
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_light_surface

// ==============================================================================
// THEME TRANSITION CONFIGURATION
// ==============================================================================

/**
 * Stages of the light/dark mode transition animation.
 * Each stage represents a specific phase in the theme switching process.
 */
private enum class ThemeTransitionStage {
    INITIAL,
    GLOW,
    INTERMEDIATE,
    FINAL,
}

/**
 * Timing constants for light/dark mode theme transition animations.
 * All values are carefully tuned for smooth visual transitions.
 */
private object ThemeTransitionTiming {
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
 * Intermediate colors used during light/dark mode transitions.
 * These provide smooth visual bridges between theme states.
 */
private data class ThemeTransitionColors(
    val surface: Color,
    val glowVariant: Color,
)

// ==============================================================================
// PUBLIC API
// ==============================================================================

/**
 * Creates animated colors for smooth light/dark mode theme transitions.
 *
 * This function handles the complex multi-stage animation process when users
 * switch between light and dark themes, providing intermediate color states
 * for a polished transition experience.
 *
 * @param targetColors The final theme colors to transition to
 * @param isDarkMode Whether transitioning to dark mode (true) or light mode (false)
 * @return KrailColors with animated transitions applied
 */
@Composable
internal fun createLightDarkModeAnimatedColors(
    targetColors: KrailColors,
    isDarkMode: Boolean,
): KrailColors {
    val intermediateColors = getLightDarkModeIntermediateColors(isDarkMode)
    val surfaceTarget = createLightDarkModeSurfaceTransition(
        targetSurface = targetColors.surface,
        intermediateColors = intermediateColors,
    )

    return KrailColors(
        // Animated surface with intermediate transition
        surface = animateColorAsState(
            targetValue = surfaceTarget,
            animationSpec = createLightDarkModeSurfaceAnimationSpec(),
            label = "surface",
        ).value,

        // Animated text colors with staggered delays
        onSurface = animateColorAsState(
            targetValue = targetColors.onSurface,
            animationSpec = createLightDarkModeTextAnimationSpec(
                delayMillis = ThemeTransitionTiming.ON_SURFACE_DELAY_MS,
            ),
            label = "onSurface",
        ).value,

        label = animateColorAsState(
            targetValue = targetColors.label,
            animationSpec = createLightDarkModeTextAnimationSpec(
                delayMillis = ThemeTransitionTiming.LABEL_DELAY_MS,
            ),
            label = "label",
        ).value,

        softLabel = animateColorAsState(
            targetValue = targetColors.softLabel,
            animationSpec = createLightDarkModeTextAnimationSpec(
                delayMillis = ThemeTransitionTiming.SOFT_LABEL_DELAY_MS,
            ),
            label = "softLabel",
        ).value,

        secondaryLabel = animateColorAsState(
            targetValue = targetColors.secondaryLabel,
            animationSpec = createLightDarkModeTextAnimationSpec(
                delayMillis = ThemeTransitionTiming.SECONDARY_LABEL_DELAY_MS,
            ),
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
        themeSelectionBackground = targetColors.themeSelectionBackground,
    )
}

// ==============================================================================
// PRIVATE IMPLEMENTATION
// ==============================================================================

/**
 * Gets intermediate colors for smooth light/dark mode transitions.
 * These colors are design tokens that provide a visually appealing bridge
 * between light and dark themes.
 *
 * @param isDarkMode Whether transitioning to dark mode (true) or light mode (false)
 * @return ThemeTransitionColors containing intermediate surface and glow colors
 */
private fun getLightDarkModeIntermediateColors(isDarkMode: Boolean): ThemeTransitionColors {
    return if (isDarkMode) {
        // Going to dark: use design tokens for warm, slightly purple-tinted intermediates
        ThemeTransitionColors(
            surface = md_theme_intermediate_to_dark_surface,
            glowVariant = md_theme_intermediate_to_dark_glow,
        )
    } else {
        // Going to light: use design tokens for cool, slightly blue-tinted intermediates
        ThemeTransitionColors(
            surface = md_theme_intermediate_to_light_surface,
            glowVariant = md_theme_intermediate_to_light_glow,
        )
    }
}

/**
 * Creates multi-stage surface transition for light/dark mode switching.
 * This involves a brief glow effect followed by an intermediate color before settling on the
 * final surface color to ensure a smooth and visually appealing transition.
 *
 * @param targetSurface The final target surface color after the transition
 * @param intermediateColors The intermediate colors used during the transition stages
 *
 * @return The current surface color based on the transition stage
 */
@Composable
private fun createLightDarkModeSurfaceTransition(
    targetSurface: Color,
    intermediateColors: ThemeTransitionColors,
): Color {
    var transitionStage by remember { mutableStateOf(ThemeTransitionStage.INITIAL) }

    LaunchedEffect(targetSurface) {
        // Stage 1: Brief glow effect using glow design token
        transitionStage = ThemeTransitionStage.GLOW
        delay(ThemeTransitionTiming.GLOW_DELAY_MS)

        // Stage 2: Main intermediate color using surface design token
        transitionStage = ThemeTransitionStage.INTERMEDIATE
        delay(ThemeTransitionTiming.INTERMEDIATE_DELAY_MS)

        // Stage 3: Final target color
        transitionStage = ThemeTransitionStage.FINAL
    }

    return when (transitionStage) {
        ThemeTransitionStage.INITIAL -> targetSurface // Default state
        ThemeTransitionStage.GLOW -> intermediateColors.glowVariant // Brief glow using design token
        ThemeTransitionStage.INTERMEDIATE -> intermediateColors.surface // Intermediate using design token
        ThemeTransitionStage.FINAL -> targetSurface // Final target
    }
}

/**
 * Animation spec for light/dark mode surface transitions.
 */
private fun createLightDarkModeSurfaceAnimationSpec() = tween<Color>(
    durationMillis = ThemeTransitionTiming.SURFACE_DURATION_MS,
    easing = FastOutSlowInEasing,
)

/**
 * Animation spec for light/dark mode text color transitions.
 */
private fun createLightDarkModeTextAnimationSpec(delayMillis: Int) = tween<Color>(
    durationMillis = ThemeTransitionTiming.TEXT_DURATION_MS,
    delayMillis = delayMillis,
    easing = FastOutSlowInEasing,
)
