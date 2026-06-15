package xyz.ksharma.krail.feature.debug.settings.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * `SandookPreferences`-backed [DebugNetworkConfigStore].
 *
 * Persistence layout (one row per key in the `app_preferences` table):
 *
 * | Key                         | Type    | Meaning                          |
 * |-----------------------------|---------|----------------------------------|
 * | `KEY_DEBUG_NETWORK_SOURCE`  | String  | `NetworkSource.name` selection   |
 * | `KEY_TRIP_TRACKING_ENABLED` | String  | "true"/"false" override          |
 *
 * Missing rows hydrate to [DebugSettingsState.default]. Unknown / corrupt
 * values fall back to the default rather than throwing.
 */
internal class RealDebugNetworkConfigStore(
    private val preferences: SandookPreferences,
    private val ioDispatcher: CoroutineDispatcher,
) : DebugNetworkConfigStore {

    private val writeMutex = Mutex()

    private val _state: MutableStateFlow<DebugSettingsState> =
        MutableStateFlow(hydrate())

    override val state: StateFlow<DebugSettingsState> = _state.asStateFlow()

    override suspend fun source(): NetworkSource = _state.value.source

    override suspend fun set(event: DebugSettingsEvent) {
        withContext(ioDispatcher) {
            writeMutex.withLock {
                val next = when (event) {
                    is DebugSettingsEvent.SetSource -> _state.value.copy(source = event.source)
                    is DebugSettingsEvent.SetTripTrackingEnabled -> _state.value.copy(
                        tripTrackingEnabled = event.enabled,
                    )
                    DebugSettingsEvent.Reset -> DebugSettingsState.default()
                }

                persist(next, event)
                _state.value = next
                log("DebugNetworkConfigStore: applied $event, state=$next")
            }
        }
    }

    private fun hydrate(): DebugSettingsState {
        val defaults = DebugSettingsState.default()
        val source = preferences.getString(KEY_DEBUG_NETWORK_SOURCE)
            ?.let { runCatching { NetworkSource.valueOf(it) }.getOrNull() }
            ?: defaults.source
        val tripTrackingEnabled = preferences.getString(KEY_TRIP_TRACKING_ENABLED)
            ?.toBooleanStrictOrNull()
            ?: defaults.tripTrackingEnabled
        return DebugSettingsState(source = source, tripTrackingEnabled = tripTrackingEnabled)
    }

    private fun persist(next: DebugSettingsState, event: DebugSettingsEvent) {
        when (event) {
            is DebugSettingsEvent.SetSource ->
                preferences.setString(KEY_DEBUG_NETWORK_SOURCE, next.source.name)

            is DebugSettingsEvent.SetTripTrackingEnabled ->
                preferences.setString(KEY_TRIP_TRACKING_ENABLED, next.tripTrackingEnabled.toString())

            DebugSettingsEvent.Reset -> {
                preferences.deletePreference(KEY_DEBUG_NETWORK_SOURCE)
                preferences.deletePreference(KEY_TRIP_TRACKING_ENABLED)
            }
        }
    }

    companion object {
        const val KEY_DEBUG_NETWORK_SOURCE = "KEY_DEBUG_NETWORK_SOURCE"
        const val KEY_TRIP_TRACKING_ENABLED = "KEY_TRIP_TRACKING_ENABLED"
    }
}
