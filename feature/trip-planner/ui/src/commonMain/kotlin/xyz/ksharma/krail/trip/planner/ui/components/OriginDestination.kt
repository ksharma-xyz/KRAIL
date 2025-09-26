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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Composable
internal fun OriginDestination(
    trip: Trip,
    timeLineColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .timeLineTop(
                    color = timeLineColor,
                    strokeWidth = 3.dp,
                    circleRadius = 5.dp,
                ),
        ) {
            AnimatedContent(
                targetState = trip.fromStopName,
                transitionSpec = {
                    (
                        fadeIn(
                            animationSpec = tween(200),
                        ) + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(500, easing = EaseOutBounce),
                        )
                        ) togetherWith (
                        fadeOut(
                            animationSpec = tween(200),
                        ) + slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(500),
                        )
                        )
                },
                contentAlignment = Alignment.CenterStart,
                label = "originStopName",
            ) { targetText ->
                Text(
                    text = targetText,
                    color = timeLineColor,
                    style = KrailTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                )
            }
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
            AnimatedContent(
                targetState = trip.toStopName,
                transitionSpec = {
                    (
                        fadeIn(
                            animationSpec = tween(200),
                        ) + slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(500, easing = EaseOutBounce),
                        )
                        ) togetherWith (
                        fadeOut(
                            animationSpec = tween(200),
                        ) + slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = tween(500),
                        )
                        )
                },
                contentAlignment = Alignment.CenterStart,
                label = "destinationStopName",
            ) { targetText ->
                Text(
                    text = targetText,
                    color = timeLineColor,
                    style = KrailTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}
