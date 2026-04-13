package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_filter
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

// Stagger timing constants
private const val BADGE_STAGGER_MS = 55           // delay between each badge
private const val BADGE_ENTER_DURATION_MS = 220   // how long each badge takes to appear
private const val BADGE_EXIT_DURATION_MS = 160    // how long each badge takes to disappear
private const val BADGE_ENTER_OFFSET_MS = 80      // head-start before badges start appearing
private const val CONTAINER_ENTER_MS = 250        // badge container expand duration

/**
 * A single horizontally-scrollable row containing a collapsible filter button and mode groups.
 *
 * **Layout**: everything lives in one `Row` + `horizontalScroll` — no second row, no column.
 * - Left: a filter pill button. Collapsed → icon only. Expanded → icon + "Lines" text (the button
 *   itself shrinks/grows via [animateContentSize] inside [SubtleButton]).
 * - Right (when expanded): one group per transport mode. Each group is a `[ModeIconButton] [badges]`
 *   pair. The mode icon is the [TransportModeIcon] circle — tapping it collapses/expands its badges.
 *
 * **Badge animations** (staggered for a satisfying "one-by-one" effect):
 * - Expand: badges grow + fade in left→right (each starts 55 ms after the previous).
 *   Origin is the left edge so they appear to emerge from the mode icon.
 * - Collapse: badges shrink + fade out right→left.
 *   Container starts collapsing immediately (no delay) so the next mode group repositions smoothly
 *   at the same time — there is no "snap" jump.
 *
 * **Why `Row` + `horizontalScroll` (not `LazyRow`)**:
 * `LazyRow` uses `SubcomposeLayout` which bypasses `AnimatedVisibility(expandHorizontally)`.
 *
 * @param departures   Full (unfiltered) list — used only to derive unique lines per mode.
 * @param selectedLine Currently active filter line, or `null` for "show all".
 * @param onLineSelect Called with the tapped line, or `null` when toggling the selection off.
 */
@Composable
internal fun LinesServedRow(
    departures: ImmutableList<StopDeparture>,
    selectedLine: String?,
    onLineSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modeGroups = departures
        .distinctBy { it.lineNumber }
        .groupBy { it.transportModeName }
        .entries
        .sortedBy { (modeName, _) ->
            TransportMode.all.find { it.name == modeName }?.priority ?: Int.MAX_VALUE
        }

    // Whether the full filter row (mode groups) is visible.
    var filterExpanded by rememberSaveable { mutableStateOf(false) }

    // Modes that are individually collapsed. Reset when the filter reopens.
    // Does not need to survive rotation — collapsing all modes on rotation is fine UX.
    var collapsedModes by remember { mutableStateOf(emptySet<String>()) }

    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Filter pill — shrinks to icon-only when closed ────────────────────
        SubtleButton(
            onClick = {
                val opening = !filterExpanded
                filterExpanded = opening
                if (opening) collapsedModes = emptySet() // all modes start expanded on open
            },
            dimensions = ButtonDefaults.mediumButtonSize(),
        ) {
            Row(
                // animateContentSize makes the SubtleButton's Box measure the intermediate width,
                // so the pill background/clip follow the animation automatically.
                modifier = Modifier.animateContentSize(tween(250)),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_filter),
                    contentDescription = if (filterExpanded) "Hide line filter" else "Show line filter",
                    colorFilter = ColorFilter.tint(ButtonDefaults.subtleButtonColors().contentColor),
                )
                if (filterExpanded) {
                    Text(text = "Lines")
                }
            }
        }

        // ── All mode groups — slide in / out as one block ─────────────────────
        AnimatedVisibility(
            visible = filterExpanded,
            enter = expandHorizontally(tween(300), expandFrom = Alignment.Start) + fadeIn(tween(200)),
            exit = shrinkHorizontally(tween(250), shrinkTowards = Alignment.Start) + fadeOut(tween(150)),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                modeGroups.forEach { (modeName, lines) ->
                    key(modeName) {
                        val isExpanded = !collapsedModes.contains(modeName)
                        val transportMode = TransportMode.all.find { it.name == modeName }

                        if (transportMode != null) {
                            // Group Row: icon + badge container.
                            // Outer spacedBy(8dp) applies between MODE GROUPS, not inside a group,
                            // so when badges collapse the container starts narrowing immediately
                            // and the next group repositions smoothly.
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                ModeIconButton(
                                    transportMode = transportMode,
                                    expanded = isExpanded,
                                    onClick = {
                                        val wasExpanded = isExpanded
                                        collapsedModes = if (wasExpanded) {
                                            collapsedModes + modeName
                                        } else {
                                            collapsedModes - modeName
                                        }
                                        // Clear filter if hiding its mode so it's never invisible.
                                        if (wasExpanded && lines.any { it.lineNumber == selectedLine }) {
                                            onLineSelect(null)
                                        }
                                    },
                                )

                                // Badge container — exits with NO delay so siblings reposition
                                // left as soon as collapse begins (not after badges fully fade).
                                val containerExitMs = maxOf(
                                    CONTAINER_ENTER_MS,
                                    (lines.size - 1) * BADGE_STAGGER_MS + BADGE_EXIT_DURATION_MS,
                                )
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandHorizontally(
                                        tween(CONTAINER_ENTER_MS),
                                        expandFrom = Alignment.Start,
                                    ),
                                    exit = shrinkHorizontally(
                                        tween(containerExitMs), // intentionally no delayMillis
                                        shrinkTowards = Alignment.Start,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        lines.forEachIndexed { index, departure ->
                                            val isSelected = selectedLine == departure.lineNumber
                                            val enterDelay = BADGE_ENTER_OFFSET_MS + index * BADGE_STAGGER_MS
                                            val exitDelay = (lines.size - 1 - index) * BADGE_STAGGER_MS

                                            AnimatedVisibility(
                                                visible = isExpanded,
                                                enter = scaleIn(
                                                    tween(BADGE_ENTER_DURATION_MS, enterDelay),
                                                    initialScale = 0.4f,
                                                    transformOrigin = TransformOrigin(0f, 0.5f),
                                                ) + fadeIn(tween(BADGE_ENTER_DURATION_MS, enterDelay)),
                                                exit = scaleOut(
                                                    tween(BADGE_EXIT_DURATION_MS, exitDelay),
                                                    targetScale = 0.4f,
                                                    transformOrigin = TransformOrigin(0f, 0.5f),
                                                ) + fadeOut(tween(BADGE_EXIT_DURATION_MS, exitDelay)),
                                            ) {
                                                TransportModeBadge(
                                                    badgeText = departure.lineNumber,
                                                    backgroundColor = departure.lineColorCode.hexToComposeColor(),
                                                    size = BadgeSize.Large,
                                                    selected = isSelected,
                                                    onClick = {
                                                        onLineSelect(if (isSelected) null else departure.lineNumber)
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The transport mode icon circle used as a tap target to expand/collapse that mode's badges.
 *
 * Springs to 1.2× scale when badges are expanded so you can tell at a glance which modes are open.
 * The white border on the icon ensures it pops against the card background in both themes.
 */
@Composable
private fun ModeIconButton(
    transportMode: TransportMode,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (expanded) 1.2f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "mode-icon-scale-${transportMode.name}",
    )

    TransportModeIcon(
        transportMode = transportMode,
        size = TransportModeIconSize.Small,
        displayBorder = true,
        borderColor = androidx.compose.ui.graphics.Color.White,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(indication = null, interactionSource = null) { onClick() },
    )
}

// region Previews

private val previewDepartures: ImmutableList<StopDeparture> = persistentListOf(
    StopDeparture(
        lineNumber = "T1",
        lineColorCode = "#F99D1C",
        transportModeName = "Train",
        destinationName = "Liverpool",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2026-04-11T01:30:00Z",
    ),
    StopDeparture(
        lineNumber = "T2",
        lineColorCode = "#0098CD",
        transportModeName = "Train",
        destinationName = "Bondi Junction",
        departureTimeText = "11:33 AM",
        departureUtcDateTime = "2026-04-11T01:33:00Z",
    ),
    StopDeparture(
        lineNumber = "T4",
        lineColorCode = "#005AA3",
        transportModeName = "Train",
        destinationName = "Illawarra",
        departureTimeText = "11:36 AM",
        departureUtcDateTime = "2026-04-11T01:36:00Z",
    ),
    StopDeparture(
        lineNumber = "309",
        lineColorCode = "#00B5EF",
        transportModeName = "Bus",
        destinationName = "Bondi Beach",
        departureTimeText = "11:31 AM",
        departureUtcDateTime = "2026-04-11T01:31:00Z",
    ),
    StopDeparture(
        lineNumber = "M50",
        lineColorCode = "#00B5EF",
        transportModeName = "Bus",
        destinationName = "Broadway",
        departureTimeText = "11:35 AM",
        departureUtcDateTime = "2026-04-11T01:35:00Z",
    ),
    StopDeparture(
        lineNumber = "L1",
        lineColorCode = "#E4022D",
        transportModeName = "Light Rail",
        destinationName = "Dulwich Hill",
        departureTimeText = "11:32 AM",
        departureUtcDateTime = "2026-04-11T01:32:00Z",
    ),
)

@Preview(name = "Filter closed")
@Composable
private fun LinesServedRowClosedPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        LinesServedRow(
            departures = previewDepartures,
            selectedLine = null,
            onLineSelect = {},
        )
    }
}

@Preview(name = "Filter open — all expanded, T2 selected")
@Composable
private fun LinesServedRowOpenPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        LinesServedRow(
            departures = previewDepartures,
            selectedLine = "T2",
            onLineSelect = {},
        )
    }
}

@Preview(name = "Bus only")
@Composable
private fun LinesServedRowBusOnlyPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        LinesServedRow(
            departures = persistentListOf(
                StopDeparture(
                    lineNumber = "309",
                    lineColorCode = "#00B5EF",
                    transportModeName = "Bus",
                    destinationName = "Bondi Beach",
                    departureTimeText = "11:31 AM",
                    departureUtcDateTime = "2026-04-11T01:31:00Z",
                ),
            ),
            selectedLine = null,
            onLineSelect = {},
        )
    }
}

// endregion
