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
import xyz.ksharma.krail.core.navigation.NavigationState
import xyz.ksharma.krail.core.navigation.NavigatorBase
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle

/**
 * Remember [Navigator] with theme loaded from database.
 *
 * ## Purpose
 *
 * Creates and remembers a [Navigator] instance that:
 * 1. Handles all navigation events
 * 2. Loads user's saved theme preference from database
 * 3. Ensures theme persists across activity recreations (rotation, etc)
 *
 * ## App Launch Flow
 *
 * ```
 * App Start → MainActivity.onCreate()
 *           → KrailApp composable
 *           → rememberNavigator(state)
 *           → LaunchedEffect loads theme from Sandook DB
 *           → navigator.updateTheme(savedColor)
 *           → LocalThemeColor provides theme to all screens
 *           → KrailTheme applies Material 3 color scheme
 * ```
 *
 * ## Theme Loading Details
 *
 * **On First App Launch:**
 * - No saved theme in DB → Uses DEFAULT_THEME_STYLE (Sydney Trains yellow)
 * - User can change theme in ThemeSelectionScreen
 * - New theme saved to DB via `productClass` field
 *
 * **On Subsequent Launches:**
 * - Loads saved `productClass` (theme ID) from Sandook DB
 * - Converts to KrailThemeStyle enum
 * - Updates Navigator.themeColor
 * - App opens with user's preferred theme
 *
 * **On Screen Rotation:**
 * - Activity recreated → rememberNavigator() called again
 * - LaunchedEffect reloads theme from DB
 * - Theme restored instantly (no flash to default)
 * - Navigation state preserved via NavigationState
 *
 * ## Why LaunchedEffect for Theme Loading?
 *
 * - **Asynchronous** - DB query runs on Dispatchers.Default (background thread)
 * - **One-time** - LaunchedEffect(Unit) only runs on first composition
 * - **Safe** - Avoids blocking main thread during app initialization
 *
 * ## Related Components
 *
 * - **Sandook.getProductClass()** - Retrieves saved theme ID from DB
 * - **KrailThemeStyle** - Enum of available themes (Sydney, Blue, Green, etc.)
 * - **LocalThemeColor** - CompositionLocal that provides theme to all screens
 * - **KrailTheme** - Root theme composable that applies Material 3 colors
 *
 * @param state NavigationState containing backstack and route management
 * @return Navigator instance with theme loaded from database
 */
@Composable
fun rememberNavigator(state: NavigationState): Navigator {
    val sandook: Sandook = koinInject()
    val navigator = remember(state) { Navigator(state) }

    // Load user's saved theme from database on app initialization
    // This ensures the theme persists across app restarts and rotations (configuration changes)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val themeId = sandook.getProductClass()?.toInt()
            val themeStyle =
                KrailThemeStyle.entries.find { it.id == themeId } ?: DEFAULT_THEME_STYLE
            log("Navigator - Loading theme from DB:")
            log("\tthemeId=$themeId, themeStyle=${themeStyle.name}, color=${themeStyle.hexColorCode}")
            navigator.updateTheme(themeStyle.hexColorCode)
        }
    }

    return navigator
}

/**
 * Handles navigation events and result passing between screens.
 * Implements NavigatorBase so feature modules can depend on the interface
 * without circular dependencies.
 */
class Navigator(val state: NavigationState) : NavigatorBase {

    // ═══════════════════════════════════════════════════════════════════════════════
    // THEME MANAGEMENT (Temporary Coupling - See Migration Plan Below)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * App theme color state stored in Navigator.
     *
     * ⚠️ IMPORTANT: This is NOT navigation logic! ⚠️
     *
     * ## Why Theme Lives in Navigator (Current Design)
     *
     * This is a **temporary coupling** for convenience during Nav 2 → Nav 3 migration.
     * The Navigator holds theme state to enable:
     *
     * 1. **Global Theme Access** - All screens access theme via `LocalThemeColor`
     * 2. **Persistence** - Theme loads from DB on app start and survives navigation
     * 3. **Dynamic Updates** - Theme selection screen can update theme globally
     *
     * ## Usage Scenarios
     *
     * ### Scenario 1: App Launch (Cold Start)
     * ```
     * 1. User opens app
     * 2. rememberNavigator() creates Navigator
     * 3. LaunchedEffect loads theme from Sandook DB
     * 4. navigator.updateTheme(savedColor) sets themeColor
     * 5. KrailTheme observes LocalThemeColor and applies theme
     * ```
     *
     * ### Scenario 2: Theme Selection
     * ```
     * 1. User navigates to ThemeSelectionScreen
     * 2. User picks a new theme (e.g., "Blue" #1976D2)
     * 3. ThemeSelectionScreen calls tripPlannerNavigator.updateTheme("#1976D2")
     * 4. Navigator.themeColor updates (triggers recomposition)
     * 5. All screens instantly reflect new theme via LocalThemeColor
     * 6. Theme saved to DB for next app launch
     * ```
     *
     * ### Scenario 3: Screen Rotation
     * ```
     * 1. Activity destroyed/recreated
     * 2. rememberNavigator() re-creates Navigator
     * 3. LaunchedEffect reloads theme from DB
     * 4. Theme state restored (no flash to default theme)
     * ```
     *
     * ## Who Uses This?
     *
     * - **rememberNavigator()** - Loads theme from DB on initialization
     * - **KrailTheme** - Observes `LocalThemeColor.current` (which reads navigator.themeColor)
     * - **ThemeSelectionScreen** - Calls `updateTheme()` when user changes theme
     * - **TripPlannerNavigator** - Exposes `updateTheme()` to feature module
     *
     * ## Why Not in ViewModel or Separate ThemeManager?
     *
     * **Historical Reason:** In Nav 2.x, Navigator was a singleton-like object that
     * survived across the entire app lifecycle, making it a convenient place to store
     * global state like theme.
     *
     * **Current Limitation:** During Nav 3 migration, we kept this pattern to minimize
     * changes. Extracting theme management would require:
     * - New ThemeManager class/module
     * - Update all feature navigator interfaces
     * - Migrate theme persistence logic
     * - Test across all screens
     *
     * For a small project mid-migration, keeping it here is acceptable.
     *
     * ## Migration Plan (Future Work)
     *
     * When Navigation 3 stabilizes and the project grows, extract theme management:
     *
     * **Step 1: Create ThemeManager**
     * ```kotlin
     * interface ThemeManager {
     *     val themeColor: StateFlow<String>
     *     fun updateTheme(hexColorCode: String)
     * }
     *
     * class ThemeManagerImpl(sandook: Sandook) : ThemeManager {
     *     private val _themeColor = MutableStateFlow(DEFAULT_THEME_STYLE.hexColorCode)
     *     override val themeColor = _themeColor.asStateFlow()
     *
     *     init {
     *         // Load from DB
     *     }
     *
     *     override fun updateTheme(hexColorCode: String) {
     *         _themeColor.value = hexColorCode
     *         // Save to DB
     *     }
     * }
     * ```
     *
     * **Step 2: Remove from Navigator**
     * ```kotlin
     * // Delete themeColor and updateTheme() from Navigator
     * class Navigator(val state: NavigationState) : NavigatorBase {
     *     // Only navigation logic
     * }
     * ```
     *
     * **Step 3: Update Feature Interfaces**
     * ```kotlin
     * interface TripPlannerNavigator {
     *     fun navigate(route: NavKey)
     *     fun goBack()
     *     // Remove: fun updateTheme(hexColorCode: String)
     * }
     * ```
     *
     * **Step 4: Inject ThemeManager in ThemeSelectionScreen**
     * ```kotlin
     * @Composable
     * fun ThemeSelectionScreen(
     *     themeManager: ThemeManager = koinInject()
     * ) {
     *     themeManager.updateTheme(selectedColor)
     * }
     * ```
     *
     * **Benefits After Migration:**
     * - ✅ Single Responsibility - Navigator only handles navigation
     * - ✅ Testability - Can mock ThemeManager independently
     * - ✅ Reusability - ThemeManager can be used outside navigation
     * - ✅ Cleaner Architecture - No coupling between navigation and theming
     *
     * ## References
     *
     * See: `docs/navigation-architecture.md` - FAQ: "Why does Navigator have updateTheme()?"
     */
    var themeColor: String by mutableStateOf(DEFAULT_THEME_STYLE.hexColorCode)
        private set

    /**
     * Updates the app's global theme color.
     *
     * Called by:
     * - rememberNavigator() on app launch (loads from DB)
     * - ThemeSelectionScreen when user picks a new theme
     *
     * This triggers recomposition of all screens observing LocalThemeColor.
     *
     * @param hexColorCode Hex color string (e.g., "#1976D2" for blue)
     */
    override fun updateTheme(hexColorCode: String) {
        themeColor = hexColorCode
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // NAVIGATION LOGIC (Core Responsibility)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Navigate to a route.
     *
     * Handles both top-level and nested navigation:
     * - If route is a top-level route → Switch to that stack
     * - If route is nested → Add to current stack
     *
     * @param route The destination route
     */
    override fun goTo(route: NavKey) {
        state.goTo(route)
    }

    /**
     * Navigate back in the current stack.
     *
     * ## Navigation 3 Behavior (Important!)
     *
     * Unlike traditional navigation where "pop" destroys the screen immediately,
     * Navigation 3 keeps the **NavEntry alive** even after removing from backstack:
     *
     * 1. **Backstack Update** - Route removed from `NavBackStack` immediately
     * 2. **Composable Lifecycle** - Screen remains composed during exit animation
     * 3. **State Preservation** - SavedStateHandle preserves state until fully destroyed
     * 4. **Disposal** - Entry disposed only after exit animation completes
     *
     * **Why This Matters:**
     * - ViewModels stay alive during back navigation animation
     * - Ongoing coroutines continue until animation finishes
     * - State is accessible for shared element transitions
     * - `rememberSaveable` state survives the animation period
     *
     * **Example Flow:**
     * ```
     * TimeTableScreen visible
     *   ↓
     * User presses back
     *   ↓
     * pop() removes route from backstack
     *   ↓
     * TimeTableScreen still composed (exit animation playing)
     *   ↓
     * ViewModel still collecting flows
     *   ↓
     * Animation completes (~300ms)
     *   ↓
     * TimeTableScreen disposed
     *   ↓
     * ViewModel cleared
     * ```
     *
     * This is different from Nav 2.x where `popBackStack()` immediately
     * destroyed the destination. Nav 3's approach enables smoother animations
     * and better state preservation.
     *
     * ## Behavior
     * - If at top-level route → Switch to start route
     * - If in nested navigation → Remove current route from stack
     */
    override fun pop() {
        state.pop()
    }

    /**
     * Replace current screen with new route.
     * Equivalent to navigate with popUpTo inclusive.
     *
     * Used for transitions like: Splash → SavedTrips/Intro
     *
     * @param route The replacement route
     */
    override fun replaceCurrent(route: NavKey) {
        state.replaceCurrent(route)
    }

    /**
     * Clear entire back stack and navigate to route.
     *
     * Used when user shouldn't be able to navigate back, such as:
     * - Intro → SavedTrips (user completes onboarding)
     * - Logout → Login (clear authenticated state)
     *
     * @param route The new root route
     */
    override fun resetRoot(route: NavKey) {
        state.resetRoot(route)
    }

    override fun goToSingleTopOrReplace(route: NavKey) {
        val currentTopRoute = try {
            state.backStacks[state.topLevelRoute]?.lastOrNull()
        } catch (t: Throwable) {
            log("Navigator - unable to read current top route: $t")
            null
        }

        // If identical object is already on top -> no-op
        if (currentTopRoute == route) {
            log("Navigator - goToSingleTopOrReplace ignored (same instance on top): $route")
            return
        }

        // If same route *type* (class) is on top -> replace it with new params
        if (currentTopRoute != null && currentTopRoute::class == route::class) {
            log("Navigator - goToSingleTopOrReplace replacing top route of same type: ${route::class.simpleName}")
            state.replaceCurrent(route)
            return
        }

        // Default: push new route
        log("Navigator - goToSingleTopOrReplace pushing new route: $route")
        state.goTo(route)
    }
}
