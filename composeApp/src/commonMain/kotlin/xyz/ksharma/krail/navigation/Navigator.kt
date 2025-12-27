package xyz.ksharma.krail.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.trip.planner.ui.api.SearchStopFieldType

/**
 * Handles navigation events and result passing between screens.
 */
class Navigator(val state: NavigationState) {

    // For passing results between screens
    // replay = 1 ensures the result is cached for when the destination screen becomes active
    private val _results = MutableSharedFlow<NavigationResult>(replay = 1)
    val results: SharedFlow<NavigationResult> = _results.asSharedFlow()

    /**
     * App theme color state.
     * Exposed for ThemeSelectionScreen to update global theme.
     */
    var themeColor: String by mutableStateOf(DEFAULT_THEME_STYLE.hexColorCode)
        private set

    fun updateTheme(hexColorCode: String) {
        themeColor = hexColorCode
    }

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it.
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /**
     * Emit a result without navigating.
     * Caller should call goBack() separately after emitting.
     */
    suspend fun emitResult(result: NavigationResult) {
        println("Navigator: Emitting result: $result")
        _results.emit(result)
        println("Navigator: Result emitted")
    }

    /**
     * Navigate back with a result.
     * This is a convenience method that emits and then navigates back.
     */
    suspend fun goBackWithResult(result: NavigationResult) {
        emitResult(result)
        println("Navigator: Navigating back after result")
        goBack()
    }

    /**
     * Navigate and replace current screen (like navigate with popUpTo inclusive).
     * Used for Splash → SavedTrips/Intro navigation.
     */
    fun navigateAndReplace(route: NavKey) {
        val currentStack = state.backStacks[state.topLevelRoute]
        currentStack?.removeLastOrNull()
        navigate(route)
    }

    /**
     * Clear entire back stack and navigate to route.
     * Used for Intro → SavedTrips (user shouldn't go back to intro).
     */
    fun clearBackStackAndNavigate(route: NavKey) {
        val currentStack = state.backStacks[state.topLevelRoute]
        currentStack?.clear()
        currentStack?.add(route)
    }

    /**
     * Check if there's a previous entry in the back stack.
     * Used for analytics in TimeTableScreen.
     */
    fun hasPreviousEntry(): Boolean {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return false
        return currentStack.size > 1
    }
}

/**
 * Sealed interface for typed results between screens.
 * Must match xyz.ksharma.krail.trip.planner.ui.api.NavigationResult exactly.
 */
sealed interface NavigationResult {
    data class StopSelected(
        val fieldType: SearchStopFieldType,
        val stopId: String,
        val stopName: String
    ) : NavigationResult

    data class DateTimeSelected(
        val dateTimeJson: String
    ) : NavigationResult
}
