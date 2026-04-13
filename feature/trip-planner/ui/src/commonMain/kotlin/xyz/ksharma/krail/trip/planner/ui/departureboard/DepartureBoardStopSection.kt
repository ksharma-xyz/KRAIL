@file:Suppress("MagicNumber", "TooManyFunctions")

package xyz.ksharma.krail.trip.planner.ui.departureboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.components.DepartureRowList
import xyz.ksharma.krail.trip.planner.ui.components.DeparturesErrorContent
import xyz.ksharma.krail.trip.planner.ui.components.LinesServedRow
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots
import xyz.ksharma.krail.trip.planner.ui.savedtrips.StopDepartureBoardEntry

// ── LazyListScope extension ───────────────────────────────────────────────────

/**
 * Adds a stop accordion section as two stable [LazyListScope] items:
 *  - `"${stopId}_header"` — sticky tappable card
 *  - `"${stopId}_content"` — collapsible content with services row + departure list
 *
 * The content item hosts [DepartureBoardAccordionContent] which owns the per-stop filter state.
 * Filter state naturally resets when the item collapses because [remember] is keyed to the
 * item's composition lifetime.
 */
fun LazyListScope.departureBoardAccordionSection(
    entry: StopDepartureBoardEntry,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onLoadPreviousDepartures: (String) -> Unit = {},
    onRefreshStop: (String) -> Unit = {},
) {
    stickyHeader(key = "${entry.stopId}_header", contentType = "stop_header") {
        DepartureBoardAccordionSectionHeader(
            entry = entry,
            isExpanded = isExpanded,
            onExpandChange = onExpandChange,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    if (isExpanded) {
        item(key = "${entry.stopId}_content", contentType = "stop_content") {
            DepartureBoardAccordionContent(
                stopId = entry.stopId,
                state = entry.state,
                onLoadPreviousDepartures = onLoadPreviousDepartures,
                onRefreshStop = onRefreshStop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Header card composable ────────────────────────────────────────────────────

/**
 * The tappable header card for a stop section.
 * Shared by [departureBoardAccordionSection] (lazy use) and [DepartureBoardAccordionSection] (preview use).
 */
@Composable
internal fun DepartureBoardAccordionSectionHeader(
    entry: StopDepartureBoardEntry,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow-rotation",
    )
    val outerPadding by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "outer-padding",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 16.dp,
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isExpanded && entry.state.silentLoading) {
                AnimatedDots(
                    modifier = Modifier.size(width = 32.dp, height = 16.dp),
                    color = KrailTheme.colors.softLabel,
                )
            }

            Image(
                painter = painterResource(Res.drawable.ic_arrow_down),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                colorFilter = ColorFilter.tint(KrailTheme.colors.softLabel),
                modifier = Modifier.size(18.dp).rotate(arrowRotation),
            )
        }
    }
}

// ── Content composable ────────────────────────────────────────────────────────

/**
 * Content area for an expanded stop section.
 *
 * Owns the per-stop line filter state locally. Filter resets automatically when this
 * composable leaves composition (i.e. when the section collapses), so re-expanding
 * always starts with all services shown.
 */
@Suppress("CyclomaticComplexMethod") // todo split this method
@Composable
private fun DepartureBoardAccordionContent(
    stopId: String,
    state: DeparturesState,
    onLoadPreviousDepartures: (String) -> Unit,
    onRefreshStop: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Empty string = no filter. Non-null String is guaranteed safe through rememberSaveable
    // (Bundle / SavedStateHandle only reliably round-trip primitives and String).
    var selectedLineKey by rememberSaveable { mutableStateOf("") }
    val selectedLine: String? = selectedLineKey.ifEmpty { null }

    var showPrevious by remember { mutableStateOf(false) }
    // Tracks whether a previous-departures fetch has been requested but isPreviousLoading
    // hasn't arrived yet. Without this, the "No previous departures" text flashes for one
    // frame before the repository sets isPreviousLoading = true.
    var previousRequested by remember { mutableStateOf(false) }

    // Clear the local guard once the repository acknowledges the request (loading starts/ends).
    LaunchedEffect(state.isPreviousLoading) {
        if (!state.isPreviousLoading) previousRequested = false
    }

    val filteredDepartures = remember(state.departures, selectedLine) {
        if (selectedLine == null) {
            state.departures
        } else {
            state.departures.filter { it.lineNumber == selectedLine }.toImmutableList()
        }
    }

    val filteredPreviousDepartures = remember(state.previousDepartures, selectedLine) {
        if (selectedLine == null) {
            state.previousDepartures
        } else {
            state.previousDepartures.filter { it.lineNumber == selectedLine }.toImmutableList()
        }
    }

    Column(modifier = modifier) {
        when {
            state.isLoading -> SectionLoadingContent()
            state.isError -> DeparturesErrorContent(onRetry = { onRefreshStop(stopId) })
            state.departures.isEmpty() -> SectionEmptyContent(hasActiveFilter = false)
            else -> {
                LinesServedRow(
                    departures = state.departures,
                    selectedLine = selectedLine,
                    onLineSelect = { selectedLineKey = it ?: "" },
                )
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                Spacer(modifier = Modifier.height(4.dp))

                // "Show previous / Hide previous" toggle
                ShowPreviousButton(
                    showPrevious = showPrevious,
                    onClick = {
                        showPrevious = !showPrevious
                        if (showPrevious && state.previousDepartures.isEmpty() && !state.isPreviousLoading) {
                            previousRequested = true
                            onLoadPreviousDepartures(stopId)
                        }
                    },
                )

                when {
                    // Previous fetch in progress (or requested but not yet acknowledged) —
                    // show inline loader above upcoming departures.
                    showPrevious && (state.isPreviousLoading || previousRequested) -> {
                        SectionLoadingContent()
                        when {
                            filteredDepartures.isEmpty() -> SectionEmptyContent(hasActiveFilter = true)
                            else -> DepartureRowList(departures = filteredDepartures)
                        }
                    }
                    // Previous fetched but none match current filter — show message then upcoming
                    showPrevious && filteredPreviousDepartures.isEmpty() -> {
                        Text(
                            text = "No previous departures in the last ${state.previousWindowMinutes} minutes.",
                            style = KrailTheme.typography.bodyMedium,
                            color = KrailTheme.colors.softLabel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        when {
                            filteredDepartures.isEmpty() -> SectionEmptyContent(hasActiveFilter = true)
                            else -> DepartureRowList(departures = filteredDepartures)
                        }
                    }
                    // Previous + upcoming combined — date labels flow as one unified list
                    showPrevious -> DepartureRowList(
                        departures = remember(filteredPreviousDepartures, filteredDepartures) {
                            (filteredPreviousDepartures + filteredDepartures).toImmutableList()
                        },
                    )
                    filteredDepartures.isEmpty() -> SectionEmptyContent(hasActiveFilter = true)
                    else -> DepartureRowList(departures = filteredDepartures)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Standalone composable (previews only) ────────────────────────────────────

/**
 * Accordion section for a single stop. Uses a plain [Column] layout.
 * Intended for standalone Compose Previews — the production saved trips screen uses
 * [departureBoardAccordionSection] instead for per-item lazy list keys.
 */
@Composable
fun DepartureBoardAccordionSection(
    entry: StopDepartureBoardEntry,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onLoadPreviousDepartures: (String) -> Unit = {},
    onRefreshStop: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DepartureBoardAccordionSectionHeader(
            entry = entry,
            isExpanded = isExpanded,
            onExpandChange = onExpandChange,
        )

        if (isExpanded) {
            Column(modifier = Modifier.background(KrailTheme.colors.surface)) {
                DepartureBoardAccordionContent(
                    stopId = entry.stopId,
                    state = entry.state,
                    onLoadPreviousDepartures = onLoadPreviousDepartures,
                    onRefreshStop = onRefreshStop,
                )
            }
        }
    }
}

// ── Show previous button ──────────────────────────────────────────────────────

@Composable
private fun ShowPreviousButton(
    showPrevious: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onClick) {
            Text(text = if (showPrevious) "Hide previous" else "Show previous")
        }
    }
}

// ── Private content slots ─────────────────────────────────────────────────────

@Composable
private fun SectionLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedDots(
            modifier = Modifier.size(width = 64.dp, height = 24.dp),
            color = KrailTheme.colors.onSurface,
        )
    }
}

@Composable
private fun SectionEmptyContent(hasActiveFilter: Boolean) {
    Text(
        text = if (hasActiveFilter) {
            "No departures match the selected line."
        } else {
            "No upcoming departures found."
        },
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

private const val TRANSPORT_MODE_TRAIN = "Train"
private const val TRANSPORT_MODE_BUS = "Bus"

private val previewTrainDepartures: ImmutableList<StopDeparture> = persistentListOf(
    StopDeparture(
        lineNumber = "T1",
        lineColorCode = "#F99D1C",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Liverpool via Strathfield",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2026-04-08T01:30:00Z",
        platformText = "Platform 4",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "T2",
        lineColorCode = "#0098CD",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Bondi Junction",
        departureTimeText = "11:33 AM",
        departureUtcDateTime = "2026-04-08T01:33:00Z",
        platformText = "Platform 7",
        isRealTime = false,
    ),
)

private val previewMixedDepartures: ImmutableList<StopDeparture> = persistentListOf(
    StopDeparture(
        lineNumber = "T1",
        lineColorCode = "#F99D1C",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Liverpool via Strathfield",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2026-04-08T01:30:00Z",
        platformText = "Platform 4",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "333",
        lineColorCode = "#00B5EF",
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = "Parramatta",
        departureTimeText = "11:35 AM",
        departureUtcDateTime = "2026-04-08T01:35:00Z",
        platformText = "Stand A",
        isRealTime = false,
    ),
)

@PreviewComponent
@Composable
private fun DepartureBoardStopSectionCollapsedPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardAccordionSection(
            entry = StopDepartureBoardEntry(
                stopId = "10111010",
                stopName = "Toongabbie Station",
                state = DeparturesState(isLoading = false, departures = previewTrainDepartures),
            ),
            isExpanded = false,
            onExpandChange = {},
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopSectionExpandedTrainPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardAccordionSection(
            entry = StopDepartureBoardEntry(
                stopId = "10111010",
                stopName = "Toongabbie Station",
                state = DeparturesState(isLoading = false, departures = previewTrainDepartures),
            ),
            isExpanded = true,
            onExpandChange = {},
        )
    }
}

@Preview(name = "Expanded — mixed modes")
@Composable
private fun DepartureBoardStopSectionMixedPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureBoardAccordionSection(
            entry = StopDepartureBoardEntry(
                stopId = "10111010",
                stopName = "Central Station",
                state = DeparturesState(isLoading = false, departures = previewMixedDepartures),
            ),
            isExpanded = true,
            onExpandChange = {},
        )
    }
}

@Preview(name = "Loading state")
@Composable
private fun DepartureBoardStopSectionLoadingPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardAccordionSection(
            entry = StopDepartureBoardEntry(
                stopId = "10111010",
                stopName = "Toongabbie Station",
                state = DeparturesState(isLoading = true),
            ),
            isExpanded = true,
            onExpandChange = {},
        )
    }
}
