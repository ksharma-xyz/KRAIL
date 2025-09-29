package xyz.ksharma.krail.taj.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Theme mode options for the app
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

/**
 * Theme controller that provides current theme mode and allows changing it
 */
data class ThemeController(
    val currentMode: ThemeMode,
    val setThemeMode: (ThemeMode) -> Unit,
) {
    fun toggleDarkMode(systemInDarkTheme: Boolean) {
        val isDarkMode = when (currentMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemInDarkTheme
        }
        setThemeMode(if (isDarkMode) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    @Composable
    fun isAppDarkMode(): Boolean = when (currentMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}

/**
 * CompositionLocal for providing theme controller throughout the app
 * Follows Google's best practice of providing immutable data with callbacks
 */
val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}
