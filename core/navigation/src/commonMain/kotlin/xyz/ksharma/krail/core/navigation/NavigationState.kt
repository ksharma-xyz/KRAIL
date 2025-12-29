package xyz.ksharma.krail.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration

/**
 * Create a navigation state that persists config changes and process death.
 * Uses polymorphic serialization for multiplatform support (iOS, Web).
 *
 * @param startRoute The initial route to show
 * @param topLevelRoutes All top-level routes in the app
 * @param serializationConfig Serialization configuration for NavKey persistence
 * @return NavigationState instance
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    serializationConfig: SavedStateConfiguration,
): NavigationState {
    // Use remember instead of rememberSaveable since NavBackStack handles persistence
    val topLevelRoute = remember {
        mutableStateOf(startRoute)
    }

    // Use polymorphic serialization config for multiplatform support
    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(serializationConfig, key)
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * State holder for navigation state.
 *
 * Manages the navigation back stacks for different top-level routes.
 * This enables features like:
 * - Multiple back stacks (e.g., for bottom navigation)
 * - State preservation across configuration changes
 * - Proper back navigation behavior
 *
 * @param startRoute The start route. The user will exit the app through this route.
 * @param topLevelRoute The current top level route
 * @param backStacks The back stacks for each top level route
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    /**
     * Navigate to a route.
     * - If route is a top-level route → Switch to that stack
     * - If route is nested → Add to current stack
     */
    fun goTo(route: NavKey) {
        if (route in backStacks.keys) {
            topLevelRoute = route
        } else {
            backStacks[topLevelRoute]?.add(route)
        }
    }

    /**
     * Navigate back in the current stack.
     * - If at top-level route → Switch to start route
     * - If in nested navigation → Remove current route from stack
     */
    fun pop() {
        val currentStack = backStacks[topLevelRoute] ?: return
        val currentRoute = currentStack.last()

        if (currentRoute == topLevelRoute) {
            topLevelRoute = startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /**
     * Clear entire back stack and navigate to route.
     * Used when user shouldn't be able to navigate back.
     */
    fun resetRoot(route: NavKey) {
        val currentStack = backStacks[topLevelRoute]
        currentStack?.clear()
        currentStack?.add(route)
    }

    /**
     * Replace current screen with new route.
     * Equivalent to navigate with popUpTo inclusive.
     */
    fun replaceCurrent(route: NavKey) {
        val currentStack = backStacks[topLevelRoute]
        currentStack?.removeLastOrNull()
        goTo(route)
    }

    /**
     * Check if there's a previous entry in the back stack.
     */
    fun hasPreviousEntry(): Boolean {
        val currentStack = backStacks[topLevelRoute] ?: return false
        return currentStack.size > 1
    }
}

@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
