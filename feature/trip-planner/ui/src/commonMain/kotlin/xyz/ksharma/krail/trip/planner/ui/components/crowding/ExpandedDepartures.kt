package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * Which departure rows the rider has opened, by `StopDeparture.rowKey`, so a row stays open
 * through a rotation and through every poll that replaces the list.
 *
 * Remembered once at the top of the board and handed down through [LocalExpandedDepartures],
 * because the board draws its rows from several branches (upcoming, previous plus upcoming,
 * filtered); state remembered inside the row list would be dropped each time the branch
 * changes. Null outside a board, which leaves rows closed and not tappable.
 */
@Stable
internal class ExpandedDepartures(initial: Set<String> = emptySet()) {
    private var keys by mutableStateOf(initial)

    fun isOpen(key: String): Boolean = key in keys

    fun toggle(key: String) {
        keys = if (key in keys) keys - key else keys + key
    }

    companion object {
        val Saver = listSaver<ExpandedDepartures, String>(
            save = { it.keys.toList() },
            restore = { ExpandedDepartures(it.toSet()) },
        )
    }
}

@Composable
internal fun rememberExpandedDepartures(): ExpandedDepartures =
    rememberSaveable(saver = ExpandedDepartures.Saver) { ExpandedDepartures() }

internal val LocalExpandedDepartures = staticCompositionLocalOf<ExpandedDepartures?> { null }

/**
 * Makes a departure row open and close, announced as a button with its expanded state. A row
 * that cannot open is left exactly as it was: not clickable and not announced as a control.
 */
internal fun Modifier.departureRowToggle(canOpen: Boolean, isExpanded: Boolean, onToggle: (() -> Unit)?): Modifier =
    if (canOpen && onToggle != null) {
        clickable(role = Role.Button, onClick = onToggle)
            .semantics { stateDescription = if (isExpanded) "Expanded" else "Collapsed" }
    } else {
        this
    }
