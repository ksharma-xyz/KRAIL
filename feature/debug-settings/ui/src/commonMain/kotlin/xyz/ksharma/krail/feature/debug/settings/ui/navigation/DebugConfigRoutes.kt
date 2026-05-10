package xyz.ksharma.krail.feature.debug.settings.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Marker interface for all Debug Config routes. */
sealed interface DebugConfigRoute : NavKey

/** Top-level Debug Config screen, list of category tiles. */
@Serializable
data object DebugConfigHomeRoute : DebugConfigRoute

/** Network source picker. */
@Serializable
data object DebugConfigNetworkRoute : DebugConfigRoute
