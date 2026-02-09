# Map Module Architecture - Visual Guide

## Current vs Proposed Structure

### Current Structure ✅
```
core/maps/
├── state/                    # ✅ Pure Kotlin models
│   ├── MapCoordinates.kt     # LatLng, BoundingBox, CameraFocus
│   └── GeoJsonProperties.kt  # Property builders & constants
│
└── ui/                       # ✅ Utilities & config
    ├── config/MapConfig.kt   # Tile providers, defaults
    └── utils/MapCameraUtils.kt # Camera calculations
```

**What's Missing**: Reusable composables, interaction handlers, analytics, testing

---

### Proposed Structure 🆕

```
core/maps/
├── state/                           # ✅ EXISTS - Keep as-is
│   ├── MapCoordinates.kt
│   └── GeoJsonProperties.kt
│
├── ui/                              # ✅ EXISTS - Keep as-is
│   ├── config/MapConfig.kt
│   └── utils/MapCameraUtils.kt
│
├── composables/                     # 🆕 NEW MODULE
│   ├── MapContainer.kt              # Base wrapper with error handling
│   ├── MapErrorView.kt              # Error display with retry
│   ├── MapLoadingState.kt           # Loading overlay
│   └── MapControls.kt               # Zoom, location buttons
│
├── layers/                          # 🆕 NEW MODULE
│   ├── LayerFactory.kt              # Factory for creating layers
│   ├── TransitLineLayer.kt          # Reusable transit line config
│   ├── WalkingPathLayer.kt          # Reusable walking path config
│   └── StopMarkerLayer.kt           # Reusable stop marker config
│
├── camera/                          # 🆕 NEW MODULE
│   ├── CameraAnimator.kt            # Smooth camera animations
│   ├── CameraPresets.kt             # Common positions (Sydney, etc.)
│   └── CameraBoundsCalculator.kt    # Advanced bounds logic
│
├── interactions/                    # 🆕 NEW MODULE
│   ├── MapClickHandler.kt           # Click event handling
│   ├── MapGestureHandler.kt         # Pinch, pan, rotate
│   └── MapSelectionManager.kt       # Selection state management
│
├── accessibility/                   # 🆕 NEW MODULE
│   └── MapAccessibility.kt          # A11y helpers & descriptions
│
├── analytics/                       # 🆕 NEW MODULE
│   ├── MapAnalytics.kt              # Analytics interface
│   └── MapAnalyticsImpl.kt          # Analytics implementation
│
└── testing/                         # 🆕 NEW MODULE
    ├── MapStateBuilder.kt           # Mock state builders
    ├── GeoJsonFixtures.kt           # Test GeoJSON data
    └── MapTestUtils.kt              # Testing helpers
```

---

## Data Flow (Current Implementation)

```
┌──────────────────────────────────────────────────────────────┐
│                    API Response                               │
│              (TripResponse with coords)                       │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                 JourneyMapMapper                              │
│  • Extract coordinates from legs                             │
│  • Determine colors from transport mode                      │
│  • Create JourneyLegFeature & JourneyStopFeature             │
│  • Calculate camera bounds                                   │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│             JourneyMapUiState.Ready                           │
│  • mapDisplay: JourneyMapDisplay                             │
│  • cameraFocus: CameraFocus?                                 │
│  • (Pure Kotlin - no MapLibre types)                         │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│            JourneyMapFeatureMapper                            │
│  • Convert state → GeoJSON FeatureCollection                 │
│  • Create LineString for routes                              │
│  • Create Point for stops                                    │
│  • Add properties for filtering                              │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                  JourneyMap Composable                        │
│  • rememberGeoJsonSource(featureCollection)                  │
│  • LineLayer (walking & transit)                             │
│  • CircleLayer (stops)                                       │
│  • SymbolLayer (labels)                                      │
└──────────────────────────────────────────────────────────────┘
```

---

## Proposed Improvements - Layer by Layer

### 1. Composables Module Architecture

```
MapContainer (New base wrapper)
    │
    ├─> MapErrorView (if error exists)
    │   ├─> Error Icon
    │   ├─> Error Message
    │   └─> Retry Button
    │
    └─> MapLibreMap (if no error)
        ├─> MapLoadingState (overlay when loading)
        ├─> Feature Layers (from child)
        └─> MapControls (overlay)
            ├─> Zoom In Button
            ├─> Zoom Out Button
            └─> Location Button
```

**Usage**:
```kotlin
@Composable
fun JourneyMap(...) {
    MapContainer(
        cameraPosition = calculatePosition(),
        onMapError = { error -> /* handle */ },
    ) {
        // Add your layers here
        LineLayer(...)
        CircleLayer(...)
    }
}
```

---

### 2. Layer Factory Pattern

**Before** (Duplicated code):
```kotlin
// In JourneyMap.kt
LineLayer(
    id = "walking-lines",
    source = source,
    color = const(Color(0xFF757575)),
    width = const(4.dp),
    dasharray = const(listOf(2f, 2f)),
    cap = const(LineCap.Round),
    join = const(LineJoin.Round),
)

// In SearchStopMap.kt (duplicated!)
LineLayer(
    id = "walking-routes",
    source = source,
    color = const(Color(0xFF757575)),
    width = const(4.dp),
    dasharray = const(listOf(2f, 2f)),
    cap = const(LineCap.Round),
    join = const(LineJoin.Round),
)
```

**After** (Reusable factory):
```kotlin
// In both files:
LayerFactory.createWalkingPathLayer(
    id = "walking-lines",
    source = source,
    filter = isWalkingFilter,
)
```

**Benefits**:
- Single source of truth for styling
- Easy to update globally
- Consistent look and feel
- Less code duplication

---

### 3. Camera Animation Flow

**Current** (Instant jump):
```kotlin
val cameraState = rememberCameraState(firstPosition = position)
// Camera instantly jumps to position - jarring UX
```

**Proposed** (Smooth animation):
```kotlin
val cameraAnimator = remember(cameraState) { 
    CameraAnimator(cameraState) 
}

LaunchedEffect(mapState.cameraFocus) {
    mapState.cameraFocus?.let { focus ->
        cameraAnimator.animateToBounds(
            bounds = focus.bounds,
            duration = 1000, // 1 second smooth animation
            padding = focus.padding
        )
    }
}
```

**User Experience**:
- Smooth transitions
- Clear visual feedback
- Professional feel
- Better spatial awareness

---

### 4. Analytics Integration

```
User Action → MapInteraction → Analytics Event
    │              │                  │
    │              │                  ├─> Firebase Analytics
    │              │                  ├─> Crashlytics
    │              │                  └─> Custom logging
    │              │
    │              └─> Track:
    │                   • Click coordinates
    │                   • Selected feature
    │                   • Zoom level
    │                   • Duration
    │
    └─> Examples:
         • User clicks stop → "stop_selected"
         • User zooms map → "map_zoomed"
         • Error occurs → "map_error"
```

**Implementation**:
```kotlin
@Composable
fun JourneyMap(..., analytics: MapAnalytics) {
    DisposableEffect(Unit) {
        val startTime = System.currentTimeMillis()
        analytics.trackMapView("journey_map")
        
        onDispose {
            val duration = System.currentTimeMillis() - startTime
            analytics.trackMapPerformance("view_duration", duration)
        }
    }
}
```

---

### 5. Testing Architecture

```
Test Type          | Location                    | What to Test
-------------------|-----------------------------|--------------------------
Unit Tests         | */commonTest/               | • Mappers
                   |                             | • State transformations
                   |                             | • Camera calculations
                   |                             | • Filter logic
                   |                             |
Integration Tests  | */commonTest/               | • GeoJSON generation
                   |                             | • End-to-end mapping
                   |                             | • State flow
                   |                             |
UI Tests           | */androidTest/              | • Composable rendering
                   | */iosTest/                  | • Layer visibility
                   |                             | • Interactions
                   |                             |
Screenshot Tests   | core/maps/testing/          | • Visual regression
                   |                             | • Cross-platform parity
```

**Test Utilities** (`core/maps/testing/`):
```kotlin
// MapStateBuilder.kt
fun buildTestJourneyMapState(
    legCount: Int = 2,
    stopsPerLeg: Int = 3,
    hasWalking: Boolean = true,
): JourneyMapUiState.Ready { ... }

// GeoJsonFixtures.kt
object GeoJsonFixtures {
    val transitLine = Feature(...)
    val walkingPath = Feature(...)
    val originStop = Feature(...)
}
```

---

## Migration Path

### Phase 1: Quick Wins (Week 1)
1. ✅ Fix font loading
2. Add `MapErrorView` component
3. Use `MapConfig` everywhere
4. Add basic analytics events

### Phase 2: Core Infrastructure (Week 2-3)
1. Create `core/maps/composables/`
2. Create `core/maps/layers/`
3. Add camera animations
4. Write unit tests

### Phase 3: Advanced Features (Week 4-5)
1. Create `core/maps/interactions/`
2. Add accessibility support
3. Create testing utilities
4. Performance optimizations

### Phase 4: Polish (Week 6)
1. Documentation updates
2. Code review and refactoring
3. Integration testing
4. Final QA

---

## Code Quality Checklist

Before considering maps "production ready":

- [ ] Error states show meaningful UI (not loading spinner)
- [ ] All interactive elements have content descriptions
- [ ] Analytics track views, interactions, errors
- [ ] Unit tests cover mappers and utilities (>80%)
- [ ] Integration tests verify GeoJSON generation
- [ ] Camera animations are smooth
- [ ] Layer styling is consistent via factory
- [ ] No hardcoded values (use constants)
- [ ] No memory leaks (proper resource cleanup)
- [ ] Performance tested with large journeys
- [ ] Cross-platform tested (Android + iOS)
- [ ] Accessibility tested with screen readers

---

## Anti-Patterns to Avoid

❌ **DON'T**: Put MapLibre types in state layer
```kotlin
// BAD - MapLibre dependency in state
data class MapState(
    val position: Position, // ❌ MapLibre type
    val features: FeatureCollection // ❌ MapLibre type
)
```

✅ **DO**: Use pure Kotlin models
```kotlin
// GOOD - Pure Kotlin
data class MapState(
    val position: LatLng, // ✅ Our type
    val legs: List<JourneyLegFeature> // ✅ Our type
)
```

---

❌ **DON'T**: Hardcode styling in composables
```kotlin
// BAD - Hardcoded values
LineLayer(
    color = const(Color(0xFF757575)), // ❌ Magic number
    width = const(4.dp), // ❌ No constant
)
```

✅ **DO**: Use factories and constants
```kotlin
// GOOD - Reusable and configurable
LayerFactory.createWalkingPathLayer(
    id = "walking",
    source = source,
)
```

---

❌ **DON'T**: Ignore errors
```kotlin
// BAD - Silent failure
is MapState.Error -> {
    // Empty - user sees nothing ❌
}
```

✅ **DO**: Show meaningful errors
```kotlin
// GOOD - User feedback
is MapState.Error -> {
    MapErrorView(
        error = state.error,
        onRetry = { reload() }
    )
}
```

---

## Resources

- Full Architecture Doc: `map-architecture.md`
- Improvement Roadmap: `IMPROVEMENTS.md`
- MapLibre Docs: https://github.com/Rallista/maplibre-compose
- GeoJSON Spec: https://geojson.org

---

**Created**: February 8, 2026
**Purpose**: Visual guide for map architecture improvements
**Audience**: Development team, code reviewers
