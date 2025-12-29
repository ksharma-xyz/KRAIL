# Navigation Architecture - Nav 2.x → Nav 3.x Migration

**Last Updated:** December 29, 2025  
**Status:** Production Ready  
**Navigation Library:** AndroidX Navigation 3.x (Alpha)

---

## Table of Contents

1. [Overview](#overview)
2. [Migration Context](#migration-context)
3. [Architecture Pattern](#architecture-pattern)
4. [Modularization Strategy](#modularization-strategy)
5. [Multibinding Approach](#multibinding-approach)
6. [Navigation Patterns](#navigation-patterns)
7. [Data Passing & State Preservation](#data-passing--state-preservation)
8. [List-Detail Pattern](#list-detail-pattern)
9. [Polymorphic Serialization](#polymorphic-serialization)
10. [Trade-offs & Future Work](#trade-offs--future-work)
11. [Implementation Guide](#implementation-guide)

---

## Overview

This document describes KRAIL's navigation architecture migrated from AndroidX Navigation 2.x to
Navigation 3.x. The architecture follows **Google's modularization guidelines** while adapting to
Koin's limitations compared to Dagger's multibinding support.

### Key Technologies

- **AndroidX Navigation 3.x** - Type-safe navigation with Kotlin Serialization
- **Koin** - Dependency injection (lacks native multibinding)
- **Jetpack Compose** - Declarative UI framework
- **Kotlin Serialization** - Type-safe route serialization

---

## Migration Context

### Why Navigation 3?

Navigation 3 introduces **type-safe navigation** using Kotlin Serialization, eliminating
string-based routes and manual argument parsing.

**Before (Nav 2.x):**

```kotlin
composable("timetable/{fromStopId}/{toStopId}") { backStackEntry ->
    val fromStopId = backStackEntry.arguments?.getString("fromStopId")
    val toStopId = backStackEntry.arguments?.getString("toStopId")
    // Manual null checks, type casting, error handling...
}
```

**After (Nav 3.x):**

```kotlin
@Serializable
data class TimeTableRoute(
    val fromStopId: String,
    val toStopId: String,
)

entry<TimeTableRoute> { route ->
    // route.fromStopId and route.toStopId are guaranteed non-null
    // Type-safe, compile-time checked
}
```

### Migration Challenges

1. **State Preservation** - Nav 3 doesn't use `savedStateHandle` the same way
2. **Modularization** - Need to decouple navigation from app module
3. **Multibinding** - Koin lacks Dagger's native multibinding support
4. **Alpha Status** - Nav 3 and Koin's integration are experimental

---

## Architecture Pattern

### Google's Modularization Approach

Following [Google's Navigation 3 Modularization Guide](https://developer.android.com/guide/navigation/navigation-3/modularize),
we implement a **feature-based navigation pattern** where:

1. Each feature module defines its own navigation entries
2. Entries are collected and aggregated at the app level
3. Dependency injection provides navigation dependencies

**Reference Architecture:**

```
Google's Approach (Dagger):
┌─────────────────────────────────────────────────┐
│ App Module                                      │
│  ├── Collects @IntoSet EntryProvider           │
│  └── Aggregates all feature navigation         │
└─────────────────────────────────────────────────┘
          ▲                    ▲
          │                    │
┌─────────┴─────────┐  ┌──────┴──────────┐
│ Feature Module A  │  │ Feature Module B│
│  @Provides        │  │  @Provides      │
│  @IntoSet         │  │  @IntoSet       │
│  EntryProvider    │  │  EntryProvider  │
└───────────────────┘  └─────────────────┘
```

---

## Modularization Strategy

### Current Implementation

KRAIL uses a **manual multibinding approach** since Koin lacks native multibinding support.

```
KRAIL Architecture:
┌─────────────────────────────────────────────────┐
│ App Module (composeApp)                         │
│  ├── collectEntryProviders()                    │
│  │   └── Manually collects from Koin           │
│  └── KrailNavHost                               │
│      └── EntryProvider aggregation              │
└─────────────────────────────────────────────────┘
          ▲                    ▲
          │                    │
┌─────────┴─────────┐  ┌──────┴──────────┐
│ feature:           │  │ composeApp:     │
│ trip-planner:ui    │  │ navigation      │
│                    │  │                 │
│ TripPlanner        │  │ Splash, App     │
│ NavigationModule   │  │ Upgrade entries │
└───────────────────┘  └─────────────────┘
```

### File Structure

```
composeApp/
└── src/commonMain/kotlin/xyz/ksharma/krail/
    ├── KrailNavHost.kt                    # Main navigation host
    └── navigation/
        ├── Navigator.kt                    # Navigation controller
        └── di/
            ├── AppNavigationModule.kt      # App-level entries
            └── EntryProviderCollector.kt   # Manual multibinding

feature/trip-planner/ui/
└── src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/
    └── entries/
        ├── TripPlannerEntries.kt          # Feature aggregator (27 lines!)
        ├── TripPlannerNavigationModule.kt # Koin module
        ├── TripPlannerNavigator.kt        # Feature navigator interface
        ├── SavedTripsEntry.kt             # Individual entries
        ├── TimeTableEntry.kt
        ├── SearchStopEntry.kt
        └── ... (8 total entry files)

core/navigation/
└── src/commonMain/kotlin/xyz/ksharma/krail/core/navigation/
    ├── EntryBuilderDescriptor.kt          # Wrapper for multibinding
    └── EntryBuilderQualifiers.kt          # Named qualifiers
```

---

## Multibinding Approach

### The Problem: Koin vs Dagger

**Dagger's Multibinding (Native):**

```kotlin
@Module
interface FeatureNavigationModule {
    @Provides
    @IntoSet  // 👈 Native multibinding
    fun provideEntryProvider(): EntryProvider = { ... }
}

// At app level, Dagger automatically collects all @IntoSet
@Inject lateinit var entryProviders: Set<EntryProvider>
```

**Koin's Limitation:**
Koin **does not support native multibinding** like Dagger's `@IntoSet` or `@IntoMap`.

**GitHub Pull Request:**  
🔗 [Koin Multibinding PR #1951](https://github.com/InsertKoinIO/koin/pull/1951)

- Status: Open since August 2024
- Not merged, not production-ready
- Experimental API with breaking changes expected

**Koin Navigation 3 Library:**  
🔗 [Koin Compose Navigation 3](https://insert-koin.io/docs/reference/koin-compose/navigation3/)

- Status: **Experimental** (`@KoinExperimentalAPI`)
- Navigation 3 itself is in **Alpha**
- Several open issues on GitHub
- Not recommended for production use yet

### Our Solution: Manual Multibinding

We implement a **manual multibinding pattern** using Koin's qualifier system.

#### Step 1: Define Entry Builder Descriptor

```kotlin
// core:navigation/EntryBuilderDescriptor.kt

/**
 * Wrapper for entry builder functions to enable multibinding-like behavior in Koin.
 *
 * Each feature module provides these descriptors, which are collected at the app level.
 * This mimics Dagger's @IntoSet multibinding approach.
 */
data class EntryBuilderDescriptor(
    val name: String,
    val builder: EntryProviderScope<NavKey>.(Any) -> Unit
)
```

#### Step 2: Provide Descriptors in Feature Modules

```kotlin
// feature:trip-planner:ui/TripPlannerNavigationModule.kt

val tripPlannerNavigationModule = module {
    // Provide descriptor with named qualifier
    factory<EntryBuilderDescriptor>(qualifier = named("tripPlanner")) {
        EntryBuilderDescriptor(
            name = "tripPlanner",
            builder = { navigator ->
                tripPlannerEntries(navigator as TripPlannerNavigator)
            }
        )
    }
}
```

#### Step 3: Manually Collect All Descriptors

```kotlin
// composeApp/navigation/di/EntryProviderCollector.kt

@Composable
fun collectEntryProviders(
    navigator: Navigator,
    tripPlannerNavigator: TripPlannerNavigator
): (NavKey) -> NavEntry<NavKey> {
    val koin = KoinContext.get()

    return entryProvider {
        // Manually get each descriptor by qualifier
        val appDescriptors = listOf(
            koin.getOrNull<EntryBuilderDescriptor>(named("splash")),
            koin.getOrNull<EntryBuilderDescriptor>(named("appUpgrade"))
        )

        val featureDescriptors = listOf(
            koin.getOrNull<EntryBuilderDescriptor>(named("tripPlanner"))
        )

        // Apply each builder
        (appDescriptors + featureDescriptors).forEach { descriptor ->
            descriptor?.let {
                // Invoke the builder with appropriate navigator
                it.builder(this, determineNavigator(it.name, navigator, tripPlannerNavigator))
            }
        }
    }
}
```

### Why This Approach?

**Pros:**
✅ Works with current Koin version (stable)  
✅ No experimental APIs  
✅ Full control over entry collection  
✅ Easy to debug  
✅ Type-safe with sealed interfaces

**Cons:**
❌ **Manual maintenance** - Must update collector when adding features  
❌ **App module dependency** - App module knows about all features  
❌ **Rebuild app module** - Changes in feature navigation require app rebuild  
❌ Not truly "automatic" like Dagger's multibinding

---

## Navigation Patterns

### 1. Route Definition (Polymorphic Serialization)

All routes inherit from a sealed interface for type-safety:

```kotlin
// core:navigation/NavKey.kt

/**
 * Base sealed interface for all navigation routes.
 * Enables polymorphic serialization and type-safe navigation.
 */
@Serializable
sealed interface NavKey

// Feature routes
@Serializable
data class TimeTableRoute(
    val fromStopId: String,
    val fromStopName: String,
    val toStopId: String,
    val toStopName: String,
) : NavKey

@Serializable
data object SavedTripsRoute : NavKey

@Serializable
data object SplashRoute : NavKey
```

**Polymorphic Serialization** allows Navigation 3 to:

- Serialize any `NavKey` to the backstack
- Deserialize back to the correct type
- Maintain type safety across the navigation graph

### 2. Entry Registration

Each screen registers its entry using the `entry<T>` DSL:

```kotlin
@Composable
internal fun EntryProviderScope<NavKey>.TimeTableEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<TimeTableRoute>(
        metadata = ListDetailSceneStrategy.detailPane() // 👈 List-Detail metadata
    ) { route ->
        val viewModel: TimeTableViewModel = koinViewModel()
        val timeTableState by viewModel.uiState.collectAsStateWithLifecycle()

        // Initialize trip
        LaunchedEffect(route.fromStopId, route.toStopId) {
            viewModel.initializeTrip(
                fromStopId = route.fromStopId,
                fromStopName = route.fromStopName,
                toStopId = route.toStopId,
                toStopName = route.toStopName
            )
        }

        TimeTableScreen(
            timeTableState = timeTableState,
            onBackClick = { tripPlannerNavigator.pop() }
        )
    }
}
```

### 3. Navigator Pattern

We use a **navigator interface** to decouple features from the app module.

```kotlin
// feature:trip-planner:ui/TripPlannerNavigator.kt

/**
 * Navigator interface for Trip Planner feature.
 *
 * This interface decouples the feature from the app's Navigator implementation,
 * improving build performance and modularity.
 */
interface TripPlannerNavigator {
    fun goTo(route: NavKey)
    fun pop()
    fun resetRoot(route: NavKey)
    fun updateTheme(hexColorCode: String)
}

// Implementation in app module
class TripPlannerNavigatorImpl(
    private val navigator: (NavKey) -> Unit,
    private val goBack: () -> Unit,
    private val onThemeUpdate: (String) -> Unit,
    private val onResetRoot: (NavKey) -> Unit
) : TripPlannerNavigator {
    override fun goTo(route: NavKey) = navigator(route)
    override fun pop() = goBack()
    override fun updateTheme(hexColorCode: String) = onThemeUpdate(hexColorCode)
    override fun resetRoot(route: NavKey) = onResetRoot(route)
}
```

**Why this pattern?**

Without the interface, passing `Navigator` directly would:

1. Create a dependency from feature → app module
2. Break modularization
3. Trigger full app rebuilds on feature changes

With the interface:

1. Feature depends only on its own interface
2. App module implements the interface
3. Changes in feature don't rebuild app module (in theory - see trade-offs)

---

## Data Passing & State Preservation

### Type-Safe Route Parameters

Parameters are part of the route class:

```kotlin
@Serializable
data class TimeTableRoute(
    val fromStopId: String,
    val toStopId: String,
) : NavKey

// Navigation
navigator.navigate(
    TimeTableRoute(
        fromStopId = "200060",
        toStopId = "200340"
    )
)

// Access in entry
entry<TimeTableRoute> { route ->
    // route.fromStopId ✅ Type-safe, non-null
    // route.toStopId   ✅ Type-safe, non-null
}
```

### State Preservation with Custom Savers

**Problem:** Configuration changes (rotation) destroy composable state.

**Solution:** Custom `Saver` implementations with `rememberSaveable`.

#### Example 1: PersistentSet Saver

```kotlin
// feature:trip-planner:ui/savers/PersistentSetSaver.kt

/**
 * Generic saver for PersistentSet that survives configuration changes.
 *
 * Uses JSON serialization to save/restore the set.
 */
inline fun <reified T> persistentSetSaver(): Saver<PersistentSet<T>, String> = Saver(
    save = { set ->
        Json.encodeToString(set.toList())
    },
    restore = { json ->
        Json.decodeFromString<List<T>>(json).toPersistentSet()
    }
)

// Usage
var alertsToDisplay by rememberSaveable(stateSaver = serviceAlertSaver()) {
    mutableStateOf(persistentSetOf<ServiceAlert>())
}
```

#### Example 2: DateTimeSelection Saver

```kotlin
// feature:trip-planner:ui/savers/DateTimeSelectionSaver.kt

/**
 * Saver for DateTimeSelectionItem.
 *
 * Survives rotation and process death.
 */
fun dateTimeSelectionSaver(): Saver<DateTimeSelectionItem?, String> = Saver(
    save = { item -> item?.toJsonString() ?: "" },
    restore = { json ->
        if (json.isEmpty()) null
        else DateTimeSelectionItem.fromJsonString(json)
    }
)

// Usage in TimeTableEntry
var dateTimeSelectionItem by rememberSaveable(
    tripId, // 👈 Key: clears when trip changes
    stateSaver = dateTimeSelectionSaver(),
) {
    mutableStateOf<DateTimeSelectionItem?>(null)
}
```

**Key Benefits:**

- ✅ Survives screen rotation
- ✅ Survives process death (if under size limit)
- ✅ Type-safe
- ✅ Scoped to specific keys (e.g., tripId)

### Returning Data to Previous Screen

**Pattern:** Use ViewModel events or shared state.

```kotlin
// In DateTimeSelectorEntry
DateTimeSelectorScreen(
    onDateTimeSelected = { selection ->
        // Update the selection in the entry's state
        dateTimeSelectionItem = selection

        // Notify ViewModel
        viewModel.onEvent(
            TimeTableUiEvent.DateTimeSelectionChanged(selection)
        )

        // Close modal
        showDateTimeSelectorModal = false
    }
)
```

This pattern:

1. Updates local state (survives rotation via saver)
2. Notifies ViewModel (business logic)
3. Closes the modal
4. State is preserved when returning to TimeTable screen

---

## List-Detail Pattern

Navigation 3 supports **adaptive layouts** with the List-Detail pattern for tablets/foldables.

### Declaring List vs Detail

Use `metadata` parameter in `entry<T>()`:

```kotlin
// LIST pane (default)
entry<SavedTripsRoute> { route ->
    SavedTripsScreen(...)
}

// DETAIL pane
entry<TimeTableRoute>(
    metadata = ListDetailSceneStrategy.detailPane() // 👈 Mark as detail
) { route ->
    TimeTableScreen(...)
}
```

### How It Works

```
Phone (Single Pane):          Tablet (Two Panes):
┌─────────────────┐          ┌─────────┬──────────────┐
│                 │          │         │              │
│  SavedTrips     │          │ Saved   │  TimeTable   │
│  (List)         │          │ Trips   │  (Detail)    │
│                 │          │ (List)  │              │
│  [Trip 1] ────► │          │ [Trip 1]│ ◄─ Selected  │
│  [Trip 2]       │          │ [Trip 2]│              │
│  [Trip 3]       │          │ [Trip 3]│              │
└─────────────────┘          └─────────┴──────────────┘

On phone: Navigate       →   On tablet: Show side-by-side
          (push detail)                  (update detail pane)
```

### Adding More Detail Screens

To make another screen a detail pane:

```kotlin
entry<YourNewRoute>(
    metadata = ListDetailSceneStrategy.detailPane() // 👈 Add this
) { route ->
    YourNewScreen(...)
}
```

The navigation system automatically:

- Shows side-by-side on large screens
- Stacks screens on small screens
- Handles back navigation appropriately

---

## Polymorphic Serialization

Navigation 3 serializes routes to save them in the backstack. For a sealed interface hierarchy, we
need **polymorphic serialization**.

### Setup

```kotlin
// core:navigation/NavKey.kt

@Serializable
sealed interface NavKey

// All routes must be @Serializable
@Serializable
data class TimeTableRoute(...) : NavKey

@Serializable
data object SplashRoute : NavKey
```

Kotlin Serialization automatically:

1. Generates serializers for each route
2. Handles polymorphic types via sealed interface
3. Includes type discriminator in JSON

### How It Works

```kotlin
// When navigating
navigator.navigate(TimeTableRoute("200060", "200340"))

// Serialized to backstack (simplified):
{
    "type": "TimeTableRoute",
    "fromStopId": "200060",
    "toStopId": "200340"
}

// Deserialized when restoring backstack:
val route: NavKey = deserialize() // ✅ Correct type restored
if (route is TimeTableRoute) {
    // Access route.fromStopId safely
}
```

---

## Trade-offs & Future Work

### Current Limitations

#### 1. App Module Compilation

**Issue:** Changes in feature navigation entries trigger app module recompilation.

**Why?**

- `collectEntryProviders()` manually lists all feature descriptors
- Adding/removing entries modifies app module code
- Gradle detects change and recompiles app module

**Impact:**

- Slower build times for navigation changes
- Not truly modular (app knows about all features)

**Mitigation:**

- Small project, acceptable for now
- Entry files are small and focused (27-170 lines)
- Most changes are in screens/ViewModels, not entries

#### 2. Manual Multibinding

**Issue:** No automatic discovery of entry providers.

**Why?**

- Koin lacks native multibinding
- [PR #1951](https://github.com/InsertKoinIO/koin/pull/1951) not merged
- Experimental Koin Navigation 3 library not production-ready

**Impact:**

- Must manually update `collectEntryProviders()` for each feature
- Easy to forget when adding new features
- More error-prone than Dagger's automatic approach

**Mitigation:**

- Clear documentation (this file!)
- Code review checklist
- Small team, easy to coordinate

#### 3. Navigator Interface Pattern

**Issue:** Still requires app module implementation.

**Why?**

- Feature defines interface
- App module implements interface
- App module passes implementation to feature

**Impact:**

- Some coupling still exists
- Not fully independent modules

**Mitigation:**

- Coupling is via interface (loose coupling)
- Feature can be tested with mock implementation
- Better than direct dependency on Navigator

### Future Improvements

#### Short-term (Next 6 months)

1. **Monitor Koin Multibinding PR**
    - Track [PR #1951](https://github.com/InsertKoinIO/koin/pull/1951)
    - Test when merged to stable release
    - Migrate when production-ready

2. **Monitor Navigation 3 Stability**
    - Currently in Alpha
    - Wait for Beta/RC
    - Review Koin's official Navigation 3 integration

3. **Add Entry Discovery Tests**
    - Unit test to verify all features are registered
    - Fails if entry builder not in collector
    - Prevents forgotten registrations

#### Long-term (1+ year)

1. **True Multibinding**
    - Migrate to Koin multibinding when stable
    - Or consider Dagger/Hilt for navigation module only
    - Automatic entry discovery

2. **Feature Module Independence**
    - Investigate better decoupling strategies
    - Possibly use navigation mediator pattern
    - Service locator for navigator instances

3. **Gradle Plugin**
    - Auto-generate `collectEntryProviders()`
    - Scan modules for entry providers
    - Update collector automatically

### Recommended Approach for New Features

When adding a new feature with navigation:

1. **Create entry files** in feature module (e.g., `feature:new-feature:ui/entries/`)
2. **Create Koin module** providing `EntryBuilderDescriptor`
3. **Update `collectEntryProviders()`** in app module
4. **Create navigator interface** if needed (for navigation callbacks)
5. **Document** in feature's README

**Checklist:**

```
□ Created NewFeatureEntry.kt
□ Created NewFeatureNavigationModule.kt
□ Added descriptor to collectEntryProviders()
□ Created NewFeatureNavigator interface (if needed)
□ Added tests for new routes
□ Updated this documentation
```

---

## Implementation Guide

### Adding a New Screen to Existing Feature

**Example:** Add "Trip History" screen to Trip Planner feature

#### Step 1: Define Route

```kotlin
// feature:trip-planner:ui/entries/TripPlannerRoutes.kt

@Serializable
data object TripHistoryRoute : NavKey
```

#### Step 2: Create Entry File

```kotlin
// feature:trip-planner:ui/entries/TripHistoryEntry.kt

@Composable
internal fun EntryProviderScope<NavKey>.TripHistoryEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<TripHistoryRoute> { route ->
        val viewModel: TripHistoryViewModel = koinViewModel()
        val historyState by viewModel.uiState.collectAsStateWithLifecycle()

        TripHistoryScreen(
            historyState = historyState,
            onBackClick = { tripPlannerNavigator.pop() },
            onTripClick = { trip ->
                tripPlannerNavigator.goTo(
                    TimeTableRoute(
                        fromStopId = trip.fromStopId,
                        toStopId = trip.toStopId,
                        fromStopName = trip.fromStopName,
                        toStopName = trip.toStopName
                    )
                )
            }
        )
    }
}
```

#### Step 3: Register in Aggregator

```kotlin
// feature:trip-planner:ui/entries/TripPlannerEntries.kt

@Composable
fun EntryProviderScope<NavKey>.tripPlannerEntries(
    tripPlannerNavigator: TripPlannerNavigator
) {
    SavedTripsEntry(tripPlannerNavigator)
    TimeTableEntry(tripPlannerNavigator)
    TripHistoryEntry(tripPlannerNavigator) // 👈 Add this
    // ... other entries
}
```

**That's it!** No changes needed in:

- ❌ App module
- ❌ Koin module
- ❌ Entry collector

The entry is automatically included via the aggregator.

### Adding a New Feature Module

**Example:** Add "Park & Ride" feature

#### Step 1: Create Feature Structure

```
feature/park-ride/
├── ui/
│   └── src/commonMain/kotlin/xyz/ksharma/krail/park/ride/ui/
│       ├── entries/
│       │   ├── ParkRideEntries.kt          # Aggregator
│       │   ├── ParkRideNavigationModule.kt # Koin module
│       │   ├── ParkRideNavigator.kt        # Navigator interface
│       │   ├── ParkRideRoutes.kt           # Route definitions
│       │   ├── ParkRideListEntry.kt        # List screen
│       │   └── ParkRideDetailEntry.kt      # Detail screen
│       └── parkride/
│           ├── ParkRideListScreen.kt
│           └── ParkRideDetailScreen.kt
└── state/
    └── src/commonMain/kotlin/.../state/
```

#### Step 2: Define Routes

```kotlin
// ParkRideRoutes.kt

@Serializable
data object ParkRideListRoute : NavKey

@Serializable
data class ParkRideDetailRoute(
    val parkRideId: String
) : NavKey
```

#### Step 3: Create Navigator Interface

```kotlin
// ParkRideNavigator.kt

interface ParkRideNavigator {
    fun goTo(route: NavKey)
    fun pop()
}
```

#### Step 4: Create Entry Files

```kotlin
// ParkRideListEntry.kt

@Composable
internal fun EntryProviderScope<NavKey>.ParkRideListEntry(
    parkRideNavigator: ParkRideNavigator
) {
    entry<ParkRideListRoute> { route ->
        ParkRideListScreen(
            onParkRideClick = { id ->
                parkRideNavigator.goTo(ParkRideDetailRoute(id))
            },
            onBackClick = { parkRideNavigator.pop() }
        )
    }
}

// ParkRideDetailEntry.kt

@Composable
internal fun EntryProviderScope<NavKey>.ParkRideDetailEntry(
    parkRideNavigator: ParkRideNavigator
) {
    entry<ParkRideDetailRoute>(
        metadata = ListDetailSceneStrategy.detailPane() // 👈 Detail pane
    ) { route ->
        ParkRideDetailScreen(
            parkRideId = route.parkRideId,
            onBackClick = { parkRideNavigator.pop() }
        )
    }
}
```

#### Step 5: Create Aggregator

```kotlin
// ParkRideEntries.kt

@Composable
fun EntryProviderScope<NavKey>.parkRideEntries(
    parkRideNavigator: ParkRideNavigator
) {
    ParkRideListEntry(parkRideNavigator)
    ParkRideDetailEntry(parkRideNavigator)
}
```

#### Step 6: Create Koin Module

```kotlin
// ParkRideNavigationModule.kt

val parkRideNavigationModule = module {
    factory<EntryBuilderDescriptor>(qualifier = named("parkRide")) {
        EntryBuilderDescriptor(
            name = "parkRide",
            builder = { navigator ->
                parkRideEntries(navigator as ParkRideNavigator)
            }
        )
    }
}
```

#### Step 7: Update App Module

**A. Implement Navigator**

```kotlin
// composeApp/KrailNavHost.kt

class ParkRideNavigatorImpl(
    private val navigator: (NavKey) -> Unit,
    private val goBack: () -> Unit,
) : ParkRideNavigator {
    override fun navigate(route: NavKey) = navigator(route)
    override fun goBack() = goBack()
}

@Composable
fun KrailNavHost(...) {
    val parkRideNavigator = remember(navigator) {
        ParkRideNavigatorImpl(
            navigator = { route -> navigator.navigate(route) },
            goBack = { navigator.goBack() }
        )
    }

    // ...
}
```

**B. Update Entry Collector**

```kotlin
// composeApp/navigation/di/EntryProviderCollector.kt

@Composable
fun collectEntryProviders(
    navigator: Navigator,
    tripPlannerNavigator: TripPlannerNavigator,
    parkRideNavigator: ParkRideNavigator // 👈 Add parameter
): (NavKey) -> NavEntry<NavKey> {
    val koin = KoinContext.get()

    return entryProvider {
        // ... existing entries

        // Park & Ride entries
        koin.getOrNull<EntryBuilderDescriptor>(named("parkRide"))?.let {
            it.builder(this, parkRideNavigator) // 👈 Add builder
        }
    }
}
```

**C. Update KrailNavHost Call**

```kotlin
val entryProvider = collectEntryProviders(
    navigator = navigator,
    tripPlannerNavigator = tripPlannerNavigator,
    parkRideNavigator = parkRideNavigator // 👈 Pass navigator
)
```

**D. Register Koin Module**

```kotlin
// composeApp/di/AppModule.kt

val appModules = listOf(
    // ... existing modules
    parkRideNavigationModule, // 👈 Add module
)
```

---

## FAQ

### Q: Why not use Koin's experimental Navigation 3 library?

**A:** It's marked `@KoinExperimentalAPI` and Navigation 3 is in Alpha. Both are subject to breaking
changes. The manual approach is more stable for production.

### Q: Will adding a screen to a feature rebuild the app module?

**A:** No, if you add it to an existing feature's aggregator (e.g., `TripPlannerEntries.kt`). Only
adding a **new feature module** requires updating the app module.

### Q: Why does Navigator have `updateTheme()`? That's not navigation!

**A:** Historical design decision. The Navigator holds the current theme color state for Material
3's adaptive theme system.

**For detailed explanation with usage scenarios and migration plan, see:**

- 📄 `composeApp/src/commonMain/kotlin/xyz/ksharma/krail/navigation/Navigator.kt`
- Look for the "THEME MANAGEMENT (Temporary Coupling)" section with comprehensive comments

**Quick Summary:**

1. **Splash screen** to load and set the saved theme
2. **All screens** to access consistent theme via `LocalThemeColor`
3. **Theme selection screen** to update theme globally

**Usage Scenarios Documented in Navigator.kt:**

- 🚀 **App Launch** - Theme loads from DB on cold start
- 🎨 **Theme Selection** - User picks new theme, updates globally
- 🔄 **Screen Rotation** - Theme restored from DB automatically

**Better design (future):** Extract theme management to a separate `ThemeManager` or use
`CompositionLocal` directly. Navigator should only handle navigation.

```kotlin
// Current (coupled) - See Navigator.kt for full explanation
interface Navigator {
    fun navigate(route: NavKey)
    fun updateTheme(hexColorCode: String) // 👈 Not navigation logic
}

// Better (decoupled) - Migration plan in Navigator.kt comments
interface Navigator {
    fun navigate(route: NavKey)
}

interface ThemeManager {
    val themeColor: StateFlow<String>
    fun updateTheme(hexColorCode: String)
}
```

**Why it exists:**

```kotlin
class Navigator(...) : NavigatorBase {
    // Theme state - NOT navigation logic!
    // See comprehensive comments in Navigator.kt explaining:
    // - Who uses this (rememberNavigator, KrailTheme, ThemeSelectionScreen)
    // - When it's used (app launch, theme change, rotation)
    // - Why it's here (temporary coupling during Nav 3 migration)
    // - How to migrate (step-by-step ThemeManager extraction plan)
    var themeColor: String by mutableStateOf(DEFAULT_THEME_STYLE.hexColorCode)
        private set

    override fun updateTheme(hexColorCode: String) {
        themeColor = hexColorCode
    }
}
```

This is a **temporary coupling** for convenience during Nav 2 → Nav 3 migration. Should be
refactored when implementing a proper theme system.

**See Navigator.kt for:**

- ✅ Complete app launch flow diagram
- ✅ Detailed usage scenarios
- ✅ Step-by-step migration plan
- ✅ Code examples for ThemeManager extraction

### Q: Why use Circuit-style naming (`pop()`, `resetRoot()`)?

**A:** KRAIL follows [Slack's Circuit navigation framework](https://slackhq.github.io/circuit/)
naming conventions for consistency with modern Compose navigation patterns.

**Circuit-Style API:**

```kotlin
navigator.goTo(route)        // Navigate to a screen
navigator.pop()              // Navigate back
navigator.resetRoot(route)   // Clear backstack and navigate
navigator.replaceCurrent(route) // Replace current screen
```

**Why Circuit?**

1. **Industry Standard** - Circuit is becoming the de-facto pattern for Compose navigation
2. **Concise** - Short, clear method names (`pop()` vs `popBackStack()`)
3. **Consistent** - Same pattern across modern Compose apps
4. **Well-Documented** - Strong community support and examples

**Navigation 3 Important Behavior:**

Even though we use `pop()`, be aware that Navigation 3 keeps the NavEntry alive during exit
animations (~300ms). See `Navigator.kt` for detailed comments explaining this lifecycle behavior.

**Example:**

```kotlin
// User on TimeTableScreen, presses back
navigator.pop()
↓
Route removed from NavBackStack ✅
↓
TimeTableScreen STILL VISIBLE(animating out)
↓
TimeTableViewModel STILL ALIVE(flows collecting)
↓
~300 ms animation completes
↓
Finally disposed and destroyed
```

**Circuit Naming Summary:**

| Action           | Circuit Name            | Legacy Name                   | Notes              |
|------------------|-------------------------|-------------------------------|--------------------|
| Navigate forward | `goTo(route)`           | `navigate(route)`             | ✅ Primary          |
| Navigate back    | `pop()`                 | `goBack()`                    | ✅ Circuit standard |
| Clear & navigate | `resetRoot(route)`      | `clearBackStackAndNavigate()` | ✅ Circuit standard |
| Replace current  | `replaceCurrent(route)` | `navigateAndReplace()`        | Common pattern     |

**Backward Compatibility:**

Legacy methods (`navigate()`, `goBack()`, `clearBackStackAndNavigate()`) are marked `@Deprecated`
with `ReplaceWith` suggestions for easy migration.

**See Also:**

- `Navigator.kt` - Implementation with detailed lifecycle comments
- [Circuit Documentation](https://slackhq.github.io/circuit/)

### Q: Can I use this pattern with Hilt/Dagger?

**A:** Yes! With Dagger, you can use native `@Multibinds`:

```kotlin
@Module
interface NavigationModule {
    @Multibinds
    fun bindEntryProviders(): Set<EntryProvider>
}

@Module
interface FeatureNavigationModule {
    @Provides
    @IntoSet
    fun provideEntryProvider(): EntryProvider = { ... }
}

// Automatic collection
@Inject lateinit var entryProviders: Set<EntryProvider>
```

No manual collector needed!

### Q: How do I debug navigation issues?

**Tips:**

1. Enable navigation logging: Add logs in `Navigator.navigate()`
2. Check Koin module registration: Verify all modules are in `appModules` list
3. Verify routes are `@Serializable`
4. Check `collectEntryProviders()` includes your feature
5. Use Android Studio's Nav Inspector (experimental for Nav 3)

---

## Conclusion

KRAIL's navigation architecture successfully migrates to Navigation 3 while working around Koin's
multibinding limitations. The manual approach is:

- ✅ **Production-ready** (no experimental APIs)
- ✅ **Type-safe** (Kotlin Serialization)
- ✅ **Modular** (feature-based entries)
- ✅ **Maintainable** (small, focused files)
- ⚠️ **Manual** (requires updating collector for new features)

As Koin and Navigation 3 mature, we can migrate to native multibinding and automatic entry
discovery, eliminating the manual steps.

**Next Steps:**

1. Monitor [Koin PR #1951](https://github.com/InsertKoinIO/koin/pull/1951)
2. Test Koin Navigation 3 library when stable
3. Consider Gradle plugin for auto-generation
4. Refactor theme management out of Navigator

**References:**

- [Google Navigation 3 Modularization](https://developer.android.com/guide/navigation/navigation-3/modularize)
- [Koin Navigation 3 Integration](https://insert-koin.io/docs/reference/koin-compose/navigation3/)
- [Koin Multibinding PR #1951](https://github.com/InsertKoinIO/koin/pull/1951)
- [Navigation 3 Release Notes](https://developer.android.com/jetpack/androidx/releases/navigation)

---

**Document Version:** 1.0  
**Last Updated:** December 29, 2025  
**Authors:** KRAIL Team  
**Status:** Living Document - Update as architecture evolves

