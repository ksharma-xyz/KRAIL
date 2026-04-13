package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import xyz.ksharma.krail.core.log.log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots

/**
 * Expand/collapse card for a stop's live departure board.
 *
 * The card header is rendered as a [SubtleButton] so it uses the app's established
 * button visual language and integrates a scale-press animation via [scalingKlickable].
 *
 * Supports two modes:
 * - **Uncontrolled** (default): manages its own expanded state via [rememberSaveable].
 *   Filter state is managed internally and persists across collapse/expand and rotation.
 *   Used in the stop details bottom sheet where only one card exists.
 * - **Controlled**: caller drives expansion via [isExpanded] + [onExpandChange].
 *   Used in the saved trips accordion where only one card can be open at a time.
 *
 * @param stopId          NSW Transport stop ID, e.g. "10111010".
 * @param state           Current [DeparturesState] from the ViewModel or repository.
 * @param onEvent         Callback to send events to the ViewModel (only used in uncontrolled mode).
 * @param isExpanded      When non-null, puts the card in controlled mode with this expansion state.
 * @param onExpandChange  When non-null, called with the desired new expanded state (controlled mode).
 * @param title           Card header label. Defaults to "Departure Board".
 * @param maxItems        Maximum number of departure rows to show. `null` means show all.
 */
@Composable
fun DepartureBoardStopCard(
    stopId: String,
    state: DeparturesState,
    onEvent: (DeparturesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null,
    onExpandChange: ((Boolean) -> Unit)? = null,
    title: String = "Departure Board",
    maxItems: Int? = null,
) {
    var internalExpanded by rememberSaveable { mutableStateOf(false) }
    val expanded = isExpanded ?: internalExpanded

    // Filter state — keyed to stopId so rotating the device while a filter is active
    // restores the same selection. Empty string is the "no filter" sentinel (primitives only
    // are safe across process death / configuration changes via rememberSaveable).
    var selectedLineKey by rememberSaveable(key = "filter_$stopId") { mutableStateOf("") }
    val selectedLine: String? = selectedLineKey.ifEmpty { null }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow-rotation",
    )

    LaunchedEffect(expanded, stopId) {
        if (isExpanded == null) {
            // Uncontrolled mode: start polling on expand, stop it on collapse.
            if (expanded) {
                log("[DEPARTURES] UI card EXPANDED stopId=$stopId — sending LoadDepartures")
                onEvent(DeparturesUiEvent.LoadDepartures(stopId))
            } else {
                log("[DEPARTURES] UI card COLLAPSED stopId=$stopId — sending StopPolling")
                onEvent(DeparturesUiEvent.StopPolling)
            }
        }
    }

    // Lifecycle events — lets us see when the Activity/Fragment goes to background / returns,
    // and correlate with repository polling behaviour in the logs.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        log("[DEPARTURES] UI ON_PAUSE stopId=$stopId expanded=$expanded")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        log("[DEPARTURES] UI ON_STOP stopId=$stopId expanded=$expanded — polling continues in repo scope")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        log("[DEPARTURES] UI ON_START stopId=$stopId expanded=$expanded")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        log("[DEPARTURES] UI ON_RESUME stopId=$stopId expanded=$expanded")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(KrailTheme.colors.surface),
    ) {
        // ── Header — SubtleButton acts as the expand/collapse toggle ─────────
        CardHeader(
            title = title,
            expanded = expanded,
            silentLoading = state.silentLoading,
            arrowRotation = arrowRotation,
            onClick = {
                val next = !expanded
                if (onExpandChange != null) onExpandChange(next) else internalExpanded = next
            },
        )

        // ── Expanded content ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) +
                fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) +
                fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            DeparturesBody(
                state = state,
                isExpanded = isExpanded,
                onEvent = onEvent,
                stopId = stopId,
                selectedLine = selectedLine,
                onLineSelect = { selectedLineKey = it ?: "" },
                maxItems = maxItems,
            )
        }
    }
}

@Composable
private fun CardHeader(
    title: String,
    expanded: Boolean,
    silentLoading: Boolean,
    arrowRotation: Float,
    onClick: () -> Unit,
) {
    SubtleButton(
        onClick = onClick,
        dimensions = ButtonDefaults.largeButtonSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = KrailTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (expanded && silentLoading) {
                    AnimatedDots(
                        modifier = Modifier.size(width = 32.dp, height = 16.dp),
                        color = KrailTheme.colors.softLabel,
                    )
                }
                Image(
                    painter = painterResource(Res.drawable.ic_arrow_down),
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    colorFilter = ColorFilter.tint(KrailTheme.colors.softLabel),
                    modifier = Modifier.size(18.dp).rotate(arrowRotation),
                )
            }
        }
    }
}

@Composable
private fun DeparturesBody(
    state: DeparturesState,
    isExpanded: Boolean?,
    onEvent: (DeparturesUiEvent) -> Unit,
    stopId: String,
    selectedLine: String?,
    onLineSelect: (String?) -> Unit,
    maxItems: Int?,
) {
    Column {
        when {
            state.isLoading -> LoadingContent()
            state.isError -> DeparturesErrorContent(
                onRetry = { if (isExpanded == null) onEvent(DeparturesUiEvent.Refresh) },
            )
            state.departures.isEmpty() -> EmptyContent()
            else -> DeparturesSuccessContent(
                state = state,
                onEvent = onEvent,
                stopId = stopId,
                selectedLine = selectedLine,
                onLineSelect = onLineSelect,
                maxItems = maxItems,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DeparturesSuccessContent(
    state: DeparturesState,
    onEvent: (DeparturesUiEvent) -> Unit,
    stopId: String,
    selectedLine: String?,
    onLineSelect: (String?) -> Unit,
    maxItems: Int?,
    modifier: Modifier = Modifier,
) {
    var showPrevious by remember { mutableStateOf(false) }

    val filteredDepartures: ImmutableList<StopDeparture> = remember(state.departures, selectedLine) {
        if (selectedLine == null) {
            state.departures
        } else {
            state.departures.filter { it.lineNumber == selectedLine }.toImmutableList()
        }
    }

    val filteredPreviousDepartures: ImmutableList<StopDeparture> = remember(state.previousDepartures, selectedLine) {
        if (selectedLine == null) {
            state.previousDepartures
        } else {
            state.previousDepartures.filter { it.lineNumber == selectedLine }.toImmutableList()
        }
    }

    Column(modifier = modifier) {
        LinesServedRow(
            departures = state.departures,
            selectedLine = selectedLine,
            onLineSelect = onLineSelect,
        )
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        // "Show previous / Hide previous" toggle
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                onClick = {
                    showPrevious = !showPrevious
                    if (showPrevious && state.previousDepartures.isEmpty() && !state.isPreviousLoading) {
                        onEvent(DeparturesUiEvent.LoadPreviousDepartures(stopId))
                    }
                },
            ) {
                Text(text = if (showPrevious) "Hide previous" else "Show previous")
            }
        }

        PreviousDeparturesSection(
            showPrevious = showPrevious,
            isPreviousLoading = state.isPreviousLoading,
            previousWindowMinutes = state.previousWindowMinutes,
            filteredDepartures = filteredDepartures,
            filteredPreviousDepartures = filteredPreviousDepartures,
            maxItems = maxItems,
        )
    }
}

@Composable
private fun PreviousDeparturesSection(
    showPrevious: Boolean,
    isPreviousLoading: Boolean,
    previousWindowMinutes: Long,
    filteredDepartures: ImmutableList<StopDeparture>,
    filteredPreviousDepartures: ImmutableList<StopDeparture>,
    maxItems: Int?,
) {
    Column {
        when {
            showPrevious && isPreviousLoading -> {
                LoadingContent()
                if (filteredDepartures.isEmpty()) {
                    FilterEmptyContent()
                } else {
                    DepartureRowList(departures = filteredDepartures, maxItems = maxItems)
                }
            }
            showPrevious && filteredPreviousDepartures.isEmpty() -> {
                Text(
                    text = "No previous departures in the last $previousWindowMinutes minutes.",
                    style = KrailTheme.typography.bodyMedium,
                    color = KrailTheme.colors.softLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                if (filteredDepartures.isEmpty()) {
                    FilterEmptyContent()
                } else {
                    DepartureRowList(departures = filteredDepartures, maxItems = maxItems)
                }
            }
            showPrevious -> DepartureRowList(
                departures = remember(filteredPreviousDepartures, filteredDepartures) {
                    (filteredPreviousDepartures + filteredDepartures).toImmutableList()
                },
                maxItems = maxItems,
            )
            filteredDepartures.isEmpty() -> FilterEmptyContent()
            else -> DepartureRowList(departures = filteredDepartures, maxItems = maxItems)
        }
    }
}

// ── Private content slots ─────────────────────────────────────────────────────

@Composable
internal fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedDots(
            modifier = Modifier.size(width = 64.dp, height = 24.dp),
            color = KrailTheme.colors.onSurface,
        )
    }
}

@Composable
private fun EmptyContent() {
    Text(
        text = "No upcoming departures found.",
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    )
}

@Composable
private fun FilterEmptyContent() {
    Text(
        text = "No departures match the selected line.",
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    )
}
