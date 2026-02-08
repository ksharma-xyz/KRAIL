# Journey Map Implementation Plan

## 🚨 CRITICAL FINDING - READ FIRST!

**The API DOES provide all the coordinates we need, but the Kotlin model is incomplete!**

After analyzing `sample_response.json`, I discovered that:

1. ✅ **Every stop has coordinates** in the API response via the `coord` field
2. ✅ **Origin and destination** both include `coord: [lat, lng]`
3. ✅ **All stops in stopSequence** have `coord` arrays
4. ✅ **Interchange paths** have detailed walking coordinates
5. ❌ **BUT**: The current `TripResponse.StopSequence` Kotlin model is missing these fields!

**Example from API**:
```json
{
  "origin": {
    "id": "2147421",
    "name": "Seven Hills Station, Platform 1, Seven Hills",
    "coord": [-33.774221, 150.935976],  // ← THIS EXISTS IN API!
    "parent": {
      "id": "214710",
      "coord": [-33.774351, 150.936123]  // ← Parent also has coords
    }
  }
}
```

**Action Required**: Update the `TripResponse.kt` model file FIRST before implementing any map features!

---

## Overview
This document outlines the plan to visualize journey routes on a map using MapLibre. The goal is to display the complete path of a trip including all legs (transit and walking), stops, and interchange routes.

## Current State Analysis

### Existing Map Infrastructure
- **MapLibre Integration**: Already integrated via `SearchStopMap.kt`
- **Map State Models**: Located in `feature/trip-planner/state/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/state/searchstop/`
  - `MapUiState.kt`: Platform-agnostic map state models
  - `LatLng`, `RouteFeature`, `StopFeature`, `MapDisplay` models exist
- **Mapper**: `StopResultsMapMapper.kt` converts state models to MapLibre GeoJSON features

### Trip Data Structure (from TripResponse API)
Each journey consists of:
1. **Legs** (`TripResponse.Leg`):
   - `origin`: StopSequence (has name, id, type, but NO coordinates)
   - `destination`: StopSequence
   - `stopSequence`: List of stops along the leg
   - `transportation`: Mode info (train/bus/ferry with colors)
   - `interchange`: Walking path between legs (HAS coordinates!)
   - `footPathInfo`: Walking directions info

2. **Interchange** (`TripResponse.Interchange`):
   - `coords`: List<List<Double>> - Lat/Lng pairs for walking paths
   - `type`: Mode type (99/100 for walking)
   - `desc`: Description

### Key Discovery: Coordinates ARE Provided!
**CRITICAL FINDING**: The API DOES provide coordinates but the TripResponse model is incomplete!

**From sample_response.json analysis**:
- ✅ `origin.coord`: `[-33.774221, 150.935976]` (latitude, longitude)
- ✅ `destination.coord`: Coordinates for destination stop
- ✅ `stopSequence[].coord`: Each stop has coordinates!
- ✅ `interchange.coords`: Walking path coordinates (if present)

**Problem**: The current `TripResponse.StopSequence` Kotlin model is missing:
1. `coord` field - List<Double> (latitude, longitude)
2. `parent` field - Contains parent stop info with coords
3. `isGlobalId` field - Boolean
4. `niveau` field - Platform level

**Available Location Data**:
1. ✅ Stop coordinates: Available in `coord` field (MODEL UPDATE NEEDED)
2. ✅ Interchange paths: Available in `interchange.coords`
3. ❌ Transit route polylines: Not provided (will draw straight lines between stops)

## Solution Architecture

### Phase 0: Fix TripResponse Model (CRITICAL - DO THIS FIRST!)

#### 0.1 Update StopSequence Model
**Location**: `feature/trip-planner/network/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/network/api/model/TripResponse.kt`

Add missing fields to `StopSequence`:

```kotlin
@Serializable
data class StopSequence(
    // ...existing fields...
    
    /**
     * Coordinates of the stop location.
     * Contains exactly two values: [latitude, longitude]
     * Example: [-33.774221, 150.935976]
     */
    @SerialName("coord") val coord: List<Double>? = null,
    
    /**
     * Parent location information
     */
    @SerialName("parent") val parent: ParentLocation? = null,
    
    /**
     * Indicates if this is a global stop ID
     */
    @SerialName("isGlobalId") val isGlobalId: Boolean? = null,
    
    /**
     * Platform level (e.g., 0 for ground level, -2 for underground)
     */
    @SerialName("niveau") val niveau: Int? = null,
    
    /**
     * List of transport modes available at this stop
     */
    @SerialName("modes") val modes: List<Int>? = null,
)

@Serializable
data class ParentLocation(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("disassembledName") val disassembledName: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("coord") val coord: List<Double>? = null,
    @SerialName("parent") val parent: GrandParentLocation? = null,
    @SerialName("properties") val properties: ParentProperties? = null,
    @SerialName("niveau") val niveau: Int? = null,
    @SerialName("isGlobalId") val isGlobalId: Boolean? = null,
)

@Serializable
data class GrandParentLocation(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("type") val type: String? = null, // "locality"
)

@Serializable
data class ParentProperties(
    @SerialName("stopId") val stopId: String? = null,
)
```

This is **CRITICAL** because without these fields, we cannot extract coordinates from the API response!

### Phase 1: Data Model Extension

#### 1.1 Create Journey Map State Models
**Location**: `feature/trip-planner/state/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/state/journeymap/`

Create new state models:

```kotlin
// JourneyMapState.kt
data class LatLng(val latitude: Double, val longitude: Double)

data class JourneyLegFeature(
    val legId: String,
    val transportMode: TransportMode,
    val colorHex: String,
    val routeSegment: RouteSegment,
)

sealed class RouteSegment {
    // For walking/interchange - has actual coordinates
    data class PathSegment(
        val points: List<LatLng>
    ) : RouteSegment()
    
    // For transit legs - connect stops with straight lines
    data class StopConnectorSegment(
        val stops: List<JourneyStopFeature>
    ) : RouteSegment()
}

data class JourneyStopFeature(
    val stopId: String,
    val stopName: String,
    val position: LatLng?,  // Nullable since API doesn't provide
    val stopType: StopType,
    val time: String,
    val platform: String?,
)

data class TransportMode(
    val modeType: Int,  // 1=train, 5=bus, 9=ferry, etc.
    val lineName: String?,
    val lineNumber: String?,
    val iconId: Long?,
)

enum class StopType {
    ORIGIN,
    DESTINATION,
    INTERCHANGE,
    REGULAR
}

sealed class JourneyMapUiState {
    object Loading : JourneyMapUiState()
    data class Ready(
        val mapDisplay: JourneyMapDisplay,
        val cameraFocus: CameraFocus? = null,
    ) : JourneyMapUiState()
    data class Error(val message: String) : JourneyMapUiState()
}

data class JourneyMapDisplay(
    val legs: List<JourneyLegFeature>,
    val stops: List<JourneyStopFeature>,
    val selectedLeg: JourneyLegFeature? = null,
)

data class CameraFocus(
    val bounds: BoundingBox,
    val padding: Int = 50,
)

data class BoundingBox(
    val southwest: LatLng,
    val northeast: LatLng,
)
```

#### 1.2 Coordinate Availability

**GOOD NEWS**: After updating the TripResponse model with the `coord` field, all stop coordinates will be available directly from the trip planning API response!

**Coordinate Format**:
- API Format: `coord: [latitude, longitude]` (e.g., `[-33.774221, 150.935976]`)
- Array of 2 doubles: `[0] = latitude`, `[1] = longitude`

**No additional API calls needed!** The stop coordinates are already in the response, we just need to extract them.

### Phase 2: Mapper Implementation

#### 2.1 TripResponse to JourneyMapState Mapper
**Location**: `feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/journeymap/business/`

```kotlin
// JourneyMapMapper.kt
object JourneyMapMapper {
    fun TripResponse.Journey.toJourneyMapState(): JourneyMapUiState.Ready {
        // Extract all legs
        val legFeatures = legs?.mapIndexed { index, leg ->
            leg.toJourneyLegFeature(index)
        } ?: emptyList()
        
        // Extract all stops (deduplicated)
        val stopFeatures = extractStopFeatures(legs)
        
        // Calculate bounding box for camera
        val bounds = calculateBounds(legFeatures, stopFeatures)
        
        return JourneyMapUiState.Ready(
            mapDisplay = JourneyMapDisplay(
                legs = legFeatures,
                stops = stopFeatures,
            ),
            cameraFocus = bounds?.let { CameraFocus(it) }
        )
    }
    
    private fun TripResponse.Leg.toJourneyLegFeature(index: Int): JourneyLegFeature {
        return when {
            // Walking leg with interchange coordinates
            interchange?.coords != null -> {
                JourneyLegFeature(
                    legId = "leg_$index",
                    transportMode = TransportMode(
                        modeType = 99,
                        lineName = "Walking",
                        lineNumber = null,
                        iconId = null,
                    ),
                    colorHex = "#757575", // Gray for walking
                    routeSegment = RouteSegment.PathSegment(
                        points = interchange.coords.map { coord ->
                            // API format: [latitude, longitude]
                            LatLng(latitude = coord[0], longitude = coord[1])
                        }
                    )
                )
            }
            // Transit leg - connect stops with their coordinates
            transportation != null -> {
                JourneyLegFeature(
                    legId = "leg_$index",
                    transportMode = transportation.toTransportMode(),
                    colorHex = getColorForMode(transportation.product?.productClass),
                    routeSegment = RouteSegment.StopConnectorSegment(
                        stops = stopSequence?.mapNotNull { stopSeq ->
                            // Extract coordinates from the updated StopSequence model
                            stopSeq.coord?.let { coords ->
                                stopSeq.toJourneyStopFeature(
                                    position = LatLng(
                                        latitude = coords[0],  // First element is latitude
                                        longitude = coords[1]  // Second element is longitude
                                    )
                                )
                            }
                        } ?: emptyList()
                    )
                )
            }
            else -> {
                // Fallback for unknown leg types
                // Create minimal representation
            }
        }
    }
    
    private fun TripResponse.StopSequence.toJourneyStopFeature(
        position: LatLng? = null
    ): JourneyStopFeature {
        val coords = this.coord?.let { 
            LatLng(latitude = it[0], longitude = it[1]) 
        } ?: position
        
        return JourneyStopFeature(
            stopId = id ?: "",
            stopName = disassembledName ?: name ?: "Unknown Stop",
            position = coords,
            stopType = StopType.REGULAR,
            time = departureTimeEstimated ?: arrivalTimeEstimated ?: "",
            platform = properties?.platform,
        )
    }
    
    private fun getColorForMode(productClass: Long?): String {
        return when (productClass) {
            1L -> "#F99D1C" // Train - Orange
            2L -> "#009B77" // Metro - Green
            4L -> "#EE3124" // Light Rail - Red
            5L -> "#00B9E4" // Bus - Blue
            7L -> "#793896" // Coach - Purple
            9L -> "#5BBE4B" // Ferry - Green
            99L, 100L -> "#757575" // Walking - Gray
            else -> "#666666" // Default
        }
    }
}
```

#### 2.2 GeoJSON Feature Mapper
**Location**: `feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/journeymap/`

```kotlin
// JourneyMapFeatureMapper.kt
object JourneyMapFeatureMapper {
    fun JourneyMapUiState.Ready.toFeatureCollection(): FeatureCollection<*, *> {
        val legFeatures = mapDisplay.legs.mapNotNull { it.toGeoJsonFeature() }
        val stopFeatures = mapDisplay.stops
            .filter { it.position != null }
            .map { it.toGeoJsonFeature() }
        
        return FeatureCollection(features = legFeatures + stopFeatures)
    }
    
    private fun JourneyLegFeature.toGeoJsonFeature(): Feature<*, *>? {
        return when (val segment = routeSegment) {
            is RouteSegment.PathSegment -> {
                if (segment.points.isEmpty()) return null
                Feature(
                    geometry = LineString(
                        segment.points.map { 
                            Position(longitude = it.longitude, latitude = it.latitude) 
                        }
                    ),
                    properties = buildJsonObject {
                        put("type", JsonPrimitive("journey_leg"))
                        put("legId", JsonPrimitive(legId))
                        put("color", JsonPrimitive(colorHex))
                        put("modeType", JsonPrimitive(transportMode.modeType))
                        put("isWalking", JsonPrimitive(true))
                    }
                )
            }
            is RouteSegment.StopConnectorSegment -> {
                val validStops = segment.stops.filter { it.position != null }
                if (validStops.size < 2) return null
                Feature(
                    geometry = LineString(
                        validStops.map { 
                            Position(
                                longitude = it.position!!.longitude, 
                                latitude = it.position.latitude
                            ) 
                        }
                    ),
                    properties = buildJsonObject {
                        put("type", JsonPrimitive("journey_leg"))
                        put("legId", JsonPrimitive(legId))
                        put("color", JsonPrimitive(colorHex))
                        put("modeType", JsonPrimitive(transportMode.modeType))
                        put("isWalking", JsonPrimitive(false))
                    }
                )
            }
        }
    }
    
    private fun JourneyStopFeature.toGeoJsonFeature(): Feature<*, *> {
        return Feature(
            geometry = Point(
                Position(
                    longitude = position!!.longitude, 
                    latitude = position.latitude
                )
            ),
            properties = buildJsonObject {
                put("type", JsonPrimitive("journey_stop"))
                put("stopId", JsonPrimitive(stopId))
                put("stopName", JsonPrimitive(stopName))
                put("stopType", JsonPrimitive(stopType.name))
                put("time", JsonPrimitive(time))
                platform?.let { put("platform", JsonPrimitive(it)) }
            }
        )
    }
}
```

### Phase 3: UI Components

#### 3.1 Journey Map Composable
**Location**: `feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/journeymap/`

```kotlin
// JourneyMap.kt
@Composable
fun JourneyMap(
    journeyMapState: JourneyMapUiState,
    onStopClick: (JourneyStopFeature) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (journeyMapState) {
        JourneyMapUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        is JourneyMapUiState.Ready -> {
            JourneyMapContent(
                mapState = journeyMapState,
                onStopClick = onStopClick,
                modifier = modifier,
            )
        }
        is JourneyMapUiState.Error -> {
            ErrorView(message = journeyMapState.message)
        }
    }
}

@Composable
private fun JourneyMapContent(
    mapState: JourneyMapUiState.Ready,
    onStopClick: (JourneyStopFeature) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraState = rememberCameraState(
        firstPosition = mapState.cameraFocus?.let {
            CameraPosition(
                target = calculateCenter(it.bounds),
                zoom = calculateZoomLevel(it.bounds),
            )
        } ?: CameraPosition(
            target = Position(latitude = -33.8727, longitude = 151.2057),
            zoom = 12.0,
        )
    )
    
    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        cameraState = cameraState,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        options = MapOptions(
            ornamentOptions = OrnamentOptions(
                padding = PaddingValues(16.dp),
                isLogoEnabled = false,
                isAttributionEnabled = true,
                attributionAlignment = Alignment.BottomEnd,
                isCompassEnabled = true,
                compassAlignment = Alignment.TopEnd,
            ),
        ),
    ) {
        val journeySource = rememberGeoJsonSource(
            data = GeoJsonData.Features(mapState.toFeatureCollection())
        )
        
        // Walking segments - dashed line
        LineLayer(
            id = "journey-walking-lines",
            source = journeySource,
            filter = (get("type").asString() eq const("journey_leg")) and 
                     (get("isWalking").asBoolean() eq const(true)),
            color = get("color").asString().convertToColor(),
            width = const(4.dp),
            dasharray = const(listOf(2f, 2f)), // Dashed pattern
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
        )
        
        // Transit segments - solid line
        LineLayer(
            id = "journey-transit-lines",
            source = journeySource,
            filter = (get("type").asString() eq const("journey_leg")) and 
                     (get("isWalking").asBoolean() eq const(false)),
            color = get("color").asString().convertToColor(),
            width = const(6.dp),
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
        )
        
        // Stop markers
        CircleLayer(
            id = "journey-stops",
            source = journeySource,
            filter = get("type").asString() eq const("journey_stop"),
            color = const(Color.White),
            radius = const(8.dp),
            strokeColor = const(Color.Black),
            strokeWidth = const(2.dp),
        )
        
        // Origin/Destination markers (larger)
        CircleLayer(
            id = "journey-endpoints",
            source = journeySource,
            filter = (get("type").asString() eq const("journey_stop")) and
                     ((get("stopType").asString() eq const("ORIGIN")) or
                      (get("stopType").asString() eq const("DESTINATION"))),
            color = case(
                condition = get("stopType").asString() eq const("ORIGIN"),
                output = const(Color.Green),
                fallback = const(Color.Red),
            ),
            radius = const(12.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(3.dp),
        )
    }
}
```

### Phase 4: Integration

#### 4.1 Update TimeTableViewModel
Add method to generate journey map state:

```kotlin
// In TimeTableViewModel
fun getJourneyMapState(journey: JourneyCardInfo): JourneyMapUiState {
    // Convert journey card info to map state
    // Will need access to original TripResponse.Journey
    // May need to store original response or enhance journey state
}
```

#### 4.2 Create Journey Detail Screen
New screen showing:
- Journey map at top
- Journey details below (existing UI)
- Clickable stops on map showing details

### Phase 5: Enhancements (Future)

#### 5.1 Stop Coordinate Resolution
- Implement `StopCoordinateRepository`
- Cache stop coordinates from StopFinder API
- Pre-fetch coordinates for common stops

#### 5.2 Real Transit Routes
- Integrate GTFS static data (already in project at `gtfs-static/`)
- Use actual route shapes from GTFS
- More accurate route visualization

#### 5.3 Interactive Features
- Tap stops to see details
- Highlight specific leg on tap
- Animate journey progression
- Show current location if journey in progress

#### 5.4 Styling Improvements
- Match transport mode colors from design system
- Add route labels
- Show direction arrows on routes
- Custom icons for different transport modes

## Implementation Steps

### Step 0: Fix API Model (CRITICAL - Day 1)
1. ✅ Update `TripResponse.StopSequence` to include `coord`, `parent`, `isGlobalId`, `niveau`, `modes` fields
2. ✅ Add `ParentLocation`, `GrandParentLocation`, `ParentProperties` models
3. ✅ Test with sample response to ensure deserialization works
4. ✅ Verify coordinates are properly extracted

### Step 1: Foundation (Week 1)
5. ✅ Create `JourneyMapState.kt` with data models
6. ✅ Create `JourneyMapMapper.kt` for TripResponse → State mapping
7. ✅ Add unit tests for mapper logic (including coordinate extraction)

### Step 2: Visualization (Week 1-2)
8. ✅ Create `JourneyMapFeatureMapper.kt` for GeoJSON conversion
9. ✅ Create `JourneyMap.kt` composable
10. ✅ Test with sample journey data (with real coordinates!)

### Step 3: Integration (Week 2)
11. ✅ Integrate with existing journey screens
12. ✅ Add navigation to journey map view
13. ✅ Handle edge cases (missing coordinates, single leg, etc.)

### Step 4: Polish (Week 3)
14. ✅ Optimize rendering for journeys with many stops
15. ✅ Add camera auto-focus and bounds calculation
16. ✅ Add loading states and error handling
17. ✅ UI/UX refinements and animations

## File Structure

```
feature/trip-planner/
├── state/src/commonMain/kotlin/.../state/
│   └── journeymap/
│       ├── JourneyMapState.kt          # Core state models
│       └── JourneyMapModels.kt         # Supporting models
│
├── ui/src/commonMain/kotlin/.../ui/
│   └── journeymap/
│       ├── JourneyMap.kt               # Main composable
│       ├── business/
│       │   ├── JourneyMapMapper.kt     # TripResponse → State
│       │   └── JourneyMapFeatureMapper.kt  # State → GeoJSON
│       └── components/
│           ├── JourneyMapLegend.kt     # Color/mode legend
│           └── StopDetailCard.kt       # Stop info popup
│
└── network/
    └── repository/
        └── StopCoordinateRepository.kt # Coordinate resolution
```

## Color Scheme

Transport mode colors (based on Sydney transport):
- 🟠 Train: `#F99D1C`
- 🟢 Metro: `#009B77`
- 🔴 Light Rail: `#EE3124`
- 🔵 Bus: `#00B9E4`
- 🟣 Coach: `#793896`
- 🟢 Ferry: `#5BBE4B`
- ⚪ Walking: `#757575` (dashed line)

Stop markers:
- 🟢 Origin: Green circle
- 🔴 Destination: Red circle
- ⚪ Regular stops: White circles with black border
- 🟡 Interchanges: Highlighted larger circles

## Testing Strategy

1. **Unit Tests**:
   - Mapper logic (TripResponse → State)
   - GeoJSON conversion
   - Color selection logic
   - Bounding box calculation

2. **Integration Tests**:
   - Map rendering with sample data
   - Camera positioning
   - Layer ordering

3. **Manual Testing**:
   - Various journey types (single leg, multi-leg, walking only)
   - Edge cases (missing data, no coordinates)
   - Different screen sizes

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Model update breaks existing code | High | Add fields as nullable, write tests, check all usages |
| Missing coordinates in some responses | Medium | Gracefully handle null coords, fallback to stop name display |
| Performance with many stops | Medium | Implement clustering for dense stops, optimize rendering |
| Map library limitations | Low | Tested with existing SearchStopMap, proven to work |

## Success Criteria

1. ✅ **Model Updated**: TripResponse properly deserializes coordinates from API
2. ✅ Display walking/interchange paths with actual coordinates
3. ✅ Show transit legs connecting all stops with their real locations
4. ✅ Color-code routes by transport mode (train/bus/ferry)
5. ✅ Auto-focus camera on journey bounds
6. ✅ Display origin (green) and destination (red) markers clearly
7. ✅ Handle journeys with 10+ stops smoothly
8. ✅ Smooth performance (60fps on map interactions)

## Notes

- **CRITICAL**: The `TripResponse.StopSequence` model MUST be updated first to include the `coord` field!
- **Coordinate Format in API**: `coord: [latitude, longitude]` - Array where index 0 is lat, index 1 is lng
- **Coordinate Format in GeoJSON**: `Position(longitude, latitude)` - REVERSED ORDER!
- **Interchange Coords Format**: `List<List<Double>>` where each inner list is `[latitude, longitude]`
- **Existing Pattern**: Follow the pattern from `SearchStopMap.kt` and `StopResultsMapMapper.kt`
- **GTFS Integration**: Project already has `gtfs-static` module - consider using for accurate route shapes in future phases
- **State Module**: Keep state models platform-agnostic (no MapLibre types)
- **Parent Location**: StopSequence has parent with additional coord data - can use as fallback

## Quick Start Checklist

To implement journey map visualization, follow these steps in order:

### Day 1: Fix the Data Model
- [ ] Update `TripResponse.kt` - Add `coord`, `parent`, `isGlobalId`, `niveau` to `StopSequence`
- [ ] Add new models: `ParentLocation`, `GrandParentLocation`, `ParentProperties`
- [ ] Test deserialization with `sample_response.json`
- [ ] Verify all coordinates are properly extracted

### Day 2-3: Create State Models
- [ ] Create `feature/trip-planner/state/src/commonMain/kotlin/.../journeymap/JourneyMapState.kt`
- [ ] Define: `LatLng`, `JourneyLegFeature`, `RouteSegment`, `JourneyStopFeature`, `TransportMode`
- [ ] Create: `JourneyMapUiState`, `JourneyMapDisplay`, `CameraFocus`, `BoundingBox`

### Day 4-5: Build Mappers
- [ ] Create `JourneyMapMapper.kt` - Convert `TripResponse.Journey` → `JourneyMapUiState`
- [ ] Create `JourneyMapFeatureMapper.kt` - Convert state → GeoJSON `FeatureCollection`
- [ ] Write unit tests for coordinate extraction and conversion

### Day 6-8: Build UI Components
- [ ] Create `JourneyMap.kt` composable with MapLibre integration
- [ ] Add line layers for walking (dashed) and transit (solid) routes
- [ ] Add circle layers for stops (origin=green, destination=red, regular=white)
- [ ] Implement camera auto-focus on journey bounds

### Day 9-10: Integration & Testing
- [ ] Integrate with existing journey detail screens
- [ ] Add navigation to map view
- [ ] Test with various journey types (single leg, multi-leg, walking-only)
- [ ] Handle edge cases (missing coords, single stop, etc.)

### Day 11-12: Polish & Optimize
- [ ] Add interactive features (tap stops to show details)
- [ ] Optimize rendering for journeys with 20+ stops
- [ ] Add loading states and error handling
- [ ] Final UI/UX refinements

## References

- **MapLibre Compose**: Existing implementation in `SearchStopMap.kt`
- **API Documentation**: https://opendata.transport.nsw.gov.au/dataset/trip-planner-apis
- **Sample Data**: `feature/trip-planner/network/sample_response.json`
- **Existing State Models**: `feature/trip-planner/state/.../searchstop/MapUiState.kt`
- **GeoJSON Spec**: https://geojson.org/
- **MapLibre Style Spec**: https://maplibre.org/maplibre-style-spec/
