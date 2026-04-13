package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
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


    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow-rotation",
    )
    val outerPadding by animateDpAsState(
        targetValue = if (expanded) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "outer-padding",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "corner-radius",
    )
    val animatedShape = RoundedCornerShape(cornerRadius)

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
            .padding(horizontal = outerPadding)
            .clip(animatedShape)
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
            DepartureBoardBody(
                state = state,
                onRetry = { if (isExpanded == null) onEvent(DeparturesUiEvent.Refresh) },
                onLoadPreviousDepartures = { onEvent(DeparturesUiEvent.LoadPreviousDepartures(stopId)) },
                maxItems = maxItems,
                modifier = Modifier.padding(top = 4.dp),
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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

// region Previews

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
private fun DepartureBoardStopCardCollapsedPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(),
            onEvent = {},
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopCardLoadingPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(isLoading = true),
            onEvent = {},
            isExpanded = true,
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopCardLoadedTrainPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(
                isLoading = false,
                departures = previewTrainDepartures,
            ),
            onEvent = {},
            isExpanded = true,
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopCardLoadedBusPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(
                isLoading = false,
                departures = previewBusDepartures,
            ),
            onEvent = {},
            isExpanded = true,
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopCardLoadedFerryPreview() {
    PreviewTheme(KrailThemeStyle.Ferry) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(
                isLoading = false,
                departures = previewFerryDepartures,
            ),
            onEvent = {},
            isExpanded = true,
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopCardErrorPreview() {
    PreviewTheme(KrailThemeStyle.Metro) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(
                isLoading = false,
                isError = true,
            ),
            onEvent = {},
            isExpanded = true,
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopCardEmptyPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(
                isLoading = false,
                departures = persistentListOf(),
            ),
            onEvent = {},
            isExpanded = true,
        )
    }
}

// endregion
