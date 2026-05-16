package xyz.ksharma.krail.feature.pro.state

import kotlinx.coroutines.flow.Flow

/**
 * Debug-only toggle for forcing KRAIL Pro active without real IAP.
 *
 * Implemented by `RealDebugNetworkConfigStore` in `:feature:debug-settings:store`.
 * The UI gate (Debug Config tile visible only when `AppInfo.isDebug`) ensures
 * this toggle is never reachable in production.
 */
interface ProDebugStore {
    /** Emits `true` when the debug Pro override is active, `false` otherwise. */
    val isProEnabled: Flow<Boolean>

    suspend fun setProEnabled(enabled: Boolean)
}
