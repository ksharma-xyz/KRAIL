package xyz.ksharma.krail.taj.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF010101)
val md_theme_light_onSurface_placeholder = Color(0xFF595959)
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_light_alert = Color(0xFFFFBA27)
val md_theme_light_softLabel = Color(0xFF767676)
val md_theme_light_secondary_label = Color(0xFF2E2E2E)
val md_theme_light_discover_chip_background = Color(0xFFF5F5F5)
val md_theme_light_discover_card_background = Color(0xFFF5F5F5)

// Light theme (Deviations colors)
val md_theme_light_onTime = Color(0xFF31DB39)
val md_theme_light_early = Color(0xFFFFC60F)
val md_theme_light_late = Color(0xFFF12525)

// Future and past journey colors
val md_theme_light_future_journey = Color(0xFF3A3A3A)
val md_theme_light_past_journey = Color(0xFFBBBBBB)

// Theme selection
val md_theme_light_theme_selection_background = Color(0xFFF5F5F5)

/**
 * KRAIL Dark theme color tokens
 */
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_surface = Color(0xFF1C1B1A)
val md_theme_dark_onSurface = Color(0xFFFCF6F1)
val md_theme_dark_onSurface_placeholder = Color(0xFFA9A5A2)
val md_theme_dark_scrim = Color(0xFF000000)
val md_theme_dark_alert = Color(0xFFF4B400)
val md_theme_dark_softLabel = Color(0xFFB0B0B0)
val md_theme_dark_secondary_label = Color(0xFFE2E2E2)
val md_theme_dark_discover_chip_background = Color(0xFF292929)
val md_theme_dark_discover_card_background = Color(0xFF292929)

// Dark theme (Deviations colors)
val md_theme_dark_onTime = Color(0xFF31DB39)
val md_theme_dark_early = Color(0xFFFFC60F)
val md_theme_dark_late = Color(0xFFFF2B2B)

// Future and past journey colors
val md_theme_dark_future_journey = Color(0xFFEEEEEE)
val md_theme_dark_past_journey = Color(0xFF8F8F8F)

// Theme selection
val md_theme_dark_theme_selection_background = Color(0xFF292929)

/**
 * Intermediate colors for smooth theme transitions
 */
// Intermediate colors when transitioning TO dark mode
val md_theme_intermediate_to_dark_surface = Color(0xFF3A3540) // Warm dark purple-gray
val md_theme_intermediate_to_dark_glow = Color(0xFF4A4550) // Lighter glow variant

// Intermediate colors when transitioning TO light mode
val md_theme_intermediate_to_light_surface = Color(0xFFF2F4F8) // Cool light blue-gray
val md_theme_intermediate_to_light_glow = Color(0xFFE8ECF0) // Darker glow variant

// Transport mode theme colors
val bus_theme = Color(0xFF00B5EF)
val train_theme = Color(0xFFF6891F)
val metro_theme = Color(0xFF009B77)
val ferry_theme = Color(0xFF5AB031)
val coach_theme = Color(0xFF742282)
val purple_drip_theme = Color(0xFFAC00C9)
val light_rail_theme = Color(0xFFE4022D)
val barbie_pink_theme = Color(0xFFE0218A)

val seed = Color(0xFFFFBA27)

val magic_yellow = Color(0xFFFFC800)

val md_theme_badge = Color(0xFFFF0000)

@Immutable
data class KrailColors(
    val label: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val onErrorContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val labelPlaceholder: Color,
    val scrim: Color,
    val alert: Color,
    val softLabel: Color,
    val secondaryLabel: Color,
    val badge: Color,
    val discoverChipBackground: Color,
    val discoverCardBackground: Color,
    val magicYellow: Color,
    // Deviation colors
    val deviationOnTime: Color,
    val deviationEarly: Color,
    val deviationLate: Color,
    // JourneyCard colors
    val pastJourney: Color,
    val futureJourney: Color,
    // theme settings
    val themeSelectionBackground: Color,
)

internal val KrailLightColors = KrailColors(
    label = md_theme_light_onSurface,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    scrim = md_theme_light_scrim,
    alert = md_theme_light_alert,
    softLabel = md_theme_light_softLabel,
    secondaryLabel = md_theme_light_secondary_label,
    badge = md_theme_badge,
    discoverChipBackground = md_theme_light_discover_chip_background,
    discoverCardBackground = md_theme_light_discover_card_background,
    magicYellow = magic_yellow,
    deviationOnTime = md_theme_light_onTime,
    deviationEarly = md_theme_light_early,
    deviationLate = md_theme_light_late,
    pastJourney = md_theme_light_past_journey,
    futureJourney = md_theme_light_future_journey,
    labelPlaceholder = md_theme_light_onSurface_placeholder,
    themeSelectionBackground = md_theme_light_theme_selection_background,
)

internal val KrailDarkColors = KrailColors(
    label = md_theme_dark_onSurface,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    scrim = md_theme_dark_scrim,
    alert = md_theme_dark_alert,
    softLabel = md_theme_dark_softLabel,
    secondaryLabel = md_theme_dark_secondary_label,
    badge = md_theme_badge,
    discoverChipBackground = md_theme_dark_discover_chip_background,
    discoverCardBackground = md_theme_dark_discover_card_background,
    magicYellow = magic_yellow,
    deviationOnTime = md_theme_dark_onTime,
    deviationEarly = md_theme_dark_early,
    deviationLate = md_theme_dark_late,
    pastJourney = md_theme_dark_past_journey,
    futureJourney = md_theme_dark_future_journey,
    labelPlaceholder = md_theme_dark_onSurface_placeholder,
    themeSelectionBackground = md_theme_dark_theme_selection_background,
)

internal val LocalKrailColors = staticCompositionLocalOf {
    KrailColors(
        label = Color.Unspecified,
        error = Color.Unspecified,
        errorContainer = Color.Unspecified,
        onError = Color.Unspecified,
        onErrorContainer = Color.Unspecified,
        surface = Color.Unspecified,
        onSurface = Color.Unspecified,
        scrim = Color.Unspecified,
        alert = Color.Unspecified,
        softLabel = Color.Unspecified,
        secondaryLabel = Color.Unspecified,
        badge = Color.Unspecified,
        discoverChipBackground = Color.Unspecified,
        discoverCardBackground = Color.Unspecified,
        magicYellow = Color.Unspecified,
        deviationOnTime = Color.Unspecified,
        deviationEarly = Color.Unspecified,
        deviationLate = Color.Unspecified,
        pastJourney = Color.Unspecified,
        futureJourney = Color.Unspecified,
        labelPlaceholder = Color.Unspecified,
        themeSelectionBackground = Color.Unspecified,
    )
}
