package xyz.ksharma.krail.trip.planner.ui.navigation.savers

import androidx.compose.runtime.saveable.Saver
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

/**
 * Saves a [StopDisplay] as its three strings, so a screen holding "which stop is the rider
 * looking at" keeps it across an Activity recreation.
 *
 * An absent label is stored as the empty string and read back as null. A label the rider has
 * set is never empty, so the two cannot be confused, and it keeps the saved form a plain list
 * of strings — the shape a Bundle can hold without any further help.
 */
fun stopDisplaySaver(): Saver<StopDisplay?, List<String>> = Saver(
    save = { stop ->
        stop?.let { listOf(it.stopId, it.name, it.label.orEmpty()) } ?: emptyList()
    },
    restore = { saved ->
        saved.takeIf { it.size == SAVED_FIELD_COUNT }?.let { fields ->
            StopDisplay(
                stopId = fields[0],
                name = fields[1],
                label = fields[2].takeIf(String::isNotEmpty),
            )
        }
    },
)

private const val SAVED_FIELD_COUNT = 3
