package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.toHex

/** How long the Intro's theme colour takes to blend from one theme to the next. */
internal const val THEME_BLEND_MS = 280

/**
 * How the theme colour moves: a [THEME_BLEND_MS] blend, and no blend at all, an instant change, when the rider asked
 * for less motion.
 */
internal fun themeBlendSpec(reduceMotion: Boolean): AnimationSpec<Color> =
    if (reduceMotion) snap() else tween(durationMillis = THEME_BLEND_MS)

/**
 * The colour of the theme the rider has chosen, as the Intro's buttons and accents draw it. The app switches theme
 * the moment a badge is tapped, so the colour under [content] would jump from one theme to the next in a frame. Here
 * it is one animated colour, so a tap on a badge blends the primary button, and everything else the Intro draws in
 * the theme colour, over [THEME_BLEND_MS], and from any colour part way through, so tapping from badge to badge never
 * flashes. With reduced motion it is the new colour at once. Once settled it is exactly the theme's own hex, not
 * a rounded copy of it.
 *
 * It re-provides [LocalThemeColor] and nothing else, and it is a no-op for the colour the first time it is composed.
 */
@Composable
internal fun IntroThemeBlend(content: @Composable () -> Unit) {
    val target by LocalThemeColor.current
    val targetColor = remember(target) { target.hexToComposeColor() }
    val blended by animateColorAsState(
        targetValue = targetColor,
        animationSpec = themeBlendSpec(isReduceMotionEnabled()),
        label = "intro-theme-colour",
    )
    val shown = remember(blended, target) { mutableStateOf(if (blended == targetColor) target else blended.toHex()) }
    CompositionLocalProvider(LocalThemeColor provides shown, content = content)
}
