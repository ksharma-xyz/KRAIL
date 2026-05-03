@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.departureboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.taj.components.AnimatedDots
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardBody
import xyz.ksharma.krail.trip.planner.ui.savedtrips.StopDepartureBoardEntry
import xyz.ksharma.krail.trip.planner.ui.state.departureboard.DepartureBoardUiEvent

// ── LazyListScope extension ───────────────────────────────────────────────────

/**
 * Adds a stop accordion section as two stable [LazyListScope] items:
 *  - `"${stopId}_header"` — sticky tappable card
 *  - `"${stopId}_content"` — collapsible content with services row + departure list
 *
 * The content item hosts [DepartureBoardAccordionContent] which delegates all state
 * to [DepartureBoardBody]. Filter and previous-departures state resets automatically
 * when the item leaves composition (section collapses).
 */
fun LazyListScope.departureBoardAccordionSection(
    entry: StopDepartureBoardEntry,
    isExpanded: Boolean,
    iconColor: Color,
    onEvent: (DepartureBoardUiEvent) -> Unit,
) {
    stickyHeader(key = "${entry.stopId}_header", contentType = "stop_header") {
        DepartureBoardAccordionSectionHeader(
            entry = entry,
            isExpanded = isExpanded,
            iconColor = iconColor,
            onExpandChange = { expand ->
                onEvent(
                    if (expand) {
                        DepartureBoardUiEvent.ExpandStop(entry.stopId)
                    } else {
                        DepartureBoardUiEvent.CollapseStop
                    },
                )
            },
            modifier = Modifier.padding(bottom = SECTION_HEADER_BOTTOM_PADDING),
        )
    }

    if (isExpanded) {
        item(key = "${entry.stopId}_content", contentType = "stop_content") {
            DepartureBoardAccordionContent(
                stopId = entry.stopId,
                state = entry.state,
                onLoadPreviousDepartures = { onEvent(DepartureBoardUiEvent.LoadPreviousDepartures(it)) },
                onRefreshStop = { onEvent(DepartureBoardUiEvent.RefreshStop(it)) },
                onLineFilterChange = { lineNumber, transportMode, selected ->
                    onEvent(
                        DepartureBoardUiEvent.LineFilterChanged(
                            stopId = entry.stopId,
                            selected = selected,
                            lineNumber = lineNumber,
                            transportMode = transportMode,
                        ),
                    )
                },
                onShowPreviousToggle = { show ->
                    onEvent(DepartureBoardUiEvent.TogglePreviousDepartures(entry.stopId, show))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Header card composable ────────────────────────────────────────────────────

/** Tappable header card for a stop section. */
@Composable
internal fun DepartureBoardAccordionSectionHeader(
    entry: StopDepartureBoardEntry,
    isExpanded: Boolean,
    iconColor: Color,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow-rotation",
    )
    val outerPadding by animateDpAsState(
        targetValue = if (isExpanded) dim.spacingNone else dim.spacingXL,
        animationSpec = tween(durationMillis = 300),
        label = "outer-padding",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) dim.spacingNone else dim.cardCornerRadius,
        animationSpec = tween(durationMillis = 300),
        label = "corner-radius",
    )
    val animatedShape = RoundedCornerShape(cornerRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = outerPadding)
            .background(color = KrailTheme.colors.surface, shape = animatedShape)
            .background(color = themeBackgroundColor(), shape = animatedShape)
            .klickable { onExpandChange(!isExpanded) }
            .padding(horizontal = dim.spacingXL, vertical = dim.spacingXL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = entry.stopName,
            style = KrailTheme.typography.titleMedium,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isExpanded && entry.state.silentLoading) {
                AnimatedDots(
                    modifier = Modifier.size(width = DOTS_WIDTH, height = DOTS_HEIGHT),
                    color = KrailTheme.colors.onSurface,
                )
            }

            Image(
                painter = painterResource(Res.drawable.ic_arrow_down),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                colorFilter = ColorFilter.tint(iconColor),
                modifier = Modifier.size(ARROW_ICON_SIZE).rotate(arrowRotation),
            )
        }
    }
}

// ── Content composable ────────────────────────────────────────────────────────

/**
 * Content area for an expanded stop section.
 *
 * All filter and previous-departures state is owned by [DepartureBoardBody] and naturally
 * resets when this composable leaves composition (i.e. when the section collapses).
 */
@Composable
private fun DepartureBoardAccordionContent(
    stopId: String,
    state: DeparturesState,
    onLoadPreviousDepartures: (String) -> Unit,
    onRefreshStop: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLineFilterChange: ((lineNumber: String?, transportMode: String?, selected: Boolean) -> Unit)? = null,
    onShowPreviousToggle: ((Boolean) -> Unit)? = null,
) {
    DepartureBoardBody(
        state = state,
        onRetry = { onRefreshStop(stopId) },
        onLoadPreviousDepartures = { onLoadPreviousDepartures(stopId) },
        onLineFilterChange = onLineFilterChange,
        onShowPreviousToggle = onShowPreviousToggle,
        modifier = modifier,
    )
}

private val SECTION_HEADER_BOTTOM_PADDING = 16.dp
private val ARROW_ICON_SIZE = 18.dp
private val DOTS_WIDTH = 32.dp
private val DOTS_HEIGHT = 16.dp
