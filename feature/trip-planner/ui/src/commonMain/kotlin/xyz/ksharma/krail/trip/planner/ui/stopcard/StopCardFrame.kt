package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.searchstop.searchStopEdge
import app.krail.taj.resources.Res as TajRes

/**
 * Where a stop card floats: at the bottom of the map pane, centred, in the one column of rule 45 and
 * never wider than it, with the map visible around it. It is a card and not a sheet, so it has no
 * handle, no snap points and no scrim, and a touch outside it falls through to the map.
 *
 * It stops short of the status bar and the form that floats above the map ([topReserve]), and above the
 * navigation bar, so a long board never covers the form. It reports its own measured height through
 * [onHeight] so the map's own controls can ride above it. The pane's horizontal safe area is honoured,
 * for a notch or the outer display of a dual screen phone.
 */
@Composable
internal fun StopCardPlacement(
    topReserve: Dp,
    onHeight: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val navigationBar = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
        // The form's own height is measured inside the status bar and its 16 dp margin, so both are
        // added here and the card stops below the form's bottom edge and not at its top.
        val statusBar = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
        val reserve = topReserve + statusBar + STOP_CARD_EDGE
        Box(
            modifier = Modifier
                .width(stopCardWidth(maxWidth))
                .padding(bottom = navigationBar + STOP_CARD_EDGE)
                .heightIn(max = stopCardMaxHeight(maxHeight, reserve, navigationBar))
                .onSizeChanged { onHeight(with(density) { it.height.toDp() }) },
        ) {
            content()
        }
    }
}

/**
 * The colours of a stop card: a [header] band and a [body], and the [content] drawn on both.
 *
 * [stopCardPalette] is the soft theme tint family of every card on the map ([mapCardColors]), which follows light and dark
 * mode. [darkStopCardPalette] is the fixed dark both bands used to be, kept for the Park & Ride pin card, whose numbers are
 * drawn by the home's Park & Ride row in the fixed light and move with that card. [edged] is whether the card wears the
 * Search Stop box's edge (the theme glow), which the dark one does not.
 */
@Immutable
internal class StopCardPalette(
    val header: Color,
    val body: Color,
    val content: Color,
    val edged: Boolean,
)

@Composable
internal fun stopCardPalette(): StopCardPalette = mapCardColors().let {
    StopCardPalette(header = it.header, body = it.body, content = it.onCard, edged = true)
}

@Composable
internal fun darkStopCardPalette(): StopCardPalette =
    StopCardPalette(header = FloatingDark.container, body = FloatingDark.container, content = FloatingDark.content, edged = false)

/**
 * The two type styles of a stop card, so the card has a hierarchy at every text size. The stop name is the card's title: the
 * app's `title`, 20 sp bold, the largest and strongest style on it. The section label (Departure Board) is a clearly smaller
 * and lighter step, `titleMediumRegular`, 16 sp at normal weight, so the two never read as the same thing.
 */
internal object StopCardType {
    val title: TextStyle
        @Composable get() = KrailTheme.typography.title

    val sectionLabel: TextStyle
        @Composable get() = KrailTheme.typography.titleMediumRegular
}

/**
 * The shell of a stop card: a header band and a body on one card with the Search Stop box's corner and edge
 * (`docs/MAP_HOME_UX.md`, the card shell and Stop card).
 *
 * The [header] is the card's title ([heading]) with the round close button at the end, and under them whatever
 * [headerContent] holds (the mode badges), all in one band of the header tint with the card's corner on its top. The title
 * may wrap inside its own space without pushing the close button out. There is no divider: the band is the separation.
 * The body is on the body tint: [content] scrolls inside the room the card has, and [footer], when there is one, is pinned
 * under it so the action never scrolls off the card. The footer scrolls itself only when the window is so short that the
 * header and the footer alone would fill the card, and it always leaves [BODY_FLOOR] for the body, so the heading of the
 * Departure Board is never squeezed out.
 *
 * The whole card is one region for a screen reader, announced by [announcement] as it opens. The page edge inset,
 * [KrailDimensions.pageHorizontalPadding], is the same in the header and the body: the header has it here and every block of
 * [content] has it itself ([cardInset], or the departure rows' own padding), so a block that runs edge to edge, such as the
 * board's dividers and filter row, can, and text in every part starts on the same left edge.
 */
@Composable
internal fun StopCardFrame(
    announcement: String,
    closeLabel: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    palette: StopCardPalette = stopCardPalette(),
    heading: @Composable () -> Unit,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.cardCornerRadius)
    val floor = BODY_FLOOR * LocalDensity.current.fontScale.coerceIn(1f, MAX_FLOOR_SCALE)
    CompositionLocalProvider(LocalTextColor provides palette.content, LocalContentColor provides palette.content) {
        Column(
            modifier = modifier
                .testTag(TripPlannerTestTags.StopCard.CARD)
                .fillMaxWidth()
                .then(if (palette.edged) Modifier.searchStopEdge(shape) else Modifier)
                .clip(shape)
                .background(palette.body)
                .semantics {
                    paneTitle = announcement
                    isTraversalGroup = true
                },
        ) {
            StopCardHeader(
                palette = palette,
                closeLabel = closeLabel,
                onClose = onClose,
                heading = heading,
                headerContent = headerContent,
            )
            Column(
                modifier = Modifier
                    .testTag(TripPlannerTestTags.StopCard.BODY)
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = dim.spacingM, bottom = if (footer == null) dim.spacingXL else 0.dp),
                verticalArrangement = Arrangement.spacedBy(dim.spacingM),
                content = content,
            )
            if (footer != null) {
                Box(
                    modifier = Modifier
                        .testTag(TripPlannerTestTags.StopCard.FOOTER)
                        .leaveRoom(floor)
                        .verticalScroll(rememberScrollState())
                        .padding(top = dim.spacingM, bottom = dim.spacingXL),
                ) {
                    footer()
                }
            }
        }
    }
}

/** The room the body keeps at 100 percent text, a Departure Board heading row (48 dp) and a little of what is under it. */
internal val BODY_FLOOR = 72.dp
private const val MAX_FLOOR_SCALE = 2f

/** The footer is measured with [floor] less room than the card has left, so the body always keeps [floor] of it. */
private fun Modifier.leaveRoom(floor: Dp): Modifier = layout { measurable, constraints ->
    val reserve = floor.roundToPx()
    val room = (constraints.maxHeight - reserve).coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = room))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/**
 * The header band: the [heading] and the 48 dp close button on one line, the title taking the rest of the width and wrapping
 * inside it, and [headerContent] under them. Its own colour, with the page edge inset on both sides.
 */
@Composable
private fun StopCardHeader(
    palette: StopCardPalette,
    closeLabel: String,
    onClose: () -> Unit,
    heading: @Composable () -> Unit,
    headerContent: (@Composable ColumnScope.() -> Unit)?,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = Modifier
            .testTag(TripPlannerTestTags.StopCard.HEADER)
            .fillMaxWidth()
            .background(palette.header)
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.weight(1f).heightIn(min = dim.textFieldHeight),
                contentAlignment = Alignment.CenterStart,
            ) {
                heading()
            }
            CloseButton(label = closeLabel, tint = palette.content, onClick = onClose)
        }
        headerContent?.invoke(this)
    }
}

/** The page edge inset on each side, which a block of a stop card puts on itself so another can run edge to edge. */
@Composable
internal fun Modifier.cardInset(): Modifier = padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)

/**
 * The close button: taj's [RoundIconButton] with no fill, a 48 dp target, and the card's own name
 * for a screen reader so it says which card it closes.
 */
@Composable
private fun CloseButton(label: String, tint: Color, onClick: () -> Unit) {
    RoundIconButton(
        onClick = onClick,
        color = Color.Transparent,
        onClickLabel = label,
        modifier = Modifier.testTag(TripPlannerTestTags.StopCard.CLOSE),
    ) {
        Image(
            painter = painterResource(TajRes.drawable.ic_close),
            contentDescription = label,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(KrailTheme.dimensions.iconS),
        )
    }
}
