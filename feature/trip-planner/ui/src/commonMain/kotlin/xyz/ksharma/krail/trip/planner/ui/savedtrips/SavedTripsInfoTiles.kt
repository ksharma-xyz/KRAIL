package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.trip.planner.ui.settings.ReferFriendManager.getReferText

/**
 * Info-tile handling for [SavedTripsViewModel], kept out of the class itself: the tiles are a
 * self-contained concern with their own manager, and the ViewModel is at its `LargeClass`
 * limit. Extensions rather than a collaborator so the call sites in `onEvent` read exactly as
 * they did when these were members.
 */
internal suspend fun SavedTripsViewModel.updateInfoTilesUiState() {
    log("Updating info tiles in UI state")
    val activeTiles = infoTileManager.getInfoTiles()
    log("Active info tiles: ${activeTiles.map { it.key }}")
    updateUiState {
        copy(infoTiles = activeTiles.toImmutableList())
    }
}

internal fun SavedTripsViewModel.onDismissInfoTile(infoTileData: InfoTileData) {
    log("onDismissInfoTile: ${infoTileData.key}")
    viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
        log("Dismissing info tile: ${infoTileData.key}")
        infoTileManager.markInfoTileDismissed(infoTileData)
        updateInfoTilesUiState()
        trackInfoTileInteraction(
            key = infoTileData.key,
            dismiss = true,
        )
    }
}

internal fun SavedTripsViewModel.onInfoTileCtaClick(infoTile: InfoTileData) {
    when (infoTile.type) {
        InfoTileData.InfoTileType.INVITE_FRIENDS -> {
            platformOps.sharePlainText(
                text = getReferText(),
                title = "Invite your Friends",
            )
            analytics.track(
                event = AnalyticsEvent.ReferFriend(
                    entryPoint = AnalyticsEvent.ReferFriend.EntryPoint.SAVED_TRIPS,
                ),
            )
        }

        InfoTileData.InfoTileType.CRITICAL_ALERT,
        InfoTileData.InfoTileType.INFO,
        InfoTileData.InfoTileType.APP_UPDATE,
        -> {
            infoTile.primaryCta?.url?.let { url ->
                platformOps.openUrl(url)
                trackInfoTileInteraction(
                    key = infoTile.key,
                    url = url,
                )
            }
        }
    }
}

internal fun SavedTripsViewModel.onInfoTileExpand(key: String) {
    trackInfoTileInteraction(key = key, expand = true)
}

internal fun SavedTripsViewModel.trackInfoTileInteraction(
    key: String,
    url: String? = null,
    dismiss: Boolean? = null,
    expand: Boolean? = null,
) {
    viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
        analytics.track(
            event = AnalyticsEvent.InfoTileInteraction(
                key = key,
                ctaUrl = url,
                dismiss = dismiss,
                expand = expand,
            ),
        )
    }
}
