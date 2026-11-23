@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.AnimatedDots
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.crowding.LocalExpandedDepartures
import xyz.ksharma.krail.trip.planner.ui.components.crowding.rememberExpandedDepartures

private val LOADING_DOTS_WIDTH = 64.dp

/** Which of its three faces the board body is showing, so a change from one to another can fade. */
private enum class BoardBodyPhase { Loading, Error, Rows }

/** The colour a host gave the board's secondary text, or null for the default. See [departureSecondaryText]. */
internal val LocalDepartureSecondaryText = compositionLocalOf<Color?> { null }

/** The board's secondary text: the host's colour from [DepartureBoardBody]'s `secondaryTextColor`, else `softLabel` as ever. */
@Composable
internal fun departureSecondaryText(): Color = LocalDepartureSecondaryText.current ?: KrailTheme.colors.softLabel

/**
 * Shared departure board content area used by both [DepartureBoardStopCard] (bottom-sheet card)
 * and the accordion sections on the Saved Trips screen.
 *
 * Owns all per-surface state:
 *  - Line filter (persisted via [rememberSaveable] so it survives config changes while visible)
 *  - "Show previous departures" toggle
 *  - `previousRequested` guard that prevents a one-frame flash of "No previous departures"
 *    before [DeparturesState.isPreviousLoading] becomes `true`.
 *
 * @param state                    Current [DeparturesState].
 * @param onRetry                  Called when the user taps "Retry" on the error state.
 * @param onLoadPreviousDepartures Called when the user first opens the "Show previous" panel
 *                                 and no previous departures have been fetched yet.
 * @param onLineFilterChange       Optional: called when the user selects or clears a line filter.
 *                                 Receives the affected line number, its transport mode, and
 *                                 whether the filter was applied (`true`) or cleared (`false`).
 *                                 The line number is always the affected line — even on deselect.
 * @param onShowPreviousToggle     Optional: called on every "Show / Hide previous" toggle.
 *                                 `true` = user showed the panel; `false` = user hid it.
 * @param maxItems                 Maximum departure rows to show. `null` = show all.
 * @param secondaryTextColor       The colour of the board's secondary text (a row's destination, the scheduled time beside a
 *                                 deviation, the empty and no previous messages). `null` keeps `softLabel`, which every
 *                                 host that draws the board on the page's own surface wants. A host that draws it on a tint
 *                                 `softLabel` does not read on (the map's stop card, 3.98 to 1 in light mode and 3.28 in dark)
 *                                 passes the text colour it already draws the rest of the card in.
 */
@Composable
internal fun DepartureBoardBody(
    state: DeparturesState,
    onRetry: () -> Unit,
    onLoadPreviousDepartures: () -> Unit,
    modifier: Modifier = Modifier,
    onLineFilterChange: ((lineNumber: String?, transportMode: String?, selected: Boolean) -> Unit)? = null,
    onShowPreviousToggle: ((show: Boolean) -> Unit)? = null,
    maxItems: Int? = null,
    crossfadePhases: Boolean = false,
    reduceMotion: Boolean = false,
    secondaryTextColor: Color? = null,
) {
    // Hoisted above every branch that draws rows, so opened rows survive the branch changing.
    CompositionLocalProvider(
        LocalExpandedDepartures provides rememberExpandedDepartures(),
        LocalDepartureSecondaryText provides secondaryTextColor,
    ) {
        DepartureBoardBodyContent(
            state = state,
            onRetry = onRetry,
            onLoadPreviousDepartures = onLoadPreviousDepartures,
            modifier = modifier,
            onLineFilterChange = onLineFilterChange,
            onShowPreviousToggle = onShowPreviousToggle,
            maxItems = maxItems,
            crossfadePhases = crossfadePhases,
            reduceMotion = reduceMotion,
        )
    }
}

@Composable
private fun DepartureBoardBodyContent(
    state: DeparturesState,
    onRetry: () -> Unit,
    onLoadPreviousDepartures: () -> Unit,
    modifier: Modifier,
    onLineFilterChange: ((lineNumber: String?, transportMode: String?, selected: Boolean) -> Unit)?,
    onShowPreviousToggle: ((show: Boolean) -> Unit)?,
    maxItems: Int?,
    crossfadePhases: Boolean,
    reduceMotion: Boolean,
) {
    val dim = KrailTheme.dimensions
    // Empty string = no filter. rememberSaveable round-trips safely as a String primitive.
    var selectedLineKey by rememberSaveable { mutableStateOf("") }
    val selectedLine: String? = selectedLineKey.ifEmpty { null }

    var showPrevious by rememberSaveable { mutableStateOf(false) }
    // Guards against a one-frame flash of "No previous departures" before the repository
    // acknowledges the request by setting isPreviousLoading = true.
    var previousRequested by rememberSaveable { mutableStateOf(false) }

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

    val phase = when {
        state.isLoading -> BoardBodyPhase.Loading
        state.isError -> BoardBodyPhase.Error
        else -> BoardBodyPhase.Rows
    }
    Column(modifier = modifier) {
        // With [crossfadePhases] the loading dots, the failure and the rows fade into one another, so the board never pops
        // from one to the next. Without it the phases swap as they always have, for the boards that never asked.
        Crossfade(
            targetState = phase,
            animationSpec = if (crossfadePhases && !reduceMotion) tween(MapMotion.CROSSFADE_MS) else snap(),
            label = "board-phase",
        ) { shown ->
            Column {
                when (shown) {
                    BoardBodyPhase.Loading -> DepartureBoardLoadingContent()

                    BoardBodyPhase.Error -> DeparturesErrorContent(onRetry = onRetry)

                    BoardBodyPhase.Rows -> DepartureBoardSuccessContent(
                        state = state,
                        selectedLine = selectedLine,
                        showPrevious = showPrevious,
                        previousRequested = previousRequested,
                        filteredDepartures = filteredDepartures,
                        filteredPreviousDepartures = filteredPreviousDepartures,
                        maxItems = maxItems,
                        onLineSelect = { newLine ->
                            // Capture the previously selected line BEFORE updating state so
                            // we can report it when the filter is being cleared (deselected).
                            val previousLine = selectedLine
                            selectedLineKey = newLine ?: ""
                            val affectedLine = newLine ?: previousLine
                            val transportMode = affectedLine?.let { line ->
                                state.departures.firstOrNull { it.lineNumber == line }?.transportModeName
                            }
                            onLineFilterChange?.invoke(affectedLine, transportMode, newLine != null)
                        },
                        onTogglePrevious = {
                            showPrevious = !showPrevious
                            onShowPreviousToggle?.invoke(showPrevious)
                            if (showPrevious &&
                                state.previousDepartures.isEmpty() &&
                                !state.isPreviousLoading
                            ) {
                                previousRequested = true
                                onLoadPreviousDepartures()
                            }
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(dim.spacingM))
    }
}

@Composable
private fun DepartureBoardSuccessContent(
    state: DeparturesState,
    selectedLine: String?,
    showPrevious: Boolean,
    previousRequested: Boolean,
    filteredDepartures: ImmutableList<StopDeparture>,
    filteredPreviousDepartures: ImmutableList<StopDeparture>,
    maxItems: Int?,
    onLineSelect: (String?) -> Unit,
    onTogglePrevious: () -> Unit,
) {
    val dim = KrailTheme.dimensions
    if (state.departures.isNotEmpty()) {
        LinesServedRow(
            departures = state.departures,
            selectedLine = selectedLine,
            onLineSelect = onLineSelect,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dim.spacingM, bottom = dim.spacingL),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onTogglePrevious) {
            Text(
                text = if (showPrevious) {
                    "Hide previous departures"
                } else {
                    "Show previous departures"
                },
            )
        }
    }

    DepartureBoardDeparturesSection(
        state = state,
        showPrevious = showPrevious,
        previousRequested = previousRequested,
        filteredDepartures = filteredDepartures,
        filteredPreviousDepartures = filteredPreviousDepartures,
        maxItems = maxItems,
    )
}

@Composable
private fun DepartureBoardDeparturesSection(
    state: DeparturesState,
    showPrevious: Boolean,
    previousRequested: Boolean,
    filteredDepartures: ImmutableList<StopDeparture>,
    filteredPreviousDepartures: ImmutableList<StopDeparture>,
    maxItems: Int?,
) {
    val dim = KrailTheme.dimensions
    when {
        // Previous fetch in-flight (or requested but not yet acknowledged) —
        // show inline loader above any upcoming departures.
        showPrevious && (state.isPreviousLoading || previousRequested) -> {
            DepartureBoardLoadingContent()
            UpcomingDeparturesOrEmpty(filteredDepartures = filteredDepartures, maxItems = maxItems)
        }

        // Previous fetched but none match the current filter —
        // show message then the upcoming list.
        showPrevious && filteredPreviousDepartures.isEmpty() -> {
            Text(
                text = "No previous departures in the last ${state.previousWindowMinutes} minutes.",
                style = KrailTheme.typography.bodyMedium,
                color = departureSecondaryText(),
                modifier = Modifier.padding(horizontal = dim.spacingXL, vertical = dim.spacingL),
            )
            UpcomingDeparturesOrEmpty(filteredDepartures = filteredDepartures, maxItems = maxItems)
        }

        // Previous + upcoming combined as one unified list with date labels.
        showPrevious -> DepartureRowList(
            departures = remember(filteredPreviousDepartures, filteredDepartures) {
                (filteredPreviousDepartures + filteredDepartures).toImmutableList()
            },
            maxItems = maxItems,
        )

        state.departures.isEmpty() -> DepartureBoardEmptyContent(hasActiveFilter = false)
        filteredDepartures.isEmpty() -> DepartureBoardEmptyContent(hasActiveFilter = true)
        else -> DepartureRowList(departures = filteredDepartures, maxItems = maxItems)
    }
}

@Composable
private fun UpcomingDeparturesOrEmpty(
    filteredDepartures: ImmutableList<StopDeparture>,
    maxItems: Int?,
) {
    when {
        filteredDepartures.isEmpty() -> DepartureBoardEmptyContent(hasActiveFilter = true)
        else -> DepartureRowList(departures = filteredDepartures, maxItems = maxItems)
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun DepartureBoardLoadingContent() {
    val dim = KrailTheme.dimensions
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dim.spacingXXXL),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedDots(
            modifier = Modifier.size(width = LOADING_DOTS_WIDTH, height = dim.spacingXXXL),
            color = KrailTheme.colors.onSurface,
        )
    }
}

@Composable
private fun DepartureBoardEmptyContent(hasActiveFilter: Boolean) {
    val dim = KrailTheme.dimensions
    Text(
        text = if (hasActiveFilter) {
            "No departures match the selected line."
        } else {
            "No upcoming departures found."
        },
        style = KrailTheme.typography.bodyMedium,
        color = departureSecondaryText(),
        modifier = Modifier.padding(horizontal = dim.spacingXL, vertical = dim.spacingXL),
    )
}
