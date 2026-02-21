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
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_dark_theme_selection_background
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_light_glow
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_light_surface
import xyz.ksharma.krail.taj.theme.md_theme_intermediate_to_light_theme_selection_background

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
object ThemeTransitionTiming {
    const val GLOW_DELAY_MS = 80L
    const val INTERMEDIATE_DELAY_MS = 100L
    const val SURFACE_DURATION_MS = 1500
    const val TEXT_DURATION_MS = 350
    const val ON_SURFACE_DELAY_MS = 100
    const val LABEL_DELAY_MS = 100
    const val SOFT_LABEL_DELAY_MS = 150
    const val SECONDARY_LABEL_DELAY_MS = 200
}

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
@Suppress("LongMethod")
@Composable
internal fun createLightDarkModeAnimatedColors(
    targetColors: KrailColors,
    isDarkMode: Boolean,
): KrailColors {
    return KrailColors(
        // Animated surface with custom multi-stage transition
        surface = animateColorAsState(
            targetValue = createMultiStageColorTransition(
                targetColor = targetColors.surface,
                transitionColor = TransitionColor.SURFACE,
                isDarkMode = isDarkMode,
            ),
            animationSpec = createLightDarkModeSurfaceAnimationSpec(),
            label = TransitionColor.SURFACE.displayName.lowercase(),
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

        // Animated theme selection background with custom multi-stage transition
        themeSelectionBackground = animateColorAsState(
            targetValue = createMultiStageColorTransition(
                targetColor = targetColors.themeSelectionBackground,
                transitionColor = TransitionColor.THEME_SELECTION_BACKGROUND,
                isDarkMode = isDarkMode,
            ),
            animationSpec = createLightDarkModeSurfaceAnimationSpec(),
            label = TransitionColor.THEME_SELECTION_BACKGROUND.displayName.lowercase(),
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
        bottomSheetBackground = targetColors.bottomSheetBackground,
        bottomSheetDragHandle = targetColors.bottomSheetDragHandle,
        walkingPath = targetColors.walkingPath,
        userLocationDot = targetColors.userLocationDot,
    )
}

// ==============================================================================
// PRIVATE IMPLEMENTATION
// ==============================================================================

/**
 * Represents a color that requires custom intermediate transition handling
 * instead of direct color interpolation to avoid jarring black/white flashes.
 */
private enum class TransitionColor(val displayName: String) {
    SURFACE("surface"),
    THEME_SELECTION_BACKGROUND("themeSelectionBackground"),
    // Add more transition colors here as needed
}

/**
 * Configuration for colors that need custom intermediate transitions.
 * Maps each transition color to its corresponding intermediate color values.
 */
private fun getIntermediateColorForTransition(
    transitionColor: TransitionColor,
    isDarkMode: Boolean,
): Color {
    return when (transitionColor) {
        TransitionColor.SURFACE -> {
            if (isDarkMode) {
                md_theme_intermediate_to_dark_surface
            } else {
                md_theme_intermediate_to_light_surface
            }
        }
        TransitionColor.THEME_SELECTION_BACKGROUND -> {
            if (isDarkMode) {
                md_theme_intermediate_to_dark_theme_selection_background
            } else {
                md_theme_intermediate_to_light_theme_selection_background
            }
        }
        // Add more color mappings here as needed
    }
}

/**
 * Gets the glow variant color for intermediate transitions.
 */
private fun getGlowVariantColor(isDarkMode: Boolean): Color {
    return if (isDarkMode) {
        md_theme_intermediate_to_dark_glow
    } else {
        md_theme_intermediate_to_light_glow
    }
}

/**
 * Generic multi-stage color transition for light/dark mode switching.
 * This provides smooth transitions for colors that would otherwise flash black/white
 * during direct interpolation between very different color values.
 *
 * @param targetColor The final target color after the transition
 * @param transitionColor The type of color being transitioned (determines intermediate color)
 * @param isDarkMode Whether transitioning to dark mode (true) or light mode (false)
 *
 * @return The current color based on the transition stage
 */
@Composable
private fun createMultiStageColorTransition(
    targetColor: Color,
    transitionColor: TransitionColor,
    isDarkMode: Boolean,
): Color {
    var transitionStage by remember { mutableStateOf(ThemeTransitionStage.INITIAL) }

    LaunchedEffect(targetColor) {
        // Stage 1: Brief glow effect
        transitionStage = ThemeTransitionStage.GLOW
        delay(ThemeTransitionTiming.GLOW_DELAY_MS)

        // Stage 2: Intermediate color specific to the transition type
        transitionStage = ThemeTransitionStage.INTERMEDIATE
        delay(ThemeTransitionTiming.INTERMEDIATE_DELAY_MS)

        // Stage 3: Final target color
        transitionStage = ThemeTransitionStage.FINAL
    }

    return when (transitionStage) {
        ThemeTransitionStage.INITIAL -> targetColor
        ThemeTransitionStage.GLOW -> getGlowVariantColor(isDarkMode)
        ThemeTransitionStage.INTERMEDIATE -> getIntermediateColorForTransition(transitionColor, isDarkMode)
        ThemeTransitionStage.FINAL -> targetColor
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
