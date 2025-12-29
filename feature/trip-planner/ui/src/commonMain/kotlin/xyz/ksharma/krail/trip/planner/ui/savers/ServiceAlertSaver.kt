package xyz.ksharma.krail.trip.planner.ui.savers

import androidx.compose.runtime.saveable.Saver
import kotlinx.collections.immutable.PersistentSet
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

/**
 * Convenience Saver for PersistentSet<ServiceAlert>.
 * Survives configuration changes (rotation, etc.)
 *
 * Usage:
 * ```kotlin
 * var alerts by rememberSaveable(stateSaver = serviceAlertSaver()) {
 *     mutableStateOf(persistentSetOf())
 * }
 * ```
 */
fun serviceAlertSaver(): Saver<PersistentSet<ServiceAlert>, String> =
    persistentSetSaver(
        serialize = { it.toJsonString() },
        deserialize = { ServiceAlert.fromJsonString(it) },
    )
