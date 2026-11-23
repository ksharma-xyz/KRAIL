package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.TEXT_CONTRAST_AA
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.UI_COMPONENT_CONTRAST_AA
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.asThemeInk
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The colour of the hero of each question: one fixed colour for each screen, and never a random one, so a rider who opens the
 * Intro again sees the same screens. Each is the colour of an app theme and is read from [KrailThemeStyle], so there is no colour
 * typed into the Intro. Park and Ride is Ferry green, the destination is Bus blue, the origin is Metro teal, the confirm screen is
 * Barbie Pink and the colour screen's question is Purple Drip. The words say everything, colour never carries a meaning alone.
 */
internal fun introAccentStyle(step: IntroStep): KrailThemeStyle = when (step) {
    IntroStep.PARK_RIDE -> KrailThemeStyle.Ferry
    IntroStep.DESTINATION -> KrailThemeStyle.Bus
    IntroStep.ORIGIN -> KrailThemeStyle.Metro
    IntroStep.CONFIRM -> KrailThemeStyle.BarbiePink
    IntroStep.THEME -> KrailThemeStyle.PurpleDrip
}

/** The theme colour of [step]'s accent, as the app defines it. */
internal fun introAccentTheme(step: IntroStep): Color = introAccentStyle(step).hexColorCode.hexToComposeColor()

/** The least contrast the hero has against the page: large text, so 3.2 to 1, a little over the 3 to 1 the bar asks of it. */
internal const val ACCENT_MIN_CONTRAST = 3.2f

// A hundredth of lightness at a time, which is the step the approved colours were chosen on.
private const val LIGHTNESS_STEPS = 100

/**
 * The hero's colour for the theme colour [theme] on the page [surface], in light mode or [dark] mode.
 *
 * - A colour that already has [ACCENT_MIN_CONTRAST] against the page is used as it is.
 * - In light mode any other is the same hue and saturation made darker, a hundredth of lightness at a time, only as far as it takes
 *   to reach [ACCENT_MIN_CONTRAST] (Bus blue becomes a deeper blue, Ferry green a deeper green).
 * - In dark mode any other takes taj's own ladder, [asThemeInk] at 3 to 1 (Purple Drip becomes a lighter purple).
 *
 * It is a pure function of three colours and a mode, computed once for a screen and not per frame.
 */
internal fun introAccentInk(theme: Color, surface: Color, dark: Boolean): Color = when {
    theme.contrastRatio(surface) >= ACCENT_MIN_CONTRAST -> theme
    dark -> theme.asThemeInk(darkMode = true, minContrast = UI_COMPONENT_CONTRAST_AA)
    else -> theme.darkenedUntil(surface, ACCENT_MIN_CONTRAST)
}

private fun Color.darkenedUntil(surface: Color, minContrast: Float): Color {
    val (hue, saturation, lightness) = toHsl()
    val start = (lightness * LIGHTNESS_STEPS).roundToInt()
    var step = start
    var candidate = this
    while (candidate.contrastRatio(surface) < minContrast && step > 0) {
        step -= 1
        candidate = hslColor(hue, saturation, step.toFloat() / LIGHTNESS_STEPS)
    }
    return candidate
}

/** The colour in HSL, with the hue in degrees and the saturation and lightness from 0 to 1. */
internal fun Color.toHsl(): Triple<Float, Float, Float> {
    val maxChannel = max(red, max(green, blue))
    val minChannel = min(red, min(green, blue))
    val delta = maxChannel - minChannel
    val lightness = (maxChannel + minChannel) / 2f
    if (delta == 0f) return Triple(0f, 0f, lightness)
    val saturation = delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    val sector = when (maxChannel) {
        red -> ((green - blue) / delta).mod(HUE_SECTORS)
        green -> (blue - red) / delta + 2f
        else -> (red - green) / delta + 4f
    }
    return Triple(sector * DEGREES_PER_SECTOR, saturation, lightness)
}

/** The opaque colour with [hue] in degrees and [saturation] and [lightness] from 0 to 1. */
internal fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val chroma = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val sector = (hue / DEGREES_PER_SECTOR).mod(HUE_SECTORS)
    val second = chroma * (1f - kotlin.math.abs(sector.mod(2f) - 1f))
    val (r, g, b) = when (sector.toInt()) {
        0 -> Triple(chroma, second, 0f)
        1 -> Triple(second, chroma, 0f)
        2 -> Triple(0f, chroma, second)
        3 -> Triple(0f, second, chroma)
        4 -> Triple(second, 0f, chroma)
        else -> Triple(chroma, 0f, second)
    }
    val match = lightness - chroma / 2f
    return Color(red = r + match, green = g + match, blue = b + match)
}

private const val HUE_SECTORS = 6f
private const val DEGREES_PER_SECTOR = 60f

/** The least contrast a white label has on the fill of the primary button: text, so 4.5 to 1. */
internal const val BUTTON_LABEL_MIN_CONTRAST = TEXT_CONTRAST_AA

/**
 * The fill of the primary button of a screen: the screen's theme colour [theme] made deeper by taj's own ink ladder
 * ([asThemeInk], light mode, 4.5 to 1), in both light and dark mode, so the white label on it has at least 4.5 to 1. The theme
 * colour is the family and the ladder takes it only as far as it has to.
 */
internal fun introButtonFill(theme: Color): Color =
    theme.asThemeInk(darkMode = false, minContrast = BUTTON_LABEL_MIN_CONTRAST)

/**
 * The fill of the primary button of [step], or null for the colour screen, whose button is the colour the rider picked and which
 * keeps the taj button's own colours. The label is always white ([Color.White], as the letter in the transport mode icon).
 */
@Composable
internal fun introPrimaryFill(step: IntroStep): Color? =
    if (step == IntroStep.THEME) null else remember(step) { introButtonFill(introAccentTheme(step)) }

/**
 * The hero colour of [step] on the page the Intro is drawn on, in the mode the app is in ([introAccentInk]). It is remembered for the
 * theme colour, the page and the mode, so it is computed once for a screen and never per frame.
 */
@Composable
internal fun introAccentColor(step: IntroStep): Color {
    val theme = remember(step) { introAccentTheme(step) }
    val surface = KrailTheme.colors.surface
    val dark = isAppInDarkMode()
    return remember(theme, surface, dark) { introAccentInk(theme, surface, dark) }
}

/**
 * Draws [content], the screen of [step], in that screen's own colour family and never in the app theme. taj's tints (the card
 * fill, the selected row, the celebration burst) and the Park and Ride tile all read the app's theme colour, which on a first run
 * is the default Train orange and sat beside a green, blue or pink hero. Here the one local they read, [LocalThemeColor], is the
 * screen's accent theme ([introAccentStyle]), so each tint is that accent made translucent by taj's own function and no colour is
 * typed. It wraps the content of a screen and nothing else: the page, the decision bar and the components outside the Intro
 * still read the app's theme. The colour screen is not wrapped, because its colours are the themes the rider picks between.
 */
@Composable
internal fun IntroScreenColours(step: IntroStep, content: @Composable () -> Unit) {
    if (step == IntroStep.THEME) {
        content()
        return
    }
    val hex = introAccentStyle(step).hexColorCode
    val screenColour = remember(hex) { mutableStateOf(hex) }
    CompositionLocalProvider(LocalThemeColor provides screenColour) { content() }
}

/** How long the decision bar's fill takes to move from one screen's colour to the next: the length of the colour changes. */
internal const val BAR_FILL_CROSSFADE_MS = 300

/** The way the bar's fill changes colour: [BAR_FILL_CROSSFADE_MS] with the Intro's own easing, and a jump with reduced motion. */
internal fun introBarFillSpec(reduceMotion: Boolean): AnimationSpec<Color> =
    if (reduceMotion) snap() else tween(durationMillis = BAR_FILL_CROSSFADE_MS, easing = FastOutSlowInEasing)

/**
 * The fill of the decision bar's primary button of [step] as it is drawn: [introPrimaryFill] and, when the step changes, a
 * crossfade from the last screen's fill to this one's ([introBarFillSpec]). The bar is one layer over every screen, so this is the
 * only accent colour that is not part of a sliding screen, and without it the button jumped from green to blue to pink in one frame.
 * It is null on the colour screen, whose button keeps the taj colours of the theme the rider picked, so that button does not move
 * from a colour to another, and the first screen starts at its own fill and never fades from nothing.
 */
@Composable
internal fun introAnimatedPrimaryFill(step: IntroStep): Color? {
    val target = introPrimaryFill(step) ?: return null
    val fill by animateColorAsState(
        targetValue = target,
        animationSpec = introBarFillSpec(isReduceMotionEnabled()),
        label = "intro-bar-fill",
    )
    return fill
}
