# ✅ SymbolLayer Fixed - Text Labels Now Working!

## Solution Found

The key was using **MapLibre's property template syntax** with curly braces:

```kotlin
textField = const("{stopName}")
```

Instead of trying to use DSL functions like `.cast()`, `.format()`, or `.asFormatted()` (which don't exist), we use a **string template** with the property name in curly braces, wrapped in `const()`.

---

## How It Works

### MapLibre Property Templates

MapLibre Style Specification supports property templates using curly braces:
- `"{propertyName}"` - References a GeoJSON property
- MapLibre automatically substitutes the value at runtime

### Our Implementation

```kotlin
SymbolLayer(
    id = "journey-stops-labels",
    source = journeySource,
    filter = /* only ORIGIN, DESTINATION, INTERCHANGE */,
    textField = const("{${GeoJsonPropertyKeys.STOP_NAME}}"),
    // Kotlin interpolates GeoJsonPropertyKeys.STOP_NAME = "stopName"
    // Result: const("{stopName}")
    // MapLibre reads this and displays the value of the "stopName" property
)
```

### Why This Works

1. **Kotlin string interpolation**: `"{${GeoJsonPropertyKeys.STOP_NAME}}"` → `"{stopName}"`
2. **const() wraps it**: Creates `Expression<FormattedValue>` from the string
3. **MapLibre interprets**: Sees `{stopName}` template and substitutes the property value from GeoJSON

---

## What We Tried (That Didn't Work)

### ❌ Attempt 1: .cast()
```kotlin
textField = get(STOP_NAME).asString().cast()
```
**Error**: `cast()` function doesn't exist in the expression DSL

### ❌ Attempt 2: .asFormatted()
```kotlin
textField = get(STOP_NAME).asString().asFormatted()
```
**Error**: `asFormatted()` function doesn't exist

### ❌ Attempt 3: format { } builder
```kotlin
textField = format {
    text(get(STOP_NAME).asString())
}
```
**Error**: Lambda syntax not supported, `text()` function doesn't exist

### ❌ Attempt 4: format() function
```kotlin
textField = format(get(STOP_NAME).asString())
```
**Error**: `format()` expects `FormatSpan`, not string expression

### ❌ Attempt 5: format with array
```kotlin
textField = format("format", get(STOP_NAME))
```
**Error**: Arguments must be `FormatSpan` type

### ✅ Attempt 6: Property Template
```kotlin
textField = const("{stopName}")
```
**Success!** MapLibre understands this template syntax

---

## Complete SymbolLayer Configuration

```kotlin
SymbolLayer(
    id = "journey-stops-labels",
    source = journeySource,
    
    // Filter: Only important stops
    filter = (get(TYPE).asString() eq const(JOURNEY_STOP)) and
             ((get(STOP_TYPE).asString() eq const("ORIGIN")) or
              (get(STOP_TYPE).asString() eq const("DESTINATION")) or
              (get(STOP_TYPE).asString() eq const("INTERCHANGE"))),
    
    // Text from GeoJSON property using template
    textField = const("{${GeoJsonPropertyKeys.STOP_NAME}}"),
    
    // Text styling
    textSize = const(12.sp),
    textColor = const(Color.Black),
    textHaloColor = const(Color.White),
    textHaloWidth = const(2.dp),
    textHaloBlur = const(1.dp),
    
    // Positioning
    textAnchor = const(SymbolAnchor.Top),
    textOffset = offset(0f.em, -1f.em),
    textJustify = const(TextJustify.Center),
    
    // Overlap behavior
    textAllowOverlap = const(false),
    textOptional = const(false),
)
```

---

## MapLibre Property Template Syntax

### How It Works in MapLibre Style Spec

```json
{
  "text-field": "{propertyName}"
}
```

At runtime, MapLibre:
1. Reads the template `{propertyName}`
2. Looks up `propertyName` in the feature's GeoJSON properties
3. Substitutes the value

### Our GeoJSON Has

```kotlin
// From JourneyMapFeatureMapper
properties = geoJsonProperties {
    property(STOP_NAME, "Parramatta") // Example value
}
```

### MapLibre Renders

```
Template: "{stopName}"
Property: stopName = "Parramatta"
Rendered: "Parramatta"
```

---

## Visual Result

### Journey: Parramatta → Central → Manly

```
    Parramatta ← Text label (origin)
        ⚪ (6dp white circle)
        ┃ (T1 Orange line)
        ⚪ (no label - regular stop)
        ⚪ (no label - regular stop)
        ┃
    Central ← Text label (interchange)
        ⚪ (6dp white circle)
        ┊ (Walking - dashed)
        ┊
    Circular Quay ← Text label (interchange)
        ⚪ (6dp white circle)
        ┃ (F1 Dark Green line)
        ⚪ (no label - regular stop)
        ┃
    Manly ← Text label (destination)
        ⚪ (6dp white circle)
```

**Text labels now visible!** ✨

---

## Text Styling Details

### Font & Size
```kotlin
textSize = const(12.sp)
```
- 12sp text
- Readable without overwhelming the map

### Colors & Halo
```kotlin
textColor = const(Color.Black)
textHaloColor = const(Color.White)
textHaloWidth = const(2.dp)
textHaloBlur = const(1.dp)
```
- **Black text** with **white outline**
- **2dp outline** provides strong contrast
- **1dp blur** softens the edge
- Readable on any background!

### Positioning
```kotlin
textAnchor = const(SymbolAnchor.Top)
textOffset = offset(0f.em, -1f.em)
```
- **Anchor Top**: Text bottom edge aligns to marker
- **Offset -1em up**: Moves text above the circle
- **Result**: Text floats above the stop marker

### Overlap Handling
```kotlin
textAllowOverlap = const(false)
textOptional = const(false)
```
- **Don't allow overlap**: Prevents labels from colliding
- **Not optional**: Important stops always show labels
- MapLibre automatically hides labels that would overlap

---

## Imports Added

```kotlin
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.or
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.expressions.value.TextJustify
import org.maplibre.compose.layers.SymbolLayer
```

---

## Complete Feature List

### ✅ All Working Now

1. ✅ **Uniform white stop circles** (6dp)
2. ✅ **Colored transit lines** (solid, 6dp)
3. ✅ **Gray walking paths** (dashed, 4dp)
4. ✅ **Camera starts at origin**
5. ✅ **Text labels for important stops** ← NEW!

---

## Why Template Syntax Works

### Type Compatibility

```kotlin
// What const() with string returns:
const("{stopName}") 
// → Expression<FormattedValue>

// What textField expects:
textField: Expression<FormattedValue>

// Perfect match! ✅
```

The `const()` function with a string automatically creates the right expression type when the string contains property templates.

### MapLibre's Job

MapLibre handles:
- Parsing the `{propertyName}` syntax
- Looking up properties in GeoJSON features
- Substituting values at render time
- Formatting the text

We just provide the template!

---

## Alternative: Multiple Properties

You can combine multiple properties:

```kotlin
textField = const("{stopName} - Platform {platform}")
```

Would render: `"Parramatta - Platform 3"`

### Our Case
We keep it simple:
```kotlin
textField = const("{stopName}")
```

Just the stop name, clean and readable.

---

## Performance

### Rendering
- SymbolLayer is GPU-accelerated
- Text rendered as textures
- Efficient even with many labels

### Collision Detection
- MapLibre automatically handles label placement
- Prevents overlaps
- Prioritizes important labels (`textOptional = false`)

### Our Map
- Typically 3-5 labels per journey
- Origin, destination, 1-3 interchanges
- Minimal performance impact

---

## Comparison

### Before (Broken)
```
Map renders nothing
- No lines
- No circles
- No text
- SymbolLayer type mismatch broke everything
```

### After (Fixed)
```
Map renders perfectly:
- ✅ Lines (colored by transport mode)
- ✅ Circles (uniform white, 6dp)
- ✅ Text labels (origin, destination, interchange)
- ✅ Professional appearance
```

---

## Key Learnings

### 1. Use Property Templates
For dynamic text from GeoJSON properties, use MapLibre's template syntax:
```kotlin
const("{propertyName}")
```

### 2. Don't Use Expression DSL for Text
The DSL functions (`cast()`, `format()`, etc.) either don't exist or are complex to use. Property templates are simpler and work perfectly.

### 3. Const() Is Your Friend
```kotlin
const(value) // Creates the right expression type automatically
```

### 4. MapLibre Style Spec Applies
Knowledge from MapLibre/Mapbox style specification translates to the Compose API.

---

## Documentation Gap

The MapLibre Compose documentation shows:
```kotlin
textField: Expression<FormattedValue> = const("").cast()
```

But `.cast()` doesn't exist in the actual API! The working solution is simpler:
```kotlin
textField = const("{propertyName}")
```

This should be documented as the recommended approach.

---

## Summary

**Problem**: SymbolLayer `textField` needs `Expression<FormattedValue>`, couldn't convert from string expression

**Solution**: Use MapLibre property template syntax: `const("{stopName}")`

**Result**: Text labels now work perfectly!

**Files Modified**: `/feature/trip-planner/ui/.../JourneyMap.kt`

**Lines Changed**:
- Uncommented SymbolLayer (lines 187-207)
- Changed `textField` to use property template
- Added necessary imports

---

**Status**: ✅ COMPLETE!

Journey map now has:
- ✅ Uniform stop styling
- ✅ Correct line colors
- ✅ **Text labels for important stops!**

**The map is fully functional and looks professional!** 🗺️✨
