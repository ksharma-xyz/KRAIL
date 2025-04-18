package xyz.ksharma.krail.splash

import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle

data class SplashState(
    val hasSeenIntro: Boolean = true,
    val themeStyle: KrailThemeStyle = DEFAULT_THEME_STYLE,
)
