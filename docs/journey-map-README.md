# Journey Map Visualization - Implementation Summary

## 📚 Documentation Index

This implementation involves displaying journey routes on a map. Three comprehensive documents have been created:

### 1. **journey-map-implementation-plan.md** (Main Plan)
   - Complete implementation roadmap
   - Phase-by-phase breakdown
   - Data models and architecture
   - Code examples for all components
   - Testing strategy
   - **Start Here** for overall understanding

### 2. **journey-map-data-flow.md** (Visual Architecture)
   - Data flow diagrams (API → UI)
   - Coordinate transformation guide
   - Transport mode color schemes
   - Camera auto-focus algorithm
   - Performance optimization strategies
   - **Use This** for understanding how data moves through the system

### 3. **journey-map-model-updates.md** (Step-by-Step Guide)
   - Exact code changes for TripResponse.kt
   - Field-by-field documentation
   - Validation and testing steps
   - Common issues and solutions
   - **Follow This** for the first implementation step

## 🎯 Quick Start Guide

### Step 1: Understand the Discovery (5 min)
**CRITICAL FINDING**: The Transport NSW API already provides all stop coordinates in the response, but the current Kotlin model doesn't capture them!

From `sample_response.json`:
```json
{
  "origin": {
    "coord": [-33.774221, 150.935976],  // ← We're missing this!
    "parent": {
      "coord": [-33.774351, 150.936123]
    }
  },
  "stopSequence": [
    {
      "coord": [-33.787238, 150.951573]  // ← And this!
    }
  ]
}
```

### Step 2: Update the Model (Day 1) 
**File**: `feature/trip-planner/network/.../TripResponse.kt`

Add to `StopSequence`:
- `coord: List<Double>?` - Stop coordinates [lat, lng]
- `parent: ParentLocation?` - Parent location info
- `isGlobalId: Boolean?` - Global ID flag
- `niveau: Int?` - Platform level
- `modes: List<Int>?` - Transport modes

**See**: `journey-map-model-updates.md` for exact code

### Step 3: Create State Models (Day 2-3)
**Location**: `feature/trip-planner/state/.../journeymap/`

Create:
- `JourneyMapState.kt` - Platform-agnostic state
- Models: `LatLng`, `JourneyLegFeature`, `RouteSegment`, `JourneyStopFeature`

**See**: `journey-map-implementation-plan.md` Section "Phase 1"

### Step 4: Build Mappers (Day 4-5)
**Location**: `feature/trip-planner/ui/.../journeymap/business/`

Create:
- `JourneyMapMapper.kt` - TripResponse → State
- `JourneyMapFeatureMapper.kt` - State → GeoJSON

**See**: `journey-map-data-flow.md` for transformation logic

### Step 5: Build UI (Day 6-8)
**Location**: `feature/trip-planner/ui/.../journeymap/`

Create:
- `JourneyMap.kt` - MapLibre composable
- Line layers (walking=dashed, transit=solid)
- Circle layers (stops with different colors)

**See**: `journey-map-implementation-plan.md` Section "Phase 3"

### Step 6: Integrate (Day 9-10)
- Connect to journey detail screens
- Add navigation
- Handle edge cases

### Step 7: Polish (Day 11-12)
- Interactions
- Optimizations
- Testing

## 🔑 Key Concepts

### Coordinate Formats (IMPORTANT!)

**API Format**:
```json
"coord": [latitude, longitude]
         [  -33.77,  150.93 ]
```

**MapLibre Format** (REVERSED!):
```kotlin
Position(longitude, latitude)
         (150.93,  -33.77)
```

**Always convert**: 
```kotlin
// From API
val apiCoord = stopSequence.coord  // [-33.77, 150.93]
val lat = apiCoord[0]              // -33.77
val lng = apiCoord[1]              // 150.93

// To MapLibre
val position = Position(
    longitude = lng,  // 150.93 (second becomes first!)
    latitude = lat    // -33.77 (first becomes second!)
)
```

### Transport Mode Colors

```
1  = Train       = 🟠 #F99D1C (Orange)
2  = Metro       = 🟢 #009B77 (Green)
4  = Light Rail  = 🔴 #EE3124 (Red)
5  = Bus         = 🔵 #00B9E4 (Blue)
7  = Coach       = 🟣 #793896 (Purple)
9  = Ferry       = 🟢 #5BBE4B (Green)
99 = Walking     = ⚪ #757575 (Gray)
```

### Stop Marker Colors

```
Origin       = 🟢 Green  (12dp radius)
Destination  = 🔴 Red    (12dp radius)
Interchange  = 🟡 Yellow (10dp radius)
Regular      = ⚪ White  (8dp radius)
```

## 📊 Architecture Layers

```
┌─────────────────────────────────────────┐
│  API Response (JSON with coordinates)   │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  TripResponse (Kotlin Model)            │ ← FIX THIS FIRST!
│  - Add coord field to StopSequence      │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  JourneyMapMapper                        │
│  - Extract coordinates                   │
│  - Determine colors                      │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  JourneyMapUiState (Platform-agnostic)  │
│  - Pure Kotlin, no MapLibre types       │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  JourneyMapFeatureMapper                 │
│  - Convert to GeoJSON FeatureCollection │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  JourneyMap Composable                   │
│  - MapLibre rendering                    │
│  - Line & Circle layers                  │
└─────────────────────────────────────────┘
```

## 🎨 Visual Example

What the final map will show:

```
┌─────────────────────────────────────────────┐
│  🗺️ Journey Map                             │
│  ┌───────────────────────────────────────┐  │
│  │                                       │  │
│  │   🟢 Seven Hills Station              │  │
│  │    ┃                                  │  │
│  │    ┃ ━━━ Orange T1 line ━━━          │  │
│  │    ┃                                  │  │
│  │   ⚪ Toongabbie Station               │  │
│  │    ┃                                  │  │
│  │   ⚪ Pendle Hill Station              │  │
│  │    ┃                                  │  │
│  │   ⚪ Wentworthville Station           │  │
│  │    ┃                                  │  │
│  │    ┃ ━━━ Orange T1 line ━━━          │  │
│  │    ┃                                  │  │
│  │   🔴 Town Hall Station                │  │
│  │                                       │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  Legend:                                    │
│  🟢 Origin  🔴 Destination  ⚪ Stop         │
│  🟠 Train  🔵 Bus  🟢 Ferry  ━ ━ Walking   │
└─────────────────────────────────────────────┘
```

## ✅ Implementation Checklist

### Phase 0: Model Update
- [ ] Add `coord` field to `StopSequence`
- [ ] Add `parent` field to `StopSequence`
- [ ] Create `ParentLocation` model
- [ ] Create `GrandParentLocation` model
- [ ] Create `ParentProperties` model
- [ ] Test deserialization with sample JSON
- [ ] Verify coordinates parse correctly

### Phase 1: State Models
- [ ] Create `journeymap` package in state module
- [ ] Define `LatLng` data class
- [ ] Define `JourneyLegFeature` data class
- [ ] Define `RouteSegment` sealed class
- [ ] Define `JourneyStopFeature` data class
- [ ] Define `TransportMode` data class
- [ ] Define `JourneyMapUiState` sealed class
- [ ] Define `JourneyMapDisplay` data class
- [ ] Define `CameraFocus` and `BoundingBox`

### Phase 2: Mappers
- [ ] Create `JourneyMapMapper.kt`
- [ ] Implement `Journey.toJourneyMapState()`
- [ ] Implement coordinate extraction
- [ ] Implement color selection logic
- [ ] Create `JourneyMapFeatureMapper.kt`
- [ ] Implement `toFeatureCollection()`
- [ ] Implement `toGeoJsonFeature()` for legs
- [ ] Implement `toGeoJsonFeature()` for stops
- [ ] Write unit tests for mappers

### Phase 3: UI Components
- [ ] Create `journeymap` package in ui module
- [ ] Create `JourneyMap.kt` composable
- [ ] Implement MapLibre integration
- [ ] Add walking line layer (dashed)
- [ ] Add transit line layer (solid)
- [ ] Add stop circle layer
- [ ] Add origin/destination markers
- [ ] Implement camera auto-focus
- [ ] Add loading state
- [ ] Add error state

### Phase 4: Integration
- [ ] Add navigation to journey map
- [ ] Connect to journey detail screen
- [ ] Pass journey data to map
- [ ] Handle back navigation
- [ ] Test with real journey data

### Phase 5: Polish
- [ ] Add stop tap interactions
- [ ] Add leg highlighting
- [ ] Optimize for many stops
- [ ] Add animations
- [ ] Handle edge cases
- [ ] Performance testing
- [ ] UI/UX refinements

## 🚨 Common Pitfalls to Avoid

1. **Coordinate Order Confusion**
   - API: `[lat, lng]`
   - MapLibre: `Position(lng, lat)`
   - Always double-check!

2. **Null Coordinates**
   - API might omit `coord` for some stops
   - Always use `coord?.let { }` or fallback to parent

3. **Invalid Coordinates**
   - Validate: lat ∈ [-90, 90], lng ∈ [-180, 180]
   - Filter out invalid values

4. **Performance with Many Stops**
   - Consider clustering for 20+ stops
   - Memoize FeatureCollection conversion

5. **Missing Model Fields**
   - Update TripResponse FIRST
   - Test deserialization before proceeding

## 📈 Success Metrics

After implementation, you should have:

- ✅ All journey stops visible on map
- ✅ Routes colored by transport mode
- ✅ Walking segments shown as dashed lines
- ✅ Transit segments shown as solid lines
- ✅ Origin marked in green
- ✅ Destination marked in red
- ✅ Camera auto-focused on journey
- ✅ Smooth 60fps performance
- ✅ Handles 50+ stop journeys
- ✅ Graceful error handling

## 📞 Need Help?

1. **Model Issues**: See `journey-map-model-updates.md`
2. **Data Flow Questions**: See `journey-map-data-flow.md`
3. **Implementation Details**: See `journey-map-implementation-plan.md`
4. **Code Examples**: All three docs have extensive code samples

## 🎓 Learning Resources

- **MapLibre Compose**: Check existing `SearchStopMap.kt`
- **GeoJSON**: https://geojson.org/
- **MapLibre Spec**: https://maplibre.org/maplibre-style-spec/
- **Sample Data**: `feature/trip-planner/network/sample_response.json`

## 🚀 Ready to Start?

1. Open `journey-map-model-updates.md`
2. Update `TripResponse.kt`
3. Test with sample JSON
4. Then follow the implementation plan!

Good luck! 🎉
