package xyz.ksharma.krail.splash

import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.trip.planner.ui.navigation.KrailRoute

data class SplashState(
    val hasSeenIntro: Boolean = true,
    val themeStyle: KrailThemeStyle = DEFAULT_THEME_STYLE,
    val themeMode: ThemeMode? = null,
    val navigationDestination: KrailRoute? = null,
)
