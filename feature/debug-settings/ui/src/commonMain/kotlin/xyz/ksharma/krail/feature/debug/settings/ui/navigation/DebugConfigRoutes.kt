package xyz.ksharma.krail.feature.debug.settings.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Marker interface for all Debug Config routes. */
sealed interface DebugConfigRoute : NavKey

/** Top-level Debug Config screen, list of category tiles. */
@Serializable
data object DebugConfigHomeRoute : DebugConfigRoute

/** Network target picker (BFF Local vs BFF Prod). */
@Serializable
data object DebugConfigNetworkRoute : DebugConfigRoute

/** Feature flag override picker for the `enable_proto_bff` rollout flag. */
@Serializable
data object DebugConfigFeatureFlagsRoute : DebugConfigRoute
