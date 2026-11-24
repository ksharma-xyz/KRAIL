package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange

/**
 * Saves a [TextFieldState] as its text and its selection, and nothing else.
 *
 * The library's own `TextFieldState.Saver`, which `rememberTextFieldState` uses, also saves the
 * undo history. On iOS, restoring a history that holds even one edit throws
 * `ClassCastException: class kotlin.collections.asList$1 cannot be cast to class kotlin.Int`
 * from `UndoManager.createSaver(...).restore`, and every edit is recorded, a programmatic
 * `clearText()` included. Nothing in the app offers undo, so the history is not saved at all.
 *
 * A screen that stays on the back stack while another opens over it is restored from its saved
 * state when the rider comes back, so any text field on such a screen must use this saver.
 */
object TextFieldTextSaver : Saver<TextFieldState, Any> {
    private const val TEXT = 0
    private const val SELECTION_START = 1
    private const val SELECTION_END = 2

    override fun SaverScope.save(value: TextFieldState): Any =
        listOf(value.text.toString(), value.selection.start, value.selection.end)

    override fun restore(value: Any): TextFieldState {
        val saved = value as List<*>
        return TextFieldState(
            initialText = saved[TEXT] as String,
            initialSelection = TextRange(saved[SELECTION_START] as Int, saved[SELECTION_END] as Int),
        )
    }
}

/**
 * [androidx.compose.foundation.text.input.rememberTextFieldState] without the undo history, see
 * [TextFieldTextSaver]. The text and the cursor survive rotation and a return from the back stack.
 */
@Composable
fun rememberTextFieldTextState(initialText: String = ""): TextFieldState =
    rememberSaveable(saver = TextFieldTextSaver) { TextFieldState(initialText) }
