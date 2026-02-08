# Journey Map Stop Styling & Text Labels ✨

## Summary

Implemented uniform stop styling and added text labels for important stops using MapLibre's **SymbolLayer**.

---

## Requirements Implemented

### 1. ✅ All Stops Same White Color
**Before**: Different colors for different stop types:
- Origin: Green (#4CAF50)
- Destination: Red (#F44336)
- Interchange: Yellow (#FFC107)
- Regular: White

**After**: All stops uniform white with black stroke

### 2. ✅ Circle Size Matches Line Width
**Before**: Different sizes (8dp, 10dp, 12dp)
**After**: All circles 6dp (matches transit line width)

### 3. ✅ Text Labels for Important Stops
Added **SymbolLayer** to show stop names for:
- ✅ Origin stops
- ✅ Destination stops
- ✅ Interchange stops
- ❌ Regular stops (no label to avoid clutter)

---

## Implementation Details

### File Modified
`/feature/trip-planner/ui/.../JourneyMap.kt`

### New Imports Added
```kotlin
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.or
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.expressions.value.TextJustify
import org.maplibre.compose.layers.SymbolLayer
```

### CircleLayer Changes

**Before** (4 separate layers):
```kotlin
// Regular stops - white, 8dp
CircleLayer(id = "journey-stops-regular", ...)

// Interchange stops - yellow, 10dp
CircleLayer(id = "journey-stops-interchange", ...)

// Origin stop - green, 12dp
CircleLayer(id = "journey-stops-origin", ...)

// Destination stop - red, 12dp
CircleLayer(id = "journey-stops-destination", ...)
```

**After** (1 unified layer):
```kotlin
// All stops - uniform white circles matching line width (6dp)
CircleLayer(
    id = "journey-stops-all",
    source = journeySource,
    filter = get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP),
    color = const(Color.White),
    radius = const(6.dp), // Same as transit line width
    strokeColor = const(Color.Black),
    strokeWidth = const(2.dp),
)
```

### SymbolLayer for Text Labels

**New layer**:
```kotlin
SymbolLayer(
    id = "journey-stops-labels",
    source = journeySource,
    
    // Filter: Only show labels for Origin, Destination, and Interchange
    filter = (get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP)) and
             ((get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const("ORIGIN")) or
              (get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const("DESTINATION")) or
              (get(GeoJsonPropertyKeys.STOP_TYPE).asString() eq const("INTERCHANGE"))),
    
    // Text content from GeoJSON properties
    textField = get(GeoJsonPropertyKeys.STOP_NAME).asString(),
    
    // Text styling
    textSize = const(12.sp),
    textColor = const(Color.Black),
    textHaloColor = const(Color.White),
    textHaloWidth = const(2.dp),
    textHaloBlur = const(1.dp),
    
    // Positioning
    textAnchor = const(SymbolAnchor.Top), // Position text above circle
    textOffset = offset(0f.em, -1f.em),   // Offset upward from circle
    textJustify = const(TextJustify.Center),
    
    // Overlap behavior
    textAllowOverlap = const(false), // Prevent text overlap
    textOptional = const(false),     // Always show text for important stops
)
```

---

## Visual Result

### Before
```
Map View:
  🟢 Parramatta (large green circle, no text)
     ┃
  ⚪ Westmead (small white circle, no text)
  ⚪ Harris Park (small white circle, no text)
     ┃
  🟡 Central (medium yellow circle, no text)
     ┃
  🔴 Town Hall (large red circle, no text)
```

### After
```
Map View:
  Parramatta ← Text label above
      ⚪ (6dp white circle)
      ┃
      ⚪ Westmead (no text - regular stop)
      ⚪ Harris Park (no text - regular stop)
      ┃
  Central ← Text label above
      ⚪ (6dp white circle)
      ┃
  Town Hall ← Text label above
      ⚪ (6dp white circle)
```

All circles are now **uniform white, 6dp radius**, matching the transit line width.

---

## SymbolLayer Features Used

### Text Content
```kotlin
textField = get(GeoJsonPropertyKeys.STOP_NAME).asString()
```
Gets stop name from GeoJSON properties (already available from `JourneyStopFeature.stopName`)

### Text Styling
| Property | Value | Purpose |
|----------|-------|---------|
| `textSize` | 12.sp | Readable size without overwhelming map |
| `textColor` | Black | High contrast on light backgrounds |
| `textHaloColor` | White | Creates outline around text |
| `textHaloWidth` | 2.dp | Thick enough to be visible |
| `textHaloBlur` | 1.dp | Smooth halo edge |

### Text Positioning
| Property | Value | Purpose |
|----------|-------|---------|
| `textAnchor` | `SymbolAnchor.Top` | Anchors text at top edge |
| `textOffset` | `(0, -1em)` | Moves text up by 1 em unit |
| `textJustify` | `TextJustify.Center` | Centers text horizontally |

The combination of `textAnchor.Top` and negative offset places the text **above** the circle.

### Overlap Handling
| Property | Value | Purpose |
|----------|-------|---------|
| `textAllowOverlap` | false | Prevents overlapping labels |
| `textOptional` | false | Always shows important stop labels |

MapLibre will automatically hide labels if they would overlap, but important stops (origin/destination/interchange) are prioritized.

---

## Filtering Logic

### Circle Layer Filter
```kotlin
filter = get(GeoJsonPropertyKeys.TYPE).asString() eq const(GeoJsonFeatureTypes.JOURNEY_STOP)
```
**Shows**: All stops (origin, destination, interchange, regular)

### Symbol Layer Filter
```kotlin
filter = (get(TYPE).asString() eq const(JOURNEY_STOP)) and
         ((get(STOP_TYPE).asString() eq const("ORIGIN")) or
          (get(STOP_TYPE).asString() eq const("DESTINATION")) or
          (get(STOP_TYPE).asString() eq const("INTERCHANGE")))
```
**Shows**: Only origin, destination, and interchange stops
**Hides**: Regular stops (to avoid label clutter)

---

## GeoJSON Properties Used

These properties already exist in the GeoJSON feature collection:

```kotlin
// From JourneyMapFeatureMapper
properties = geoJsonProperties {
    property(GeoJsonPropertyKeys.TYPE, GeoJsonFeatureTypes.JOURNEY_STOP)
    property(GeoJsonPropertyKeys.STOP_ID, stopId)
    property(GeoJsonPropertyKeys.STOP_NAME, stopName)        // ← Used for text!
    property(GeoJsonPropertyKeys.STOP_TYPE, stopType.name)   // ← Used for filtering!
    propertyIfNotNull(GeoJsonPropertyKeys.TIME, time)
    propertyIfNotNull(GeoJsonPropertyKeys.PLATFORM, platform)
}
```

No changes needed to the mapper - the data is already there!

---

## Layer Rendering Order

MapLibre renders layers in the order they're declared:

```
1. Walking lines (dashed gray) - Bottom
2. Transit lines (solid colored) - Middle
3. Circle layer (white stops) - Above lines
4. Symbol layer (text labels) - Top
```

Text labels appear **on top** of everything else, ensuring they're always visible.

---

## Example Journey: Parramatta → Manly

### Route
1. Parramatta (origin)
2. Westmead (regular)
3. Harris Park (regular)
4. Central (interchange)
5. Wynyard (regular - walking)
6. Circular Quay (interchange)
7. Neutral Bay (regular)
8. Manly (destination)

### Map Display

```
      Parramatta ← Label
          ⚪
          ┃ (T1 Orange line)
          ┃
          ⚪ (no label)
          ⚪ (no label)
          ┃
      Central ← Label
          ⚪
          ┊ (Walking - dashed)
          ⚪ (no label)
          ┊
      Circular Quay ← Label
          ⚪
          ┃ (F1 Dark Green line)
          ┃
          ⚪ (no label)
          ┃
      Manly ← Label
          ⚪
```

**Labels shown**: 4 (Parramatta, Central, Circular Quay, Manly)
**Labels hidden**: 4 (Westmead, Harris Park, Wynyard, Neutral Bay)
**All circles**: Uniform white, 6dp

---

## Benefits

### 1. Cleaner Visual Design ✅
- All stops look uniform
- Less visual clutter
- Easier to follow the route line

### 2. Better Hierarchy ✅
- Important stops have labels
- Regular stops don't clutter the map
- Clear start and end points

### 3. Matches Line Width ✅
- 6dp circles match 6dp transit lines
- Visually consistent
- Professional appearance

### 4. Readable Labels ✅
- White halo provides contrast
- Positioned above circles (no overlap)
- Large enough to read easily

### 5. Smart Label Management ✅
- `textAllowOverlap = false` prevents overlap
- `textOptional = false` prioritizes important stops
- MapLibre handles collision detection automatically

---

## Comparison

### Before
| Stop Type | Color | Size | Label |
|-----------|-------|------|-------|
| Origin | 🟢 Green | 12dp | ❌ No |
| Destination | 🔴 Red | 12dp | ❌ No |
| Interchange | 🟡 Yellow | 10dp | ❌ No |
| Regular | ⚪ White | 8dp | ❌ No |

### After
| Stop Type | Color | Size | Label |
|-----------|-------|------|-------|
| Origin | ⚪ White | 6dp | ✅ **Yes** |
| Destination | ⚪ White | 6dp | ✅ **Yes** |
| Interchange | ⚪ White | 6dp | ✅ **Yes** |
| Regular | ⚪ White | 6dp | ❌ No |

---

## MapLibre SymbolLayer Capabilities

### What We Used
- ✅ `textField` - Dynamic text from properties
- ✅ `textSize`, `textColor` - Styling
- ✅ `textHaloColor`, `textHaloWidth`, `textHaloBlur` - Contrast/readability
- ✅ `textAnchor`, `textOffset` - Positioning
- ✅ `textAllowOverlap`, `textOptional` - Collision handling

### What's Also Available (Not Used)
- ❌ `iconImage` - Could use custom icons instead of circles
- ❌ `iconSize`, `iconRotate` - Icon manipulation
- ❌ `textTransform` - Uppercase/lowercase
- ❌ `textFont` - Custom fonts
- ❌ `textRotate` - Rotated text
- ❌ `textMaxWidth` - Text wrapping

We could enhance further by adding custom icons for origin/destination if needed!

---

## Performance Notes

### Layer Count
**Before**: 4 circle layers (1 per stop type)
**After**: 1 circle layer + 1 symbol layer

**Impact**: Slightly better performance (fewer layers)

### Label Rendering
- MapLibre handles label collision detection efficiently
- Only 3-5 labels per journey typically (origin, destination, 1-3 interchanges)
- Minimal performance impact

### Filter Complexity
```kotlin
// Circle: Simple filter
get(TYPE).asString() eq const(JOURNEY_STOP)

// Symbol: Compound filter with OR conditions
(get(TYPE) eq JOURNEY_STOP) and
    ((get(STOP_TYPE) eq "ORIGIN") or
     (get(STOP_TYPE) eq "DESTINATION") or
     (get(STOP_TYPE) eq "INTERCHANGE"))
```

**Impact**: Negligible - filters are evaluated on GPU

---

## Future Enhancements (Optional)

### 1. Custom Icons Instead of Circles
```kotlin
SymbolLayer(
    iconImage = case(
        get(STOP_TYPE) eq "ORIGIN" to const("icon-origin"),
        get(STOP_TYPE) eq "DESTINATION" to const("icon-destination"),
        get(STOP_TYPE) eq "INTERCHANGE" to const("icon-interchange"),
    )
)
```

### 2. Colored Text Based on Transport Mode
```kotlin
SymbolLayer(
    textColor = get(GeoJsonPropertyKeys.COLOR).asString().convertToColor(),
    // Would use the transport line color for text
)
```

### 3. Show Regular Stop Names on Zoom
```kotlin
SymbolLayer(
    minZoom = 14.0f, // Only show regular stops when zoomed in
    filter = get(STOP_TYPE) eq "REGULAR",
    textField = get(STOP_NAME).asString(),
)
```

---

## Testing Checklist

After Gradle sync:

- [ ] All stops are white circles
- [ ] All circles are same size (6dp)
- [ ] Origin stop has label above it
- [ ] Destination stop has label above it
- [ ] Interchange stops have labels
- [ ] Regular stops have NO labels
- [ ] Text is readable (white halo on black text)
- [ ] Text doesn't overlap circles
- [ ] Text doesn't overlap other text

---

## Summary

**Changes Made**:
1. ✅ Replaced 4 circle layers with 1 uniform layer
2. ✅ Changed all stops to white, 6dp (matching line width)
3. ✅ Added SymbolLayer for text labels
4. ✅ Text shows only for important stops (origin, destination, interchange)
5. ✅ Text positioned above circles with white halo for readability

**Result**: Cleaner map with uniform stops and helpful labels for navigation! 🗺️✨

**User Experience**: Users can now see where important stops are by name without clutter from every single stop label!
