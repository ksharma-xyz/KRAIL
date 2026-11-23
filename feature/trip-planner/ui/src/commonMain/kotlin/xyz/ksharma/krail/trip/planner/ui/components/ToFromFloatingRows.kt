package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

/** What the two floating rows say and do, in one holder so the block hands it over as one argument. */
@Immutable
internal class FloatingRowsContent(
    val to: BarContent,
    val from: BarContent,
    val editing: ToFromField?,
    val editor: ToFromEditor?,
    val showFrom: Boolean,
    val firstLeading: @Composable () -> Unit,
    val toTrailing: (@Composable () -> Unit)?,
    val fromTrailing: (@Composable () -> Unit)?,
    val showGo: Boolean,
    val onGoClick: () -> Unit,
)

/**
 * The To and From block as two soft pills on one card (`docs/MAP_HOME_UX.md` rules 33, 35 and 36), From on top and To
 * below it.
 *
 * Both bars are taj's [TextField] in [softFieldColors]: a pill that is a touch darker than the card in light mode
 * and a touch lighter in dark mode, with the page's own text colour, so they read as shapes without a border and
 * follow the app mode. The colours are the same whether the bar has focus or not, so nothing swaps between fills
 * when focus moves. The bar being edited holds the one query field, empty with its fixed prompt as the placeholder. The
 * other is the same field unfocused: it shows its stop as its text, or its prompt, and a tap on it is the tap that
 * edits it. [rowGap] separates the pills, with no divider, and each pill is 48 tall with the same two 48 columns
 * [toFromBarInsets] measures. The first bar's start column is [FloatingRowsContent.firstLeading], the back arrow, which is
 * From's when both show and To's when it is the only one; the second bar's is the destination pin. The go circle is the
 * last bar's end column and the clear X is the end column of the bar being edited. A pill is one line and its text ends
 * in an ellipsis, so the pair is the same height at every text size.
 */
@Composable
internal fun ToFromFloatingRows(
    rows: FloatingRowsContent,
    insets: ToFromBarInsets,
    rowGap: Dp,
    modifier: Modifier = Modifier,
    fixedLight: Boolean = false,
) {
    val colors = floatingFieldColors(fixedLight)
    Column(modifier = modifier.fillMaxWidth()) {
        // The gap belongs to the From row, so a hidden From leaves nothing above To.
        AnimatedVisibility(
            visible = rows.showFrom,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(350)),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = rowGap)) {
                FloatingBar(
                    bar = rows.from,
                    isEditing = ToFromField.From.hasDarkFill(rows.editing),
                    editor = rows.editor,
                    insets = insets,
                    colors = colors,
                    leading = rows.firstLeading,
                    trailing = rows.fromTrailing,
                )
            }
        }
        FloatingBar(
            bar = rows.to,
            isEditing = ToFromField.To.hasDarkFill(rows.editing),
            editor = rows.editor,
            insets = insets,
            colors = colors,
            leading = if (rows.showFrom) {
                { DestinationPin() }
            } else {
                rows.firstLeading
            },
            trailing = rows.toTrailing ?: if (rows.showGo) {
                { FloatingGoCircle(insets = insets, onClick = rows.onGoClick) }
            } else {
                null
            },
        )
    }
}

/**
 * The bars' colours: the shared [softFieldColors], or, when [fixedLight] is set, the Search Stop box's own look from
 * [mapCardColors]: its tint for the fill, its ink for the text and caret and its placeholder. On the map that is the light palette
 * in both app modes, and on the gradient of the list state it is the app mode's own (see `CardGroundScope`).
 */
@Composable
private fun floatingFieldColors(fixedLight: Boolean): TextFieldColors {
    if (!fixedLight) return softFieldColors()
    val card = mapCardColors()
    return TextFieldColors(
        containerColor = card.tint,
        contentColor = card.onTint,
        placeholderColor = card.placeholder,
        cursorColor = card.onTint,
    )
}

/**
 * The go circle of the floating rows: the opposite of the pill, its text colour for the circle and its fill for the
 * arrow. It is the last bar's end column, and a caller that fills that column itself (the clear X while typing)
 * draws this when it has nothing else to show.
 */
@Composable
internal fun FloatingGoCircle(insets: ToFromBarInsets, onClick: () -> Unit) {
    val colors = softFieldColors()
    GoCircle(
        circle = colors.contentColor,
        arrow = colors.containerColor,
        diameter = goCircleDiameter(insets.column, KrailTheme.dimensions.spacingXS),
        onClick = onClick,
    )
}

/** One row: the query field when it is the one being edited and an editor is there, else the same field idle. */
@Composable
private fun FloatingBar(
    bar: BarContent,
    isEditing: Boolean,
    editor: ToFromEditor?,
    insets: ToFromBarInsets,
    colors: TextFieldColors,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        if (isEditing && editor != null) {
            EditingBar(bar, editor, insets, colors, leading, trailing)
        } else {
            IdleFloatingBar(bar, insets, colors, leading, trailing)
        }
    }
}

/**
 * The bar at rest: the same taj [TextField], read only, holding its stop as text or showing its placeholder.
 * It cannot take focus, and a tap over the text between its two columns is the tap that edits it. The two
 * columns stay the controls' own, so the back arrow, the origin dot and the go circle are never under that tap.
 */
@Composable
private fun IdleFloatingBar(
    bar: BarContent,
    insets: ToFromBarInsets,
    colors: TextFieldColors,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    val state = remember(bar.text, bar.hasStop) { TextFieldState(if (bar.hasStop) bar.text else "") }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(bar.tag)
            .semantics { selected = false },
    ) {
        TextField(
            state = state,
            placeholder = bar.placeholder,
            readOnly = true,
            modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
            colors = colors,
            leadingIcon = { BarSlot(insets.column, leading) },
            trailingIcon = trailing?.let { slot -> { BarSlot(insets.column, slot) } },
            contentPadding = PaddingValues(end = if (trailing == null) KrailTheme.dimensions.spacingXL else 0.dp),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = insets.column, end = if (trailing != null) insets.column else 0.dp)
                .clip(RoundedCornerShape(50))
                .klickable(onClick = bar.onClick)
                .semantics { contentDescription = bar.label },
        )
    }
}
