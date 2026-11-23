package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.info.tiles.ui.infoTileGlow
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.ToFromBlockStyle
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/** Everything the form needs, in one holder so the two layouts take it as one argument. */
internal data class SearchStopFormModel(
    val shape: FormShape,
    val editing: ToFromField?,
    val toPlaceholder: String,
    val fromPlaceholder: String,
    val toStop: StopItem?,
    val fromStop: StopItem?,
    val fieldState: TextFieldState,
    val focusRequester: FocusRequester,
    val onFlowEvent: (SearchStopFlowEvent) -> Unit,
    val onGoClick: () -> Unit,
    val onTextChange: (String) -> Unit,
)

/**
 * The top of Search Stop: the [SearchStopGroupCard] and, directly under it while the location is not allowed, the
 * [LocationStatusChip], one [SearchStopGaps.betweenCards] below the box and at its left edge. The chip is not part
 * of the box, so the box holds only the two pills. On the map state this is everything that floats at the top. The
 * block stands [SearchStopGaps.edge] in from the start, top and end of where it is placed. [showLocationChip] is false
 * in the single pane list state, which is the box and the list and nothing between them.
 */
@Composable
internal fun SearchStopTopBlock(
    form: SearchStopFormModel,
    location: LocationPillState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showLocationChip: Boolean = true,
) {
    val gaps = rememberSearchStopGaps()
    // The edge is this block's own, outside whatever [modifier] measures, so a caller that reads the block's size gets the
    // box and the chip and not the margin around them. There is none at the bottom: what comes next brings its own.
    Column(modifier = Modifier.padding(start = gaps.edge, top = gaps.edge, end = gaps.edge).then(modifier)) {
        SearchStopGroupCard(form = form, onBack = onBack)
        if (showLocationChip && locationChipShownFor(location.pill)) {
            LocationStatusChip(
                pill = location.pill,
                onClick = location.onClick,
                modifier = Modifier.padding(top = gaps.betweenCards),
            )
        }
    }
}

/**
 * The form at the top of Search Stop, drawn by how many fields it shows ([groupCardShownFor], never by the screen's
 * name): with two, From and To, the [SearchStopCard] holds both pills, and with one it is the bare [SearchStopPill].
 */
@Composable
internal fun SearchStopGroupCard(
    form: SearchStopFormModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groupCardShownFor(form.shape)) {
        SearchStopCard(form = form, onBack = onBack, modifier = modifier)
    } else {
        SearchStopPill(form = form, onBack = onBack, modifier = modifier)
    }
}

/**
 * The edge of the Search Stop box, the card and the lone pill alike: the info tile's glow ([infoTileGlow], the tile's
 * theme coloured shadow and border) around [shape]. It is the one place the edge is drawn, so dropping the glow is one
 * change here.
 */
@Composable
private fun Modifier.searchStopEdge(shape: Shape): Modifier = infoTileGlow(shape)

/**
 * The group card (rules 33, 35 and 36 of `docs/MAP_HOME_UX.md`): one rounded box on taj's own `surface`, white in light
 * mode and dark in dark mode, so it follows the app mode and is never a fixed dark. It wears the edge from
 * [searchStopEdge] around its own corner, which is what lifts a white box off the light map, and it has no plain outline.
 * Inside, from the top and in the order [groupCardRows] gives: the From and To pills, 48 tall and
 * [SearchStopGaps.betweenRows] apart with no divider. The box's padding and corner are the card's own tokens, and it is
 * the same height at every text size because each pill is one fixed line.
 *
 * The pills are rows on this one surface, and the location chip and the recents or results are separate, under
 * this one, never inside it.
 */
@Composable
private fun SearchStopCard(
    form: SearchStopFormModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gaps = rememberSearchStopGaps()
    val shape = RoundedCornerShape(gaps.cardCorner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
            .searchStopEdge(shape)
            .clip(shape)
            .background(KrailTheme.colors.surface)
            .padding(gaps.cardPadding),
        verticalArrangement = Arrangement.spacedBy(gaps.betweenRows),
    ) {
        SearchStopFormFor(form = form, onBack = onBack, style = ToFromBlockStyle.FloatingRows(rowGap = gaps.betweenRows))
    }
}

/**
 * The single field, one bare pill directly on the map: full radius, 48 dp tall, filled with the card's own `surface`
 * (not the soft fill, which would nearly vanish on the light map when it stands alone) with the same edge as the card,
 * and the back arrow inside it. No card, no padding and no card test tag: it is the pill, and it is as wide as the card
 * would have been.
 */
@Composable
private fun SearchStopPill(
    form: SearchStopFormModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gaps = rememberSearchStopGaps()
    val shape = RoundedCornerShape(percent = PILL_PERCENT)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .searchStopEdge(shape)
            .clip(shape),
    ) {
        SearchStopFormFor(
            form = form,
            onBack = onBack,
            style = ToFromBlockStyle.FloatingRows(rowGap = gaps.betweenRows, fillSurface = true),
        )
    }
}

private const val PILL_PERCENT = 50

@Composable
private fun SearchStopFormFor(
    form: SearchStopFormModel,
    onBack: () -> Unit,
    style: ToFromBlockStyle,
) {
    SearchStopForm(
        shape = form.shape,
        editing = form.editing,
        toPlaceholder = form.toPlaceholder,
        fromPlaceholder = form.fromPlaceholder,
        toStop = form.toStop,
        fromStop = form.fromStop,
        fieldState = form.fieldState,
        focusRequester = form.focusRequester,
        onFlowEvent = form.onFlowEvent,
        onBack = onBack,
        onGoClick = form.onGoClick,
        onTextChange = form.onTextChange,
        style = style,
    )
}
