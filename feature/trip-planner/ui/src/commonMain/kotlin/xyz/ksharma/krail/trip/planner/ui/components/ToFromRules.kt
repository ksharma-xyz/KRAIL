package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

// The rules of the To and From block (rules 34 to 36 of `docs/MAP_HOME_UX.md`) that are plain
// functions, so each is pinned without a layout.

/** The field the rider is editing, which takes the dark fill. Null is neither, as on home. */
internal enum class ToFromField { To, From }

/** How a `ToFromBlock` draws its two bars. */
internal sealed interface ToFromBlockStyle {

    /** The home's own look: the idle bar is taj's text field button and the bar being edited is the dark text field. */
    data object Classic : ToFromBlockStyle

    /**
     * Search Stop's two soft pills on the one card the caller draws: both are the same taj text field in the soft
     * pill colours, [rowGap] apart with no divider, and which bar has focus changes the caret and the keyboard and
     * nothing else.
     */
    data class FloatingRows(val rowGap: Dp) : ToFromBlockStyle
}

/**
 * Whether this field is the dark bar while [editing] is being edited: only the one edited. The
 * bar being edited is the dark text field and the other is the light button, so the dark fill
 * never moves and no row moves with it.
 */
internal fun ToFromField.hasDarkFill(editing: ToFromField?): Boolean = this == editing

/**
 * Diameter of the go circle: its [column] less [margin] on every side (rule 36, 40 dp in a 48 dp
 * column with 4 dp around it). Pure, so the rule is pinned without a layout.
 */
internal fun goCircleDiameter(column: Dp, margin: Dp): Dp = column - margin * 2

/**
 * What a screen reader says for a field. A field with a stop says which end it is and the stop in
 * full (the text on screen may end in an ellipsis, this never does). An empty field says its own
 * placeholder, which already names it.
 */
internal fun toFromFieldLabel(role: String, placeholder: String, stopName: String?): String =
    if (stopName.isNullOrBlank()) placeholder else "$role, $stopName"

/**
 * The room a bar keeps for its two end columns, so a stop name never runs under a control.
 *
 * Each end has a [column] for the control. The start column is followed by [startGap] before the
 * text, and the end column by [endGap], which is the gap taj's text field keeps between its text
 * and a trailing control. The idle bar (taj's text field button) and the editing bar (taj's text
 * field) both reserve exactly these, which is why nothing moves when the dark fill does.
 */
@Immutable
internal class ToFromBarInsets(val column: Dp, val startGap: Dp, val endGap: Dp) {

    /** Where the text starts, measured from the bar's start edge. */
    val textStart: Dp get() = column + startGap

    /** What the text leaves at the bar's end edge when the bar has an end control. */
    val textEndReserved: Dp get() = column + endGap

    /** Both together: the width a bar gives up to its own controls, which the action placement needs. */
    val reserved: Dp get() = textStart + textEndReserved
}

/**
 * The text field the bar being edited holds, in place of its stop. Search Stop supplies one so the
 * rider types where the stop is shown. The block draws it as taj's text field, so everything
 * specific to the field (focus, the tag, the length cap, the filter) is carried here by the caller.
 */
@Immutable
internal class ToFromEditor(
    val state: TextFieldState,
    val modifier: Modifier = Modifier,
    val maxLength: Int = Int.MAX_VALUE,
    val filter: (CharSequence) -> CharSequence = { it },
    val onTextChange: (CharSequence) -> Unit = {},
)
