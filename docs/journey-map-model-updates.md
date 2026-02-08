# TripResponse Model Updates - Detailed Guide

## 🎯 Objective
Update the `TripResponse.kt` model to capture coordinate data that the API already provides but our current model is missing.

## 📋 Files to Modify

### File: `feature/trip-planner/network/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/network/api/model/TripResponse.kt`

## 🔧 Changes Required

### 1. Update StopSequence Data Class

**Current Code** (around line 102):
```kotlin
@Serializable
data class StopSequence(
    @SerialName("arrivalTimeEstimated") val arrivalTimeEstimated: String? = null,
    @SerialName("arrivalTimePlanned") val arrivalTimePlanned: String? = null,
    @SerialName("departureTimeEstimated") val departureTimeEstimated: String? = null,
    @SerialName("departureTimePlanned") val departureTimePlanned: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("disassembledName") val disassembledName: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("properties") val properties: DestinationProperties? = null,
)
```

**Updated Code** (add these fields):
```kotlin
@Serializable
data class StopSequence(
    @SerialName("arrivalTimeEstimated") val arrivalTimeEstimated: String? = null,
    @SerialName("arrivalTimePlanned") val arrivalTimePlanned: String? = null,
    @SerialName("departureTimeEstimated") val departureTimeEstimated: String? = null,
    @SerialName("departureTimePlanned") val departureTimePlanned: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("disassembledName") val disassembledName: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("properties") val properties: DestinationProperties? = null,
    
    // ========== NEW FIELDS BELOW ==========
    
    /**
     * Coordinates of the stop/platform location.
     * Array format: [latitude, longitude]
     * Example: [-33.774221, 150.935976]
     */
    @SerialName("coord") val coord: List<Double>? = null,
    
    /**
     * Parent location information (usually the station when this is a platform)
     */
    @SerialName("parent") val parent: ParentLocation? = null,
    
    /**
     * Indicates if the id is a global stop ID
     */
    @SerialName("isGlobalId") val isGlobalId: Boolean? = null,
    
    /**
     * Level/floor of the platform
     * 0 = ground level, negative = underground, positive = elevated
     * Example: -2 for Town Hall underground platform
     */
    @SerialName("niveau") val niveau: Int? = null,
    
    /**
     * List of transport mode IDs available at this stop
     * Values: 1=Train, 2=Metro, 4=Light Rail, 5=Bus, 7=Coach, 9=Ferry, 11=School Bus
     */
    @SerialName("modes") val modes: List<Int>? = null,
    
    /**
     * Planned departure time from the base timetable (non-real-time)
     */
    @SerialName("departureTimeBaseTimetable") val departureTimeBaseTimetable: String? = null,
    
    /**
     * Planned arrival time from the base timetable (non-real-time)
     */
    @SerialName("arrivalTimeBaseTimetable") val arrivalTimeBaseTimetable: String? = null,
)
```

### 2. Add New Parent Location Models

**Add these new data classes** (after StopSequence, around line 159):

```kotlin
/**
 * Parent location information for a stop.
 * Usually represents the station when the stop is a specific platform.
 */
@Serializable
data class ParentLocation(
    /**
     * ID of the parent location
     */
    @SerialName("id") val id: String? = null,
    
    /**
     * Full name of the parent location
     */
    @SerialName("name") val name: String? = null,
    
    /**
     * Short name without suburb
     */
    @SerialName("disassembledName") val disassembledName: String? = null,
    
    /**
     * Type of location: "stop", "platform", "locality", etc.
     */
    @SerialName("type") val type: String? = null,
    
    /**
     * Coordinates of the parent location
     * Array format: [latitude, longitude]
     */
    @SerialName("coord") val coord: List<Double>? = null,
    
    /**
     * Grandparent location (usually locality/suburb)
     */
    @SerialName("parent") val parent: GrandParentLocation? = null,
    
    /**
     * Additional properties of the parent location
     */
    @SerialName("properties") val properties: ParentProperties? = null,
    
    /**
     * Level/floor of the parent location
     */
    @SerialName("niveau") val niveau: Int? = null,
    
    /**
     * Indicates if this is a global ID
     */
    @SerialName("isGlobalId") val isGlobalId: Boolean? = null,
)

/**
 * Grandparent location (typically the suburb/locality)
 */
@Serializable
data class GrandParentLocation(
    /**
     * ID of the locality
     * Format: "placeID:95308035:1"
     */
    @SerialName("id") val id: String? = null,
    
    /**
     * Name of the locality/suburb
     * Example: "Seven Hills", "Sydney"
     */
    @SerialName("name") val name: String? = null,
    
    /**
     * Type is typically "locality"
     */
    @SerialName("type") val type: String? = null,
)

/**
 * Additional properties for parent locations
 */
@Serializable
data class ParentProperties(
    /**
     * The legacy stop ID
     * Example: "10101234"
     */
    @SerialName("stopId") val stopId: String? = null,
)
```

### 3. Update DestinationProperties (Optional Enhancement)

The current `DestinationProperties` is good, but you can add these additional fields found in the API:

```kotlin
@Serializable
data class DestinationProperties(
    @SerialName("WheelchairAccess") val wheelchairAccess: String? = null,
    @SerialName("downloads") val downloads: List<Download>? = null,
    @SerialName("occupancy") val occupancy: String? = null,
    @SerialName("platform") val platform: String? = null,
    
    // ========== OPTIONAL NEW FIELDS ==========
    
    /**
     * Platform name (e.g., "Platform 1", "Stand A")
     */
    @SerialName("platformName") val platformName: String? = null,
    
    /**
     * Planned platform name from timetable
     */
    @SerialName("plannedPlatformName") val plannedPlatformName: String? = null,
    
    /**
     * Stopping point planned designation
     */
    @SerialName("stoppingPointPlanned") val stoppingPointPlanned: String? = null,
    
    /**
     * Platform area number
     */
    @SerialName("area") val area: String? = null,
    
    /**
     * Number of cars in the train
     */
    @SerialName("NumberOfCars") val numberOfCars: String? = null,
)
```

## ✅ Validation Steps

### 1. Test Deserialization

Create a test to ensure the updated model works:

```kotlin
// In your test file
@Test
fun `test TripResponse deserialization with coordinates`() {
    val json = File("sample_response.json").readText()
    val response = Json.decodeFromString<TripResponse>(json)
    
    // Verify coordinates are parsed
    val firstLeg = response.journeys?.firstOrNull()?.legs?.firstOrNull()
    assertNotNull(firstLeg?.origin?.coord)
    
    val coords = firstLeg?.origin?.coord
    assertEquals(2, coords?.size) // Should have [lat, lng]
    
    // Verify it's valid coordinates
    val lat = coords?.get(0)
    val lng = coords?.get(1)
    assertTrue(lat!! in -90.0..90.0)
    assertTrue(lng!! in -180.0..180.0)
    
    // Verify parent location
    assertNotNull(firstLeg?.origin?.parent)
    assertNotNull(firstLeg?.origin?.parent?.coord)
    
    println("Origin: ${firstLeg?.origin?.name}")
    println("Coordinates: [$lat, $lng]")
    println("Parent: ${firstLeg?.origin?.parent?.name}")
}
```

### 2. Verify Existing Code Still Works

Run all existing tests that use `TripResponse`:
```bash
./gradlew :feature:trip-planner:network:test
./gradlew :feature:trip-planner:ui:test
```

### 3. Check Breaking Changes

Since all new fields are nullable with default values, there should be NO breaking changes. Existing code will continue to work, and new code can access the coordinate fields.

## 📝 Example Usage After Update

### Extract Coordinates from Journey

```kotlin
fun extractJourneyCoordinates(journey: TripResponse.Journey): List<LatLng> {
    return journey.legs?.flatMap { leg ->
        // Get all stop coordinates from this leg
        leg.stopSequence?.mapNotNull { stop ->
            stop.coord?.let { coords ->
                LatLng(
                    latitude = coords[0],
                    longitude = coords[1]
                )
            }
        } ?: emptyList()
    } ?: emptyList()
}
```

### Get Stop Coordinate with Fallback

```kotlin
fun TripResponse.StopSequence.getCoordinate(): LatLng? {
    // Try direct coordinate first
    coord?.let { 
        return LatLng(latitude = it[0], longitude = it[1])
    }
    
    // Fallback to parent coordinate
    parent?.coord?.let {
        return LatLng(latitude = it[0], longitude = it[1])
    }
    
    return null
}
```

### Extract Walking Path Coordinates

```kotlin
fun extractWalkingPath(leg: TripResponse.Leg): List<LatLng> {
    return leg.interchange?.coords?.map { coord ->
        LatLng(
            latitude = coord[0],
            longitude = coord[1]
        )
    } ?: emptyList()
}
```

## 🐛 Common Issues & Solutions

### Issue 1: Deserialization Fails
**Problem**: JSON parsing throws exception
**Solution**: Check that all new fields have `? = null` defaults

### Issue 2: Wrong Coordinate Order
**Problem**: Map shows locations in wrong place
**Solution**: Remember API uses `[latitude, longitude]` order
```kotlin
// CORRECT ✅
val lat = coord[0]  // First element
val lng = coord[1]  // Second element

// WRONG ❌
val lng = coord[0]  // Don't swap!
val lat = coord[1]
```

### Issue 3: Empty Coordinate Arrays
**Problem**: `coord` exists but is empty `[]`
**Solution**: Check size before accessing
```kotlin
coord?.takeIf { it.size >= 2 }?.let { coords ->
    LatLng(coords[0], coords[1])
}
```

### Issue 4: Parent Coordinates Missing
**Problem**: `coord` is null but `parent.coord` has data
**Solution**: Use fallback pattern
```kotlin
val coordinate = coord ?: parent?.coord
```

## 🎉 Benefits After Update

1. ✅ **No More Extra API Calls**: All coordinates in one response
2. ✅ **Accurate Locations**: Use official coordinate data
3. ✅ **Platform-Level Precision**: Distinguish between platforms at same station
4. ✅ **Real-Time Map Support**: Can show journey progress on map
5. ✅ **Walking Path Visualization**: Combined with interchange.coords
6. ✅ **Backward Compatible**: Existing code keeps working

## 🚀 Next Steps After Model Update

Once the model is updated and tested:

1. Create `JourneyMapState.kt` state models
2. Create `JourneyMapMapper.kt` to extract coordinates
3. Build map visualization with MapLibre
4. Integrate with journey detail screens

---

**Estimated Time**: 30-60 minutes for model update + testing
**Priority**: HIGH - This unblocks all map visualization work
**Risk**: LOW - All fields are nullable with defaults
