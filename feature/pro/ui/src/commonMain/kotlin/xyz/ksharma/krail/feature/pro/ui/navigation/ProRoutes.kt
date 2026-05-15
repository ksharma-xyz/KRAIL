package xyz.ksharma.krail.feature.pro.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface ProRoute : NavKey

@Serializable
data object ProUpgradeRoute : ProRoute
