package xyz.ksharma.krail.taj.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun KrailTheme(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    var currentThemeMode by remember { mutableStateOf(initialThemeMode) }

    val themeController = remember(currentThemeMode) {
        ThemeController(
            currentMode = currentThemeMode,
            setThemeMode = { newMode -> currentThemeMode = newMode },
        )
    }

    val darkTheme = when (currentThemeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

    val krailColors = when {
        darkTheme -> KrailDarkColors
        else -> KrailLightColors
    }

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
