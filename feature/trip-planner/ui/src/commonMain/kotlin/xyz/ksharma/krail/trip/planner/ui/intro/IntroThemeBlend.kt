package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.asThemeInk
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.toHex
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// What the colour of one moment is made from: the theme the screen is on, and the colours of the app it is drawn in, which are read
// when a run starts and not while it runs, so a change of light and dark in the middle of one never restarts it.
private class ThemeBlendInputs(
    val themeId: Int?,
    val hex: String,
    val dark: Boolean,
    val surface: Color,
    val onSurface: Color,
    val reduceMotion: Boolean,
)

private fun ThemeBlendInputs.request(tapped: Int, origin: Offset): ThemeChangeRequest {
    val colour = hex.hexToComposeColor()
    val headline = introThemeHeadline(themeId)
    val chosen = themeId != null
    return ThemeChangeRequest(
        reduceMotion = reduceMotion,
        target = ThemeChangeTarget(themeId, hex, colour),
        toText = if (chosen) colour.asThemeInk(dark) else onSurface,
        toGround = if (chosen) introGroundColour(colour, surface, dark) else surface,
        headline = HeadlineContent(headline.lines, headline.emphasisIndex, introEmojiFor(IntroStep.THEME, themeId)),
        tapped = tapped,
        origin = origin,
    )
}

/**
 * The colour of the theme the rider has chosen, as the Intro's buttons and accents draw it, and the one clock the whole of the
 * colour screen's change of theme runs on ([IntroThemeChange]). The app switches theme the moment a badge is tapped, so the colour
 * under [content] would jump from one theme to the next in a frame. Here it is one blended colour, so a tap blends the primary button,
 * the headline's text, the ink and everything else the Intro draws in the theme colour from one progress value, over
 * [THEME_COLOUR_MS], from any colour part way through, so tapping from badge to badge never flashes. With reduced motion it is a plain
 * [THEME_CHANGE_REDUCED_MS] cross-fade. Once settled it is exactly the theme's own hex, not a rounded copy of it.
 *
 * The theme is [selectedThemeId] when there is one, and the app's own colour before, which is what the screens before the colour
 * screen run in. The first time it is composed nothing plays. It re-provides [LocalThemeColor] and [LocalIntroThemeChange].
 */
@Composable
internal fun IntroThemeBlend(selectedThemeId: Int? = null, content: @Composable () -> Unit) {
    val appHex by LocalThemeColor.current
    val hex = themeHexFor(selectedThemeId) ?: appHex
    val inputs by rememberUpdatedState(
        ThemeBlendInputs(
            themeId = selectedThemeId,
            hex = hex,
            dark = isAppInDarkMode(),
            surface = KrailTheme.colors.surface,
            onSurface = KrailTheme.colors.onSurface,
            reduceMotion = isReduceMotionEnabled(),
        ),
    )
    val change = remember {
        val first = inputs.request(tapped = -1, origin = Offset.Unspecified)
        IntroThemeChange(settledThemeChangeSpec(first, ground = first.toGround, text = first.toText))
    }
    val shown = remember { mutableStateOf(hex) }

    LaunchedEffect(hex, selectedThemeId) {
        val spec = change.spec
        if (spec.target.themeId == selectedThemeId && spec.target.hex == hex) return@LaunchedEffect
        val (tapped, origin) = change.takeTap()
        change.play(inputs.request(tapped, origin))
    }
    LaunchedEffect(change) {
        snapshotFlow { change.colourNow() }.collect { colour ->
            val spec = change.spec
            shown.value = if (change.clock.value >= spec.durationMs) spec.target.hex else colour.toHex()
        }
    }
    CompositionLocalProvider(
        LocalThemeColor provides shown,
        LocalIntroThemeChange provides change,
        content = content,
    )
}
