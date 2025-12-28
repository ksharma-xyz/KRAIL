package xyz.ksharma.krail.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.entries.ResultEventBus

/**
 * Remember Navigator with theme loaded from database.
 * This ensures theme persists across activity recreations (rotation, etc).
 */
@Composable
fun rememberNavigator(state: NavigationState): Navigator {
    val sandook: Sandook = koinInject()

    val navigator = remember(state) {
        Navigator(state)
    }

    // Load theme from database on initialization
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val themeId = sandook.getProductClass()?.toInt()
            val themeStyle = KrailThemeStyle.entries.find { it.id == themeId } ?: DEFAULT_THEME_STYLE
            log("Navigator - Loading theme from DB: themeId=$themeId, themeStyle=${themeStyle.name}, color=${themeStyle.hexColorCode}")
            navigator.updateTheme(themeStyle.hexColorCode)
        }
    }

    return navigator
}

/**
 * Handles navigation events and result passing between screens.
 */
class Navigator(val state: NavigationState) {

    // Event bus for passing results between screens
    val resultEventBus = ResultEventBus()

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

