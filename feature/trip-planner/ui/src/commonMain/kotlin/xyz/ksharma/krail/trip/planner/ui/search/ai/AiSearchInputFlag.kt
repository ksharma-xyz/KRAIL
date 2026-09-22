package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore

/**
 * Whether Ask KRAIL is switched on for this build: the debug override in debug builds, the
 * `ai_search_input_enabled` Remote Config flag otherwise.
 *
 * One definition because two surfaces read it, the Ask KRAIL dialog and `app_start`, and the
 * value reported on `app_start` is only meaningful if it is the same answer the dialog got.
 * Call it on each use rather than once: Remote Config can activate a new value mid-session.
 */
fun isAiSearchInputEnabled(
    isDebug: Boolean,
    debugNetworkConfigStore: DebugNetworkConfigStore,
    flag: Flag,
): Boolean = if (isDebug) {
    debugNetworkConfigStore.state.value.aiSearchInputEnabled
} else {
    flag.getFlagValue(FlagKeys.AI_SEARCH_INPUT_ENABLED.key).asBoolean(false)
}
