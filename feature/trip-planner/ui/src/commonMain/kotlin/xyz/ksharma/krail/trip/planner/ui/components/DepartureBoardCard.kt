@file:Suppress("MagicNumber", "TooManyFunctions", "CyclomaticComplexMethod")

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
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
fun DepartureBoardCard(
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

    val filteredDepartures: ImmutableList<StopDeparture> = remember(state.departures, selectedLine) {
        if (selectedLine == null) {
            state.departures
        } else {
            state.departures.filter { it.lineNumber == selectedLine }.toImmutableList()
        }
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow-rotation",
    )

    LaunchedEffect(expanded, stopId) {
        if (expanded && isExpanded == null) {
            onEvent(DeparturesUiEvent.LoadDepartures(stopId))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(KrailTheme.colors.surface),
    ) {
        // ── Header — SubtleButton acts as the expand/collapse toggle ─────────
        SubtleButton(
            onClick = {
                val next = !expanded
                if (onExpandChange != null) onExpandChange(next) else internalExpanded = next
            },
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
                    if (expanded && state.silentLoading) {
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

        // ── Expanded content ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) +
                fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) +
                fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            Column {
                when {
                    state.isLoading -> LoadingContent()
                    state.isError -> ErrorContent(
                        onRetry = { if (isExpanded == null) onEvent(DeparturesUiEvent.Refresh) },
                    )
                    state.departures.isEmpty() -> EmptyContent()
                    else -> {
                        LinesServedRow(
                            departures = state.departures,
                            selectedLine = selectedLine,
                            onLineSelect = { selectedLineKey = it ?: "" },
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        when {
                            filteredDepartures.isEmpty() -> FilterEmptyContent()
                            else -> DepartureRowList(
                                departures = filteredDepartures,
                                maxItems = maxItems,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── Private content slots ─────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
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
private fun ErrorContent(onRetry: () -> Unit) {
    ErrorMessage(
        title = "Couldn't load departures",
        message = "Check your connection and try again.",
        actionData = ActionData(
            actionText = "Retry",
            onActionClick = onRetry,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
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

// ── Previews ──────────────────────────────────────────────────────────────────

private const val TRANSPORT_MODE_TRAIN = "Train"
private const val TRANSPORT_MODE_BUS = "Bus"
private const val TRANSPORT_MODE_FERRY = "Ferry"
private const val SAMPLE_DATE_PREFIX = "2026-04-08T01:"

private val previewTrainDepartures = persistentListOf(
    StopDeparture(
        lineNumber = "T1",
        lineColorCode = "#F99D1C",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Liverpool via Strathfield",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}30:00Z",
        platformText = "Platform 4",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "T2",
        lineColorCode = "#0098CD",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Bondi Junction",
        departureTimeText = "11:33 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}33:00Z",
        platformText = "Platform 7",
        isRealTime = false,
    ),
    StopDeparture(
        lineNumber = "T4",
        lineColorCode = "#005AA3",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Illawarra via Sydenham",
        departureTimeText = "11:36 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}36:00Z",
        platformText = "Platform 2",
        isRealTime = true,
    ),
)

private val previewBusDepartures = persistentListOf(
    StopDeparture(
        lineNumber = "333",
        lineColorCode = "#00B5EF",
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = "Parramatta Interchange",
        departureTimeText = "11:40 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}40:00Z",
        platformText = "Stand B",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "370",
        lineColorCode = "#00B5EF",
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = "Coogee Beach",
        departureTimeText = "11:45 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}45:00Z",
        platformText = null,
        isRealTime = false,
    ),
)

private val previewFerryDepartures = persistentListOf(
    StopDeparture(
        lineNumber = "F1",
        lineColorCode = "#00774B",
        transportModeName = TRANSPORT_MODE_FERRY,
        destinationName = "Manly Wharf",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}30:00Z",
        platformText = "Wharf 1",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "F2",
        lineColorCode = "#144734",
        transportModeName = TRANSPORT_MODE_FERRY,
        destinationName = "Parramatta River",
        departureTimeText = "11:45 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}45:00Z",
        platformText = "Wharf 3",
        isRealTime = false,
    ),
)

@Preview(name = "Collapsed", showBackground = true)
@Composable
private fun DepartureBoardCardCollapsedPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardCard(
            stopId = "10101010",
            state = DeparturesState(),
            onEvent = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun DepartureBoardCardLoadingPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        Column { LoadingContent() }
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardCardLoadedTrainPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        Column {
            LinesServedRow(
                departures = previewTrainDepartures,
                selectedLine = "T2",
                onLineSelect = {},
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            DepartureRowList(departures = previewTrainDepartures)
        }
    }
}

@Preview(name = "Loaded — Bus theme", showBackground = true)
@Composable
private fun DepartureBoardCardLoadedBusPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        Column {
            LinesServedRow(
                departures = previewBusDepartures,
                selectedLine = null,
                onLineSelect = {},
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            DepartureRowList(departures = previewBusDepartures)
        }
    }
}

@Preview(name = "Error state", showBackground = true)
@Composable
private fun DepartureBoardCardErrorPreview() {
    PreviewTheme(KrailThemeStyle.Metro) {
        Column { ErrorContent(onRetry = {}) }
    }
}

@Preview(name = "Ferry theme", showBackground = true)
@Composable
private fun DepartureBoardCardFerryPreview() {
    PreviewTheme(KrailThemeStyle.Ferry) {
        Column {
            LinesServedRow(
                departures = previewFerryDepartures,
                selectedLine = null,
                onLineSelect = {},
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            DepartureRowList(departures = previewFerryDepartures)
        }
    }
}
