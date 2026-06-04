package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.feature.track.TrackedStop
import xyz.ksharma.krail.taj.theme.KrailTheme
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Shared per-stop colours/radii/segment data computed once in [TrackedLegView] and threaded
 * through the timeline section composables. Keeping this immutable holder out of the big
 * composable keeps each section's branching local to its own function.
 */
@Suppress("LongParameterList")
internal class TrackedLegTimeline(
    val currentStopIndex: Int?,
    val currentSegmentFraction: Float?,
    val nextStopIndex: Int?,
    val pulseScale: Float,
    val lineColor: Color,
    val pendingColor: Color,
    val circleRadius: Dp,
    val strokeWidth: Dp,
) {
    // A segment starting at fromIdx is "completed" when the vehicle has left that stop.
    // Using >= so the current stop's BELOW line also flows in lineColor, matching the split spacer.
    fun segmentColor(fromIdx: Int): Color = when {
        currentStopIndex == null -> pendingColor
        currentStopIndex >= fromIdx -> lineColor
        else -> pendingColor
    }

    fun stopCircleColor(stopIdx: Int): Color = when {
        currentStopIndex == null -> pendingColor
        stopIdx <= currentStopIndex -> lineColor
        else -> pendingColor
    }

    fun stopCircleRadius(stopIdx: Int): Dp =
        if (currentStopIndex == stopIdx) circleRadius * pulseScale else circleRadius

    fun isActiveSegment(fromIdx: Int): Boolean =
        currentStopIndex == fromIdx && currentSegmentFraction != null
}

@OptIn(ExperimentalTime::class)
internal fun approachingText(effective: Instant?, now: Instant): String? {
    val stopInstant = effective ?: return null
    val secs = (stopInstant - now).inWholeSeconds
    return when {
        secs <= 0L -> "arriving now"
        secs < SECONDS_PER_MINUTE -> "in ${secs}s"
        else -> {
            val mins = secs / SECONDS_PER_MINUTE
            val rem = secs % SECONDS_PER_MINUTE
            if (rem > 0L) "in ${mins}m ${rem}s" else "in ${mins}m"
        }
    }
}

@Composable
internal fun TrackedLegFirstStop(
    timeline: TrackedLegTimeline,
    firstStop: TrackedStop,
    isArrived: Boolean,
) {
    val dim = KrailTheme.dimensions
    TrackedStopRow(
        time = firstStop.estimatedTime ?: firstStop.scheduledTime,
        scheduledTime = firstStop.estimatedTime?.let { firstStop.scheduledTime },
        stopName = firstStop.name,
        isPast = timeline.currentStopIndex != null,
        isApproaching = false,
        isArrived = isArrived,
        lineColor = timeline.lineColor,
        modifier = Modifier
            .timeLineTop(
                color = timeline.segmentColor(0),
                strokeWidth = timeline.strokeWidth,
                circleRadius = timeline.stopCircleRadius(0),
                circleColor = timeline.stopCircleColor(0),
            )
            .padding(start = dim.spacingXL),
    )

    TrackedLegSegmentSpacer(timeline = timeline, fromIdx = 0, collapsedHeight = dim.spacingL)
}

@Composable
internal fun TrackedLegIntermediateStop(
    timeline: TrackedLegTimeline,
    stop: TrackedStop,
    stopIdx: Int,
    isArrived: Boolean,
    approachingTimeText: String?,
) {
    val dim = KrailTheme.dimensions
    val isPast = timeline.currentStopIndex != null && stopIdx <= timeline.currentStopIndex
    val isApproaching = stopIdx == timeline.nextStopIndex

    // The approaching stop's ABOVE line must be pendingColor so it connects
    // flush with the split spacer's pendingColor tail, not a jarring lineColor flash.
    val aboveColor =
        if (isApproaching) timeline.pendingColor else timeline.segmentColor(stopIdx - 1)

    TrackedStopRow(
        time = stop.estimatedTime ?: stop.scheduledTime,
        scheduledTime = stop.estimatedTime?.let { stop.scheduledTime },
        stopName = stop.name,
        isPast = isPast,
        isApproaching = isApproaching,
        isArrived = isArrived,
        lineColor = timeline.lineColor,
        approachingTimeText = if (isApproaching) approachingTimeText else null,
        modifier = Modifier
            .timeLineCenterWithStop(
                color = aboveColor,
                strokeWidth = timeline.strokeWidth,
                circleRadius = timeline.stopCircleRadius(stopIdx),
                circleColor = timeline.stopCircleColor(stopIdx),
            )
            .timeLineTop(
                color = timeline.segmentColor(stopIdx),
                strokeWidth = timeline.strokeWidth,
                circleRadius = timeline.stopCircleRadius(stopIdx),
                circleColor = timeline.stopCircleColor(stopIdx),
            )
            .padding(start = dim.spacingXL),
    )

    TrackedLegSegmentSpacer(timeline = timeline, fromIdx = stopIdx, collapsedHeight = dim.spacingXL)
}

@Composable
internal fun TrackedLegLastStop(
    timeline: TrackedLegTimeline,
    lastStop: TrackedStop,
    lastIdx: Int,
    isArrived: Boolean,
    approachingTimeText: String?,
) {
    val dim = KrailTheme.dimensions
    val isLastApproaching = lastIdx == timeline.nextStopIndex
    // Same rule: if last stop is approaching, its incoming line must be pendingColor.
    val lastIncomingColor =
        if (isLastApproaching) timeline.pendingColor else timeline.segmentColor(lastIdx - 1)
    TrackedStopRow(
        time = lastStop.estimatedTime ?: lastStop.scheduledTime,
        scheduledTime = lastStop.estimatedTime?.let { lastStop.scheduledTime },
        stopName = lastStop.name,
        isPast = timeline.currentStopIndex != null && lastIdx <= timeline.currentStopIndex,
        isApproaching = isLastApproaching,
        isArrived = isArrived,
        isDestination = true,
        lineColor = timeline.lineColor,
        approachingTimeText = if (isLastApproaching) approachingTimeText else null,
        modifier = Modifier
            .timeLineBottom(
                color = lastIncomingColor,
                strokeWidth = timeline.strokeWidth,
                circleRadius = timeline.stopCircleRadius(lastIdx),
                circleColor = timeline.stopCircleColor(lastIdx),
            )
            .padding(start = dim.spacingXL),
    )
}

@Composable
private fun TrackedLegSegmentSpacer(
    timeline: TrackedLegTimeline,
    fromIdx: Int,
    collapsedHeight: Dp,
) {
    val dim = KrailTheme.dimensions
    if (timeline.isActiveSegment(fromIdx)) {
        Spacer(
            modifier = Modifier.height(dim.spacingXL)
                .timeLineCenterSplit(
                    completedColor = timeline.lineColor,
                    pendingColor = timeline.pendingColor,
                    strokeWidth = timeline.strokeWidth,
                    fraction = timeline.currentSegmentFraction ?: 0f,
                ),
        )
    } else {
        Spacer(
            modifier = Modifier.height(collapsedHeight)
                .timeLineCenter(
                    color = timeline.segmentColor(fromIdx),
                    strokeWidth = timeline.strokeWidth,
                ),
        )
    }
}

private const val SECONDS_PER_MINUTE = 60L
