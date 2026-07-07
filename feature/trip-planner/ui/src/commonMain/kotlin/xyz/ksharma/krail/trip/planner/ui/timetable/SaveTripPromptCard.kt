package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent

// Static content for the "Save this trip?" nudge (story A2). Rendered with the
// shared InfoTile component so the prompt looks and behaves like every other
// actionable tile in the app. Dismissal tracking is handled by
// TimeTableViewModel, not InfoTileManager — the tile's key is never
// registered with the remote info-tile pipeline.
private val saveTripPromptTileData = InfoTileData(
    key = "save_trip_prompt",
    title = "Save this trip?",
    description = "Your trip will be one tap away on the home screen.",
    primaryCta = InfoTileCta(text = "Save"),
    dismissCtaText = "Not now",
    type = InfoTileData.InfoTileType.INFO,
)

/**
 * Save nudge for an unsaved pair (story A2). A list item below the header,
 * so it never covers journey results. Frequency rules live in
 * [TimeTableViewModel].
 */
internal fun LazyListScope.saveTripPromptItem(
    showSaveTripPrompt: Boolean,
    onEvent: (TimeTableUiEvent) -> Unit,
    onSaveAccepted: () -> Unit = {},
) {
    if (!showSaveTripPrompt) return
    item(key = "save-trip-prompt") {
        InfoTile(
            infoTileData = saveTripPromptTileData,
            initialState = InfoTileState.EXPANDED,
            onCtaClick = {
                onEvent(TimeTableUiEvent.SaveTripPromptAccepted)
                // Kick off the title-bar star celebration so the user sees
                // where the save control permanently lives.
                onSaveAccepted()
            },
            onDismissClick = { onEvent(TimeTableUiEvent.SaveTripPromptDismissed) },
            modifier = Modifier
                // Horizontal inset matches the OriginDestination header and the
                // trip-actions row above, so all timetable cards share one edge.
                .padding(horizontal = KrailTheme.dimensions.spacingL)
                .padding(top = KrailTheme.dimensions.spacingL)
                .animateItem(),
        )
    }
}

// region Preview

@PreviewComponent
@Composable
private fun PreviewSaveTripPromptTile() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        InfoTile(
            infoTileData = saveTripPromptTileData,
            initialState = InfoTileState.EXPANDED,
            onCtaClick = {},
            onDismissClick = {},
        )
    }
}

// endregion
