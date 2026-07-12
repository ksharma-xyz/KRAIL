package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

// Nudge shown while zero labels are set — never registered with the remote
// info-tile pipeline, same pattern as the save-trip-prompt (story A2).
private val setUpLabelsTileData = InfoTileData(
    key = "set_up_stop_labels",
    title = "Set up Home & Work",
    description = "Save your regular stops for one-tap trips.",
    primaryCta = InfoTileCta(text = "Set up"),
    showDismissButton = false,
    type = InfoTileData.InfoTileType.INFO,
)

/**
 * Pill row v2 (redesign) — tap-only, no choose-mode. Renders ONLY labels that already
 * have a stop; unset labels never appear here (see `STOP_LABEL_UX_REDESIGN_PROPOSAL.md`).
 * While zero labels are set, an [InfoTile] nudge replaces the row entirely rather than
 * showing a bare, unexplained manage icon.
 */
@Composable
internal fun StopLabelPillRow(
    stopLabels: ImmutableList<StopLabel>,
    onPillClick: (StopLabel) -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val setLabels = stopLabels.filter { it.isSet }
    val dim = KrailTheme.dimensions

    if (setLabels.isEmpty()) {
        InfoTile(
            infoTileData = setUpLabelsTileData,
            initialState = InfoTileState.EXPANDED,
            onCtaClick = { onManageClick() },
            onDismissClick = {},
            modifier = modifier.padding(horizontal = dim.pageHorizontalPadding),
        )
        return
    }

    // Row (not LazyRow) + IntrinsicSize.Min so every child can fillMaxHeight() and
    // match the tallest sibling's *measured* height — a handful of pills never needs
    // virtualization, and this is what lets the manage circle track the pills' real
    // height (which grows with font scale) instead of a hardcoded dp that would fall
    // short of the pills at large accessibility font sizes.
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(
                start = dim.pageHorizontalPadding,
                end = dim.pageHorizontalPadding,
                top = dim.spacingS,
                bottom = dim.spacingS,
            )
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        setLabels.forEach { label ->
            SetLabelPill(
                label = label,
                modifier = Modifier
                    .fillMaxHeight()
                    .klickable { onPillClick(label) },
            )
        }

        // Touch target fills the row's height (so it matches pill height at any font
        // scale) with a 48dp floor for the accessibility minimum at small scales;
        // the drawn circle inside stays a fixed, compact 28dp regardless — visual
        // weight and touch target are deliberately decoupled (same split Material's
        // own IconButton uses).
        //
        // defaultMinSize goes FIRST so it participates in the row's
        // IntrinsicSize.Min pass (guarantees the row itself is never shorter than
        // 48dp); fillMaxHeight then consumes whatever height that pass resolved to
        // (48dp normally, taller once pills grow past it at large font scale);
        // aspectRatio derives width from that final height last.
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = MANAGE_TOUCH_TARGET_MIN_SIZE, minHeight = MANAGE_TOUCH_TARGET_MIN_SIZE)
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(CircleShape)
                .klickable(onClick = onManageClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(MANAGE_VISIBLE_CIRCLE_SIZE)
                    .clip(CircleShape)
                    // Same fill as SetLabelPill so this reads as part of the
                    // same solid-pill family sitting next to it, not a lighter
                    // outline chip.
                    .background(KrailTheme.colors.stopLabelSurface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Manage labels",
                    tint = KrailTheme.colors.onStopLabelSurface,
                    modifier = Modifier.size(MANAGE_ICON_SIZE),
                )
            }
        }
    }
}

private val MANAGE_TOUCH_TARGET_MIN_SIZE = 48.dp
private val MANAGE_VISIBLE_CIRCLE_SIZE = 28.dp
private val MANAGE_ICON_SIZE = 16.dp

// region Previews

@PreviewComponent
@Composable
private fun PreviewStopLabelPillRow_Empty() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        StopLabelPillRow(
            stopLabels = StopLabel.defaults,
            onPillClick = {},
            onManageClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewStopLabelPillRow_OneSet() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        StopLabelPillRow(
            stopLabels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Central Station"),
                StopLabel(emoji = "💼", label = "Work"),
            ),
            onPillClick = {},
            onManageClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewStopLabelPillRow_MultipleSet() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        StopLabelPillRow(
            stopLabels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home", stopId = "1", stopName = "Central Station"),
                StopLabel(emoji = "💼", label = "Work", stopId = "2", stopName = "Town Hall"),
                StopLabel(emoji = "🏋", label = "Gym", stopId = "3", stopName = "Bondi Junction"),
            ),
            onPillClick = {},
            onManageClick = {},
        )
    }
}

// endregion
