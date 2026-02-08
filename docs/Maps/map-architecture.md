# KRAIL Map Architecture

## Overview

KRAIL uses MapLibre for map visualizations across the application. This document describes the technical architecture, design principles, and implementation patterns for map features.

## Technology Stack

- **Map Library**: MapLibre (Kotlin Multiplatform)
- **Tile Provider**: OpenFreeMap (https://tiles.openfreemap.org)
- **Map Style**: Liberty (default), with support for Positron and Dark Matter
- **GeoJSON**: For feature rendering (routes, stops, markers)

## Core Principles

### 1. Platform-Agnostic State
All map state models are **pure Kotlin** with no MapLibre dependencies. This ensures:
- Cross-platform compatibility (Android, iOS)
- Easy testing and mocking
- Clear separation of concerns
- State can be shared across features

### 2. Layered Architecture
Map features follow a strict layered architecture:

```
┌─────────────────────────────────────────┐
│  API/Network Layer                      │
│  (TripResponse, StopResults, etc.)      │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  Business/Mapper Layer                  │
│  (State transformation logic)           │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  State Layer (Pure Kotlin)              │
│  - LatLng, BoundingBox, CameraFocus     │
│  - Feature models (stops, routes, legs) │
│  - No MapLibre types                    │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  GeoJSON Mapper Layer                   │
│  (Convert state → GeoJSON features)     │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  UI/Composable Layer                    │
│  (MapLibre rendering, layers, styling)  │
└─────────────────────────────────────────┘
```

### 3. Reusable Core Components
Shared map infrastructure located in `core/maps/`:

**State Models** (`core/maps/state/`):
- `LatLng`: Coordinate representation
- `BoundingBox`: Camera bounds (southwest, northeast)
- `CameraFocus`: Auto-focus configuration with padding
- `GeoJsonPropertyKeys`: Standardized GeoJSON property names

**UI Utilities** (`core/maps/ui/`):
- `MapConfig`: Central configuration (tile providers, defaults, zoom levels)
- `MapCameraUtils`: Camera calculations (zoom levels, center points, bounds)

## Feature-Specific Implementations

### 1. Journey Map Visualization

**Location**: `feature/trip-planner/`

**Purpose**: Display transit journeys on a map showing:
- Transit routes (train, bus, ferry, etc.) as colored lines
- Walking segments as dashed gray lines
- Stops as circular markers (color-coded by type)
- Stop labels with icons

#### State Models (`state/journeymap/`)

```kotlin
sealed class JourneyMapUiState {
    object Loading
    data class Ready(
        val mapDisplay: JourneyMapDisplay,
        val cameraFocus: CameraFocus?
    )
    data class Error(val message: String)
}

data class JourneyMapDisplay(
    val legs: List<JourneyLegFeature>,
    val stops: List<JourneyStopFeature>,
    val selectedLeg: JourneyLegFeature?
)

data class JourneyLegFeature(
    val legId: String,
    val transportMode: TransportMode?,
    val lineName: String?, // "T1", "333", etc.
    val lineColor: String, // Hex color
    val routeSegment: RouteSegment
)

sealed class RouteSegment {
    // For walking paths with explicit coordinates
    data class PathSegment(val points: List<LatLng>)
    
    // For transit routes connecting stops
    data class StopConnectorSegment(val stops: List<JourneyStopFeature>)
}

data class JourneyStopFeature(
    val stopId: String,
    val stopName: String,
    val position: LatLng?,
    val stopType: StopType, // ORIGIN, DESTINATION, INTERCHANGE, REGULAR
    val time: String?,
    val platform: String?
)
```

#### Business Logic (`ui/journeymap/business/`)

**JourneyMapMapper**: 
- Transforms `TripResponse.Journey` → `JourneyMapUiState.Ready`
- Extracts coordinates from API response
- Determines transport mode colors
- Calculates camera bounds to fit entire journey

**JourneyMapFeatureMapper**:
- Converts state models → GeoJSON `FeatureCollection`
- Creates `LineString` features for routes
- Creates `Point` features for stops
- Adds properties for MapLibre filtering/styling

**JourneyMapFilters**:
- Reusable filter helper functions
- Eliminates hardcoded filter expressions
- Examples:
  - `isJourneyLeg()`: Filter for route lines
  - `isStopType(StopType.ORIGIN)`: Filter for origin stop
  - `isStopType(StopType.DESTINATION, StopType.INTERCHANGE)`: Multiple types

#### UI Implementation (`ui/journeymap/`)

**JourneyMap.kt** - Main composable with:
- **Line Layers**:
  - Walking: Dashed gray lines (`dasharray = [2, 2]`)
  - Transit: Solid colored lines (color from GeoJSON properties)
- **Circle Layers**:
  - Origin: Green marker (12dp radius)
  - Destination: Red marker (12dp radius)
  - Interchange: Yellow marker (10dp radius)
  - Regular: White marker (6dp radius)
- **Symbol Layers**:
  - Stop labels with location icons
  - Origin label (larger text)
  - Destination/Interchange labels

**Key Features**:
- Auto-focus camera on journey start (origin stop)
- Dynamic zoom level based on journey bounds
- Color-coded routes by transport mode
- Responsive to state changes

### 2. Stop Search Map

**Location**: `feature/trip-planner/searchstop/`

**Purpose**: Display search results for transit stops on a map

Uses similar architecture patterns:
- `MapUiState` in state layer
- `StopResultsMapMapper` for GeoJSON conversion
- `SearchStopMap` composable for rendering

## Design Patterns

### 1. Coordinate Transformation

**CRITICAL**: API and MapLibre use different coordinate formats!

```kotlin
// API Format: [latitude, longitude]
"coord": [-33.774221, 150.935976]

// Extract from API
val lat = apiCoord[0]  // -33.774221
val lng = apiCoord[1]  // 150.935976

// MapLibre Format: Position(longitude, latitude) - REVERSED!
Position(
    longitude = lng,  // 150.935976 (second → first!)
    latitude = lat    // -33.774221 (first → second!)
)
```

Always use helper methods in mappers to handle this transformation correctly.

### 2. Transport Mode Colors

Centralized in `TransportMode` enum:

```kotlin
1  = Train       = #F99D1C (Orange)
2  = Metro       = #009B77 (Green)
4  = Light Rail  = #EE3124 (Red)
5  = Bus         = #00B9E4 (Blue)
7  = Coach       = #793896 (Purple)
9  = Ferry       = #5BBE4B (Green)
99 = Walking     = #757575 (Gray)
```

### 3. GeoJSON Properties

Standardized property keys in `GeoJsonPropertyKeys`:

```kotlin
object GeoJsonPropertyKeys {
    const val TYPE = "type"
    const val COLOR = "color"
    const val IS_WALKING = "isWalking"
    const val STOP_TYPE = "stopType"
    const val STOP_NAME = "stopName"
    const val LINE_NAME = "lineName"
    // ... etc.
}
```

### 4. Filter Helpers

Reusable filter functions eliminate repetitive code:

```kotlin
// Before (verbose, error-prone)
filter = (get("type").asString() eq const("journey_leg")) and
         (get("isWalking").asBoolean() eq const(true))

// After (clean, reusable)
filter = JourneyMapFilters.isJourneyLeg() and
         (get(GeoJsonPropertyKeys.IS_WALKING).asBoolean() eq const(true))
```

## Scalability

### Adding New Map Features

1. **Create State Models** in `feature/*/state/`:
   - Define pure Kotlin data classes
   - Use existing `LatLng`, `BoundingBox`, `CameraFocus` from core
   - Follow sealed class pattern for UI state

2. **Create Mapper** in `feature/*/ui/*/business/`:
   - Transform domain/network models → state models
   - Calculate camera bounds if needed
   - Handle coordinate conversions

3. **Create GeoJSON Mapper**:
   - Convert state → `FeatureCollection`
   - Add properties for filtering/styling
   - Use `GeoJsonPropertyKeys` constants

4. **Create Composable**:
   - Use `MaplibreMap` composable
   - Define layers (Line, Circle, Symbol, etc.)
   - Use filter helpers for clean code
   - Follow styling conventions

### Extending Existing Features

**To add new layer types**:
- Create new filter helper in `*Filters` object
- Add layer definition in composable
- Update GeoJSON mapper to include necessary properties

**To add interactions**:
- Use MapLibre's click handlers
- Update state with selection
- Re-filter/re-style layers based on selection state

**To add animations**:
- Use Compose's animation APIs
- Animate camera position changes
- Animate layer property changes

## Configuration

### Map Tile Providers

Configured in `MapConfig.kt`:
- Default: OpenFreeMap Liberty
- Light theme: OpenFreeMap Positron
- Dark theme: OpenFreeMap Dark Matter

### Default Settings

**Camera Position** (Sydney):
- Latitude: -33.8727
- Longitude: 151.2057
- Zoom: 12.0

**Ornaments**:
- Logo: Disabled
- Attribution: Enabled (bottom-right)
- Compass: Enabled (top-right)
- Scale bar: Disabled

**Zoom Levels**:
- Large area (>100km): 9.0
- City area (~10-50km): 12.0
- Neighborhood (~2-5km): 14.0
- Small area (<2km): 15.0

## Testing Strategy

### Unit Tests
- Test mappers with sample data
- Verify coordinate transformations
- Test camera calculation logic
- Test filter helpers

### Integration Tests
- Test state flow through layers
- Verify GeoJSON structure
- Test layer filtering logic

### UI Tests
- Snapshot tests for map rendering
- Interaction tests (clicks, selections)
- Camera position tests

## Performance Considerations

### 1. GeoJSON Optimization
- Use `remember` to cache feature collections
- Only regenerate when state changes
- Avoid creating unnecessary features

### 2. Layer Management
- Use filters instead of creating separate sources
- Minimize number of layers
- Use appropriate min/max zoom levels

### 3. State Updates
- Keep state immutable
- Use structural sharing (Kotlin collections)
- Debounce rapid camera changes

### 4. Memory Management
- Clean up resources when composables leave composition
- Use appropriate lifecycle scoping
- Avoid retaining large coordinate arrays

## Common Pitfalls

1. **Coordinate Order**: Always remember API uses `[lat, lng]`, MapLibre uses `Position(lng, lat)`
2. **Hardcoded Values**: Use constants from `GeoJsonPropertyKeys`, `MapConfig`, etc.
3. **MapLibre in State**: Never put MapLibre types in state layer
4. **Filter Complexity**: Use helper functions instead of inline filter expressions
5. **Camera Positioning**: Calculate bounds to ensure all features are visible

## Future Enhancements

- **Clustering**: Add marker clustering for dense stop displays
- **Real-time Updates**: Integrate live vehicle positions
- **Offline Maps**: Cache tiles for offline use
- **Custom Styles**: Brand-specific map styling
- **Accessibility**: Improve screen reader support for map features
- **3D Visualization**: Add elevation/building layers for better context

## Related Documentation

- MapLibre Compose: https://github.com/Rallista/maplibre-compose
- OpenFreeMap: https://openfreemap.org
- GeoJSON Specification: https://geojson.org
- Transport NSW API: https://opendata.transport.nsw.gov.au

## Architectural Review & Improvements

### Current Strengths ✅

1. **Clean Separation of Concerns**: Pure Kotlin state layer with no MapLibre dependencies
2. **Reusable Core Infrastructure**: Shared `LatLng`, `BoundingBox`, `CameraFocus` models
3. **Type-Safe GeoJSON Builders**: `GeoJsonPropertiesBuilder` eliminates hardcoded strings
4. **Filter Helpers**: `JourneyMapFilters` reduces complexity and improves maintainability
5. **Modular Structure**: Split between `core/maps/state` and `core/maps/ui`

### Production Readiness Issues ⚠️

#### 1. **Missing Error Handling** (HIGH PRIORITY)

**Problem**: Error states render a loading spinner instead of meaningful UI
```kotlin
is JourneyMapUiState.Error -> {
    Box(modifier = modifier.fillMaxSize()) {
        // TODO: Add error UI component ⚠️
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}
```

**Impact**: Users can't tell if something failed vs still loading

**Solution**: Create proper error UI components
- Show error message
- Provide retry action
- Offer fallback options

#### 2. **Hardcoded Font in UI** (MEDIUM PRIORITY)

**Problem**: Using "Noto Sans Regular" which causes 404 errors
```kotlin
textFont = const(listOf("Noto Sans Regular"))
```

**Impact**: Font loading failures, console errors

**Solution**: ✅ FIXED - Now uses `LayerDefaults.FontNames` from maplibre-compose

#### 3. **No Loading States for Camera Operations** (MEDIUM PRIORITY)

**Problem**: Camera position calculated synchronously, could lag on large journeys
```kotlin
val cameraPosition = remember(mapState.cameraFocus, mapState.mapDisplay.stops) {
    // Complex calculations happen on composition thread
}
```

**Impact**: UI thread blocking on heavy computations

**Solution**: Move camera calculations to background coroutine

#### 4. **Missing Accessibility** (HIGH PRIORITY for PROD)

**Problem**: No content descriptions, screen reader support, or keyboard navigation

**Impact**: App unusable for users with disabilities

**Solution**: 
- Add `contentDescription` to all interactive elements
- Provide text alternatives for visual information
- Support TalkBack/VoiceOver

#### 5. **No Analytics/Telemetry** (MEDIUM PRIORITY)

**Problem**: Can't track map feature usage, performance, or errors

**Impact**: No visibility into production issues or user behavior

**Solution**: Add analytics events:
- Map view events
- Interaction tracking (zoom, pan, stop clicks)
- Error tracking
- Performance metrics (render time, feature count)

#### 6. **Insufficient Testing Infrastructure** (HIGH PRIORITY)

**Problem**: No tests exist for mappers, filters, or camera calculations

**Impact**: Regressions can slip into production

**Solution**: Add comprehensive test suite (see Testing Improvements section)

### Proposed Module Improvements

#### Should More Things Be in `core/maps`?

**YES** - The following should be extracted:

1. **`core/maps/composables`** - Reusable map composables
   - `MapContainer`: Base map wrapper with error handling
   - `MapLoadingState`: Loading indicator overlay
   - `MapErrorView`: Error display with retry
   - `MapControls`: Zoom buttons, location button, etc.

2. **`core/maps/interactions`** - Interaction handling
   - `MapClickHandler`: Centralized click handling
   - `MapGestureHandler`: Pinch, pan, rotate
   - `MapSelectionManager`: Selection state management

3. **`core/maps/layers`** - Reusable layer definitions
   - `TransitLineLayer`: Configurable transit line rendering
   - `StopMarkerLayer`: Configurable stop markers
   - `WalkingPathLayer`: Dashed walking paths
   - Allows consistent styling across features

4. **`core/maps/camera`** - Advanced camera operations
   - `CameraAnimator`: Smooth camera transitions
   - `CameraBoundsCalculator`: More sophisticated bounds logic
   - `CameraPresets`: Common camera positions (Sydney, Melbourne, etc.)

5. **`core/maps/offline`** - Offline support (future)
   - Tile caching
   - Offline route rendering
   - Sync management

#### Should There Be Additional Modules?

**YES** - Consider these new modules:

1. **`core/maps/analytics`** - Map-specific analytics
   ```kotlin
   interface MapAnalytics {
       fun trackMapView(featureType: String)
       fun trackMapInteraction(action: String, properties: Map<String, Any>)
       fun trackMapError(error: Throwable)
       fun trackMapPerformance(metric: String, value: Long)
   }
   ```

2. **`core/maps/accessibility`** - Accessibility utilities
   ```kotlin
   object MapAccessibility {
       fun routeDescription(legs: List<JourneyLegFeature>): String
       fun stopAnnouncement(stop: JourneyStopFeature): String
       fun mapRegionDescription(bounds: BoundingBox): String
   }
   ```

3. **`core/maps/testing`** - Testing utilities
   - Mock map state builders
   - GeoJSON test fixtures
   - Screenshot testing helpers
   - Performance benchmarking tools

### Specific Code Improvements

#### 1. Extract Map Container Component

**Create**: `core/maps/composables/MapContainer.kt`
```kotlin
@Composable
fun MapContainer(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition,
    mapOptions: MapOptions = MapOptions(),
    onMapReady: () -> Unit = {},
    onMapError: (Throwable) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var mapError by remember { mutableStateOf<Throwable?>(null) }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (mapError != null) {
            MapErrorView(
                error = mapError!!,
                onRetry = { mapError = null }
            )
        } else {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = rememberCameraState(firstPosition = cameraPosition),
                baseStyle = BaseStyle.Uri(MapTileProvider.DEFAULT),
                options = mapOptions,
                content = content,
            )
        }
    }
}
```

**Benefit**: Centralized error handling, consistent map setup

#### 2. Add Camera Animation Support

**Create**: `core/maps/camera/CameraAnimator.kt`
```kotlin
class CameraAnimator(private val cameraState: CameraState) {
    suspend fun animateToBounds(
        bounds: BoundingBox,
        duration: Int = 1000,
        padding: Int = 50,
    ) {
        val center = MapCameraUtils.calculateCenter(bounds)
        val zoom = MapCameraUtils.calculateZoomLevel(bounds)
        
        cameraState.animateTo(
            target = Position(center.longitude, center.latitude),
            zoom = zoom,
            duration = duration
        )
    }
    
    suspend fun animateToPosition(
        position: LatLng,
        zoom: Double? = null,
        duration: Int = 500,
    ) {
        cameraState.animateTo(
            target = Position(position.longitude, position.latitude),
            zoom = zoom ?: cameraState.position.zoom,
            duration = duration
        )
    }
}
```

**Usage in JourneyMap**:
```kotlin
val animator = remember(cameraState) { CameraAnimator(cameraState) }

LaunchedEffect(mapState.cameraFocus) {
    mapState.cameraFocus?.let { focus ->
        animator.animateToBounds(focus.bounds, padding = focus.padding)
    }
}
```

**Benefit**: Smooth transitions, better UX

#### 3. Create Reusable Layer Factory

**Create**: `core/maps/layers/LayerFactory.kt`
```kotlin
object LayerFactory {
    fun createTransitLineLayer(
        id: String,
        source: Source,
        filter: Expression<BooleanValue>? = null,
        colorExpression: Expression<ColorValue>,
        width: Dp = 6.dp,
    ): LineLayer = LineLayer(
        id = id,
        source = source,
        filter = filter,
        color = colorExpression,
        width = const(width),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )
    
    fun createWalkingPathLayer(
        id: String,
        source: Source,
        filter: Expression<BooleanValue>? = null,
    ): LineLayer = LineLayer(
        id = id,
        source = source,
        filter = filter,
        color = const(Color(0xFF757575)),
        width = const(4.dp),
        dasharray = const(listOf(2f, 2f)),
        cap = const(LineCap.Round),
        join = const(LineJoin.Round),
    )
    
    fun createStopMarkerLayer(
        id: String,
        source: Source,
        filter: Expression<BooleanValue>? = null,
        colorExpression: Expression<ColorValue>,
        radius: Dp = 8.dp,
    ): CircleLayer = CircleLayer(
        id = id,
        source = source,
        filter = filter,
        color = colorExpression,
        radius = const(radius),
        strokeColor = const(Color.Black),
        strokeWidth = const(2.dp),
    )
}
```

**Benefit**: Consistent styling, reduced duplication, easier to update

#### 4. Add Map Analytics

**Create**: `core/maps/analytics/MapAnalyticsImpl.kt`
```kotlin
class MapAnalyticsImpl(private val analytics: Analytics) : MapAnalytics {
    override fun trackMapView(featureType: String) {
        analytics.trackEvent("map_view", mapOf(
            "feature_type" to featureType,
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    override fun trackMapInteraction(action: String, properties: Map<String, Any>) {
        analytics.trackEvent("map_interaction", properties + mapOf(
            "action" to action,
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    override fun trackMapError(error: Throwable) {
        analytics.trackError(error, mapOf(
            "error_type" to "map_error",
            "error_message" to (error.message ?: "Unknown")
        ))
    }
    
    override fun trackMapPerformance(metric: String, value: Long) {
        analytics.trackEvent("map_performance", mapOf(
            "metric" to metric,
            "value" to value,
            "timestamp" to System.currentTimeMillis()
        ))
    }
}
```

**Usage**:
```kotlin
@Composable
fun JourneyMap(..., mapAnalytics: MapAnalytics) {
    DisposableEffect(Unit) {
        mapAnalytics.trackMapView("journey_map")
        onDispose { }
    }
    
    // Track interactions
    LaunchedEffect(selectedStop) {
        selectedStop?.let {
            mapAnalytics.trackMapInteraction("stop_selected", mapOf(
                "stop_id" to it.stopId,
                "stop_type" to it.stopType.name
            ))
        }
    }
}
```

#### 5. Improve Error Handling

**Create**: `core/maps/composables/MapErrorView.kt`
```kotlin
@Composable
fun MapErrorView(
    error: Throwable,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_error),
            contentDescription = "Error",
            tint = KrailTheme.colors.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Unable to load map",
            style = KrailTheme.typography.titleMedium,
            color = KrailTheme.colors.onSurface,
        )
        
        Text(
            text = error.message ?: "Unknown error occurred",
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            dimensions = ButtonDefaults.mediumButtonSize(),
        ) {
            Text("Retry")
        }
    }
}
```

**Update JourneyMap.kt**:
```kotlin
is JourneyMapUiState.Error -> {
    MapErrorView(
        error = Throwable(journeyMapState.message),
        onRetry = { /* Handle retry */ },
        modifier = modifier,
    )
}
```

#### 6. Add Accessibility Support

**Create**: `core/maps/accessibility/MapAccessibility.kt`
```kotlin
object MapAccessibility {
    fun routeDescription(legs: List<JourneyLegFeature>): String {
        return legs.joinToString(separator = ", then ") { leg ->
            when {
                leg.transportMode == null -> "walk for ${leg.routeSegment.estimatedDuration()}"
                else -> "take ${leg.lineName ?: leg.transportMode.name} ${leg.transportMode.productName}"
            }
        }
    }
    
    fun stopAnnouncement(stop: JourneyStopFeature): String {
        val type = when (stop.stopType) {
            StopType.ORIGIN -> "Starting point"
            StopType.DESTINATION -> "Destination"
            StopType.INTERCHANGE -> "Transfer point"
            StopType.REGULAR -> "Stop"
        }
        return "$type: ${stop.stopName}${stop.platform?.let { ", Platform $it" } ?: ""}"
    }
    
    fun mapRegionDescription(bounds: BoundingBox): String {
        val size = MapCameraUtils.calculateZoomLevel(bounds)
        val area = when {
            size <= MapConfig.ZoomLevels.LARGE_AREA -> "large area"
            size <= MapConfig.ZoomLevels.CITY_AREA -> "city-wide"
            size <= MapConfig.ZoomLevels.SUBURB_AREA -> "local area"
            else -> "small area"
        }
        return "Map showing $area with journey route"
    }
}
```

**Usage in JourneyMap**:
```kotlin
Box(modifier = modifier.fillMaxSize().semantics {
    contentDescription = MapAccessibility.mapRegionDescription(
        mapState.cameraFocus?.bounds ?: BoundingBox(...)
    )
    heading() // Mark as heading for screen readers
}) {
    MaplibreMap(...) {
        // Layers with accessibility
    }
}
```

### Testing Improvements

#### 1. Unit Tests for Mappers

**Create**: `feature/trip-planner/ui/src/commonTest/kotlin/JourneyMapMapperTest.kt`
```kotlin
class JourneyMapMapperTest {
    @Test
    fun `toJourneyMapState extracts coordinates correctly`() {
        val journey = TripResponse.Journey(
            legs = listOf(
                TripResponse.Leg(
                    origin = TripResponse.StopSequence(
                        coord = listOf(-33.774221, 150.935976),
                        name = "Seven Hills"
                    ),
                    // ...
                )
            )
        )
        
        val result = with(JourneyMapMapper) { journey.toJourneyMapState() }
        
        assertTrue(result is JourneyMapUiState.Ready)
        assertEquals(1, result.mapDisplay.legs.size)
        // More assertions...
    }
    
    @Test
    fun `calculateBounds returns null for empty coordinates`() {
        val bounds = JourneyMapMapper.calculateBounds(emptyList())
        assertNull(bounds)
    }
    
    @Test
    fun `coordinate transformation is correct`() {
        // API: [lat, lng] = [-33.77, 150.93]
        val apiCoord = listOf(-33.77, 150.93)
        val latLng = LatLng(latitude = apiCoord[0], longitude = apiCoord[1])
        
        // MapLibre: Position(lng, lat)
        val position = Position(longitude = latLng.longitude, latitude = latLng.latitude)
        
        assertEquals(150.93, position.longitude)
        assertEquals(-33.77, position.latitude)
    }
}
```

#### 2. Integration Tests for GeoJSON Conversion

**Create**: `feature/trip-planner/ui/src/commonTest/kotlin/JourneyMapFeatureMapperTest.kt`
```kotlin
class JourneyMapFeatureMapperTest {
    @Test
    fun `toFeatureCollection creates valid GeoJSON`() {
        val state = JourneyMapUiState.Ready(
            mapDisplay = JourneyMapDisplay(
                legs = listOf(/* test legs */),
                stops = listOf(/* test stops */)
            )
        )
        
        val result = with(JourneyMapFeatureMapper) { state.toFeatureCollection() }
        
        assertTrue(result.features.isNotEmpty())
        // Verify GeoJSON structure
    }
    
    @Test
    fun `empty state returns dummy feature`() {
        val state = JourneyMapUiState.Ready(mapDisplay = JourneyMapDisplay())
        val result = with(JourneyMapFeatureMapper) { state.toFeatureCollection() }
        
        assertEquals(1, result.features.size)
        assertEquals("empty", result.features[0].getStringProperty("type"))
    }
}
```

#### 3. Camera Calculation Tests

**Create**: `core/maps/ui/src/commonTest/kotlin/MapCameraUtilsTest.kt`
```kotlin
class MapCameraUtilsTest {
    @Test
    fun `calculateCenter returns midpoint`() {
        val bounds = BoundingBox(
            southwest = LatLng(-34.0, 150.0),
            northeast = LatLng(-33.0, 151.0)
        )
        
        val center = MapCameraUtils.calculateCenter(bounds)
        
        assertEquals(-33.5, center.latitude, 0.001)
        assertEquals(150.5, center.longitude, 0.001)
    }
    
    @Test
    fun `calculateZoomLevel returns appropriate zoom`() {
        val smallBounds = BoundingBox(
            southwest = LatLng(-33.88, 151.20),
            northeast = LatLng(-33.87, 151.21)
        )
        
        val zoom = MapCameraUtils.calculateZoomLevel(smallBounds)
        
        assertTrue(zoom >= MapConfig.ZoomLevels.SMALL_AREA)
    }
}
```

### Performance Optimizations

#### 1. Lazy Camera Calculations
```kotlin
val cameraPosition = remember(mapState.cameraFocus) {
    mapState.cameraFocus?.let { focus ->
        val center = MapCameraUtils.calculateCenter(focus.bounds)
        val zoom = MapCameraUtils.calculateZoomLevel(focus.bounds)
        CameraPosition(
            target = Position(center.longitude, center.latitude),
            zoom = zoom
        )
    } ?: CameraPosition(/* default */)
}
```

#### 2. Debounced Feature Collection Updates
```kotlin
val debouncedFeatureCollection = remember {
    snapshotFlow { mapState }
        .debounce(100) // Wait 100ms before recalculating
        .map { it.toFeatureCollection() }
}.collectAsState(initial = FeatureCollection(emptyList()))
```

#### 3. Feature Collection Caching
```kotlin
private val featureCollectionCache = mutableMapOf<String, FeatureCollection<*, *>>()

fun JourneyMapUiState.Ready.toFeatureCollectionCached(): FeatureCollection<*, *> {
    val cacheKey = "${mapDisplay.legs.size}_${mapDisplay.stops.size}"
    return featureCollectionCache.getOrPut(cacheKey) {
        toFeatureCollection()
    }
}
```

### Recommended Module Structure (Final)

```
core/
├── maps/
│   ├── state/              # Pure Kotlin models (existing)
│   ├── ui/                 # Config & utils (existing)
│   ├── composables/        # NEW: Reusable map components
│   │   ├── MapContainer.kt
│   │   ├── MapErrorView.kt
│   │   └── MapLoadingState.kt
│   ├── layers/             # NEW: Reusable layer definitions
│   │   ├── LayerFactory.kt
│   │   ├── TransitLineLayer.kt
│   │   └── StopMarkerLayer.kt
│   ├── camera/             # NEW: Camera operations
│   │   ├── CameraAnimator.kt
│   │   └── CameraPresets.kt
│   ├── interactions/       # NEW: Interaction handling
│   │   ├── MapClickHandler.kt
│   │   └── MapSelectionManager.kt
│   ├── accessibility/      # NEW: A11y support
│   │   └── MapAccessibility.kt
│   ├── analytics/          # NEW: Analytics
│   │   └── MapAnalytics.kt
│   └── testing/            # NEW: Test utilities
│       ├── MapStateBuilder.kt
│       └── GeoJsonFixtures.kt
```

### Priority Implementation Order

1. **P0 (Production Blockers)**:
   - Error handling UI
   - Accessibility support
   - Analytics tracking

2. **P1 (Important for Quality)**:
   - Testing infrastructure
   - Camera animations
   - Layer factory

3. **P2 (Nice to Have)**:
   - Map controls
   - Offline support
   - Performance optimizations

### Summary

**Current State**: Good foundation with clean architecture
**Production Ready**: No - missing critical error handling, accessibility, testing
**Recommended**: Extract common patterns to `core/maps/` modules
**Module Structure**: Good separation, but needs more granular modules for composables, layers, camera, etc.

The code shows solid architectural principles but needs production-readiness improvements before launch.

## Maintenance

**Code Owners**: Feature teams own their map implementations, core team owns `core/maps/`

**Version Compatibility**: Keep MapLibre library updated, test thoroughly on version bumps

**Documentation**: Update this document when adding new patterns or major features
