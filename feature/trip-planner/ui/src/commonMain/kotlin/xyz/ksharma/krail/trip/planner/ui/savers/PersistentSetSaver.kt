package xyz.ksharma.krail.trip.planner.ui.savers

import androidx.compose.runtime.saveable.Saver
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

/**
 * Generic Saver for PersistentSet of serializable items.
 * Reusable for any @Serializable type within trip-planner module.
 *
 * @param serialize Function to convert item to JSON string
 * @param deserialize Function to convert JSON string back to item (returns null on failure)
 * @param separator String used to separate multiple items (default: "|||")
 */
fun <T> persistentSetSaver(
    serialize: (T) -> String,
    deserialize: (String) -> T?,
    separator: String = "|||"
): Saver<PersistentSet<T>, String> = Saver(
    save = { set ->
        set.joinToString(separator = separator) { serialize(it) }
    },
    restore = { savedString ->
        if (savedString.isEmpty()) {
            persistentSetOf()
        } else {
            savedString.split(separator)
                .mapNotNull(deserialize)
                .toPersistentSet()
        }
    }
)

