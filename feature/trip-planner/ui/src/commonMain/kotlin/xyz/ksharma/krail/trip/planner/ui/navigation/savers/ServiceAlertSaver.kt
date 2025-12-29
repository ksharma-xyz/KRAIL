package xyz.ksharma.krail.trip.planner.ui.navigation.savers

import androidx.compose.runtime.saveable.Saver
import kotlinx.collections.immutable.PersistentSet
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

fun serviceAlertSaver(): Saver<PersistentSet<ServiceAlert>, String> =
    persistentSetSaver(
        serialize = { it.toJsonString() },
        deserialize = { ServiceAlert.fromJsonString(it) },
    )
