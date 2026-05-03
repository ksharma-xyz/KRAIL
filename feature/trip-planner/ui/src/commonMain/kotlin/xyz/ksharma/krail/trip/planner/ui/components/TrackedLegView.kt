package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.track.TrackedLeg
import xyz.ksharma.krail.feature.track.TrackedStop
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.ensureMinimumContrast
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalTime::class)
@Composable
internal fun TrackedLegView(
    leg: TrackedLeg.Transport,
    now: Instant,
    modifier: Modifier = Modifier,
    isArrived: Boolean = false,
    stopDelays: Map<String, Int> = emptyMap(),
) {
    val dim = KrailTheme.dimensions
    val circleRadius = dim.spacingM
    val strokeWidth = dim.strokeThick
    val lineColor = remember(leg.lineColorCode) { leg.lineColorCode.hexToComposeColor() }
    val pendingColor = KrailTheme.colors.softLabel

    // Compute the authoritative real-time instant for a stop.
    // When GTFS-RT delay is available, apply it to the scheduled time — this is the most
    // up-to-date signal between trip API re-polls. Falls back to the trip API estimated time.
    fun effectiveInstant(stop: TrackedStop): Instant? {
        val delay = stopDelays[stop.stopId]
        return if (delay != null) {
            runCatching {
                Instant.parse(stop.scheduledUtcTime) + delay.seconds
            }.getOrNull()
        } else {
            runCatching { Instant.parse(stop.utcTime) }.getOrNull()
        }
    }

    // Last stop index whose real-time adjusted arrival is <= now.
    // Using GTFS-RT delay means we only advance past a stop when the API confirms it.
    val currentStopIndex: Int? = remember(leg.stops, stopDelays, now) {
        leg.stops.indexOfLast { stop ->
            (effectiveInstant(stop) ?: return@indexOfLast false) <= now
        }.takeIf { it >= 0 }
    }

    // How far (0..1) through the active segment (from currentStopIndex to currentStopIndex+1)
    val currentSegmentFraction: Float? = remember(currentStopIndex, stopDelays, now) {
        currentStopIndex?.let { idx ->
            val nextIdx = idx + 1
            if (nextIdx < leg.stops.size) {
                val prev = effectiveInstant(leg.stops[idx])
                val next = effectiveInstant(leg.stops[nextIdx])
                if (prev != null && next != null) {
                    val total = (next - prev).inWholeMilliseconds.toFloat()
                    val elapsed = (now - prev).inWholeMilliseconds.toFloat()
                    if (total > 0f) (elapsed / total).coerceIn(0f, 1f) else null
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    val nextStopIndex: Int? = currentStopIndex?.let { it + 1 }?.takeIf { it < leg.stops.size }

    val infiniteTransition = rememberInfiniteTransition(label = "trackedPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    // A segment starting at fromIdx is "completed" when the vehicle has left that stop
    // Using >= so the current stop's BELOW line also flows in lineColor, matching the split spacer
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

    fun isActiveSegment(fromIdx: Int) =
        currentStopIndex == fromIdx && currentSegmentFraction != null

    fun approachingText(stop: TrackedStop): String? {
        val stopInstant = effectiveInstant(stop) ?: return null
        val secs = (stopInstant - now).inWholeSeconds
        return when {
            secs <= 0L -> "arriving now"
            secs < 60L -> "in ${secs}s"
            else -> {
                val mins = secs / 60L
                val rem = secs % 60L
                if (rem > 0L) "in ${mins}m ${rem}s" else "in ${mins}m"
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Route header: badge + headsign
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dim.spacingXL, top = dim.spacingXL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportModeBadge(
                backgroundColor = lineColor,
                badgeText = leg.lineName,
                modifier = Modifier.padding(end = dim.spacingML),
            )
            leg.headsign?.let { headsign ->
                Text(
                    text = headsign,
                    style = KrailTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(start = dim.spacingXS)) {
            // First stop (always prominent — origin)
            val firstStop = leg.stops.first()
            TrackedStopRow(
                time = firstStop.estimatedTime ?: firstStop.scheduledTime,
                scheduledTime = firstStop.estimatedTime?.let { firstStop.scheduledTime },
                stopName = firstStop.name,
                isPast = currentStopIndex != null,
                isApproaching = false,
                isArrived = isArrived,
                lineColor = lineColor,
                modifier = Modifier
                    .timeLineTop(
                        color = segmentColor(0),
                        strokeWidth = strokeWidth,
                        circleRadius = stopCircleRadius(0),
                        circleColor = stopCircleColor(0),
                    )
                    .padding(start = dim.spacingXL),
            )

            if (isActiveSegment(0)) {
                Spacer(
                    modifier = Modifier.height(dim.spacingXL)
                        .timeLineCenterSplit(
                            completedColor = lineColor,
                            pendingColor = pendingColor,
                            strokeWidth = strokeWidth,
                            fraction = currentSegmentFraction ?: 0f,
                        ),
                )
            } else {
                Spacer(
                    modifier = Modifier.height(dim.spacingL)
                        .timeLineCenter(color = segmentColor(0), strokeWidth = strokeWidth),
                )
            }

            // Intermediate stops
            leg.stops.drop(1).dropLast(1).forEachIndexed { idx, stop ->
                val stopIdx = idx + 1
                val isPast = currentStopIndex != null && stopIdx <= currentStopIndex
                val isApproaching = stopIdx == nextStopIndex

                // The approaching stop's ABOVE line must be pendingColor so it connects
                // flush with the split spacer's pendingColor tail, not a jarring lineColor flash.
                val aboveColor = if (isApproaching) pendingColor else segmentColor(stopIdx - 1)

                TrackedStopRow(
                    time = stop.estimatedTime ?: stop.scheduledTime,
                    scheduledTime = stop.estimatedTime?.let { stop.scheduledTime },
                    stopName = stop.name,
                    isPast = isPast,
                    isApproaching = isApproaching,
                    isArrived = isArrived,
                    lineColor = lineColor,
                    approachingTimeText = if (isApproaching) approachingText(stop) else null,
                    modifier = Modifier
                        .timeLineCenterWithStop(
                            color = aboveColor,
                            strokeWidth = strokeWidth,
                            circleRadius = stopCircleRadius(stopIdx),
                            circleColor = stopCircleColor(stopIdx),
                        )
                        .timeLineTop(
                            color = segmentColor(stopIdx),
                            strokeWidth = strokeWidth,
                            circleRadius = stopCircleRadius(stopIdx),
                            circleColor = stopCircleColor(stopIdx),
                        )
                        .padding(start = dim.spacingXL),
                )

                if (isActiveSegment(stopIdx)) {
                    Spacer(
                        modifier = Modifier.height(dim.spacingXL)
                            .timeLineCenterSplit(
                                lineColor,
                                pendingColor,
                                strokeWidth,
                                currentSegmentFraction ?: 0f,
                            ),
                    )
                } else {
                    Spacer(
                        modifier = Modifier.height(dim.spacingXL).timeLineCenter(
                            color = segmentColor(stopIdx),
                            strokeWidth = strokeWidth,
                        ),
                    )
                }
            }

            // Last stop (always prominent — destination)
            val lastIdx = leg.stops.size - 1
            val lastStop = leg.stops.last()
            val isLastApproaching = lastIdx == nextStopIndex
            // Same rule: if last stop is approaching, its incoming line must be pendingColor
            val lastIncomingColor =
                if (isLastApproaching) pendingColor else segmentColor(lastIdx - 1)
            TrackedStopRow(
                time = lastStop.estimatedTime ?: lastStop.scheduledTime,
                scheduledTime = lastStop.estimatedTime?.let { lastStop.scheduledTime },
                stopName = lastStop.name,
                isPast = currentStopIndex != null && lastIdx <= currentStopIndex,
                isApproaching = isLastApproaching,
                isArrived = isArrived,
                isDestination = true,
                lineColor = lineColor,
                approachingTimeText = if (isLastApproaching) approachingText(lastStop) else null,
                modifier = Modifier
                    .timeLineBottom(
                        color = lastIncomingColor,
                        strokeWidth = strokeWidth,
                        circleRadius = stopCircleRadius(lastIdx),
                        circleColor = stopCircleColor(lastIdx),
                    )
                    .padding(start = dim.spacingXL),
            )
        }
    }
}

@Composable
private fun TrackedStopRow(
    time: String,
    scheduledTime: String?,
    stopName: String,
    isPast: Boolean,
    isApproaching: Boolean,
    lineColor: Color,
    modifier: Modifier = Modifier,
    isArrived: Boolean = false,
    isDestination: Boolean = false,
    approachingTimeText: String? = null,
    backgroundColor: Color = KrailTheme.colors.surface,
) {
    val onSurface = KrailTheme.colors.onSurface
    val softLabel = KrailTheme.colors.softLabel

    val contentColor = when {
        isArrived && isDestination -> lineColor.ensureMinimumContrast(backgroundColor)
        isArrived -> softLabel
        isApproaching -> lineColor.ensureMinimumContrast(backgroundColor)
        isPast -> onSurface
        else -> onSurface
    }
    val timeStyle = KrailTheme.typography.bodyMedium
    val stopNameStyle = when {
        isArrived && isDestination -> KrailTheme.typography.titleMedium
        isApproaching -> KrailTheme.typography.titleMedium
        else -> KrailTheme.typography.bodyMedium
    }

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(end = KrailTheme.dimensions.spacingM),
                horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingS),
            ) {
                Text(text = time, style = timeStyle, color = contentColor)
                scheduledTime?.let {
                    Text(
                        text = it,
                        style = KrailTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = KrailTheme.colors.softLabel,
                    )
                }
            }

            if (scheduledTime == null) {
                ApproachingTimeText(approachingTimeText, contentColor)
            }
        }

        if (scheduledTime != null) {
            ApproachingTimeText(approachingTimeText, contentColor)
        }

        Text(text = stopName, style = stopNameStyle, color = contentColor)
    }
}

@Composable
private fun ApproachingTimeText(
    approachingTimeText: String?,
    contentColor: Color,
) {
    approachingTimeText?.let { timeText ->
        Text(
            text = "($timeText)",
            style = KrailTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.padding(vertical = APPROACHING_TIME_VERTICAL_PADDING),
        )
    }
}

// region Previews

@OptIn(ExperimentalTime::class)
@ScreenshotTest
@PreviewComponent
@Composable
private fun TrackedLegViewPreview() {
    val now = Instant.parse("2024-01-01T12:02:30Z")
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        TrackedLegView(
            leg = TrackedLeg.Transport(
                transportMode = TransportMode.Train,
                lineName = "T1",
                lineColorCode = "#F6891F",
                headsign = "City via Strathfield",
                stops = persistentListOf(
                    TrackedStop(
                        name = "Parramatta Station",
                        scheduledTime = "12:00 PM",
                        estimatedTime = null,
                        utcTime = "2024-01-01T12:00:00Z",
                        stopId = "1234",
                        scheduledUtcTime = "2024-01-01T12:00:00Z",
                    ),
                    TrackedStop(
                        name = "Harris Park Station",
                        scheduledTime = "12:02 PM",
                        estimatedTime = "12:03 PM",
                        utcTime = "2024-01-01T12:02:00Z",
                        stopId = "1235",
                        scheduledUtcTime = "2024-01-01T12:00:00Z",
                    ),
                    TrackedStop(
                        name = "Granville Station",
                        scheduledTime = "12:05 PM",
                        estimatedTime = null,
                        utcTime = "2024-01-01T12:05:00Z",
                        stopId = "1236",
                        scheduledUtcTime = "2024-01-01T12:00:00Z",
                    ),
                    TrackedStop(
                        name = "XYZ Future Station",
                        scheduledTime = "12:15 PM",
                        estimatedTime = null,
                        utcTime = "2024-01-01T12:15:00Z",
                        stopId = "1237",
                        scheduledUtcTime = "2024-01-01T12:00:00Z",
                    ),
                ),
            ),
            now = now,
            modifier = Modifier.padding(KrailTheme.dimensions.spacingXL),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun TrackedStopRowPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Column(
            modifier = Modifier.padding(KrailTheme.dimensions.spacingXL),
            verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXL),
        ) {
            TrackedStopRow(
                time = "12:05 PM",
                scheduledTime = "12:03 PM",
                stopName = "Granville Station",
                isPast = false,
                isApproaching = true,
                lineColor = Color(0xFFF6891F),
                approachingTimeText = "in 2m 5s",
            )

            TrackedStopRow(
                time = "12:00 PM",
                scheduledTime = null,
                stopName = "Parramatta Station",
                isPast = true,
                isApproaching = false,
                lineColor = Color(0xFFF6891F),
            )
        }
    }
}

// endregion

private val APPROACHING_TIME_VERTICAL_PADDING = 2.dp
