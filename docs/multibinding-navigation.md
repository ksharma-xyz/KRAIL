# Multibinding Approach for Navigation in KRAIL

This document explains the custom multibinding pattern implemented for Koin, inspired by Google's Dagger multibinding approach.

## Overview

The multibinding pattern allows different modules to contribute navigation entries independently without creating circular dependencies. Each feature module provides its navigation entries through Koin modules, which are then collected and composed into a unified navigation graph.

## Architecture

### Core Components

1. **EntryBuilderDescriptor** (`core/navigation/Multibinding.kt`)
   - Wrapper class that holds entry builder metadata
   - Contains a `name` identifier and a `builder` function
   - Type: `data class EntryBuilderDescriptor`

2. **FeatureEntryBuilder** Type Alias
   - Function type for entry builders: `@Composable EntryProviderScope<NavKey>.(Navigator: Any) -> Unit`
   - Accepts any navigator type (cast to specific type in implementation)
   - Returns Unit

### Module Structure

#### App-Level Module (`appNavigationModule`)
Located at: `composeApp/src/commonMain/kotlin/xyz/ksharma/krail/navigation/di/AppNavigationModule.kt`

Provides app-level navigation entries:
- Splash screen entry
- App upgrade screen entry

```kotlin
val appNavigationModule = module {
    factory<EntryBuilderDescriptor>(qualifier = named("splash")) {
        EntryBuilderDescriptor(
            name = "splash",
            builder = { navigator -> splashEntry(navigator as Navigator) }
        )
    }
    
    factory<EntryBuilderDescriptor>(qualifier = named("appUpgrade")) {
        EntryBuilderDescriptor(
            name = "appUpgrade",
            builder = { navigator -> appUpgradeEntry() }
        )
    }
}
```

#### Feature-Level Module (`tripPlannerNavigationModule`)
Located at: `feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/di/TripPlannerNavigationModule.kt`

Provides Trip Planner feature navigation entries:
- SavedTrips, SearchStop, TimeTable, Settings, etc.

```kotlin
val tripPlannerNavigationModule = module {
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

### Entry Collection

#### EntryProviderCollector
Located at: `composeApp/src/commonMain/kotlin/xyz/ksharma/krail/navigation/di/EntryProviderCollector.kt`

Collects all entry builders from Koin and composes them into a unified entry provider:

```kotlin
@Composable
fun collectEntryProviders(
    navigator: Navigator,
    tripPlannerNavigator: TripPlannerNavigator,
): (NavKey) -> NavEntry<NavKey> {
    // Inject all entry builders from Koin
    val splashEntryBuilder: EntryBuilderDescriptor = koinInject(named("splash"))
    val appUpgradeEntryBuilder: EntryBuilderDescriptor = koinInject(named("appUpgrade"))
    val tripPlannerEntryBuilder: EntryBuilderDescriptor = koinInject(named("tripPlanner"))
    
    return entryProvider {
        // Invoke app-level entry builders
        splashEntryBuilder.builder(this, navigator)
        appUpgradeEntryBuilder.builder(this, navigator)
        
        // Invoke feature-level entry builders
        tripPlannerEntryBuilder.builder(this, tripPlannerNavigator)
    }
}
```

## Benefits

### 1. No Circular Dependencies
Each feature module only depends on its own navigator interface, not the app's Navigator class.

### 2. Modular and Scalable
New features can be added by:
1. Creating a navigation module in the feature
2. Providing entry builders through Koin
3. Registering the module in `initKoin()`
4. Injecting the entry builder in `collectEntryProviders()`

### 3. Type Safety
Each feature receives its specific navigator type, ensuring compile-time safety.

### 4. Testability
Each entry builder can be tested independently by injecting mock navigators.

## Usage Example

### Adding a New Feature Module

1. **Create the feature's navigation module:**

```kotlin
// feature/discover/ui/di/DiscoverNavigationModule.kt
val discoverNavigationModule = module {
    factory<EntryBuilderDescriptor>(qualifier = named("discover")) {
        EntryBuilderDescriptor(
            name = "discover",
            builder = { navigator ->
                // Wrap Navigator in feature-specific implementation if needed
                val commonNavigator = navigator as Navigator
                val discoverNavigator = DiscoverNavigatorImpl(
                    navigator = { route -> commonNavigator.navigate(route) },
                    goBack = { commonNavigator.goBack() }
                )
                discoverEntries(discoverNavigator)
            }
        )
    }
}
```

2. **Register the module in KoinApp:**

```kotlin
// composeApp/di/KoinApp.kt
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        modules(
            // ... existing modules
            discoverNavigationModule,
        )
    }
}
```

3. **Inject and use in EntryProviderCollector:**

```kotlin
@Composable
fun collectEntryProviders(
    navigator: Navigator,
): (NavKey) -> NavEntry<NavKey> {
    val discoverEntryBuilder: EntryBuilderDescriptor = koinInject(named("discover"))
    
    return entryProvider {
        // ... existing builders
        discoverEntryBuilder.builder(this, navigator)
    }
}
```

## Comparison with Dagger Multibinding

### Dagger Approach
```kotlin
@Module
interface NavigationModule {
    @Multibinds
    fun entryBuilders(): Set<EntryBuilder>
}

@Module
class SplashModule {
    @Provides
    @IntoSet
    fun providesSplashEntry(): EntryBuilder = { ... }
}
```

### Koin Approach (This Implementation)
```kotlin
val splashModule = module {
    factory<EntryBuilderDescriptor>(qualifier = named("splash")) {
        EntryBuilderDescriptor(
            name = "splash",
            builder = { navigator -> splashEntry(navigator as Navigator) }
        )
    }
}

// Collection
val splashEntry = koinInject<EntryBuilderDescriptor>(named("splash"))
splashEntry.builder(this, navigator)
```

## Future Improvements

1. **Automatic Collection**: Create a Koin DSL extension to automatically collect all `EntryBuilderDescriptor` instances into a Set, eliminating manual injection in `collectEntryProviders()`.

2. **Type-Safe Navigator Passing**: Use generics to ensure type-safe navigator passing without explicit casting.

3. **Lazy Loading**: Implement lazy loading of entry builders to improve startup performance for large apps.

## Related Files

- `core/navigation/src/commonMain/kotlin/xyz/ksharma/krail/core/navigation/Multibinding.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/krail/navigation/di/AppNavigationModule.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/krail/navigation/di/EntryProviderCollector.kt`
- `feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/di/TripPlannerNavigationModule.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/krail/di/KoinApp.kt`
- `composeApp/src/commonMain/kotlin/xyz/ksharma/krail/KrailNavHost.kt`

