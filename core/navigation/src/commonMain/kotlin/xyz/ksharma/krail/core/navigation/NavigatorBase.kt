package xyz.ksharma.krail.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Base navigator interface that can be implemented by the app's Navigator
 * and consumed by feature modules without creating circular dependencies.
 *
 * This interface lives in the core:navigation module, which both the app
 * and feature modules depend on.
 */
interface NavigatorBase {
    fun navigate(route: NavKey)
    fun goBack()
    fun updateTheme(hexColorCode: String)
    fun clearBackStackAndNavigate(route: NavKey)
}

