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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
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
    val lineColor = remember(leg.lineColorCode) { leg.lineColorCode.hexToComposeColor() }
    val pendingColor = KrailTheme.colors.softLabel

    val effectiveInstants: List<Instant?> = remember(leg.stops, stopDelays) {
        leg.stops.map { stop -> effectiveInstant(stop, stopDelays) }
    }

    // Last stop index whose real-time adjusted arrival is <= now.
    // Using GTFS-RT delay means we only advance past a stop when the API confirms it.
    val currentStopIndex: Int? = remember(effectiveInstants, now) {
        effectiveInstants.indexOfLast { instant ->
            (instant ?: return@indexOfLast false) <= now
        }.takeIf { it >= 0 }
    }

    // How far (0..1) through the active segment (from currentStopIndex to currentStopIndex+1)
    val currentSegmentFraction: Float? = remember(currentStopIndex, effectiveInstants, now) {
        currentStopIndex?.let { idx ->
            val nextIdx = idx + 1
            if (nextIdx < effectiveInstants.size) {
                val prev = effectiveInstants[idx]
                val next = effectiveInstants[nextIdx]
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

    val timeline = TrackedLegTimeline(
        currentStopIndex = currentStopIndex,
        currentSegmentFraction = currentSegmentFraction,
        nextStopIndex = nextStopIndex,
        pulseScale = pulseScale,
        lineColor = lineColor,
        pendingColor = pendingColor,
        circleRadius = dim.spacingM,
        strokeWidth = dim.strokeThick,
    )

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
            TrackedLegFirstStop(
                timeline = timeline,
                firstStop = leg.stops.first(),
                isArrived = isArrived,
            )

            // Intermediate stops
            leg.stops.drop(1).dropLast(1).forEachIndexed { idx, stop ->
                val stopIdx = idx + 1
                TrackedLegIntermediateStop(
                    timeline = timeline,
                    stop = stop,
                    stopIdx = stopIdx,
                    isArrived = isArrived,
                    approachingTimeText = approachingText(effectiveInstants[stopIdx], now),
                )
            }

            // Last stop (always prominent — destination)
            val lastIdx = leg.stops.size - 1
            TrackedLegLastStop(
                timeline = timeline,
                lastStop = leg.stops.last(),
                lastIdx = lastIdx,
                isArrived = isArrived,
                approachingTimeText = approachingText(effectiveInstants[lastIdx], now),
            )
        }
    }
}

// Compute the authoritative real-time instant for a stop.
// When GTFS-RT delay is available, apply it to the scheduled time — this is the most
// up-to-date signal between trip API re-polls. Falls back to the trip API estimated time.
@OptIn(ExperimentalTime::class)
private fun effectiveInstant(stop: TrackedStop, stopDelays: Map<String, Int>): Instant? {
    val delay = stopDelays[stop.stopId]
    return if (delay != null) {
        runCatching {
            Instant.parse(stop.scheduledUtcTime) + delay.seconds
        }.getOrNull()
    } else {
        runCatching { Instant.parse(stop.utcTime) }.getOrNull()
    }
}

@Composable
internal fun TrackedStopRow(
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
