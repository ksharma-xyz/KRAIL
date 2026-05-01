package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

private val TIMELINE_CIRCLE_RADIUS = 5.dp
private const val LABELLED_NAME_CAPTION_ALPHA = 0.65f
private val CLICKABLE_CORNER_RADIUS = 8.dp
private val CLICKABLE_VERTICAL_PADDING = 6.dp

/**
 * Vertical timeline showing the origin and destination of a trip. Each stop
 * renders the user's label as primary text when set, with the raw stop name
 * as a smaller caption underneath; unlabelled stops keep the existing
 * single-line treatment.
 *
 * Optional click handlers light up only when the caller passes them — the
 * timetable screen wires them to a stop-details sheet, while TrackTrip and
 * the intro screen leave them null and stay non-interactive.
 */
@Composable
internal fun OriginDestination(
    origin: StopDisplay,
    destination: StopDisplay,
    timeLineColor: Color,
    modifier: Modifier = Modifier,
    onOriginClick: ((StopDisplay) -> Unit)? = null,
    onDestinationClick: ((StopDisplay) -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dim.spacingXL),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .timeLineTop(
                    color = timeLineColor,
                    strokeWidth = dim.strokeMedium,
                    circleRadius = TIMELINE_CIRCLE_RADIUS,
                ),
        ) {
            StopColumn(
                display = origin,
                timeLineColor = timeLineColor,
                onClick = onOriginClick,
                animationLabel = "originStopName",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth(),
            )
        }

        Spacer(
            modifier = Modifier.fillMaxWidth()
                .height(12.dp)
                .timeLineCenter(color = timeLineColor, strokeWidth = 3.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth()
                .timeLineBottom(
                    color = timeLineColor,
                    strokeWidth = 3.dp,
                    circleRadius = 5.dp,
                ),
        ) {
            StopColumn(
                display = destination,
                timeLineColor = timeLineColor,
                onClick = onDestinationClick,
                animationLabel = "destinationStopName",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StopColumn(
    display: StopDisplay,
    timeLineColor: Color,
    onClick: ((StopDisplay) -> Unit)?,
    animationLabel: String,
    modifier: Modifier = Modifier,
) {
    val labelled = display.label != null
    val primaryText = display.label ?: display.name
    val primaryStyle = if (labelled) {
        KrailTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
    } else {
        KrailTheme.typography.titleLarge
    }
    // When clickable, give the touch target some breathing room and round its
    // corners so the ripple reads as a tappable affordance rather than bleeding
    // into adjacent content. Non-clickable surfaces (TrackTrip, intro) keep
    // today's exact layout — no clip, no padding.
    val clickModifier = onClick?.let { handler ->
        Modifier
            .clip(RoundedCornerShape(CLICKABLE_CORNER_RADIUS))
            .klickable { handler(display) }
            .padding(vertical = CLICKABLE_VERTICAL_PADDING)
    } ?: Modifier

    Column(modifier = modifier.then(clickModifier)) {
        AnimatedContent(
            targetState = primaryText,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(200)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(500, easing = EaseOutBounce),
                        )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(200)) +
                        slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(500),
                        )
                    )
            },
            contentAlignment = Alignment.CenterStart,
            label = animationLabel,
        ) { targetText ->
            Text(
                text = targetText,
                color = timeLineColor,
                style = primaryStyle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (labelled) {
            Text(
                text = display.name,
                color = timeLineColor.copy(alpha = LABELLED_NAME_CAPTION_ALPHA),
                style = KrailTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
    }
}
