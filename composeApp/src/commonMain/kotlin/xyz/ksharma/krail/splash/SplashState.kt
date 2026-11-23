package xyz.ksharma.krail.splash

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeMode

/**
 * What the splash knows. [hasSeenIntro], [hasSavedTheme] and [themeStyle] are the defaults of a rider who has been here before until
 * [loaded] says the stored values have been read, so nothing is drawn from them before then ([logoShowsTheme]).
 *
 * @property hasSeenIntro The Intro was finished. It is written when the rider presses the last button of the Intro, and an upgrade
 * from the earlier Intro has it too.
 * @property hasSavedTheme A theme is stored. The Intro stores the colour the moment the rider taps it on its last screen, which is
 * before it writes [hasSeenIntro]. A rider who has never chosen a colour has none, and still sees the default Train orange.
 * @property loaded The stored values above were read. False until then, and set by the one load that reads them.
 */
data class SplashState(
    val hasSeenIntro: Boolean = true,
    val themeStyle: KrailThemeStyle = DEFAULT_THEME_STYLE,
    val themeMode: ThemeMode? = null,
    val navigationDestination: NavKey? = null,
    val hasSavedTheme: Boolean = false,
    val loaded: Boolean = false,
) {
    /**
     * The very first launch: the Intro has not been finished and no colour has been chosen. Killing the app part way through the
     * Intro leaves it so, and the rider sees the neutral splash again. A rider with a stored colour is not on a first run, whatever
     * the Intro flag says, so a colour that was chosen is never hidden, and a rider who upgrades from the earlier Intro has the flag.
     */
    val isFirstRun: Boolean get() = !hasSeenIntro && !hasSavedTheme

    /**
     * Whether the logo and its line are drawn in the theme colour. Never before [loaded], because the defaults would show the Train
     * orange on a first run, and never on a first run, where the colour was not chosen by anyone.
     */
    val logoShowsTheme: Boolean get() = loaded && !isFirstRun
}
