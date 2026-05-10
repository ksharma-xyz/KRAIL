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
import xyz.ksharma.krail.feature.debug.settings.state.EndpointScope
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * `SandookPreferences`-backed [DebugNetworkConfigStore].
 *
 * Persistence layout (one row per key in the `app_preferences` table):
 *
 * | Key                              | Type    | Meaning                              |
 * |----------------------------------|---------|--------------------------------------|
 * | `KEY_DEBUG_TARGET_TRIP_RESULTS`  | String  | `NetworkTarget.name` for that scope  |
 * | `KEY_DEBUG_TARGET_DEPARTURES`    | String  | `NetworkTarget.name` for that scope  |
 * | `KEY_DEBUG_TARGET_PARK_RIDE`     | String  | `NetworkTarget.name` for that scope  |
 * | `KEY_DEBUG_TARGET_TRACK`         | String  | `NetworkTarget.name` for that scope  |
 * | `KEY_DEBUG_KILL_SWITCH`          | Boolean | global force-NSW                     |
 * | `KEY_DEBUG_COMPARE_MODE`         | Boolean | dual-fetch + log-diff toggle         |
 *
 * Missing rows hydrate to [DebugSettingsState.default]. Unknown / corrupt
 * `NetworkTarget` strings (e.g. an enum value removed in a later app version)
 * fall back to the default for that scope rather than throwing.
 *
 * The store is a singleton — held in Koin as `single`. Hydration runs once
 * on construction (synchronous via `SandookPreferences`); subsequent reads
 * come from the in-memory `MutableStateFlow`. Writes serialize through a
 * mutex to prevent the read-modify-write race when two scopes are toggled
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

    override suspend fun set(event: DebugSettingsEvent) {
        withContext(ioDispatcher) {
            writeMutex.withLock {
                val next = when (event) {
                    is DebugSettingsEvent.SetTarget -> _state.value.copy(
                        targetsByScope = _state.value.targetsByScope.toMutableMap().apply {
                            put(event.scope, event.target)
                        },
                    )

                    is DebugSettingsEvent.SetKillSwitch -> _state.value.copy(
                        killSwitchEnabled = event.enabled,
                    )

                    is DebugSettingsEvent.SetCompareMode -> _state.value.copy(
                        compareModeEnabled = event.enabled,
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
        val targets = EndpointScope.entries.associateWith { scope ->
            val raw = preferences.getString(targetKey(scope))
            raw?.let { runCatching { NetworkTarget.valueOf(it) }.getOrNull() }
                ?: defaults.targetsByScope.getValue(scope)
        }
        return DebugSettingsState(
            targetsByScope = targets,
            killSwitchEnabled = preferences.getBoolean(KEY_DEBUG_KILL_SWITCH)
                ?: defaults.killSwitchEnabled,
            compareModeEnabled = preferences.getBoolean(KEY_DEBUG_COMPARE_MODE)
                ?: defaults.compareModeEnabled,
        )
    }

    private fun persist(next: DebugSettingsState, event: DebugSettingsEvent) {
        when (event) {
            is DebugSettingsEvent.SetTarget -> {
                preferences.setString(
                    targetKey(event.scope),
                    next.targetsByScope.getValue(event.scope).name,
                )
            }

            is DebugSettingsEvent.SetKillSwitch -> {
                preferences.setBoolean(KEY_DEBUG_KILL_SWITCH, next.killSwitchEnabled)
            }

            is DebugSettingsEvent.SetCompareMode -> {
                preferences.setBoolean(KEY_DEBUG_COMPARE_MODE, next.compareModeEnabled)
            }

            DebugSettingsEvent.Reset -> {
                EndpointScope.entries.forEach { preferences.deletePreference(targetKey(it)) }
                preferences.deletePreference(KEY_DEBUG_KILL_SWITCH)
                preferences.deletePreference(KEY_DEBUG_COMPARE_MODE)
            }
        }
    }

    companion object {
        const val KEY_DEBUG_KILL_SWITCH = "KEY_DEBUG_KILL_SWITCH"
        const val KEY_DEBUG_COMPARE_MODE = "KEY_DEBUG_COMPARE_MODE"

        private const val TARGET_KEY_PREFIX = "KEY_DEBUG_TARGET_"

        internal fun targetKey(scope: EndpointScope): String =
            "$TARGET_KEY_PREFIX${scope.name}"
    }
}
