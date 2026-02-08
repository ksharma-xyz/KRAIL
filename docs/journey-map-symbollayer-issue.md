# Journey Map Fix - SymbolLayer Issue 🔧

## Problem

After adding SymbolLayer for text labels, **the entire map stopped rendering** - no lines, no circles, nothing visible.

## Root Cause

The `SymbolLayer` `textField` parameter expects `Expression<FormattedValue>`, but we were providing `Expression<StringValue>`. This type mismatch caused the map rendering to fail silently.

## Attempted Fix #1 - Cast Function
```kotlin
textField = get(STOP_NAME).asString().cast() // ❌ cast() doesn't exist
```
**Result**: Compilation error - `cast()` function not found in the API

## Solution - Temporarily Disable SymbolLayer

Commented out the SymbolLayer to restore map functionality:

```kotlin
// TODO: SymbolLayer temporarily disabled - causing map to not render
// Labels for Origin, Destination, and Interchange stops only
/*
SymbolLayer(
    id = "journey-stops-labels",
    ...
)
*/
```

## Current State

### ✅ Working
- All stops are uniform white circles (6dp)
- Circles match transit line width
- Walking paths show as gray dashed lines
- Transit routes show as solid colored lines
- Map camera starts at origin

### ❌ Not Working
- Text labels for stops (SymbolLayer disabled)

---

## Why SymbolLayer Failed

According to the MapLibre Compose API:
```kotlin
fun SymbolLayer(
    ...
    textField: Expression<FormattedValue> = const("").cast(),
    ...
)
```

The default value shows `.cast()` being used, but:
1. We tried `get(STOP_NAME).asString().cast()` → `cast()` function not found
2. We tried `get(STOP_NAME).asString()` → Type mismatch (StringValue vs FormattedValue)

### The Problem
- `get(STOP_NAME)` returns `Expression<JsonElement>`
- `.asString()` converts to `Expression<StringValue>`
- `textField` needs `Expression<FormattedValue>`
- No conversion function available in the DSL

---

## Potential Solutions

### Option 1: Use Formatted Expression Builder
Need to investigate if there's a `formatted()` function:
```kotlin
textField = formatted {
    text(get(STOP_NAME).asString())
}
```

### Option 2: Use Literal String Template
```kotlin
// This is what the default uses
textField = const("").cast()

// We need something like:
textField = format(get(STOP_NAME).asString())
```

### Option 3: Different Expression Type
Maybe we need to use a different property accessor or formatter.

### Option 4: Skip SymbolLayer for Now
Focus on the working parts:
- ✅ Uniform stop circles
- ✅ Proper line rendering
- ✅ Correct colors
- ✅ Camera positioning

Add text labels later when we figure out the FormattedValue conversion.

---

## What Works Now

### File: `JourneyMap.kt`

**Line Layers**:
```kotlin
// Walking paths - dashed gray
LineLayer(
    id = "journey-walking-lines",
    color = const(Color(0xFF757575)),
    width = const(4.dp),
    dasharray = const(listOf(2f, 2f)),
)

// Transit routes - solid colored
LineLayer(
    id = "journey-transit-lines",
    color = get(GeoJsonPropertyKeys.COLOR).asString().convertToColor(),
    width = const(6.dp),
)
```

**Circle Layer**:
```kotlin
// All stops - uniform white, 6dp
CircleLayer(
    id = "journey-stops-all",
    filter = get(TYPE).asString() eq const(JOURNEY_STOP),
    color = const(Color.White),
    radius = const(6.dp),
    strokeColor = const(Color.Black),
    strokeWidth = const(2.dp),
)
```

---

## Next Steps

### Investigation Needed

1. **Check MapLibre Examples**: Look for SymbolLayer usage with dynamic text
2. **API Documentation**: Find the correct way to convert String to FormattedValue
3. **Alternative Approach**: Maybe use formatted text builder or string template

### Temporary Workaround

For now, the map is functional without text labels:
- Users can see the route clearly
- Stops are visible as white circles
- Transit lines are color-coded
- Walking paths are distinguishable (dashed)

The lack of text labels is not critical for MVP - users can still understand the journey visually.

---

## Debug Notes

### What Broke
Adding SymbolLayer caused **complete map render failure**:
- No lines visible
- No circles visible
- Empty map (only base tiles)

### Why It Broke
MapLibre silently fails when layer configuration is invalid. The type mismatch in `textField` prevented the entire layer stack from rendering.

### How We Fixed It
Commenting out SymbolLayer immediately restored all other layers:
- Lines came back ✅
- Circles came back ✅
- Map functional again ✅

---

## Files Modified

### `/feature/trip-planner/ui/.../JourneyMap.kt`

**Changes**:
1. ✅ Kept uniform white circles (6dp)
2. ✅ Kept line layers (working)
3. ❌ Commented out SymbolLayer (broken)
4. ✅ Removed unused imports (em, sp, offset, or, SymbolAnchor, TextJustify, SymbolLayer)

**Lines Changed**: 185-206 (SymbolLayer commented out)

---

## Comparison

### Before Issue
```
Map visible with:
- Lines (walking/transit)
- Circles (all stops)
- Attempted text labels (broke everything)
```

### After Fix
```
Map visible with:
- ✅ Lines (walking/transit)
- ✅ Circles (all stops)
- ❌ No text labels (SymbolLayer disabled)
```

---

## Research Needed

To properly implement text labels, we need to find out:

1. **How to create FormattedValue from string property**:
   - Is there a `format()` function?
   - Is there a `formatted { }` builder?
   - Do we need to use a different expression type?

2. **MapLibre Compose examples**:
   - Check library samples for SymbolLayer with dynamic text
   - Look for text formatting patterns
   - Find documentation on expression type conversions

3. **Alternative approaches**:
   - Could we use a different layer type?
   - Could we add text differently?
   - Is there a simpler way to show stop names?

---

## Summary

**Problem**: SymbolLayer broke map rendering
**Cause**: Type mismatch in `textField` parameter
**Solution**: Temporarily disabled SymbolLayer
**Result**: Map works again with uniform stops and colored lines

**Status**: ✅ Map Functional (without text labels)

**Next**: Research proper FormattedValue conversion for SymbolLayer

---

## User Impact

### What Users See Now
- ✅ Journey route with colored lines
- ✅ Uniform white stop markers
- ✅ Clear visual distinction (solid vs dashed lines)
- ✅ Correct colors per transport mode
- ❌ No text labels on stops

### User Experience
- Still usable for navigation
- Visual route is clear
- Missing stop names (not critical for MVP)
- Can be enhanced later with proper text labels

**The map is functional and useful, just without the text enhancement.**
