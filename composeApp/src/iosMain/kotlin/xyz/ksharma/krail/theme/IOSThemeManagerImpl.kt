package xyz.ksharma.krail.theme

import xyz.ksharma.krail.taj.theme.ThemeMode

/**
 * iOS implementation of ThemeManager
 * iOS handles theme changes automatically, so this is a no-op
 */
class IOSThemeManagerImpl : ThemeManager {
    override fun applyThemeMode(themeMode: ThemeMode) {
        // iOS handles theme changes automatically through the system
        // No action needed here
    }
}
