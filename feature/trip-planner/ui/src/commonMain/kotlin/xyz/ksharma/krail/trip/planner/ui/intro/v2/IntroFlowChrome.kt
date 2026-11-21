package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_chevron_left
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep

private const val STEP_FADE_OUT_MS = 120
private const val STEP_FADE_IN_MS = 180
private const val DOT_SLIDE_MS = 200

/** What a screen reader says for the progress dots, for example Step 1 of 2. */
internal fun introStepLabel(step: IntroStep): String = "Step ${step.ordinal + 1} of ${IntroStep.entries.size}"

/**
 * How step 2 replaces step 1 and back: a short cross-fade, or with reduced motion a plain swap
 * with no fade at all.
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
 * The bar above the step: a back button on step 2 only, and the progress dots in the middle. Both
 * sides are reserved at the same width on every step, so the dots never move when the back button
 * appears.
 */
@Composable
internal fun IntroTopBar(step: IntroStep, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dim.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(dim.iconXXXL), contentAlignment = Alignment.Center) {
            if (step == IntroStep.FIRST_TRIP) IntroBackButton(onClick = onBack)
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            IntroProgressDots(step = step)
        }
        Spacer(modifier = Modifier.size(dim.iconXXXL))
    }
}

@Composable
private fun IntroBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            .size(dim.iconXXXL)
            .clip(CircleShape)
            .klickable(onClick = onClick)
            // One node with a name, so a screen reader announces a Back button and not a picture.
            .semantics { contentDescription = BACK_DESCRIPTION },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_chevron_left),
            contentDescription = null,
            colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
            modifier = Modifier.size(dim.iconLarge),
        )
    }
}

/**
 * One dot per step, the current one stretched. The current step is told by width as well as by
 * colour, so colour is never the only signal, and a screen reader reads the whole row as one
 * label.
 */
@Composable
internal fun IntroProgressDots(step: IntroStep, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    val reduceMotion = isReduceMotionEnabled()
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = introStepLabel(step) },
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IntroStep.entries.forEach { dotStep ->
            val current = dotStep == step
            val width: Dp by animateDpAsState(
                targetValue = if (current) dim.spacingXXXL else dim.spacingM,
                animationSpec = if (reduceMotion) snap() else tween(durationMillis = DOT_SLIDE_MS),
                label = "intro-dot-width",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(dim.spacingM)
                    .clip(CircleShape)
                    .background(if (current) KrailTheme.colors.onSurface else KrailTheme.colors.softLabel),
            )
        }
    }
}

private const val BACK_DESCRIPTION = "Back"
