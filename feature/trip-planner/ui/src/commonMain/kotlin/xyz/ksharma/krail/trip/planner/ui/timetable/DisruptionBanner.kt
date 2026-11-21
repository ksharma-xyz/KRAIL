package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.WarningIcon
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.alerts.HtmlText
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionBannerState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.timetable.business.formatDisruptionWindow

/**
 * Service disruption above the journey results, reusing the shared [InfoTile] so it looks and
 * behaves like every other tile. Collapsed by default: the headline already says what is out and
 * between where, and an open tile took about 45 percent of a Pixel 6 Pro screen and pushed the
 * results off it. Tapping it opens when the alert is in force.
 */
internal fun LazyListScope.disruptionBannerItem(
    banner: DisruptionBannerState,
    onEvent: (TimeTableUiEvent) -> Unit,
) {
    if (banner is DisruptionBannerState.None) return
    item(key = "disruption-banner") {
        // A lazy item only composes once it is in or near the viewport, so this is the first moment the
        // banner can be said to have reached the rider. The ViewModel de-dupes recompositions.
        LaunchedEffect(Unit) { onEvent(TimeTableUiEvent.DisruptionBannerDisplayed) }
        val bannerModifier = Modifier
            .padding(horizontal = KrailTheme.dimensions.spacingL)
            // The header card to the actions row is spacingM (the card's own bottom padding) plus
            // spacingL (the row's top padding). Match it so the gap above the banner equals the gap
            // above the actions row, as the Save prompt does.
            .padding(top = KrailTheme.dimensions.spacingL + KrailTheme.dimensions.spacingM)
            .animateItem()
        val onExpand = { onEvent(TimeTableUiEvent.DisruptionBannerExpanded) }
        val onMoreInfo = { url: String -> onEvent(TimeTableUiEvent.DisruptionBannerCtaClicked(url)) }
        when (banner) {
            is DisruptionBannerState.Shown -> DisruptionBanner(
                disruption = banner.disruption,
                onExpand = onExpand,
                onMoreInfo = onMoreInfo,
                modifier = bannerModifier,
            )
            is DisruptionBannerState.ReplacementBuses -> ReplacementBusesBanner(
                disruption = banner.disruption,
                lineAlert = banner.lineAlert,
                onExpand = onExpand,
                onMoreInfo = onMoreInfo,
                modifier = bannerModifier,
            )
            DisruptionBannerState.None -> Unit
        }
    }
}

/** The title when replacement buses are in the results. It says what is on the rider's screen, not where. */
internal const val REPLACEMENT_BUSES_TITLE = "Replacement buses on this trip"

/**
 * Replacement buses are in the results. The expanded detail is the metro alert's headline and window when
 * one also says buses replace the line, else the planner's own line alert, else there is nothing more to
 * say and the tile does not expand. Both cases use the same tile as [DisruptionBanner].
 */
@Composable
internal fun ReplacementBusesBanner(
    disruption: Disruption?,
    lineAlert: ServiceAlert?,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onMoreInfo: (String) -> Unit = {},
    initialState: InfoTileState = InfoTileState.COLLAPSED,
) {
    val uriHandler = LocalUriHandler.current
    val moreInfoUrl = disruption?.moreInfoUrl ?: lineAlert?.url
    val tile = remember(disruption, lineAlert) {
        InfoTileData(
            key = "disruption:replacement:${disruption?.id ?: lineAlert?.alertId}",
            title = REPLACEMENT_BUSES_TITLE,
            // Only used by the plain InfoTile layout; the expanded content below replaces it.
            description = REPLACEMENT_BUSES_TITLE,
            type = InfoTileData.InfoTileType.CRITICAL_ALERT,
            primaryCta = moreInfoUrl?.let { InfoTileCta(text = "More info", url = it) },
            showDismissButton = false,
        )
    }
    InfoTile(
        infoTileData = tile,
        initialState = initialState,
        onCtaClick = { data ->
            data.primaryCta?.url?.let { url ->
                onMoreInfo(url)
                uriHandler.openUri(url)
            }
        },
        onDismissClick = {},
        onTileExpand = { onExpand() },
        modifier = modifier,
        expandable = disruption != null || lineAlert != null,
        expandedContent = {
            when {
                disruption != null -> DisruptionWindowContent(disruption = disruption, showHeadline = true)
                lineAlert != null -> LineAlertContent(alert = lineAlert)
            }
        },
        leadingIcon = { DisruptionWarningBadge() },
    )
}

/** The planner's line alert as NSW wrote it, with its own markup, in the app's existing alert text. */
@Composable
private fun LineAlertContent(alert: ServiceAlert) {
    HtmlText(
        text = alert.message,
        onClick = {},
        color = KrailTheme.colors.onSurface,
        urlColor = KrailTheme.colors.onSurface,
    )
}

/**
 * The warning triangle on a yellow circle, centred on the first line of the title. The circle is the
 * alert amber riders already know; the triangle stays near black in both themes because `onSurface`
 * is white in dark mode and would not read on yellow.
 */
@Composable
internal fun DisruptionWarningBadge(modifier: Modifier = Modifier) {
    val titleLine = KrailTheme.typography.titleMedium.lineHeight
    val lineHeight = with(LocalDensity.current) {
        if (titleLine.isSpecified) titleLine.toDp() else BADGE_SIZE
    }
    Box(
        modifier = modifier
            // The tile top-aligns its icon, so push the circle down to the middle of the first line.
            .padding(top = ((lineHeight - BADGE_SIZE) / 2).coerceAtLeast(0.dp))
            .size(BADGE_SIZE)
            .clip(CircleShape)
            .background(KrailTheme.colors.alert),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = WarningIcon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(BADGE_GLYPH_COLOR),
            modifier = Modifier.size(BADGE_GLYPH_SIZE),
        )
    }
}

private val BADGE_SIZE = 20.dp
private val BADGE_GLYPH_SIZE = 12.dp
private val BADGE_GLYPH_COLOR = Color(0xFF010101)

@Composable
internal fun DisruptionBanner(
    disruption: Disruption,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onMoreInfo: (String) -> Unit = {},
    initialState: InfoTileState = InfoTileState.COLLAPSED,
) {
    val uriHandler = LocalUriHandler.current
    val tile = remember(disruption) {
        InfoTileData(
            key = "disruption:${disruption.id}",
            title = disruption.headline,
            // Only used by the plain InfoTile layout; the expanded content below replaces it.
            description = disruption.headline,
            type = InfoTileData.InfoTileType.CRITICAL_ALERT,
            primaryCta = disruption.moreInfoUrl?.let { InfoTileCta(text = "More info", url = it) },
            showDismissButton = false,
        )
    }
    InfoTile(
        infoTileData = tile,
        initialState = initialState,
        onCtaClick = { data ->
            data.primaryCta?.url?.let { url ->
                onMoreInfo(url)
                uriHandler.openUri(url)
            }
        },
        onDismissClick = {},
        onTileExpand = { onExpand() },
        modifier = modifier,
        expandedContent = { DisruptionWindowContent(disruption = disruption, showHeadline = false) },
        leadingIcon = { DisruptionWarningBadge() },
    )
}

/**
 * When the alert is in force, from the alert's own start and end fields. The free text NSW writes beside
 * them is not shown: its plain-text and HTML versions of the same alert can disagree. With
 * [showHeadline] the headline sits above, for a tile whose own title is not the headline.
 */
@Composable
private fun DisruptionWindowContent(disruption: Disruption, showHeadline: Boolean) {
    val window = remember(disruption.startEpochSec, disruption.endEpochSec) {
        formatDisruptionWindow(disruption.startEpochSec, disruption.endEpochSec)
    }
    Column(verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingL)) {
        if (showHeadline) Text(text = disruption.headline, style = KrailTheme.typography.bodyMedium)
        Text(text = window, style = KrailTheme.typography.bodyMedium)
    }
}
