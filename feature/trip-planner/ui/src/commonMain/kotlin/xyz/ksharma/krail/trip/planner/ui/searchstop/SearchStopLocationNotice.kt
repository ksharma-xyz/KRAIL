package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults
import xyz.ksharma.krail.info.tiles.ui.InfoTileStyle
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.CardGroundScope
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.components.mapChipEdge
import xyz.ksharma.krail.trip.planner.ui.components.mapSizeFadeIn
import xyz.ksharma.krail.trip.planner.ui.components.mapSizeFadeOut

/**
 * The location notice under the Search Stop box while location is not allowed: the app's own [InfoTile] in its `Plain`
 * style (the soft theme tint, no glow), read in the box's own ground ([CardGroundScope], [onGradient]: the fixed light tint and
 * text on the always light map, and the app mode's own on the gradient), expanded, left aligned and the box's width,
 * [SearchStopGaps.betweenCards] under it. It
 * says what is wrong and what to do ([LOCATION_NOTICE_TITLE], [LOCATION_NOTICE_BODY]), with one call to action that does what
 * the old chip did, ask or open Settings ([onCtaClick]), and the tile's own text button Not now ([onDismiss]).
 *
 * Not now hides it for this visit only. The flag is the screen's own ([dismissed], held by `rememberSaveable` where it is
 * called), so it is neither persisted nor routed through the info tile manager, and it is back on the next visit. It leaves
 * with the shared fade and size change when the permission is granted or it is dismissed, and arrives the same way.
 *
 * The tile does not collapse on a tap (there is nothing more to say), and it takes every touch over it, so a tap that misses
 * its buttons never reaches the map under it. It wears the same 1 dp hairline as the box ([mapChipEdge]) in both modes: its own
 * themed border is the tint over the tint and is no edge against the map.
 */
@Composable
internal fun LocationNotice(
    pill: LocationPill,
    dismissed: Boolean,
    onDismiss: () -> Unit,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGradient: Boolean = false,
) {
    val reduceMotion = isReduceMotionEnabled()
    val gaps = rememberSearchStopGaps()
    // The call to action keeps its words while the notice is leaving, when the pill has already gone back to Hidden.
    val lastPill = remember { mutableStateOf(pill) }
    if (pill != LocationPill.Hidden) lastPill.value = pill
    AnimatedVisibility(
        visible = locationNoticeShown(pill, dismissed),
        modifier = modifier,
        enter = mapSizeFadeIn(reduceMotion),
        exit = mapSizeFadeOut(reduceMotion),
    ) {
        Box(modifier = Modifier.padding(top = gaps.betweenCards)) {
            CardGroundScope(onGradient = onGradient) {
                InfoTile(
                    infoTileData = InfoTileData(
                        key = LOCATION_NOTICE_KEY,
                        title = LOCATION_NOTICE_TITLE,
                        description = LOCATION_NOTICE_BODY,
                        dismissCtaText = LOCATION_NOTICE_DISMISS,
                        primaryCta = InfoTileCta(text = locationNoticeCta(lastPill.value)),
                    ),
                    modifier = Modifier
                        .testTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
                        .mapChipEdge(InfoTileDefaults.shape)
                        .pointerInput(Unit) {},
                    initialState = InfoTileState.EXPANDED,
                    onCtaClick = { onCtaClick() },
                    onDismissClick = { onDismiss() },
                    style = InfoTileStyle.Plain,
                    leadingIcon = { LocationPin() },
                    expandable = false,
                )
            }
        }
    }
}

/** The app's pin at the start of the title, centred on the title's first line however tall that line is at large text. */
@Composable
private fun LocationPin() {
    val density = LocalDensity.current
    val iconSize = KrailTheme.dimensions.iconSmall
    val firstLine = with(density) { KrailTheme.typography.titleMedium.lineHeight.toDp() }
    Image(
        painter = painterResource(Res.drawable.ic_location_on),
        contentDescription = null,
        colorFilter = ColorFilter.tint(mapCardColors().onTint),
        modifier = Modifier.padding(top = maxOf(0.dp, (firstLine - iconSize) / 2f)).size(iconSize),
    )
}

/** Whether the notice is drawn: location is not allowed and the rider has not said Not now this visit. */
internal fun locationNoticeShown(pill: LocationPill, dismissed: Boolean): Boolean = locationChipShownFor(pill) && !dismissed

/** The call to action: Allow while the rider can still be asked, Settings once they have to go there. */
internal fun locationNoticeCta(pill: LocationPill): String = when (pill) {
    LocationPill.OpenSettings -> LOCATION_NOTICE_CTA_SETTINGS
    LocationPill.Ask, LocationPill.Hidden -> LOCATION_NOTICE_CTA_ALLOW
}

private const val LOCATION_NOTICE_KEY = "search_stop_location_off"
internal const val LOCATION_NOTICE_TITLE = "Location is off"
internal const val LOCATION_NOTICE_BODY = "Turn on location to see stops near you"
internal const val LOCATION_NOTICE_DISMISS = "Not now"
internal const val LOCATION_NOTICE_CTA_ALLOW = "Allow"
internal const val LOCATION_NOTICE_CTA_SETTINGS = "Settings"
