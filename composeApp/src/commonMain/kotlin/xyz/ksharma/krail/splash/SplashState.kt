package xyz.ksharma.krail.splash

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeMode

data class SplashState(
    val hasSeenIntro: Boolean = true,
    val themeStyle: KrailThemeStyle = DEFAULT_THEME_STYLE,
    val themeMode: ThemeMode? = null,
    val navigationDestination: NavKey? = null,
)
