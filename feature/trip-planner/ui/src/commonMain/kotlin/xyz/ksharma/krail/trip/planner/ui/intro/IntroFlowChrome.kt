package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.NavActionButton
import xyz.ksharma.krail.taj.theme.KrailTheme

private const val STEP_FADE_OUT_MS = 120
private const val STEP_FADE_IN_MS = 180

internal const val BACK_DESCRIPTION = "Back"

/**
 * How one screen replaces another: a short cross-fade, or with reduced motion a plain swap with no
 * fade at all. A later commit of the rebuild replaces this with the slide.
 */
internal fun introStepTransition(reduceMotion: Boolean): ContentTransform =
    ContentTransform(
        targetContentEnter = if (reduceMotion) {
            EnterTransition.None
        } else {
            fadeIn(tween(durationMillis = STEP_FADE_IN_MS, delayMillis = STEP_FADE_OUT_MS))
        },
        initialContentExit = if (reduceMotion) {
            ExitTransition.None
        } else {
            fadeOut(tween(durationMillis = STEP_FADE_OUT_MS))
        },
        sizeTransform = null,
    )

/**
 * The bar above the screen: the app's own back button, top left, when there is a screen to go back
 * to. The room for it is reserved on every screen, so nothing moves when it appears. There is
 * nothing else in the bar, and in particular no progress marker.
 */
@Composable
internal fun IntroTopBar(showBack: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dim.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(dim.iconXXXL), contentAlignment = Alignment.Center) {
            if (showBack) {
                NavActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    iconContentDescription = BACK_DESCRIPTION,
                    onClick = onBack,
                )
            }
        }
    }
}
