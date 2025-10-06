package xyz.ksharma.krail.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.Job
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.theme.ThemeMode

/**
 * Android implementation of ThemeManager that handles system UI updates
 * when theme mode changes to ensure status bar and navigation bar
 * colors are updated correctly.
 */
class AndroidThemeManagerImpl(
    val context: Context,
) : ThemeManager {

    override fun applyThemeMode(themeMode: ThemeMode) {
        log("Applying theme mode: $themeMode")
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val nightMode = themeMode.toNightMode()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(nightMode)
        } else {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    private fun ThemeMode.toNightMode() = when (this) {
        ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
    }
}
