package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Search Stop stays on the back stack while the trip results are open, so its text field is saved
 * when the rider taps go and restored when they come back. These tests walk that save and restore
 * with a field that has been typed in and cleared, which is the state the rider leaves it in.
 *
 * This file runs on the iOS simulator as well (`:taj` is in the iOS unit test lane), and that is
 * where the round trip used to abort the app: the library saver throws on iOS when the saved undo
 * history is not empty.
 */
class TextFieldTextSaverTest {

    private val scope = SaverScope { true }

    private fun TextFieldState.saved(): Any = with(TextFieldTextSaver) { scope.save(this@saved) }
        .let { assertNotNull(it) }

    /** The field as go leaves it: a stop typed and picked twice, each pick clearing the text. */
    private fun fieldAfterTwoPicks() = TextFieldState().apply {
        setTextAndPlaceCursorAtEnd("Central")
        clearText()
        setTextAndPlaceCursorAtEnd("Town Hall")
        clearText()
    }

    @Test
    fun fieldWithEditHistory_restoresWithoutThrowing() {
        val restored = TextFieldTextSaver.restore(fieldAfterTwoPicks().saved())

        assertEquals("", restored.text.toString())
        assertEquals(TextRange(0), restored.selection)
    }

    @Test
    fun typedText_andCursor_comeBack() {
        val field = TextFieldState().apply {
            setTextAndPlaceCursorAtEnd("Central")
            clearText()
            setTextAndPlaceCursorAtEnd("Town Hall")
            edit { selection = TextRange(2, 6) }
        }

        val restored = TextFieldTextSaver.restore(field.saved())

        assertEquals("Town Hall", restored.text.toString())
        assertEquals(TextRange(2, 6), restored.selection)
    }

    @Test
    fun savedForm_isTextAndSelectionOnly_neverTheUndoHistory() {
        val field = fieldAfterTwoPicks().apply { setTextAndPlaceCursorAtEnd("Cen") }

        assertEquals(listOf<Any>("Cen", 3, 3), field.saved())
    }
}
