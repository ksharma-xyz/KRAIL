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
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.TextField
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
    val toLeading: @Composable () -> Unit,
    val toTrailing: (@Composable () -> Unit)?,
    val fromTrailing: (@Composable () -> Unit)?,
    val showGo: Boolean,
    val onGoClick: () -> Unit,
)

/**
 * The To and From block as two rows on one fixed dark card (`docs/MAP_HOME_UX.md` rules 33, 35 and 36).
 *
 * Neither bar has a fill, a ring or a card of its own: both are taj's [TextField] in [FloatingDark.barColors],
 * the same colours whether the bar has focus or not, so nothing swaps between dark and cream when focus moves.
 * The bar being edited holds the one query field. The other is the same field unfocused: it shows its stop,
 * or its placeholder, and a tap on it is the tap that edits it. A thin divider with [rowGap] above and below
 * sits between the rows, and each row is 48 tall with the same two 48 columns [toFromBarInsets] measures, so
 * the back arrow, the origin dot, the clear X and the go circle sit where they always did.
 */
@Composable
internal fun ToFromFloatingRows(
    rows: FloatingRowsContent,
    insets: ToFromBarInsets,
    rowGap: Dp,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val ink = FloatingDark.content
    val fromInk = FloatingDark.container
    Column(modifier = modifier.fillMaxWidth()) {
        FloatingBar(
            bar = rows.to,
            isEditing = ToFromField.To.hasDarkFill(rows.editing),
            editor = rows.editor,
            insets = insets,
            leading = rows.toLeading,
            trailing = rows.toTrailing,
        )
        // The divider and the gaps around it belong to the From row, so a hidden From leaves nothing under To.
        AnimatedVisibility(
            visible = rows.showFrom,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(350)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.padding(vertical = rowGap))
                FloatingBar(
                    bar = rows.from,
                    isEditing = ToFromField.From.hasDarkFill(rows.editing),
                    editor = rows.editor,
                    insets = insets,
                    leading = { OriginDot() },
                    trailing = rows.fromTrailing
                        ?: if (rows.showGo) {
                            {
                                GoCircle(
                                    circle = ink,
                                    arrow = fromInk,
                                    diameter = goCircleDiameter(insets.column, dim.spacingXS),
                                    onClick = rows.onGoClick,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

/** One row: the query field when it is the one being edited and an editor is there, else the same field idle. */
@Composable
private fun FloatingBar(
    bar: BarContent,
    isEditing: Boolean,
    editor: ToFromEditor?,
    insets: ToFromBarInsets,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    CompositionLocalProvider(LocalContentColor provides FloatingDark.content) {
        if (isEditing && editor != null) {
            EditingBar(bar, editor, insets, FloatingDark.barColors(), leading, trailing)
        } else {
            IdleFloatingBar(bar, insets, leading, trailing)
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
            colors = FloatingDark.barColors(),
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
