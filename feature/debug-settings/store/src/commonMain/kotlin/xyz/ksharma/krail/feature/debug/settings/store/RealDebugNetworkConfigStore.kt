package xyz.ksharma.krail.feature.debug.settings.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
 *
 * Missing rows hydrate to [DebugSettingsState.default]. Unknown / corrupt
 * enum strings (e.g. an enum value removed in a later app version) fall
 * back to the default rather than throwing.
 *
 * The store is a singleton, held in Koin as `single`. Hydration runs once
 * on construction (synchronous via `SandookPreferences`); subsequent reads
 * come from the in-memory `MutableStateFlow`. Writes serialise through a
 * mutex to prevent the read-modify-write race when two events fire
 * back-to-back.
 */
internal class RealDebugNetworkConfigStore(
    private val preferences: SandookPreferences,
    private val ioDispatcher: CoroutineDispatcher,
) : DebugNetworkConfigStore {

    private val writeMutex = Mutex()

    private val _state: MutableStateFlow<DebugSettingsState> =
        MutableStateFlow(hydrate())

    override val state: Flow<DebugSettingsState> = _state.asStateFlow()

    // ProDebugStore impl — maps from the shared state flow
    override val isProEnabled: Flow<Boolean> = _state.map { it.isProEnabled }

    override suspend fun source(): NetworkSource = _state.value.source

    override suspend fun setProEnabled(enabled: Boolean) {
        set(DebugSettingsEvent.TogglePro(enabled))
    }

    override suspend fun set(event: DebugSettingsEvent) {
        withContext(ioDispatcher) {
            writeMutex.withLock {
                val next = when (event) {
                    is DebugSettingsEvent.SetSource -> _state.value.copy(source = event.source)
                    is DebugSettingsEvent.TogglePro -> _state.value.copy(isProEnabled = event.enabled)
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
        val isProEnabled = preferences.getString(KEY_DEBUG_PRO_ENABLED)?.toBoolean() ?: false
        return DebugSettingsState(source = source, isProEnabled = isProEnabled)
    }

    private fun persist(next: DebugSettingsState, event: DebugSettingsEvent) {
        when (event) {
            is DebugSettingsEvent.SetSource -> {
                preferences.setString(KEY_DEBUG_NETWORK_SOURCE, next.source.name)
            }

            is DebugSettingsEvent.TogglePro -> {
                preferences.setString(KEY_DEBUG_PRO_ENABLED, next.isProEnabled.toString())
            }

            DebugSettingsEvent.Reset -> {
                preferences.deletePreference(KEY_DEBUG_NETWORK_SOURCE)
                preferences.deletePreference(KEY_DEBUG_PRO_ENABLED)
            }
        }
    }

    companion object {
        const val KEY_DEBUG_NETWORK_SOURCE = "KEY_DEBUG_NETWORK_SOURCE"
        const val KEY_DEBUG_PRO_ENABLED = "KEY_DEBUG_PRO_ENABLED"
    }
}
