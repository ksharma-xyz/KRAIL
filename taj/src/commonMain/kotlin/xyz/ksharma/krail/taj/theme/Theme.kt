package xyz.ksharma.krail.taj.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun KrailTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    var currentThemeMode by remember { mutableStateOf(themeMode) }
    val themeController = remember(currentThemeMode) {
        ThemeController(
            currentMode = currentThemeMode,
            setThemeMode = { newMode -> currentThemeMode = newMode },
        )
    }

    val krailColors = if (themeController.isAppDarkMode()) KrailDarkColors else KrailLightColors

    CompositionLocalProvider(
        LocalKrailColors provides krailColors,
        LocalKrailTypography provides krailTypography,
        LocalThemeController provides themeController,
        content = content,
    )
}

object KrailTheme {

    val colors: KrailColors
        @Composable
        get() = LocalKrailColors.current

    val typography: KrailTypography
        @Composable
        get() = LocalKrailTypography.current
}
