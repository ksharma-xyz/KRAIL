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
    /** Navigate to a route */
    fun goTo(route: NavKey)

    /** Navigate back */
    fun pop()

    /** Clear backstack and navigate to route */
    fun resetRoot(route: NavKey)

    /** Replace current screen with new route */
    fun replaceCurrent(route: NavKey)

    /** Update app theme (temporary - see Navigator.kt for migration plan) */
    fun updateTheme(hexColorCode: String)

    @Deprecated("Use resetRoot() instead", ReplaceWith("resetRoot(route)"))
    fun clearBackStackAndNavigate(route: NavKey) = resetRoot(route)

    fun pushSingleInstance(route: NavKey)
}
