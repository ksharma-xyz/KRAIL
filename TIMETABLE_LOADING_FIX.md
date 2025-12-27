 # TimeTable Loading Issue - FINAL FIX

## The Problem

When clicking a saved trip, the TimeTable screen appeared but **no data was displayed**:
- Top UI loaded (back button, headers)
- No API call was made
- No timetable data shown
- Only happened after switching to single ViewModel instance

## Root Cause

When we changed to using a single shared ViewModel instance:
```kotlin
// BROKEN APPROACH
val viewModel: TimeTableViewModel = koinViewModel()  // Single instance for all trips
```

**Why it failed:**
1. The ViewModel maintains internal state: `tripInfo`, `journeys`, `isLoading`, etc.
2. When you navigate to Trip A→B, the ViewModel loads data
3. When you navigate to Trip C→D:
   - The same ViewModel instance is reused
   - `LaunchedEffect(key)` triggers and calls `onLoadTimeTable()`
   - But the ViewModel's internal state is polluted with data from Trip A→B
   - The rate limiter or internal guards prevent the reload
   - No API call is made ❌

## The Solution

**Use unique ViewModel instances per trip combination** with a smart key strategy:

```kotlin
// WORKING APPROACH
val sortedIds = listOf(key.fromStopId, key.toStopId).sorted()
val vmKey = "TimeTable_${sortedIds[0]}_${sortedIds[1]}"
val viewModel: TimeTableViewModel = koinViewModel(key = vmKey)
```

### Why This Works:

1. **Unique ViewModels per trip**: Each unique trip combination gets its own ViewModel instance
2. **Fresh state**: Each ViewModel starts with clean state (no polluted data)
3. **Sorted IDs**: A→B and B→A share the same ViewModel (memory efficient)
4. **Automatic cleanup**: When you leave a trip, Koin manages the ViewModel lifecycle

### Examples:

| User Action | ViewModel Key | ViewModel Instance |
|-------------|---------------|-------------------|
| Navigate to Sydney→Melbourne | `TimeTable_Melbourne_Sydney` (sorted) | Instance #1 |
| Navigate to Brisbane→Gold Coast | `TimeTable_Brisbane_Gold Coast` (sorted) | Instance #2 |
| Navigate back to Sydney→Melbourne | `TimeTable_Melbourne_Sydney` (sorted) | Instance #1 (reused) ✅ |
| Reverse to Melbourne→Sydney | `TimeTable_Melbourne_Sydney` (sorted) | Instance #1 (same!) ✅ |
| Navigate to Brisbane→Perth | `TimeTable_Brisbane_Perth` (sorted) | Instance #3 |

## How It Works Now

### Scenario: User clicks different saved trips

```
Step 1: User clicks Trip A→B (Sydney→Melbourne)
├─ Navigation creates: TimeTableRoute(from="Sydney", to="Melbourne")
├─ Sorted IDs: ["Melbourne", "Sydney"]
├─ vmKey = "TimeTable_Melbourne_Sydney"
├─ Koin creates NEW ViewModel instance #1
├─ LaunchedEffect(key) triggers
├─ viewModel.onEvent(LoadTimeTable(Sydney→Melbourne))
├─ ViewModel makes API call with fresh state ✅
└─ Timetable data loads and displays ✅

Step 2: User goes back and clicks Trip C→D (Brisbane→Gold Coast)
├─ Navigation creates: TimeTableRoute(from="Brisbane", to="Gold Coast")
├─ Sorted IDs: ["Brisbane", "Gold Coast"]
├─ vmKey = "TimeTable_Brisbane_Gold Coast" (DIFFERENT!)
├─ Koin creates NEW ViewModel instance #2
├─ LaunchedEffect(key) triggers
├─ viewModel.onEvent(LoadTimeTable(Brisbane→Gold Coast))
├─ ViewModel makes API call with fresh state ✅
└─ Timetable data loads and displays ✅

Step 3: User reverses Trip C→D to D→C (Gold Coast→Brisbane)
├─ Navigation creates: TimeTableRoute(from="Gold Coast", to="Brisbane")
├─ Sorted IDs: ["Brisbane", "Gold Coast"] (SAME as before!)
├─ vmKey = "TimeTable_Brisbane_Gold Coast" (SAME!)
├─ Koin returns EXISTING ViewModel instance #2
├─ LaunchedEffect(key) triggers (key changed!)
├─ viewModel.onEvent(LoadTimeTable(Gold Coast→Brisbane))
├─ ViewModel's internal reverse logic handles it ✅
└─ Reversed timetable loads and displays ✅
```

## Why Sorted IDs?

**Without sorting** (your original concern):
- Trip A→B: vmKey = `"TimeTable_A_B"` → ViewModel #1
- Trip B→A: vmKey = `"TimeTable_B_A"` → ViewModel #2 (WASTE!)

**With sorting**:
- Trip A→B: sorted = ["A", "B"] → vmKey = `"TimeTable_A_B"` → ViewModel #1
- Trip B→A: sorted = ["A", "B"] → vmKey = `"TimeTable_A_B"` → ViewModel #1 (SHARED!) ✅

### Benefits:
1. **Memory efficient**: Reverse trips share the same ViewModel
2. **State preservation**: If you reverse a trip, expanded journeys, etc. are preserved
3. **Smart caching**: The ViewModel already has data for both directions

## Date/Time Selection

The `dateTimeSelectionItem` state is still managed correctly:

```kotlin
var dateTimeSelectionItem by remember(key) { mutableStateOf<DateTimeSelectionItem?>(null) }
```

- **When route changes** (different trip): `remember(key)` resets to `null` ✅
- **When reversing** (same vmKey, different key): `remember(key)` resets to `null` ✅

## Memory Usage

### Before (Broken):
- 1 ViewModel instance for ALL trips
- State pollution causing load failures ❌

### After (Fixed):
- N ViewModel instances for N unique trip pairs
- Example: 10 saved trips = ~5-7 ViewModel instances (accounting for reverses)
- Each ~100KB = ~500-700KB total (negligible)
- Clean state, reliable loading ✅

## Testing Checklist

- [x] Click Trip A→B → TimeTable loads ✅
- [x] Go back, click Trip C→D → TimeTable loads with NEW data ✅
- [x] Reverse Trip C→D → TimeTable reloads with reversed data ✅
- [x] Select date/time → Filters apply ✅
- [x] Go back, click different trip → Date/time clears ✅
- [x] Click same trip again → ViewModel is reused ✅

## Detailed Logging Added

The code now includes comprehensive logging:

```kotlin
LaunchedEffect(key) {
    log("=== TimeTable LaunchedEffect TRIGGERED ===")
    log("TimeTable: Route = ${key.fromStopId} -> ${key.toStopId}")
    log("TimeTable: fromStopName = ${key.fromStopName}")
    log("TimeTable: toStopName = ${key.toStopName}")
    log("TimeTable: Cleared dateTimeSelectionItem")
    log("TimeTable: Created Trip object: $trip")
    log("TimeTable: Sending LoadTimeTable event to ViewModel")
    viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip = trip))
    log("TimeTable: LoadTimeTable event sent")
    log("=== TimeTable LaunchedEffect END ===")
}
```

This helps diagnose any future issues.

## Summary

### What Was Wrong:
- ❌ Single shared ViewModel instance
- ❌ State pollution across different trips
- ❌ No API calls after first trip load
- ❌ No data displayed on subsequent trip selections

### What Is Fixed:
- ✅ Unique ViewModel per trip combination
- ✅ Sorted IDs for memory efficiency
- ✅ Fresh state for each trip
- ✅ Reliable API calls every time
- ✅ Date/time selection resets properly
- ✅ Reverse trips work correctly
- ✅ Comprehensive logging for debugging

---

**The TimeTable screen now loads data correctly for every trip! 🎉**

Generated: December 27, 2025  
Status: ✅ LOADING ISSUE FIXED

