package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionSeverity

/**
 * Service disruption above the journey results, reusing the shared [InfoTile] so it looks and
 * behaves like every other tile. Collapsed by default: the headline already says what is out and
 * between where, and an open tile took about 45 percent of a Pixel 6 Pro screen and pushed the
 * results off it. Tapping it opens the replacement times.
 */
internal fun LazyListScope.disruptionBannerItem(banner: DisruptionBannerState) {
    val shown = (banner as? DisruptionBannerState.Shown)?.disruption ?: return
    item(key = "disruption-banner") {
        DisruptionBanner(
            disruption = shown,
            modifier = Modifier
                .padding(horizontal = KrailTheme.dimensions.spacingL)
                .padding(top = KrailTheme.dimensions.spacingL)
                .animateItem(),
        )
    }
}

@Composable
private fun DisruptionBanner(disruption: Disruption, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val tile = remember(disruption) {
        InfoTileData(
            key = "disruption:${disruption.id}",
            title = disruption.headline,
            description = disruption.detail.ifBlank { disruption.headline },
            type = InfoTileData.InfoTileType.CRITICAL_ALERT,
            primaryCta = disruption.moreInfoUrl?.let { InfoTileCta(text = "More info", url = it) },
            showDismissButton = false,
        )
    }
    InfoTile(
        infoTileData = tile,
        initialState = InfoTileState.COLLAPSED,
        onCtaClick = { data -> data.primaryCta?.url?.let(uriHandler::openUri) },
        onDismissClick = {},
        modifier = modifier,
    )
}

@PreviewComponent
@Composable
private fun PreviewDisruptionBanner() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        DisruptionBanner(
            disruption = Disruption(
                id = "metro:preview",
                feed = DisruptionFeed.METRO,
                severity = DisruptionSeverity.NOT_RUNNING,
                headline = "Buses replace metro services between Bankstown and Sydenham",
                detail = "Monday 23 November\nFrom 11:10PM, buses replace metro services between " +
                    "Bankstown and Sydenham.",
                startEpochSec = 0L,
                endEpochSec = null,
                moreInfoUrl = "https://transportnsw.info/alerts",
                replacedByBuses = true,
            ),
        )
    }
}
