package xyz.ksharma.krail.taj.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import xyz.ksharma.krail.taj.animations.createLightDarkModeAnimatedColors

@Composable
fun KrailTheme(
    themeController: ThemeController,
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
