package xyz.ksharma.krail.taj.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Theme mode options for the app
 */
enum class ThemeMode(val displayName: String, val code: Long) {
    LIGHT(displayName = "Light", code = 1),
    DARK(displayName = "Dark", code = 2),
    SYSTEM(displayName = "System", code = 9),
}

fun Long?.toThemeMode(): ThemeMode =
    ThemeMode.entries.firstOrNull { it.code == this } ?: ThemeMode.SYSTEM

/**
 * Theme controller that provides current theme mode and allows changing it
 */
data class ThemeController(
    val currentMode: ThemeMode,
    val setThemeMode: (ThemeMode) -> Unit,
) {
    @Composable
    fun isAppDarkMode(): Boolean {
        // On iOS, isSystemInDarkTheme() gets stuck returning true after background→foreground
        // transitions. LocalSystemDarkThemeOverride provides a reliable override when set by
        // the platform (e.g. iOS MainViewController via UIApplicationDidBecomeActiveNotification).
        val systemInDarkTheme = LocalSystemDarkThemeOverride.current ?: isSystemInDarkTheme()

        return when (currentMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemInDarkTheme
        }
    }
}

/**
 * Platform-specific override for the system dark mode value. On most platforms this is `null`
 * (falls back to [isSystemInDarkTheme]). iOS provides a reliable value here to work around
 * a Compose Multiplatform bug where [isSystemInDarkTheme] returns a stale result after the
 * app returns from the background.
 */
val LocalSystemDarkThemeOverride = compositionLocalOf<Boolean?> { null }

@Composable
fun isAppInDarkMode(): Boolean {
    val themeController = LocalThemeController.current
    val systemInDarkTheme = LocalSystemDarkThemeOverride.current ?: isSystemInDarkTheme()
    return when (themeController.currentMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemInDarkTheme
    }
}

/**
 * CompositionLocal for providing theme controller throughout the app
 * Follows Google's best practice of providing immutable data with callbacks
 */
val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}
