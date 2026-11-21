package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
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
import xyz.ksharma.krail.trip.planner.ui.timetable.business.DisruptionDetail
import xyz.ksharma.krail.trip.planner.ui.timetable.business.LastService
import xyz.ksharma.krail.trip.planner.ui.timetable.business.factRanges
import xyz.ksharma.krail.trip.planner.ui.timetable.business.parseDisruptionDetail

/**
 * Service disruption above the journey results, reusing the shared [InfoTile] so it looks and
 * behaves like every other tile. Collapsed by default: the headline already says what is out and
 * between where, and an open tile took about 45 percent of a Pixel 6 Pro screen and pushed the
 * results off it. Tapping it opens the replacement times.
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
 * Replacement buses are in the results. The expanded detail is the metro alert when one also says buses
 * replace the line, else the planner's own line alert, else there is nothing more to say and the tile
 * does not expand. Both cases use the same tile as [DisruptionBanner].
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
    val detail = remember(disruption?.detail) { disruption?.let { parseDisruptionDetail(it.detail) } }
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
                disruption != null && detail != null ->
                    DisruptionDetailContent(detail = detail, fallback = disruption.detail)
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
    val detail = remember(disruption.detail) { parseDisruptionDetail(disruption.detail) }
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
        expandedContent = { DisruptionDetailContent(detail = detail, fallback = disruption.detail) },
        leadingIcon = { DisruptionWarningBadge() },
    )
}

/**
 * The alert's details with the facts in bold: times and places, so the rider can find "what, where and
 * when" without reading every word. The last services are a small board, the time large and the place
 * beside it. The text of the alert is always shown in full; bolding only adds weight.
 */
@Composable
private fun DisruptionDetailContent(detail: DisruptionDetail, fallback: String) {
    val dim = KrailTheme.dimensions
    Column(verticalArrangement = Arrangement.spacedBy(dim.spacingL)) {
        // Nothing recognised and nothing left over: show whatever NSW sent rather than an empty tile.
        val nothingParsed = detail.paragraphs.isEmpty() && detail.lastServices.isEmpty()
        val paragraphs = if (nothingParsed) listOf(fallback) else detail.paragraphs
        paragraphs.forEach { paragraph -> FactText(text = paragraph) }
        if (detail.lastServices.isNotEmpty()) {
            LastServicesBoard(services = detail.lastServices)
        }
    }
}

@Composable
private fun FactText(text: String) {
    val annotated = remember(text) {
        buildAnnotatedString {
            append(text)
            factRanges(text).forEach { range ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first, range.last + 1)
            }
        }
    }
    Text(text = annotated, style = KrailTheme.typography.bodyMedium)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LastServicesBoard(services: List<LastService>) {
    val dim = KrailTheme.dimensions
    val secondary = KrailTheme.colors.onSurface.copy(alpha = SECONDARY_TEXT_ALPHA)
    Column(verticalArrangement = Arrangement.spacedBy(dim.spacingL)) {
        Text(text = "Last metro before buses", style = KrailTheme.typography.labelMedium, color = secondary)
        services.forEach { service ->
            // A flow, not a fixed time column: at a large font size or on a narrow pane the place moves
            // under the time instead of squeezing it onto two lines.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
                verticalArrangement = Arrangement.spacedBy(dim.spacingXXS),
            ) {
                Text(
                    text = boardTime(service.leaves),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.widthIn(min = BOARD_TIME_MIN_WIDTH),
                )
                Column {
                    Text(text = service.from, style = KrailTheme.typography.titleMedium)
                    Text(
                        text = "arrives ${service.to} ${service.arrives}",
                        style = KrailTheme.typography.bodySmall,
                        color = secondary,
                    )
                }
            }
        }
    }
}

/** "11:03 PM" with the clock large and the am or pm small, so the time stays on one line. */
@Composable
private fun boardTime(time: String): AnnotatedString {
    val clock = time.substringBeforeLast(' ')
    val period = time.substringAfterLast(' ', missingDelimiterValue = "")
    val titleStyle = KrailTheme.typography.titleMedium
    return buildAnnotatedString {
        withStyle(titleStyle.copy(fontSize = BOARD_TIME_SIZE, fontFeatureSettings = "tnum").toSpanStyle()) {
            append(clock)
        }
        if (period.isNotEmpty()) {
            withStyle(titleStyle.copy(fontSize = BOARD_PERIOD_SIZE).toSpanStyle()) { append(" $period") }
        }
    }
}

private const val SECONDARY_TEXT_ALPHA = 0.7f
private val BOARD_TIME_SIZE = 22.sp
private val BOARD_PERIOD_SIZE = 13.sp
private val BOARD_TIME_MIN_WIDTH = 96.dp
