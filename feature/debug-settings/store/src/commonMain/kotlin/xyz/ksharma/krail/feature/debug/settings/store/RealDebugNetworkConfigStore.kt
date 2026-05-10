package xyz.ksharma.krail.feature.debug.settings.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * `SandookPreferences`-backed [DebugNetworkConfigStore].
 *
 * Persistence layout (one row per key in the `app_preferences` table):
 *
 * | Key                          | Type    | Meaning                            |
 * |------------------------------|---------|------------------------------------|
 * | `KEY_DEBUG_NETWORK_TARGET`   | String  | `NetworkTarget.name` selection     |
 * | `KEY_DEBUG_FLAG_OVERRIDE`    | String  | `FlagOverride.name` selection      |
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

    override suspend fun flagOverride(): FlagOverride = _state.value.flagOverride

    override suspend fun networkTarget(): NetworkTarget = _state.value.networkTarget

    override suspend fun set(event: DebugSettingsEvent) {
        withContext(ioDispatcher) {
            writeMutex.withLock {
                val next = when (event) {
                    is DebugSettingsEvent.SetNetworkTarget -> _state.value.copy(
                        networkTarget = event.target,
                    )

                    is DebugSettingsEvent.SetFlagOverride -> _state.value.copy(
                        flagOverride = event.override,
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
        val target = preferences.getString(KEY_DEBUG_NETWORK_TARGET)
            ?.let { runCatching { NetworkTarget.valueOf(it) }.getOrNull() }
            ?: defaults.networkTarget
        val override = preferences.getString(KEY_DEBUG_FLAG_OVERRIDE)
            ?.let { runCatching { FlagOverride.valueOf(it) }.getOrNull() }
            ?: defaults.flagOverride
        return DebugSettingsState(networkTarget = target, flagOverride = override)
    }

    private fun persist(next: DebugSettingsState, event: DebugSettingsEvent) {
        when (event) {
            is DebugSettingsEvent.SetNetworkTarget -> {
                preferences.setString(KEY_DEBUG_NETWORK_TARGET, next.networkTarget.name)
            }

            is DebugSettingsEvent.SetFlagOverride -> {
                preferences.setString(KEY_DEBUG_FLAG_OVERRIDE, next.flagOverride.name)
            }

            DebugSettingsEvent.Reset -> {
                preferences.deletePreference(KEY_DEBUG_NETWORK_TARGET)
                preferences.deletePreference(KEY_DEBUG_FLAG_OVERRIDE)
            }
        }
    }

    companion object {
        const val KEY_DEBUG_NETWORK_TARGET = "KEY_DEBUG_NETWORK_TARGET"
        const val KEY_DEBUG_FLAG_OVERRIDE = "KEY_DEBUG_FLAG_OVERRIDE"
    }
}
