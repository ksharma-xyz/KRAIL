# Navigation Architecture

> **LLM usage note** — This document is designed to be fed directly to an LLM so it can
> understand the current navigation setup and make changes to it. Every section that affects
> "what to change when doing X" is prefixed with a **Checklist** block. Read the full doc
> first, then use the checklists when editing or porting.

---

## Quick orientation

KRAIL uses **Jetpack Navigation 3** (`navigation3-ui`, alpha) with **Koin** DI and
**kotlinx.serialization** for Compose Multiplatform (Android + iOS).

The design goals are:
- No circular dependencies between app and feature modules.
- Back stack and route args survive configuration changes and process death.
- Adaptive (list-detail) layout for tablets/foldables with zero extra code in features.
- Feature modules contribute screens without the app module knowing about them directly.

---

## Library versions

```toml
# gradle/libs.versions.toml
multiplatform-nav3-ui          = "1.0.0-alpha06"
compose-multiplatform-adaptive = "1.3.0-alpha05"
koin                           = "4.1.1"
```

```toml
[libraries]
jetbrains-navigation3-ui            = { module = "org.jetbrains.androidx.navigation3:navigation3-ui",               version.ref = "multiplatform-nav3-ui" }
jetbrains-lifecycle-viewmodelNavigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "compose-multiplatform-lifecycle" }
jetbrains-material3-adaptiveNavigation3  = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation3",  version.ref = "compose-multiplatform-adaptive" }
di-koinAndroid                = { module = "io.insert-koin:koin-android",               version.ref = "koin" }
di-koinComposeViewmodel       = { module = "io.insert-koin:koin-compose-viewmodel",      version.ref = "koin" }
di-koinComposeViewmodelNav    = { module = "io.insert-koin:koin-compose-viewmodel-navigation", version.ref = "koin" }
```

### Module build.gradle.kts (core:navigation)

```kotlin
plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)   // ← required for @Serializable on routes
    alias(libs.plugins.ksp)
    alias(libs.plugins.krail.android.kmp.library)
}

commonMain.dependencies {
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
    implementation(libs.jetbrains.material3.adaptiveNavigation3)
    implementation(libs.kotlinx.serialization.json)
}
```

---

## File map

```
composeApp/
  src/commonMain/kotlin/xyz/ksharma/krail/
    KrailNavHost.kt                     ← root composable
    navigation/
      Navigator.kt                      ← Navigator class + rememberNavigator()
      SerializationConfig.kt            ← krailNavSerializationConfig
      di/
        AppNavigationModule.kt          ← Koin entries: Splash, AppUpgrade
        EntryProviderCollector.kt       ← collectEntryProviders()

core/navigation/
  src/commonMain/…/core/navigation/
    AppRoutes.kt                        ← SplashRoute, AppUpgradeRoute
    NavigationState.kt                  ← NavigationState + rememberNavigationState() + toEntries()
    NavigatorBase.kt                    ← interface used by feature modules
    EntryBuilderDescriptor.kt           ← FeatureEntryBuilder typealias + descriptor wrapper
    EntryBuilderQualifiers.kt           ← Koin named() qualifiers
    ResultEventBus.kt                   ← singleton channel-based event bus + LocalResultEventBusObj
    ResultEffect.kt                     ← composable consumer helper

feature/trip-planner/ui/
  src/commonMain/…/trip/planner/ui/
    navigation/
      TripPlannerRoutes.kt              ← @Serializable route classes
      TripPlannerNavigator.kt           ← feature navigator interface
      TripPlannerNavigatorImpl.kt       ← wraps NavigatorBase
      StopSelectedResult.kt             ← result data class passed via ResultEventBus
      entries/
        TripPlannerEntries.kt           ← aggregates all entry<> calls
        SavedTripsEntry.kt              ← list pane entry with ResultEffect
        SearchStopEntry.kt              ← detail pane entry that sends results
        TimeTableEntry.kt
        …
    di/
      TripPlannerNavigationModule.kt    ← provides EntryBuilderDescriptor to Koin
```

---

## Concept 1 — Routes

Every screen destination is a `NavKey`. The rules are:

| Rule | Reason |
|------|--------|
| Annotate with `@Serializable` | Required for back-stack persistence |
| Use `data object` for no-arg routes | Singleton, works as map key |
| Use `data class` for routes with args | Args are serialized into `SavedState` |
| All field types must also be `@Serializable` | kotlinx.serialization requirement |
| Implement (or seal under) `NavKey` | Navigation 3 requirement |
| Register in `krailNavSerializationConfig` | Required for iOS / Web / process-death restore |

```kotlin
// core/navigation — app-level routes
@Serializable data object SplashRoute    : NavKey
@Serializable data object AppUpgradeRoute : NavKey

// feature/trip-planner/ui — feature routes grouped under a sealed interface
sealed interface TripPlannerRoute : NavKey

@Serializable data object SavedTripsRoute : TripPlannerRoute

@Serializable data class SearchStopRoute(
    val fieldType: SearchStopFieldType,   // @Serializable enum
) : TripPlannerRoute

@Serializable data class TimeTableRoute(
    val fromStopId:   String,
    val fromStopName: String,
    val toStopId:     String,
    val toStopName:   String,
) : TripPlannerRoute

@Serializable data class JourneyMapRoute(val journeyId: String) : TripPlannerRoute

// Workaround for a complex non-serializable type: encode as JSON string
@Serializable data class DateTimeSelectorRoute(
    val dateTimeSelectionItemJson: String? = null,
) : TripPlannerRoute
```

**Checklist — adding a new route:**
- [ ] Create `@Serializable data object/class MyRoute : NavKey` in the feature's
      `navigation/` package.
- [ ] Add `subclass(MyRoute::class, MyRoute.serializer())` to `krailNavSerializationConfig`.
- [ ] Add a new `entry<MyRoute> { … }` in the feature's entries file.
- [ ] Add a navigation method to the feature navigator interface + impl.

---

## Concept 2 — Serialization config

`SavedStateConfiguration` tells Navigation 3 how to serialize `NavKey` instances.
On non-JVM platforms (iOS, Web) Kotlin reflection is unavailable, so every subclass
must be registered explicitly.

```kotlin
// composeApp/navigation/SerializationConfig.kt
val krailNavSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SplashRoute::class,           SplashRoute.serializer())
            subclass(AppUpgradeRoute::class,       AppUpgradeRoute.serializer())
            subclass(SavedTripsRoute::class,       SavedTripsRoute.serializer())
            subclass(SearchStopRoute::class,       SearchStopRoute.serializer())
            subclass(TimeTableRoute::class,        TimeTableRoute.serializer())
            subclass(JourneyMapRoute::class,       JourneyMapRoute.serializer())
            subclass(ThemeSelectionRoute::class,   ThemeSelectionRoute.serializer())
            subclass(ServiceAlertRoute::class,     ServiceAlertRoute.serializer())
            subclass(SettingsRoute::class,         SettingsRoute.serializer())
            subclass(DateTimeSelectorRoute::class, DateTimeSelectorRoute.serializer())
            subclass(OurStoryRoute::class,         OurStoryRoute.serializer())
            subclass(IntroRoute::class,            IntroRoute.serializer())
            subclass(DiscoverRoute::class,         DiscoverRoute.serializer())
        }
    }
}
```

> **LLM note**: When you add a new route anywhere in the project you MUST also add it here.
> Forgetting causes a `SerializationException` at runtime on iOS/Web and on Android after
> process death.

---

## Concept 3 — NavigationState and the back stack

### `rememberNavigationState`

```kotlin
// core/navigation/NavigationState.kt
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    serializationConfig: SavedStateConfiguration,
): NavigationState
```

Internally calls `rememberNavBackStack(serializationConfig, key)` for **each** top-level
route. `rememberNavBackStack` is a Navigation 3 API backed by `rememberSaveable`, so the
back stack survives:
- Configuration changes (screen rotation, font-scale change)
- Process death → recreation (Android)

### `NavigationState` class

```kotlin
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute:  MutableState<NavKey>,           // which tab is active
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey                        // read/write

    // Which stacks to render: [startRoute] or [startRoute, activeTab]
    val stacksInUse: List<NavKey>

    fun goTo(route: NavKey)            // push to current stack, or switch tab
    fun pop()                          // remove last entry; go to startRoute if at root
    fun resetRoot(route: NavKey)       // clear stack, push single route (no back possible)
    fun replaceCurrent(route: NavKey)  // pop top + goTo (equivalent to popUpTo inclusive)
}
```

`stacksInUse` is what drives the list-detail layout: when only `startRoute` is in scope
the app shows a single pane; when two stacks are active it shows list + detail side by side
on wide screens.

### `toEntries` extension

```kotlin
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>>
```

- Applies `rememberSaveableStateHolderNavEntryDecorator` to each back stack — this is what
  preserves `rememberSaveable` state (scroll position, text input, etc.) when navigating back.
- Flattens `stacksInUse` into a single `SnapshotStateList` fed to `NavDisplay`.

---

## Concept 4 — Navigator

### `Navigator` class (composeApp)

```kotlin
class Navigator(val state: NavigationState) : NavigatorBase {

    // Theme color (Compose state — triggers recomposition when changed)
    var themeColor: String by mutableStateOf(DEFAULT_THEME_STYLE.hexColorCode)
        private set

    override fun updateTheme(hexColorCode: String) { themeColor = hexColorCode }

    override fun goTo(route: NavKey)           { state.goTo(route) }
    override fun pop()                         { state.pop() }
    override fun resetRoot(route: NavKey)      { state.resetRoot(route) }
    override fun replaceCurrent(route: NavKey) { state.replaceCurrent(route) }

    // No-op if same route type already on top; replaces if same type with different args
    override fun pushSingleInstance(route: NavKey) { … }
}
```

> **LLM note**: `themeColor` is temporary coupling. There is a migration plan inside
> `Navigator.kt` to extract it to a `ThemeManager`. Don't add more non-navigation state here.

### `rememberNavigator`

```kotlin
@Composable
fun rememberNavigator(state: NavigationState): Navigator {
    val sandook: Sandook = koinInject()                  // Koin-injected DB
    val navigator = remember(state) { Navigator(state) }

    LaunchedEffect(Unit) {                               // runs once per composition
        withContext(Dispatchers.Default) {
            val themeId = sandook.getProductClass()?.toInt()
            val themeStyle = KrailThemeStyle.entries.find { it.id == themeId } ?: DEFAULT_THEME_STYLE
            navigator.updateTheme(themeStyle.hexColorCode)
        }
    }

    return navigator
}
```

- `remember(state)` — stable object identity across recompositions.
- `LaunchedEffect(Unit)` — reloads theme from DB on every (re)composition entry, which
  includes post-rotation, so the theme is always consistent with the database.

### `NavigatorBase` interface (core:navigation)

Lives in `core:navigation` — the only module both app and feature modules can depend on.
Feature modules depend on this interface, **not** on `Navigator` from `composeApp`.

```kotlin
interface NavigatorBase {
    fun goTo(route: NavKey)
    fun pop()
    fun resetRoot(route: NavKey)
    fun replaceCurrent(route: NavKey)
    fun updateTheme(hexColorCode: String)
    fun pushSingleInstance(route: NavKey)
}
```

### Feature-specific navigator (pattern)

```kotlin
// feature/trip-planner/ui
interface TripPlannerNavigator {
    fun navigateToSearchStop(fieldType: SearchStopFieldType)
    fun navigateToTimeTable(fromStopId: String, fromStopName: String,
                            toStopId: String, toStopName: String)
    fun navigateToSettings()
    fun goBack()
    fun updateTheme(hexColorCode: String)
    // … one method per navigation action the feature needs
}

internal class TripPlannerNavigatorImpl(
    private val baseNavigator: NavigatorBase,
) : TripPlannerNavigator {
    override fun navigateToSearchStop(fieldType: SearchStopFieldType) =
        baseNavigator.goTo(SearchStopRoute(fieldType))

    override fun navigateToTimeTable(
        fromStopId: String, fromStopName: String,
        toStopId: String,   toStopName: String,
    ) = baseNavigator.pushSingleInstance(
        TimeTableRoute(fromStopId, fromStopName, toStopId, toStopName)
    )

    override fun navigateToSettings() = baseNavigator.goTo(SettingsRoute)
    override fun goBack()             = baseNavigator.pop()
    override fun updateTheme(hex: String) = baseNavigator.updateTheme(hex)
    // …
}
```

**Checklist — adding a new feature navigator:**
- [ ] Create `MyFeatureNavigator` interface in `feature/my-feature/ui/navigation/`.
- [ ] Create `MyFeatureNavigatorImpl(baseNavigator: NavigatorBase)` in the same package.
- [ ] Add one method per navigation action; delegate to `baseNavigator.goTo/pop/etc.`.
- [ ] Do NOT depend on `Navigator` (composeApp) — only depend on `NavigatorBase` (core).

---

## Concept 5 — KrailNavHost

The root composable. Placed once in the app's main content composable.

```kotlin
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun KrailNavHost(modifier: Modifier = Modifier) {

    // 1. Build navigation state (persists through config changes)
    val navigationState = rememberNavigationState(
        startRoute          = SplashRoute,
        topLevelRoutes      = setOf(SplashRoute),   // ← add more for bottom-nav tabs
        serializationConfig = krailNavSerializationConfig,
    )

    // 2. Build navigator (wraps state, loads theme from DB)
    val navigator = rememberNavigator(navigationState)

    // 3. Singleton event bus for screen-to-screen result passing
    val resultEventBus = remember { ResultEventBus.getInstance() }

    // 4. Collect all feature entry providers from Koin
    val entryProvider = collectEntryProviders(navigator = navigator)

    // 5. Adaptive layout directive (zero gutter between panes — b/418201867)
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    // 6. Provide globals down the tree
    CompositionLocalProvider(
        LocalThemeColor        provides mutableStateOf(navigator.themeColor),
        LocalThemeContentColor provides mutableStateOf(themeContentColor),
        LocalTextColor         provides KrailTheme.colors.onSurface,
        LocalResultEventBusObj provides resultEventBus,
    ) {
        val entries = navigationState.toEntries(entryProvider)

        if (entries.isNotEmpty()) {
            NavDisplay(
                entries       = entries,
                onBack        = { navigator.pop() },
                sceneStrategy = listDetailStrategy,
                modifier      = modifier.fillMaxSize(),
            )
        } else {
            NoEntriesUI(navigator)   // graceful fallback; clicking resets to SplashRoute
        }
    }
}
```

Key design decisions:
- `topLevelRoutes = setOf(SplashRoute)` — single back stack. To add a bottom-nav tab add
  its route here and pass it to `navigator.goTo()` from the tab bar.
- `horizontalPartitionSpacerSize = 0.dp` — removes the default gutter between panes
  (workaround for `b/418201867`).
- `NoEntriesUI` guards against a momentary empty entry list on first composition before
  Koin injection completes.
- `@Suppress("RememberMissing")` is on the function because `navigationState.toEntries()`
  is called twice (once to check emptiness, once inside `NavDisplay`). This is intentional.

---

## Concept 6 — Koin multibinding (entry providers)

Each feature contributes an `EntryBuilderDescriptor` to Koin. The app collects them all.

### Core types

```kotlin
// core/navigation/EntryBuilderDescriptor.kt

// The actual builder function — receives the navigator as Any (cast inside the feature)
typealias FeatureEntryBuilder = @Composable EntryProviderScope<NavKey>.(navigator: Any) -> Unit

data class EntryBuilderDescriptor(
    val name: String,
    val builder: FeatureEntryBuilder,
)
```

```kotlin
// core/navigation/EntryBuilderQualifiers.kt
object EntryBuilderQualifiers {
    object Names {
        const val SPLASH       = "splash"
        const val APP_UPGRADE  = "appUpgrade"
        const val TRIP_PLANNER = "tripPlanner"
    }
    val SPLASH       = named(Names.SPLASH)
    val APP_UPGRADE  = named(Names.APP_UPGRADE)
    val TRIP_PLANNER = named(Names.TRIP_PLANNER)
}
```

### Feature module provides (pattern)

```kotlin
// feature/trip-planner/ui/di/TripPlannerNavigationModule.kt
val tripPlannerNavigationModule = module {
    factory<EntryBuilderDescriptor>(qualifier = EntryBuilderQualifiers.TRIP_PLANNER) {
        EntryBuilderDescriptor(
            name = EntryBuilderQualifiers.Names.TRIP_PLANNER,
            builder = { navigator ->
                val baseNavigator      = navigator as NavigatorBase          // cast from Any
                val tripPlannerNav     = TripPlannerNavigatorImpl(baseNavigator)
                TripPlannerEntries(tripPlannerNav)                           // aggregator fn
            },
        )
    }
}
```

### App module collects

```kotlin
// composeApp/navigation/di/EntryProviderCollector.kt
@Composable
fun collectEntryProviders(navigator: Navigator): (NavKey) -> NavEntry<NavKey> {
    val splashDescriptor:      EntryBuilderDescriptor = koinInject(EntryBuilderQualifiers.SPLASH)
    val appUpgradeDescriptor:  EntryBuilderDescriptor = koinInject(EntryBuilderQualifiers.APP_UPGRADE)
    val tripPlannerDescriptor: EntryBuilderDescriptor = koinInject(EntryBuilderQualifiers.TRIP_PLANNER)

    return entryProvider {
        splashDescriptor.builder(this, navigator)
        appUpgradeDescriptor.builder(this, navigator)
        tripPlannerDescriptor.builder(this, navigator)
    }
}
```

**Checklist — adding a new feature module:**
- [ ] Add a constant to `EntryBuilderQualifiers.Names` and a `named()` qualifier.
- [ ] Create `MyFeatureNavigationModule` in the feature, providing `EntryBuilderDescriptor`.
- [ ] Add the new `koinInject(EntryBuilderQualifiers.MY_FEATURE)` line in
      `collectEntryProviders`.
- [ ] Call `myFeatureDescriptor.builder(this, navigator)` inside `entryProvider { … }`.
- [ ] Include `myFeatureNavigationModule` in the Koin module list at app start.

---

## Concept 7 — Screen entries and ViewModels

A screen entry is a `@Composable` extension on `EntryProviderScope<NavKey>`. It calls
`entry<MyRoute> { key -> … }` to register the composable for a specific route.

### Full example — list pane with ViewModel and result listener

```kotlin
// feature/trip-planner/ui/navigation/entries/SavedTripsEntry.kt
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.SavedTripsEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<SavedTripsRoute>(
        metadata = ListDetailSceneStrategy.listPane(),  // marks this as the list pane
    ) {
        // ViewModel scoped to this navigation entry.
        // key = "SavedTripsNav" keeps the same ViewModel instance
        // even if SavedTripsRoute appears in multiple back stacks.
        val viewModel: SavedTripsViewModel = koinViewModel(key = "SavedTripsNav")
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        // Receive results from SearchStop screen via the singleton event bus.
        // This works even in two-pane layouts where the screens are in separate
        // composition scopes.
        ResultEffect<StopSelectedResult> { result ->
            when (result.fieldType) {
                FROM -> viewModel.onEvent(SavedTripUiEvent.FromStopChanged(result.stopId))
                TO   -> viewModel.onEvent(SavedTripUiEvent.ToStopChanged(result.stopId))
            }
        }

        SavedTripsScreen(
            savedTripsState     = state,
            fromButtonClick     = { tripPlannerNavigator.navigateToSearchStop(FROM) },
            toButtonClick       = { tripPlannerNavigator.navigateToSearchStop(TO) },
            onSavedTripCardClick = { from, to ->
                tripPlannerNavigator.navigateToTimeTable(
                    fromStopId   = from.stopId,
                    fromStopName = from.stopName,
                    toStopId     = to.stopId,
                    toStopName   = to.stopName,
                )
            },
            onEvent = viewModel::onEvent,
        )
    }
}
```

### ViewModel creation rules

| Where | How | Why |
|-------|-----|-----|
| Inside `entry<Route> { … }` | `koinViewModel()` | ViewModel is scoped to the nav entry lifecycle |
| Custom scope key | `koinViewModel(key = "…")` | Share a ViewModel across multiple entries or prevent duplicate instances |
| Preview | Not used (pass fake state) | Koin is unavailable in `@Preview` |

ViewModel lifecycle in Navigation 3:
1. `entry<MyRoute>` is activated → ViewModel created.
2. User navigates back → route removed from backstack.
3. **Screen stays composed** for the duration of the exit animation.
4. After animation → entry disposed → ViewModel cleared.

This differs from Navigation 2 where `popBackStack()` caused immediate disposal.

### App-level entry example (Splash)

```kotlin
// composeApp/navigation/di/AppNavigationModule.kt
@Composable
private fun EntryProviderScope<NavKey>.SplashEntry(navigator: Navigator) {
    entry<SplashRoute> { _ ->
        val viewModel: SplashViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        // Apply theme from splash state before navigating away
        LaunchedEffect(state.themeStyle) {
            navigator.updateTheme(state.themeStyle.hexColorCode)
        }

        // Navigate once destination is determined
        LaunchedEffect(state.navigationDestination) {
            state.navigationDestination?.let { dest ->
                navigator.replaceCurrent(dest)   // Splash disappears, no back possible
            }
        }

        SplashScreen(
            splashState               = state,
            logoColor                 = navigator.themeColor.hexToComposeColor(),
            onSplashAnimationComplete = { viewModel.onUiEvent(SplashAnimationComplete) },
        )
    }
}
```

---

## Concept 8 — Result passing between screens

### Why `ResultEventBus` instead of `SavedStateHandle`

In a two-pane layout the list and detail screens exist in **separate** `NavDisplay` panes
(separate composition scopes). `SavedStateHandle` results are scoped to a single entry and
cannot cross pane boundaries. A singleton channel-based bus solves this.

### Send a result (detail pane → list pane)

```kotlin
// SearchStopScreen
val bus = LocalResultEventBusObj.current
bus.sendResult<StopSelectedResult>(result = StopSelectedResult(
    stopId    = stop.id,
    stopName  = stop.name,
    fieldType = fieldType,
))
navigator.goBack()
```

### Receive a result

```kotlin
// SavedTripsScreen entry (see Concept 7 example)
ResultEffect<StopSelectedResult> { result ->
    viewModel.onEvent(FromStopChanged(result.stopId))
}
```

`ResultEffect` wraps a `LaunchedEffect` that collects from a `Channel`-backed flow. It is
automatically cancelled when the composable leaves composition.

### Result data class

```kotlin
data class StopSelectedResult(
    val stopId:    String,
    val stopName:  String,
    val fieldType: SearchStopFieldType,
)
```

No `@Serializable` needed — results are in-memory only, not persisted.

**Checklist — adding a new result type:**
- [ ] Create a data class for the result in the feature's `navigation/` package.
- [ ] Call `bus.sendResult<MyResult>(result = MyResult(…))` in the sender screen.
- [ ] Call `ResultEffect<MyResult> { … }` in the receiver entry.
- [ ] No Koin registration needed.

---

## Configuration-change survival summary

| What is preserved | Mechanism |
|-------------------|-----------|
| Back stack route list | `rememberNavBackStack` → `rememberSaveable` |
| Route argument values | kotlinx.serialization via `SavedStateConfiguration` |
| `rememberSaveable` state inside screens | `rememberSaveableStateHolderNavEntryDecorator` in `toEntries()` |
| ViewModel instance | Standard Jetpack ViewModel scoped to nav entry |
| App theme | Reloaded from DB in `LaunchedEffect(Unit)` inside `rememberNavigator` |

---

## Porting this pattern to another CMP project

### Step 1 — Gradle

```toml
# libs.versions.toml additions
multiplatform-nav3-ui          = "1.0.0-alpha06"
compose-multiplatform-adaptive = "1.3.0-alpha05"
koin                           = "4.1.1"
```

Apply in your core-navigation module:

```kotlin
plugins { alias(libs.plugins.kotlin.serialization) }
commonMain.dependencies {
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.nav3)
    implementation(libs.adaptive.navigation3)
    implementation(libs.kotlinx.serialization.json)
}
```

### Step 2 — Routes

```kotlin
@Serializable data object HomeRoute     : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data class  DetailRoute(val id: String) : NavKey
```

### Step 3 — Serialization config

```kotlin
val myAppSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeRoute::class,     HomeRoute.serializer())
            subclass(SettingsRoute::class, SettingsRoute.serializer())
            subclass(DetailRoute::class,   DetailRoute.serializer())
        }
    }
}
```

### Step 4 — NavigationState

```kotlin
val navState = rememberNavigationState(
    startRoute          = HomeRoute,
    topLevelRoutes      = setOf(HomeRoute),   // add more for bottom-nav tabs
    serializationConfig = myAppSerializationConfig,
)
```

### Step 5 — Navigator interface and impl

```kotlin
// In core module
interface AppNavigatorBase {
    fun goTo(route: NavKey)
    fun pop()
    fun resetRoot(route: NavKey)
    fun replaceCurrent(route: NavKey)
}

// In app module
class AppNavigator(val state: NavigationState) : AppNavigatorBase {
    override fun goTo(route: NavKey)           = state.goTo(route)
    override fun pop()                         = state.pop()
    override fun resetRoot(route: NavKey)      = state.resetRoot(route)
    override fun replaceCurrent(route: NavKey) = state.replaceCurrent(route)
}

@Composable
fun rememberAppNavigator(state: NavigationState): AppNavigator =
    remember(state) { AppNavigator(state) }
```

### Step 6 — NavHost

```kotlin
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavHost() {
    val navState  = rememberNavigationState(HomeRoute, setOf(HomeRoute), myAppSerializationConfig)
    val navigator = rememberAppNavigator(navState)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        entries       = navState.toEntries(myEntryProvider(navigator)),
        onBack        = { navigator.pop() },
        sceneStrategy = listDetailStrategy,
        modifier      = Modifier.fillMaxSize(),
    )
}

@Composable
fun myEntryProvider(navigator: AppNavigator): (NavKey) -> NavEntry<NavKey> = entryProvider {
    entry<HomeRoute>     { HomeScreen(onGoToDetail = { id -> navigator.goTo(DetailRoute(id)) }) }
    entry<SettingsRoute> { SettingsScreen(onBack = { navigator.pop() }) }
    entry<DetailRoute>   { key -> DetailScreen(id = key.id, onBack = { navigator.pop() }) }
}
```

> **Important**: `toEntries()` (the KRAIL extension) applies
> `rememberSaveableStateHolderNavEntryDecorator` automatically. If you write raw
> `entryProvider` without it, `rememberSaveable` state will be lost on back navigation.
> Either copy `toEntries()` or apply the decorator manually:
>
> ```kotlin
> rememberDecoratedNavEntries(
>     backStack        = navState.backStacks[navState.topLevelRoute]!!,
>     entryDecorators  = listOf(rememberSaveableStateHolderNavEntryDecorator()),
>     entryProvider    = rawEntryProvider,
> )
> ```

### Step 7 — ViewModels in entries

```kotlin
entry<HomeRoute> {
    val viewModel: HomeViewModel = koinViewModel()   // or hiltViewModel() for Hilt
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onEvent = viewModel::onEvent)
}
```

### Step 8 — Result passing (optional, needed for two-pane)

Copy `ResultEventBus`, `LocalResultEventBusObj`, and `ResultEffect` from
`core/navigation` into your project. The singleton pattern is required for two-pane
layouts; for single-pane apps you can use a simpler `SavedStateHandle` approach.

---

## Common mistakes and how to avoid them

| Mistake | Symptom | Fix |
|---------|---------|-----|
| New route not in `krailNavSerializationConfig` | `SerializationException` on iOS/Web; crash after process death on Android | Always add both: route class **and** `subclass(…)` entry |
| Feature module depends on `Navigator` (composeApp) | Circular dependency build error | Depend on `NavigatorBase` (core:navigation) only |
| Using `remember` instead of `rememberSaveable` in a screen while relying on the decorator | State lost on back navigation | Trust the decorator; use `rememberSaveable` normally inside screen composables |
| Calling `Navigator.pop()` from inside an `entry<>` lambda directly | Works but bypasses the `onBack` handler of `NavDisplay` | Always pass a navigator to the screen and call its `goBack()` method |
| Two result listeners for the same type in one composition | Second listener never fires | Use distinct `resultKey` params in `ResultEffect` |
| Large non-serializable object as a route arg | Compile error or runtime crash | Serialize to JSON string and pass the string (see `DateTimeSelectorRoute`) |
| Forgetting `koinViewModel(key = …)` for a shared ViewModel | Two separate ViewModel instances for the same logical screen | Pass an explicit stable string key |

---

## FAQ

**Q: Why is `SavedStateConfiguration` needed on Android if reflection works there?**
For consistency with iOS/Web. A single config runs on all targets and avoids maintaining
separate code paths.

**Q: Why use `factory` not `single` for `EntryBuilderDescriptor` in Koin?**
`EntryBuilderDescriptor` is a data class with no mutable state. A new instance per
injection is fine and avoids accidental state leakage between feature modules.

**Q: Can I add args to a `data object` route later?**
No. Change it to a `data class` and update `krailNavSerializationConfig`. Any existing
saved state containing the old serialized form will be incompatible — add a migration or
reset the back stack.

**Q: How do I add a second bottom-nav tab?**
1. Create `@Serializable data object MyTabRoute : NavKey`.
2. Add it to `topLevelRoutes` in `rememberNavigationState`.
3. Register in `krailNavSerializationConfig`.
4. Call `navigator.goTo(MyTabRoute)` from the tab bar item.
   `NavigationState.goTo` detects it is a top-level key and switches stacks.
5. Add its feature entries to `collectEntryProviders`.

**Q: Why is `themeColor` on `Navigator` instead of a ViewModel?**
Historical coupling from Nav 2 migration. See the full migration plan documented in
`Navigator.kt`. Don't add more non-navigation state here.

**Q: What is `pushSingleInstance`?**
A navigation action that is a no-op if the exact same route object is already on top, and
replaces the top entry if the same route *type* (different args) is on top. Used for
`TimeTableRoute` to avoid duplicate time-table screens when the user taps the same trip
repeatedly.
