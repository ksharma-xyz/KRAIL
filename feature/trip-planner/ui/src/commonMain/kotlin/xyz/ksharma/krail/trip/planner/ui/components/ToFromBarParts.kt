package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.SeparatorIcon
import xyz.ksharma.krail.taj.components.ThemeTextFieldPlaceholderText
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

// The parts a To or From bar is made of besides taj's own text field and text field button.

/** A bar's end column: [column] square, the control centred in it. A control is never a bar's child. */
@Composable
internal fun BarSlot(column: Dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(column), contentAlignment = Alignment.Center) { content() }
}

/** The text of an idle bar: the stop, or the placeholder that names the bar, sliding when it changes. */
@Composable
internal fun StopFieldText(text: String, hasStop: Boolean, slideUp: Boolean, label: String) {
    val enterOffset: (Int) -> Int = if (slideUp) ({ it / 2 }) else ({ -it / 2 })
    val exitOffset: (Int) -> Int = if (slideUp) ({ -it / 2 }) else ({ it / 2 })
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = enterOffset,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                ) togetherWith fadeOut(animationSpec = tween(200)) +
                slideOutVertically(targetOffsetY = exitOffset, animationSpec = tween(500))
        },
        contentAlignment = Alignment.CenterStart,
        label = label,
    ) { targetText ->
        ThemeTextFieldPlaceholderText(text = targetText, isActive = hasStop)
    }
}

/**
 * The dot that marks the From bar: taj's [SeparatorIcon], a filled dot, in the bar's own content
 * colour so it reads on the light bar and on the dark one. Sized by modifier to a step of the
 * spacing scale.
 */
@Composable
internal fun OriginDot() {
    SeparatorIcon(
        modifier = Modifier
            .size(KrailTheme.dimensions.spacingM)
            .testTag(TripPlannerTestTags.SEARCH_ROW_ORIGIN_DOT),
        color = LocalContentColor.current,
    )
}

/**
 * The go circle: taj's [RoundIconButton], one state and a plain arrow. The screen it opens owns
 * loading, success and error, so this one never shows any of them.
 *
 * It is [diameter] across, sized by modifier, filled with [circle] (the bar's content colour) and
 * drawn with an arrow in [arrow] (the bar's own fill), so it is the opposite of the bar on a white
 * bar and on a dark one. The button is the tap target and carries
 * [TripPlannerTestTags.SEARCH_ROW_SEARCH] as the Search button always did; the box around it is
 * the circle for [TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE].
 */
@Composable
internal fun GoCircle(circle: Color, arrow: Color, diameter: Dp, onClick: () -> Unit) {
    Box(modifier = Modifier.testTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE)) {
        RoundIconButton(
            onClick = onClick,
            color = circle,
            modifier = Modifier.size(diameter).testTag(TripPlannerTestTags.SEARCH_ROW_SEARCH),
        ) {
            Image(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Search",
                colorFilter = ColorFilter.tint(arrow),
                modifier = Modifier.size(KrailTheme.dimensions.iconDefault),
            )
        }
    }
}
