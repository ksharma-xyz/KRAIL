# Navigation Architecture

This document describes how KRAIL implements navigation using Jetpack Navigation 3 for
Compose Multiplatform (Android + iOS). It covers the back stack, configuration-change
survival, serialization, the `KrailNavHost`, the Koin multibinding wiring, and how to
port this pattern to another CMP project.

---

## Library versions (as of March 2026)

```toml
# gradle/libs.versions.toml
multiplatform-nav3-ui        = "1.0.0-alpha06"
compose-multiplatform-adaptive = "1.3.0-alpha05"
koin                         = "4.1.1"
```

```toml
# artifact coordinates
jetbrains-navigation3-ui            = "org.jetbrains.androidx.navigation3:navigation3-ui"
jetbrains-lifecycle-viewmodelNavigation3 = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3"
jetbrains-material3-adaptiveNavigation3  = "org.jetbrains.compose.material3.adaptive:adaptive-navigation3"
di-koinAndroid                      = "io.insert-koin:koin-android"
di-koinComposeViewmodel             = "io.insert-koin:koin-compose-viewmodel"
di-koinComposeViewmodelNav          = "io.insert-koin:koin-compose-viewmodel-navigation"
```

### Gradle module dependencies (core:navigation)

```kotlin
// core/navigation/build.gradle.kts
plugins {
    alias(libs.plugins.krail.kotlin.multiplatform)
    alias(libs.plugins.krail.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)   // required for @Serializable on routes
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

## Module layout

```
composeApp/
  src/commonMain/kotlin/xyz/ksharma/krail/
    KrailNavHost.kt               ← root composable
    navigation/
      Navigator.kt                ← app Navigator + rememberNavigator()
      SerializationConfig.kt      ← krailNavSerializationConfig
      di/
        AppNavigationModule.kt    ← Koin entries: Splash, AppUpgrade
        EntryProviderCollector.kt ← collectEntryProviders()

core/navigation/
  src/commonMain/kotlin/xyz/ksharma/krail/core/navigation/
    AppRoutes.kt                  ← SplashRoute, AppUpgradeRoute
    NavigationState.kt            ← NavigationState + rememberNavigationState()
    NavigatorBase.kt              ← interface consumed by feature modules
    EntryBuilderDescriptor.kt     ← FeatureEntryBuilder typealias + descriptor
    EntryBuilderQualifiers.kt     ← Koin named() qualifiers
    ResultEventBus.kt             ← singleton channel-based event bus
    ResultEffect.kt               ← composable consumer helper

feature/trip-planner/ui/
  src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/
    navigation/
      TripPlannerRoutes.kt        ← @Serializable route data classes
      TripPlannerNavigator.kt     ← feature navigator interface
      TripPlannerNavigatorImpl.kt ← wraps NavigatorBase
      entries/
        TripPlannerEntries.kt     ← aggregates all screen entries
        SavedTripsEntry.kt
        SearchStopEntry.kt
        ...
    di/
      TripPlannerNavigationModule.kt ← Koin entry builder
```

---

## Route definition

Every screen destination is a `NavKey`. Routes must be:

1. `@Serializable` (kotlinx.serialization)
2. A `data object` (no args) or `data class` (with serializable args)
3. Registered in `krailNavSerializationConfig` (see below)

```kotlin
// core/navigation — app-level routes
@Serializable data object SplashRoute    : NavKey
@Serializable data object AppUpgradeRoute : NavKey

// feature/trip-planner/ui — feature routes
sealed interface TripPlannerRoute : NavKey

@Serializable data object SavedTripsRoute   : TripPlannerRoute
@Serializable data class  SearchStopRoute(val fieldType: SearchStopFieldType) : TripPlannerRoute
@Serializable data class  TimeTableRoute(
    val fromStopId: String, val fromStopName: String,
    val toStopId: String,   val toStopName: String,
) : TripPlannerRoute
@Serializable data class JourneyMapRoute(val journeyId: String) : TripPlannerRoute
// … more routes
```

The sealed-interface pattern (`TripPlannerRoute : NavKey`) is optional but useful for
exhaustive `when` expressions within a feature.

---

## Serialization configuration

### Why it is needed

Navigation 3 persists the back stack via `SavedState`. On non-JVM targets (iOS, Web)
Kotlin reflection is unavailable, so the library cannot discover serializers at runtime.
You must register every concrete `NavKey` subclass explicitly.

### The config

```kotlin
// composeApp/navigation/SerializationConfig.kt
val krailNavSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SplashRoute::class,            SplashRoute.serializer())
            subclass(AppUpgradeRoute::class,        AppUpgradeRoute.serializer())
            subclass(SavedTripsRoute::class,        SavedTripsRoute.serializer())
            subclass(SearchStopRoute::class,        SearchStopRoute.serializer())
            subclass(TimeTableRoute::class,         TimeTableRoute.serializer())
            subclass(JourneyMapRoute::class,        JourneyMapRoute.serializer())
            subclass(ThemeSelectionRoute::class,    ThemeSelectionRoute.serializer())
            subclass(ServiceAlertRoute::class,      ServiceAlertRoute.serializer())
            subclass(SettingsRoute::class,          SettingsRoute.serializer())
            subclass(DateTimeSelectorRoute::class,  DateTimeSelectorRoute.serializer())
            subclass(OurStoryRoute::class,          OurStoryRoute.serializer())
            subclass(IntroRoute::class,             IntroRoute.serializer())
            subclass(DiscoverRoute::class,          DiscoverRoute.serializer())
        }
    }
}
```

**Rule**: every new route added to the app must also be added here. Forgetting this causes
a runtime crash on iOS/Web and may silently break back-stack restoration on Android.

---

## NavigationState and the back stack

### `rememberNavigationState`

```kotlin
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    serializationConfig: SavedStateConfiguration,
): NavigationState
```

Internally it calls `rememberNavBackStack(serializationConfig, key)` for each
top-level route. `rememberNavBackStack` is a Navigation 3 API that wraps a
`NavBackStack` in `rememberSaveable`, so the back stack survives:

- Configuration changes (rotation)
- Process death and recreation (Android)

The returned `NavigationState` is itself wrapped in `remember`, so the object
identity is stable across recompositions.

### `NavigationState`

```kotlin
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey           // which top-level stack is active
    val stacksInUse: List<NavKey>       // list pane + optional detail pane

    fun goTo(route: NavKey)             // push or switch top-level tab
    fun pop()                           // remove last entry; switch to start if root
    fun resetRoot(route: NavKey)        // clear stack, push single route
    fun replaceCurrent(route: NavKey)   // pop top, push new (like popUpTo inclusive)
}
```

`stacksInUse` drives the List-Detail pane strategy: if the active top-level route
equals `startRoute`, only one stack (the list pane) is in scope; otherwise both the
start stack and the active stack are included, enabling two-pane layouts on tablets.

### `toEntries` extension

```kotlin
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>>
```

Decorates each back stack with `rememberSaveableStateHolderNavEntryDecorator` (which
is what saves/restores `rememberSaveable` state per entry) and flattens `stacksInUse`
into a single observable list for `NavDisplay`.

---

## Navigator

`Navigator` is the app-level class that wraps `NavigationState` and implements the
`NavigatorBase` interface. Feature modules receive a `NavigatorBase`, not `Navigator`,
which avoids circular dependencies.

```kotlin
class Navigator(val state: NavigationState) : NavigatorBase {
    // Theme (temporary coupling — see migration plan in Navigator.kt)
    var themeColor: String by mutableStateOf(DEFAULT_THEME_STYLE.hexColorCode)
        private set
    override fun updateTheme(hexColorCode: String) { themeColor = hexColorCode }

    override fun goTo(route: NavKey)            { state.goTo(route) }
    override fun pop()                          { state.pop() }
    override fun resetRoot(route: NavKey)       { state.resetRoot(route) }
    override fun replaceCurrent(route: NavKey)  { state.replaceCurrent(route) }
    override fun pushSingleInstance(route: NavKey) { /* no-op if same type on top */ }
}
```

### `rememberNavigator`

```kotlin
@Composable
fun rememberNavigator(state: NavigationState): Navigator {
    val sandook: Sandook = koinInject()
    val navigator = remember(state) { Navigator(state) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val themeStyle = loadThemeFromDb(sandook)
            navigator.updateTheme(themeStyle.hexColorCode)
        }
    }

    return navigator
}
```

- `remember(state)` — one `Navigator` instance per `NavigationState`.
- `LaunchedEffect(Unit)` — runs once per composition (including after rotation), loads
  the user's theme from the database, and updates `navigator.themeColor`.

---

## NavigatorBase interface

Lives in `core:navigation` so feature modules can depend on it without depending on
the `composeApp` module (which would create a circular dependency).

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

Feature modules wrap `NavigatorBase` in their own interface:

```kotlin
// feature/trip-planner/ui
interface TripPlannerNavigator {
    fun navigateToSearchStop(fieldType: SearchStopFieldType)
    fun navigateToTimeTable(from: Stop, to: Stop)
    fun pop()
    fun updateTheme(hexColorCode: String)
    // …
}

class TripPlannerNavigatorImpl(private val base: NavigatorBase) : TripPlannerNavigator {
    override fun navigateToSearchStop(fieldType: SearchStopFieldType) =
        base.goTo(SearchStopRoute(fieldType))
    // …
}
```

---

## KrailNavHost

The root composable. Call it once from the app's `Content` composable.

```kotlin
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun KrailNavHost(modifier: Modifier = Modifier) {
    val navigationState = rememberNavigationState(
        startRoute = SplashRoute,
        topLevelRoutes = setOf(SplashRoute),
        serializationConfig = krailNavSerializationConfig,
    )
    val navigator = rememberNavigator(navigationState)
    val resultEventBus = remember { ResultEventBus.getInstance() }

    val themeContentColor = getForegroundColor(navigator.themeColor.hexToComposeColor()).toHex()
    val entryProvider = collectEntryProviders(navigator = navigator)

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    val entries = navigationState.toEntries(entryProvider)

    CompositionLocalProvider(
        LocalThemeColor        provides mutableStateOf(navigator.themeColor),
        LocalThemeContentColor provides mutableStateOf(themeContentColor),
        LocalTextColor         provides KrailTheme.colors.onSurface,
        LocalResultEventBusObj provides resultEventBus,
    ) {
        if (entries.isNotEmpty()) {
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack  = { navigator.pop() },
                sceneStrategy = listDetailStrategy,
                modifier = modifier.fillMaxSize(),
            )
        } else {
            NoEntriesUI(navigator)   // graceful fallback
        }
    }
}
```

Key points:
- `topLevelRoutes = setOf(SplashRoute)` — single top-level stack; extend this set to
  support bottom-navigation with multiple independent back stacks.
- `horizontalPartitionSpacerSize = 0.dp` — removes the default gutter between
  list and detail panes (workaround for `b/418201867`).
- `NoEntriesUI` handles the edge case where the entry list is momentarily empty
  (e.g., Koin injection hasn't completed yet), preventing a crash.

---

## Koin multibinding wiring

### Overview

Each feature provides an `EntryBuilderDescriptor` to Koin tagged with a named qualifier.
The app module collects all descriptors in `collectEntryProviders` and passes them to
`entryProvider { … }`.

### Types

```kotlin
// core/navigation
typealias FeatureEntryBuilder = @Composable EntryProviderScope<NavKey>.(navigator: Any) -> Unit

data class EntryBuilderDescriptor(
    val name: String,
    val builder: FeatureEntryBuilder,
)

object EntryBuilderQualifiers {
    val SPLASH        = named("splash")
    val APP_UPGRADE   = named("appUpgrade")
    val TRIP_PLANNER  = named("tripPlanner")
}
```

### Feature module provides

```kotlin
// feature/trip-planner/ui
val tripPlannerNavigationModule = module {
    factory<EntryBuilderDescriptor>(qualifier = EntryBuilderQualifiers.TRIP_PLANNER) {
        EntryBuilderDescriptor(
            name = EntryBuilderQualifiers.Names.TRIP_PLANNER,
            builder = { navigator ->
                val baseNavigator = navigator as NavigatorBase
                TripPlannerEntries(TripPlannerNavigatorImpl(baseNavigator))
            },
        )
    }
}
```

### App module collects

```kotlin
// composeApp
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

### Screen entry definition (inside a feature)

```kotlin
@Composable
fun EntryProviderScope<NavKey>.TripPlannerEntries(navigator: TripPlannerNavigator) {
    SavedTripsEntry(navigator)
    SearchStopEntry(navigator)
    TimeTableEntry(navigator)
    // …
}

@Composable
private fun EntryProviderScope<NavKey>.SavedTripsEntry(navigator: TripPlannerNavigator) {
    entry<SavedTripsRoute>(
        metadata = ListDetailSceneStrategy.listPane()   // marks as list in adaptive layout
    ) { _ ->
        val viewModel: SavedTripsViewModel = koinViewModel()
        ResultEffect<StopSelectedResult> { result -> viewModel.onEvent(…) }
        SavedTripsScreen(viewModel = viewModel, navigator = navigator)
    }
}
```

---

## Result passing between screens

When a "detail" screen needs to return data to the screen that opened it
(or to a list pane in a two-pane layout), use `ResultEventBus`.

### Why not `BackStackEntry.savedStateHandle`?

In a two-pane layout the list and detail screens are in **separate composition scopes**
(different `NavDisplay` panes). The Navigation 3 `SavedStateHandle` is scoped to a single
entry and cannot cross pane boundaries. A singleton channel-based bus solves this.

### Send a result

```kotlin
// SearchStopScreen (detail pane)
val bus = LocalResultEventBusObj.current
bus.sendResult(result = StopSelectedResult(stopId, stopName, fieldType))
navigator.pop()
```

### Receive a result

```kotlin
// SavedTripsScreen (list pane)
ResultEffect<StopSelectedResult> { result ->
    viewModel.onEvent(StopSelected(result))
}
```

`ResultEffect` wraps a `LaunchedEffect` that collects from the channel. It is
automatically cancelled when the composable leaves composition.

---

## Configuration-change survival summary

| What                  | Mechanism                                      |
|-----------------------|------------------------------------------------|
| Back stack entries    | `rememberNavBackStack` → `rememberSaveable`   |
| Route objects         | kotlinx.serialization via `SavedStateConfiguration` |
| `rememberSaveable` state inside screens | `rememberSaveableStateHolderNavEntryDecorator` applied in `toEntries()` |
| ViewModel             | Standard `koinViewModel()` scoped to entry     |
| App theme             | Reloaded from DB in `LaunchedEffect(Unit)` inside `rememberNavigator` |

---

## Applying this pattern to another CMP project

### Step 1 — Gradle setup

Add to `gradle/libs.versions.toml`:

```toml
[versions]
multiplatform-nav3-ui          = "1.0.0-alpha06"
compose-multiplatform-adaptive = "1.3.0-alpha05"
koin                           = "4.1.1"

[libraries]
navigation3-ui              = { module = "org.jetbrains.androidx.navigation3:navigation3-ui",              version.ref = "multiplatform-nav3-ui" }
lifecycle-viewmodel-nav3    = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "compose-multiplatform-lifecycle" }
adaptive-navigation3        = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation3",  version.ref = "compose-multiplatform-adaptive" }
koin-android                = { module = "io.insert-koin:koin-android",                    version.ref = "koin" }
koin-compose-viewmodel      = { module = "io.insert-koin:koin-compose-viewmodel",           version.ref = "koin" }
```

Apply in your navigation module:

```kotlin
plugins {
    alias(libs.plugins.kotlin.serialization)
}
commonMain.dependencies {
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.nav3)
    implementation(libs.adaptive.navigation3)
    implementation(libs.kotlinx.serialization.json)
}
```

### Step 2 — Define routes

```kotlin
// Every route: @Serializable data object or data class, implements NavKey
@Serializable data object HomeRoute     : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data class  DetailRoute(val id: String) : NavKey
```

### Step 3 — Create the serialization config

```kotlin
val myAppNavSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomeRoute::class,     HomeRoute.serializer())
            subclass(SettingsRoute::class, SettingsRoute.serializer())
            subclass(DetailRoute::class,   DetailRoute.serializer())
        }
    }
}
```

### Step 4 — Create NavigationState

```kotlin
val navState = rememberNavigationState(
    startRoute          = HomeRoute,
    topLevelRoutes      = setOf(HomeRoute),   // add more for bottom-nav tabs
    serializationConfig = myAppNavSerializationConfig,
)
```

### Step 5 — Create Navigator

```kotlin
interface AppNavigatorBase {
    fun goTo(route: NavKey)
    fun pop()
    fun resetRoot(route: NavKey)
    fun replaceCurrent(route: NavKey)
}

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

### Step 6 — Wire NavDisplay

```kotlin
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavHost() {
    val navState  = rememberNavigationState(HomeRoute, setOf(HomeRoute), myAppNavSerializationConfig)
    val navigator = rememberAppNavigator(navState)

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        entries       = navState.toEntries(myEntryProvider(navigator)),
        onBack        = { navigator.pop() },
        sceneStrategy = listDetailStrategy,
        modifier      = Modifier.fillMaxSize(),
    )
}

fun myEntryProvider(navigator: AppNavigator): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        entry<HomeRoute>     { HomeScreen(navigator) }
        entry<SettingsRoute> { SettingsScreen(navigator) }
        entry<DetailRoute>   { key -> DetailScreen(id = key.id, navigator = navigator) }
    }
```

### Step 7 — Add `rememberSaveableStateHolderNavEntryDecorator` (important)

If you use the `toEntries` extension from this project, the decorator is applied
automatically. If you write `entryProvider` yourself, wrap it:

```kotlin
val entries = rememberDecoratedNavEntries(
    backStack      = navState.backStacks[navState.topLevelRoute]!!,
    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    entryProvider  = myEntryProvider(navigator),
)
```

Without this decorator, `rememberSaveable` state (scroll position, text field content,
etc.) is lost when navigating back to a screen.

---

## FAQ

**Why is `SavedStateConfiguration` needed on Android if reflection works there?**
For consistency with iOS/Web. A single config runs on all targets.

**Why does the entry provider use `factory` not `single` in Koin?**
`EntryBuilderDescriptor` is a data class; there is no shared mutable state, so a new
instance per injection is fine and avoids accidental state leakage.

**What happens if I forget to register a route in `krailNavSerializationConfig`?**
On Android you may see a runtime crash when the system tries to restore the saved back
stack after process death. On iOS/Web the crash occurs immediately on first navigation.

**Can I use `@Serializable` data classes with non-primitive fields?**
Yes, as long as every field type is also `@Serializable`. Avoid `Any`, interfaces, or
platform-specific types. Use JSON-encoded strings as a workaround for complex types
(see `DateTimeSelectorRoute.dateTimeSelectionItemJson`).

**How do I add a new top-level tab (e.g., for bottom navigation)?**
1. Create a new `@Serializable data object MyTabRoute : NavKey`.
2. Add it to `topLevelRoutes` in `rememberNavigationState`.
3. Register it in `krailNavSerializationConfig`.
4. Call `navigator.goTo(MyTabRoute)` from your bottom-navigation bar.
   `NavigationState.goTo` detects that the route is a top-level key and switches stacks
   instead of pushing onto the current stack.
