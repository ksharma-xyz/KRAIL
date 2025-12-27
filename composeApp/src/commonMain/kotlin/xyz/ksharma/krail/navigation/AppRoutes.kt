package xyz.ksharma.krail.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * App-level routes (not feature-specific)
 */
@Serializable
data object SplashRoute : NavKey

@Serializable
data object AppUpgradeRoute : NavKey

