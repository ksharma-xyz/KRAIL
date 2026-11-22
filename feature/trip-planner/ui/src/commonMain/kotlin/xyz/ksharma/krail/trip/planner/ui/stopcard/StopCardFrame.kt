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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.krail.taj.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingContent
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
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
 * The shell of a stop card, the saved trip card's own shape: the card corners and 16 dp above and below,
 * one block under another 8 dp apart (`docs/MAP_HOME_UX.md`, the card shell and Stop card). Its surface is
 * the fixed dark and its content the fixed light ([FloatingDark]), the same in light and dark app modes,
 * because the map under it is always light and a translucent theme wash is unreadable over it. It is the
 * card's only surface: nothing inside it draws a card, a background or an inner padding of its own, so a
 * block sits straight on it.
 *
 * Line 1 is the [heading] with the 48 dp close button at the right. A card closes, a field does not.
 * Everything under it scrolls inside the room the card has, so the close button never leaves the
 * screen. The whole card is one region for a screen reader, announced by [announcement] as it opens.
 *
 * The page edge inset, [KrailDimensions.pageHorizontalPadding], is applied once. The heading line
 * has it here and every block of [content] has it itself ([cardInset], or the departure rows' own
 * padding), so a block that runs edge to edge, such as the board's dividers and filter row, can, and
 * text in every block starts on the same left edge.
 */
@Composable
internal fun StopCardFrame(
    announcement: String,
    closeLabel: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    heading: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .testTag(TripPlannerTestTags.StopCard.CARD)
            .fillMaxWidth()
            .clip(CardShape)
            .background(FloatingDark.container, CardShape)
            .semantics {
                paneTitle = announcement
                isTraversalGroup = true
            }
            .padding(vertical = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        FloatingContent {
            Row(
                modifier = Modifier.fillMaxWidth().cardInset(),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = dim.textFieldHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    heading()
                }
                CloseButton(label = closeLabel, onClick = onClose)
            }
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dim.spacingM),
                content = content,
            )
        }
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
private fun CloseButton(label: String, onClick: () -> Unit) {
    RoundIconButton(
        onClick = onClick,
        color = Color.Transparent,
        onClickLabel = label,
        modifier = Modifier.testTag(TripPlannerTestTags.StopCard.CLOSE),
    ) {
        Image(
            painter = painterResource(TajRes.drawable.ic_close),
            contentDescription = label,
            colorFilter = ColorFilter.tint(FloatingDark.content),
            modifier = Modifier.size(KrailTheme.dimensions.iconS),
        )
    }
}
