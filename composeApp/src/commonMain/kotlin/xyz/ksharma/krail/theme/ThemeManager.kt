package xyz.ksharma.krail.theme

import xyz.ksharma.krail.taj.theme.ThemeMode

/**
 * Platform-specific theme manager interface
 * Inject [ThemeManager] to get platform-specific implementation
 */
interface ThemeManager {
    fun applyThemeMode(themeMode: ThemeMode)
}
