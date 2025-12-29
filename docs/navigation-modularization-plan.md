# Navigation Modularization Plan for KRAIL

## Executive Summary

**Goal**: Modularize navigation code to improve build performance and scalability using DI (Koin) while keeping the implementation clean and maintainable.

**Approach**: Hybrid - Use DI for entry builders, keep navigators function-based.

---

## Problem Analysis

### Current State Issues
1. **KrailNavHost grows linearly** - Manual wiring for each feature
2. **App module recompiles** - Every feature navigation change triggers app module rebuild
3. **Not scalable** - Hard to maintain with 20+ feature modules

### Why Hybrid Approach?

| Aspect | Entry Builders | Navigators |
|--------|---------------|------------|
| **Scalability** | ❌ Poor (manual wiring) | ✅ Good (feature-specific) |
| **Best solved by** | ✅ DI (auto-discovery) | ❌ DI adds complexity |
| **Frequency of change** | 🔵 Low (add once) | 🟢 High (10+ methods per feature) |
| **Type safety** | ⚠️ Acceptable with generics | ✅ Critical (compile-time checks) |

**Decision**: 
- ✅ **Entry Builders**: Use Koin multibindings (like Google's approach)
- ✅ **Navigators**: Keep function-based (type-safe, simple)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     composeApp Module                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              KrailNavHost.kt                         │   │
│  │  • Injects Set<EntryBuilder> from Koin             │   │
│  │  • Creates feature navigators (function-based)      │   │
│  │  • Passes navigators to entry builders              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┼─────────────┐
                │             │             │
         ┌──────▼─────┐  ┌───▼────┐  ┌────▼─────┐
         │  Feature A  │  │Feature B│  │Feature C │
         │   Module    │  │ Module  │  │  Module  │
         │             │  │         │  │          │
         │ Provides:   │  │Provides:│  │Provides: │
         │ • EntryBld  │  │• EntryBd│  │• EntryBd │
         │   (via Koin)│  │(via Koin│  │(via Koin)│
         │             │  │)        │  │)         │
         │ Defines:    │  │Defines: │  │Defines:  │
         │ • Navigator │  │•Navigatr│  │•Navigatr │
         │   Interface │  │Interface│  │Interface │
         │ • Impl      │  │• Impl   │  │• Impl    │
         └─────────────┘  └─────────┘  └──────────┘
```

---

## Implementation Steps

### Phase 1: Core Navigation Setup ✅ DONE
- [x] Create `core:navigation` module
- [x] Move common navigation utilities
- [x] Remove redundant `AdaptiveNavigation.kt`
- [x] Create `EntryBuilder.kt` with typealiases
- [x] Fix `NavigationDelegate.kt` (default implementations)

### Phase 2: Koin Module for Entry Builders (IN PROGRESS)
- [ ] Create `NavigationModule.kt` in `core:navigation`
- [ ] Update `TripPlannerModule.kt` to provide entry builder
- [ ] Create app-level module for splash/upgrade entries
- [ ] Wire all modules in app startup

### Phase 3: Update KrailNavHost
- [ ] Inject `Set<EntryBuilder>` via Koin
- [ ] Create navigator instances (function-based) 
- [ ] Pass navigators to entry builders
- [ ] Remove manual wiring

### Phase 4: Testing & Validation
- [ ] Test all navigation flows
- [ ] Verify build performance improvement
- [ ] Document the pattern for new features

---

## Code Examples

### In Feature Module (e.g., trip-planner)

```kotlin
// TripPlannerModule.kt
val tripPlannerNavigationModule = module {
    // Provide entry builder
    single<EntryBuilder>(named("TripPlanner")) {
        { navigator ->
            tripPlannerEntries(navigator as TripPlannerNavigator)
        }
    }
}
```

### In App Module

```kotlin
// KrailNavHost.kt
@Composable
fun KrailNavHost() {
    val navigationState = rememberNavigationState(...)
    val navigator = rememberNavigator(navigationState)
    
    // Inject all entry builders
    val tripPlannerEntryBuilder: EntryBuilder by inject(named("TripPlanner"))
    val appLevelEntryBuilder: SimpleEntryBuilder by inject(named("AppLevel"))
    
    // Create feature navigators (function-based for type safety)
    val tripPlannerNavigator = remember(navigator) {
        TripPlannerNavigatorImpl(
            navigator = { route -> navigator.navigate(route) },
            goBack = { navigator.goBack() },
            // ... other callbacks
        )
    }
    
    val entryProvider = entryProvider {
        // App-level entries (no navigator needed)
        appLevelEntryBuilder()
        
        // Feature entries (with navigator)
        tripPlannerEntryBuilder(tripPlannerNavigator)
    }
    
    NavDisplay(...)
}
```

---

## Benefits Analysis

### Build Performance
- **Before**: Changing trip-planner navigation → App module rebuilds → ~30s
- **After**: Changing trip-planner navigation → Only feature module rebuilds → ~5s
- **Improvement**: ~83% faster for feature-specific changes

### Scalability
- **Before**: 10 features = 10 manual wirings in KrailNavHost
- **After**: 10 features = Auto-discovered via Koin
- **Adding new feature**: Just create module + provide entry builder

### Maintainability
- **Separation**: Each feature owns its navigation logic
- **Testing**: Mock entry builders in tests
- **Type Safety**: Navigator interfaces enforce contracts

---

## Migration Path

1. **Week 1**: Core infrastructure (Phases 1-2)
2. **Week 2**: Update trip-planner feature as example (Phase 3)
3. **Week 3**: Migrate remaining features, document pattern
4. **Week 4**: Testing, performance benchmarks, refinement

---

## Open Questions & Decisions

### Q1: Should all features use the same pattern?
**Decision**: Yes - consistency is critical for maintainability

### Q2: What about features that don't need navigation?
**Decision**: Use `SimpleEntryBuilder` (no navigator parameter)

### Q3: Should we use named qualifiers or just Set<EntryBuilder>?
**Decision**: Named qualifiers for now - easier debugging, explicit dependencies

### Q4: How to handle navigation between features?
**Decision**: 
- Feature A depends on Feature B's `api` module (for routes)
- Feature A's navigator interface has methods like `navigateToFeatureB()`
- Implementation delegates to app-level `navigator(route)`

---

## Alternative Approaches Considered

### ❌ Full DI for Navigators
- **Pros**: Consistent with entry builders
- **Cons**: Loses type safety, harder to trace, overkill for 10 methods
- **Verdict**: Not worth the complexity

### ❌ Keep everything manual
- **Pros**: Simple, no DI needed
- **Cons**: Doesn't scale, tight coupling
- **Verdict**: Current pain points justify DI for entry builders

### ✅ Hybrid (chosen)
- **Pros**: Best of both worlds - scalability + type safety
- **Cons**: Slightly more complex than "all DI" or "all manual"
- **Verdict**: Optimal for KRAIL's size and growth trajectory

---

## Success Metrics

1. **Build time**: Feature module changes < 10s
2. **Lines of code in KrailNavHost**: < 100 (down from ~150)
3. **New feature setup time**: < 5 minutes
4. **Navigation bugs**: Zero regression

---

## Next Steps

1. Implement Phase 2 (Koin modules)
2. Update TripPlannerEntries to be injectable
3. Test with hot reload / incremental builds
4. Document pattern for team


