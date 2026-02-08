# Journey Map - Data Flow Architecture

## Overview
This document visualizes how data flows from the Transport NSW API through the app layers to render journey routes on a map.

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                      TRANSPORT NSW API                               │
│  https://api.transport.nsw.gov.au/v1/tp/trip                        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ JSON Response
                                │ (with coordinates!)
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     NETWORK LAYER                                    │
│  feature/trip-planner/network/                                      │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ TripResponse (Kotlin Model) - NEEDS UPDATE!                 │  │
│  │ ├─ journeys: List<Journey>                                   │  │
│  │ │  └─ legs: List<Leg>                                        │  │
│  │ │     ├─ origin: StopSequence                                │  │
│  │ │     │  ├─ coord: [lat, lng] ← ADD THIS!                   │  │
│  │ │     │  ├─ parent: ParentLocation ← ADD THIS!              │  │
│  │ │     │  └─ name, id, type, times...                        │  │
│  │ │     ├─ destination: StopSequence                           │  │
│  │ │     ├─ stopSequence: List<StopSequence>                    │  │
│  │ │     │  └─ Each has coord: [lat, lng] ← ADD THIS!         │  │
│  │ │     ├─ transportation: Transportation                      │  │
│  │ │     │  └─ product.class (1=train, 5=bus, etc)            │  │
│  │ │     └─ interchange: Interchange                            │  │
│  │ │        └─ coords: [[lat,lng], [lat,lng]...]              │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ Parsed Model
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      MAPPER LAYER                                    │
│  feature/trip-planner/ui/journeymap/business/                       │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ JourneyMapMapper                                             │  │
│  │                                                              │  │
│  │ TripResponse.Journey → JourneyMapUiState.Ready              │  │
│  │                                                              │  │
│  │ For each leg:                                                │  │
│  │   • Extract coordinates from leg.origin.coord                │  │
│  │   • Extract coordinates from leg.stopSequence[].coord        │  │
│  │   • Extract walking path from leg.interchange.coords         │  │
│  │   • Determine transport mode color                           │  │
│  │   • Create JourneyLegFeature                                 │  │
│  │                                                              │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ Platform-Agnostic State
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      STATE LAYER                                     │
│  feature/trip-planner/state/journeymap/                             │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ JourneyMapUiState.Ready                                      │  │
│  │ ├─ mapDisplay: JourneyMapDisplay                             │  │
│  │ │  ├─ legs: List<JourneyLegFeature>                          │  │
│  │ │  │  └─ routeSegment:                                       │  │
│  │ │  │     ├─ PathSegment (walking with coords)               │  │
│  │ │  │     └─ StopConnectorSegment (transit stops)            │  │
│  │ │  └─ stops: List<JourneyStopFeature>                        │  │
│  │ │     └─ position: LatLng(lat, lng)                         │  │
│  │ └─ cameraFocus: CameraFocus                                  │  │
│  │    └─ bounds: BoundingBox                                    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ Pure Kotlin State
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   GEOJSON MAPPER LAYER                               │
│  feature/trip-planner/ui/journeymap/                                │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ JourneyMapFeatureMapper                                      │  │
│  │                                                              │  │
│  │ JourneyMapUiState.Ready → FeatureCollection                 │  │
│  │                                                              │  │
│  │ Creates GeoJSON Features:                                    │  │
│  │   • LineString features for routes                           │  │
│  │   • Point features for stops                                 │  │
│  │   • Properties for styling (color, type, etc)                │  │
│  │                                                              │  │
│  │ IMPORTANT: Position(longitude, latitude) ← REVERSED!        │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ GeoJSON FeatureCollection
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        UI LAYER                                      │
│  feature/trip-planner/ui/journeymap/                                │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ JourneyMap.kt (@Composable)                                  │  │
│  │                                                              │  │
│  │ MaplibreMap {                                                │  │
│  │   val source = rememberGeoJsonSource(featureCollection)     │  │
│  │                                                              │  │
│  │   // Walking paths - dashed gray lines                       │  │
│  │   LineLayer(                                                 │  │
│  │     filter = isWalking == true,                             │  │
│  │     dasharray = [2, 2],                                     │  │
│  │     color = #757575                                          │  │
│  │   )                                                          │  │
│  │                                                              │  │
│  │   // Transit routes - solid colored lines                    │  │
│  │   LineLayer(                                                 │  │
│  │     filter = isWalking == false,                            │  │
│  │     color = from properties.color,                          │  │
│  │     width = 6dp                                              │  │
│  │   )                                                          │  │
│  │                                                              │  │
│  │   // Stop markers                                            │  │
│  │   CircleLayer(                                               │  │
│  │     origin = green, destination = red, regular = white      │  │
│  │   )                                                          │  │
│  │ }                                                            │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ Rendered Map
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      USER SCREEN                                     │
│                                                                      │
│  ┌────────────────────────────────────────────────────────┐         │
│  │  🗺️  Journey Map                                       │         │
│  │  ┌──────────────────────────────────────────────────┐ │         │
│  │  │                                                  │ │         │
│  │  │   🟢 Seven Hills Station                         │ │         │
│  │  │    ┃ (origin - green marker)                     │ │         │
│  │  │    ┃ ━━━ Orange solid line (T1 Train) ━━━        │ │         │
│  │  │    ┃                                              │ │         │
│  │  │   ⚪ Toongabbie Station                          │ │         │
│  │  │    ┃ (stop - white marker)                       │ │         │
│  │  │    ┃                                              │ │         │
│  │  │   ⚪ Pendle Hill Station                         │ │         │
│  │  │    ┃                                              │ │         │
│  │  │    ┃ ━━━ Orange solid line continues ━━━         │ │         │
│  │  │    ┃                                              │ │         │
│  │  │   🔴 Town Hall Station                           │ │         │
│  │  │    (destination - red marker)                    │ │         │
│  │  │                                                  │ │         │
│  │  └──────────────────────────────────────────────────┘ │         │
│  │  Legend: 🟠 Train | 🔵 Bus | 🟢 Ferry | ⚪ Walking    │         │
│  └────────────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────────────┘
```

## Coordinate Format Transformation

### API → Kotlin Model
```kotlin
// API JSON
{
  "coord": [-33.774221, 150.935976]
  //         ↑           ↑
  //      latitude   longitude
}

// Kotlin extraction
val coord = stopSequence.coord // List<Double>?
val lat = coord?.get(0)  // -33.774221
val lng = coord?.get(1)  // 150.935976
```

### Kotlin Model → State
```kotlin
// Create platform-agnostic LatLng
val position = LatLng(
    latitude = coord[0],   // -33.774221
    longitude = coord[1]   // 150.935976
)
```

### State → GeoJSON
```kotlin
// IMPORTANT: Position expects (longitude, latitude) - REVERSED!
val geoPosition = Position(
    longitude = latLng.longitude,  // 150.935976 (second!)
    latitude = latLng.latitude     // -33.774221 (first!)
)
```

## Transport Mode Colors

```kotlin
fun getColorForMode(productClass: Long?): String {
    return when (productClass) {
        1L  -> "#F99D1C"  // 🟠 Train - Orange
        2L  -> "#009B77"  // 🟢 Metro - Green  
        4L  -> "#EE3124"  // 🔴 Light Rail - Red
        5L  -> "#00B9E4"  // 🔵 Bus - Blue
        7L  -> "#793896"  // 🟣 Coach - Purple
        9L  -> "#5BBE4B"  // 🟢 Ferry - Green
        99L -> "#757575"  // ⚪ Walking - Gray
        else -> "#666666" // Default - Dark Gray
    }
}
```

## Stop Marker Types

```
┌──────────────┬──────────────┬──────────────────────┐
│ Stop Type    │ Color        │ Radius               │
├──────────────┼──────────────┼──────────────────────┤
│ Origin       │ 🟢 Green     │ 12dp (larger)        │
│ Destination  │ 🔴 Red       │ 12dp (larger)        │
│ Interchange  │ 🟡 Yellow    │ 10dp (medium)        │
│ Regular      │ ⚪ White     │ 8dp (smaller)        │
└──────────────┴──────────────┴──────────────────────┘
```

## Line Styles

### Walking/Interchange Lines
- **Style**: Dashed line
- **Pattern**: `[2f, 2f]` (2px line, 2px gap)
- **Color**: Gray `#757575`
- **Width**: 4dp
- **Cap**: Round
- **Join**: Round

### Transit Lines  
- **Style**: Solid line
- **Color**: Dynamic (based on transport mode)
- **Width**: 6dp
- **Cap**: Round
- **Join**: Round

## Example Journey Breakdown

Using the sample response from `sample_response.json`:

```
Journey: Seven Hills → Town Hall
├─ Leg 1: T1 Train (Orange #F99D1C)
│  ├─ Origin: Seven Hills Station
│  │  └─ coord: [-33.774221, 150.935976]
│  ├─ Stop 1: Toongabbie Station
│  │  └─ coord: [-33.787238, 150.951573]
│  ├─ Stop 2: Pendle Hill Station
│  │  └─ coord: [-33.801276, 150.956386]
│  ├─ Stop 3: Wentworthville Station
│  │  └─ coord: [-33.807017, 150.972583]
│  └─ Destination: Town Hall Station
│     └─ coord: [-33.873654, 151.20672]
│
└─ Map Rendering:
   1. Extract all coordinates from stopSequence
   2. Create LineString with positions
   3. Style with orange color (#F99D1C)
   4. Add stop markers at each coordinate
   5. Make origin green, destination red
```

## Camera Auto-Focus Algorithm

```kotlin
fun calculateBounds(legs: List<JourneyLegFeature>): BoundingBox {
    // Collect all coordinates from all legs
    val allCoordinates = legs.flatMap { leg ->
        when (val segment = leg.routeSegment) {
            is PathSegment -> segment.points
            is StopConnectorSegment -> segment.stops.mapNotNull { it.position }
        }
    }
    
    if (allCoordinates.isEmpty()) return defaultBounds
    
    // Find min/max lat/lng
    val minLat = allCoordinates.minOf { it.latitude }
    val maxLat = allCoordinates.maxOf { it.latitude }
    val minLng = allCoordinates.minOf { it.longitude }
    val maxLng = allCoordinates.maxOf { it.longitude }
    
    return BoundingBox(
        southwest = LatLng(minLat, minLng),
        northeast = LatLng(maxLat, maxLng)
    )
}

fun calculateCenter(bounds: BoundingBox): Position {
    val centerLat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2
    val centerLng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2
    return Position(longitude = centerLng, latitude = centerLat)
}

fun calculateZoomLevel(bounds: BoundingBox): Double {
    val latDiff = abs(bounds.northeast.latitude - bounds.southwest.latitude)
    val lngDiff = abs(bounds.northeast.longitude - bounds.southwest.longitude)
    val maxDiff = max(latDiff, lngDiff)
    
    // Rough zoom level calculation
    return when {
        maxDiff > 1.0 -> 9.0   // Large area
        maxDiff > 0.5 -> 10.0  // Medium area
        maxDiff > 0.1 -> 12.0  // City area
        maxDiff > 0.05 -> 13.0 // Suburb area
        else -> 14.0           // Neighborhood
    }
}
```

## Error Handling Strategy

```
┌─────────────────────────────────────────────────┐
│ Scenario                │ Handling Strategy     │
├─────────────────────────┼───────────────────────┤
│ Missing coord field     │ Skip that stop,       │
│                         │ use parent.coord or   │
│                         │ fallback to name      │
├─────────────────────────┼───────────────────────┤
│ Empty stopSequence      │ Draw direct line from │
│                         │ origin to destination │
├─────────────────────────┼───────────────────────┤
│ Single stop journey     │ Show single marker,   │
│                         │ no lines              │
├─────────────────────────┼───────────────────────┤
│ No coordinates at all   │ Show error message,   │
│                         │ fallback to list view │
├─────────────────────────┼───────────────────────┤
│ Invalid coordinates     │ Validate range:       │
│ (out of bounds)         │ lat: [-90, 90]       │
│                         │ lng: [-180, 180]     │
└─────────────────────────┴───────────────────────┘
```

## Performance Optimization

### For journeys with many stops (20+):

1. **Simplify Polylines**: Use Douglas-Peucker algorithm to reduce points
2. **Cluster Stops**: Group nearby stops when zoomed out
3. **Lazy Loading**: Load GeoJSON features on demand
4. **Debounce**: Debounce camera movements to reduce re-renders
5. **Memoization**: Cache FeatureCollection conversion results

### Example:
```kotlin
@Composable
fun JourneyMap(state: JourneyMapUiState.Ready) {
    val featureCollection = remember(state) {
        // Expensive computation cached
        state.toFeatureCollection()
    }
    
    // Map rendering...
}
```

## Testing Checklist

- [ ] Single leg journey (direct trip)
- [ ] Multi-leg journey with transfers
- [ ] Journey with walking segments
- [ ] Journey with 20+ stops
- [ ] Journey with missing coordinates (graceful degradation)
- [ ] Journey crossing date line (edge case)
- [ ] Camera focuses correctly on small journeys
- [ ] Camera focuses correctly on large journeys
- [ ] Colors match transport modes
- [ ] Stop markers render correctly
- [ ] Line dash patterns work for walking
- [ ] Tap interactions work
- [ ] Performance: 60fps on map pan/zoom

## Next Steps

1. **Start Here**: Update `TripResponse.StopSequence` model
2. **Verify**: Test with sample_response.json deserialization
3. **Build**: Create state models in journeymap package
4. **Map**: Implement mappers (domain → state → geojson)
5. **Render**: Build composable with MapLibre layers
6. **Integrate**: Connect to existing journey screens
7. **Polish**: Add interactions, animations, optimizations
