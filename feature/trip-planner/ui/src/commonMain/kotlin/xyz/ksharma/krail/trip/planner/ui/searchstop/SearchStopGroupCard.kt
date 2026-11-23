package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.MapLightScope
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.components.mapChipEdge
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
    /** The saved label of the stop each row holds (Home, Work), or null. The card names the stop with it and draws its icon. */
    val fromLabel: String? = null,
    val toLabel: String? = null,
    /** Reverses From and To. Null draws no swap circle: the rider has picked no stop yet, so there is nothing to reverse. */
    val onSwap: (() -> Unit)? = null,
)

/**
 * The top of Search Stop: the [SearchStopGroupCard] and, directly under it while the location is not allowed, the
 * [LocationNotice], one [SearchStopGaps.betweenCards] below the box and at its left edge. The notice is not part
 * of the box, so the box holds only the two rows. On the map state this is everything that floats at the top. The
 * block stands [SearchStopGaps.edge] in from the start, top and end of where it is placed. [showLocationChip] is false
 * in the single pane list state, which is the box and the list and nothing between them. The Not now of the notice is held
 * here, so it lasts the visit through the list state and back, and is not saved anywhere. The box and the notice are the same
 * fixed light look on the map and on the cloud gradient of the list state, in both app modes.
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
    var noticeDismissed by rememberSaveable { mutableStateOf(false) }
    // The edge is this block's own, outside whatever [modifier] measures, so a caller that reads the block's size gets the
    // box and the chip and not the margin around them. There is none at the bottom: what comes next brings its own.
    Column(modifier = Modifier.padding(start = gaps.edge, top = gaps.edge, end = gaps.edge).then(modifier)) {
        SearchStopGroupCard(form = form, onBack = onBack)
        if (showLocationChip) {
            LocationNotice(
                pill = location.pill,
                dismissed = noticeDismissed,
                onDismiss = { noticeDismissed = true },
                onCtaClick = location.onClick,
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
    // The card and the pill fade into one another and the box takes the new size softly, with no jump, on the motion spec. The
    // box is not clipped while it does, so its edge is never cut.
    val reduceMotion = isReduceMotionEnabled()
    AnimatedContent(
        targetState = groupCardShownFor(form.shape),
        modifier = modifier,
        transitionSpec = {
            val fade = if (reduceMotion) snap<Float>() else tween(MapMotion.ENTER_MS, easing = MapMotion.ENTER_EASING)
            (fadeIn(animationSpec = fade) togetherWith fadeOut(animationSpec = fade))
                .using(SizeTransform(clip = false))
        },
        label = "search-stop-box",
    ) { showsCard ->
        if (showsCard) {
            SearchStopCard(form = form)
        } else {
            SearchStopPill(form = form, onBack = onBack)
        }
    }
}

/**
 * The edge of the stop card, the one surface of Search Stop that is not the fixed light look. There is no glow anywhere on this
 * screen. The stop card in light mode is a pale shape on the light map (1.0 to 1.3 against it), so it wears a 1 dp `softLabel`
 * hairline ([mapChipEdge], 3.8 against the map ground and 3.4 against the water). On the dark map it already stands 5.9 to 10
 * against it and wears none. The box, the pill, the Options card and the notice are fixed light and wear [mapChipEdge] in both
 * modes, so they do not come through here.
 */
@Composable
internal fun Modifier.searchStopEdge(shape: Shape): Modifier =
    if (searchStopEdgeShown(darkMode = isAppInDarkMode())) mapChipEdge(shape) else this

/** Whether the stop card's hairline is drawn: in light mode only. A plain function so it is pinned as a table. */
internal fun searchStopEdgeShown(darkMode: Boolean): Boolean = !darkMode

/**
 * The group card (`docs/MAP_HOME_UX.md`, The box): the Timetable's own From and To card, one rounded box in the fixed light tint
 * ([MapCardColors.tint], the theme colour at 15 percent over white in light and dark app mode alike, since the map behind it is
 * always light), with no glow and no back arrow (system back is the two step back). It is the card's own corner and it wears
 * the hairline ([mapChipEdge]) in both modes. Inside, [ToFromCardRows] draws the From row, the divider and the To row, and the
 * right column of controls. The card has no padding of its own: each row keeps the grid's 16 dp at the start and the controls'
 * column at the end.
 *
 * The location notice and the recents or results are separate, under this one, never inside it.
 */
@Composable
private fun SearchStopCard(
    form: SearchStopFormModel,
    modifier: Modifier = Modifier,
) {
    val gaps = rememberSearchStopGaps()
    val shape = RoundedCornerShape(gaps.cardCorner)
    MapLightScope {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
                .mapChipEdge(shape)
                .clip(shape)
                .background(mapCardColors().tint),
        ) {
            SearchStopFormFor(form = form, onBack = {}, style = ToFromBlockStyle.CardRows)
        }
    }
}

/**
 * The single field, one bare pill directly on the map: full radius, 48 dp tall, in the box's own fixed light tint and text with
 * the same hairline in both modes (no glow), and the back arrow inside it. No card, no padding and no card test tag: it is the
 * pill, and it is as wide as the card would have been.
 */
@Composable
private fun SearchStopPill(
    form: SearchStopFormModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gaps = rememberSearchStopGaps()
    val shape = RoundedCornerShape(percent = PILL_PERCENT)
    MapLightScope {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .mapChipEdge(shape)
                .clip(shape),
        ) {
            SearchStopFormFor(
                form = form,
                onBack = onBack,
                style = ToFromBlockStyle.FloatingRows(rowGap = gaps.betweenRows, fixedLight = true),
            )
        }
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
        fromLabel = form.fromLabel,
        toLabel = form.toLabel,
        onSwap = form.onSwap,
        style = style,
    )
}
