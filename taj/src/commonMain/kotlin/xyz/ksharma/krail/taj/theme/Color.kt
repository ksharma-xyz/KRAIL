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
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_light_alert = Color(0xFFFFBA27)
val md_theme_light_softLabel = Color(0xFF767676)

val md_theme_light_secondary_label = Color(0xFF404040)

// Dark Color tokens
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_surface = Color(0xFF1C1B1A)
val md_theme_dark_onSurface = Color(0xFFFCF6F1)
val md_theme_dark_scrim = Color(0xFF000000)
val md_theme_dark_alert = Color(0xFFF4B400)
val md_theme_dark_softLabel = Color(0xFFB0B0B0)
val md_theme_dark_secondary_label = Color(0xFFEBEBEB)

val bus_theme = Color(0xFF00B5EF)
val train_theme = Color(0xFFF6891F)
val metro_theme = Color(0xFF009B77)
val ferry_theme = Color(0xFF5AB031)
val coach_theme = Color(0xFF742282)
val purple_drip_theme = Color(0xFFAC00C9)
val light_rail_theme = Color(0xFFE4022D)
val barbie_pink_theme = Color(0xFFE0218A)

val seed = Color(0xFFFFBA27)

@Immutable
data class KrailColors(
    val label: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val onErrorContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val scrim: Color,
    val alert: Color,
    val softLabel: Color,
    val secondaryLabel: Color,
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
    )
}
