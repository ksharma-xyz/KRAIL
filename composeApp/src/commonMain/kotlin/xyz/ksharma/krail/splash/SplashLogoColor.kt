package xyz.ksharma.krail.splash

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor

/**
 * The colour of the logo and its line, or null for the plain text ink (`onSurface`, which `SplashScreen` draws when it is given none).
 *
 * It is the rider's theme colour when [SplashState.logoShowsTheme], read from the stored [SplashState.themeStyle] and not from the
 * app's live theme, which is set a moment after the load and would show the default for a frame. It is the ink on the very first
 * launch, so a rider who has not chosen a colour is not shown the default orange, and until the stored values are read.
 */
fun splashLogoColor(state: SplashState): Color? =
    if (state.logoShowsTheme) state.themeStyle.hexColorCode.hexToComposeColor() else null
