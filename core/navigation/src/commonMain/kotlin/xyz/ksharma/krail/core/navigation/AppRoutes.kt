package xyz.ksharma.krail.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * App-level routes (not feature-specific)
 * These are top-level navigation destinations.
 */
@Serializable
data object SplashRoute : NavKey

@Serializable
data object AppUpgradeRoute : NavKey
