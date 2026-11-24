package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode

/**
 * The app's theme and nothing else, for the device matrix. `PreviewTheme` pads for the system bars itself, which hands the
 * screen insets that are already used up, so a screen that ignores them would pass. The matrix never wraps a screen in it.
 */
@Composable
internal fun BareKrailTheme(
    mode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    KrailTheme(themeController = ThemeController(currentMode = mode, setThemeMode = {})) {
        CompositionLocalProvider(
            LocalThemeColor provides remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) },
        ) {
            content()
        }
    }
}
