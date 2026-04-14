@file:Suppress("MagicNumber", "CyclomaticComplexMethod")

package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots

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
 * @param maxItems                 Maximum departure rows to show. `null` = show all.
 */
@Composable
internal fun DepartureBoardBody(
    state: DeparturesState,
    onRetry: () -> Unit,
    onLoadPreviousDepartures: () -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int? = null,
) {
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

    Column(modifier = modifier) {
        when {
            state.isLoading -> DepartureBoardLoadingContent()

            state.isError -> DeparturesErrorContent(onRetry = onRetry)

            else -> {
                if (state.departures.isNotEmpty()) {
                    LinesServedRow(
                        departures = state.departures,
                        selectedLine = selectedLine,
                        onLineSelect = { selectedLineKey = it ?: "" },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(
                        onClick = {
                            showPrevious = !showPrevious
                            if (showPrevious &&
                                state.previousDepartures.isEmpty() &&
                                !state.isPreviousLoading
                            ) {
                                previousRequested = true
                                onLoadPreviousDepartures()
                            }
                        },
                    ) {
                        Text(
                            text = if (showPrevious) {
                                "Hide previous departures"
                            } else {
                                "Show previous departures"
                            },
                        )
                    }
                }

                when {
                    // Previous fetch in-flight (or requested but not yet acknowledged) —
                    // show inline loader above any upcoming departures.
                    showPrevious && (state.isPreviousLoading || previousRequested) -> {
                        DepartureBoardLoadingContent()
                        when {
                            filteredDepartures.isEmpty() -> DepartureBoardEmptyContent(hasActiveFilter = true)
                            else -> DepartureRowList(departures = filteredDepartures, maxItems = maxItems)
                        }
                    }

                    // Previous fetched but none match the current filter —
                    // show message then the upcoming list.
                    showPrevious && filteredPreviousDepartures.isEmpty() -> {
                        Text(
                            text = "No previous departures in the last ${state.previousWindowMinutes} minutes.",
                            style = KrailTheme.typography.bodyMedium,
                            color = KrailTheme.colors.softLabel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        when {
                            filteredDepartures.isEmpty() -> DepartureBoardEmptyContent(hasActiveFilter = true)
                            else -> DepartureRowList(departures = filteredDepartures, maxItems = maxItems)
                        }
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
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun DepartureBoardLoadingContent() {
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
private fun DepartureBoardEmptyContent(hasActiveFilter: Boolean) {
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
