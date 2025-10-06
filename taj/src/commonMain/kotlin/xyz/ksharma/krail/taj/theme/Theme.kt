package xyz.ksharma.krail.taj.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import xyz.ksharma.krail.taj.animations.createLightDarkModeAnimatedColors

@Composable
fun KrailTheme(
    // default value is only set for usage in Previews, in prod code, this is be passed from KrailApp
    themeController: ThemeController = ThemeController(
        currentMode = ThemeMode.SYSTEM,
        setThemeMode = {},
    ),
    content: @Composable () -> Unit,
) {
    val targetColors = if (themeController.isAppDarkMode()) KrailDarkColors else KrailLightColors
    val animatedColors = createLightDarkModeAnimatedColors(
        targetColors = targetColors,
        isDarkMode = themeController.isAppDarkMode(),
    )

    CompositionLocalProvider(
        LocalKrailColors provides animatedColors,
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
