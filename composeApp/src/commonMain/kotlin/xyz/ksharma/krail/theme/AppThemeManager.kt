package xyz.ksharma.krail.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.toThemeMode

/**
 * Composable that creates and manages the app's [ThemeController] with persistence.
 *
 * This handles:
 * - Reading saved theme preference on app startup
 * - Creating [ThemeController] with proper initialization
 * - Saving theme changes to preferences automatically
 * - Applying platform-specific theme changes
 */
@Composable
fun rememberAppThemeController(): ThemeController {
    val themeManager: ThemeManager = koinInject()

    /**
     * Inject [SandookPreferences] to read persisted theme mode on app startup.
     * This is critical for maintaining theme consistency across app sessions:
     *
     * 1. App Startup: Without this, the app would always default to SYSTEM theme mode
     *    regardless of user's previous selection (Light/Dark mode).
     *
     * 2. Activity Recreation: When the app is recreated (device rotation, memory pressure,
     *    coming back from background), the [ThemeController] needs to restore the user's
     *    selected theme mode from persistent storage.
     *
     * 3. Theme Selection Flow: When user selects a theme in ThemeSelectionScreen,
     *    the selection is saved to preferences. On next app launch or recreation,
     *    this saved preference must be read to maintain the selected theme.
     *
     * Without preferences injection here, users would experience theme resets to SYSTEM
     * mode every time the app starts, losing their personalized theme choice.
     */
    val preferences: SandookPreferences = koinInject()

    val initialThemeMode = remember {
        preferences.getLong(SandookPreferences.KEY_THEME_MODE).toThemeMode()
    }

    var currentThemeMode by remember { mutableStateOf(initialThemeMode) }

    return remember(currentThemeMode) {
        ThemeController(
            currentMode = currentThemeMode,
            setThemeMode = { newThemeMode ->
                log("Theme mode changed to $newThemeMode")
                currentThemeMode = newThemeMode
                // Save to preferences immediately when theme changes
                preferences.setLong(SandookPreferences.KEY_THEME_MODE, newThemeMode.code)
                // Apply theme changes to platform-specific system UI
                themeManager.applyThemeMode(newThemeMode)
            },
        )
    }
}
