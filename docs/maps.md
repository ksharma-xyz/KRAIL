Below is a **clean, copy-paste ready Markdown (`.md`) document** you can drop into your repo (e.g. `docs/maplibre-routes.md`).
It’s written like **internal developer docs / instructions**, not a blog post.

---

# MapLibre Compose — Routes, Stops & Interactive Markers

This document explains how to implement interactive transport routes and stops using **MapLibre Compose**.

We will cover:

1. Drawing a **route line** with **circle dots (stops)** in between  
2. Displaying **many stops (≈50)** with colored or custom markers  
3. Showing **multiple connected routes** (e.g. Train → Bus) with different colors  
4. Handling **click interactions** to display additional information  

---

## Prerequisites

- MapLibre Compose `v0.12+`
- Basic knowledge of Jetpack Compose
- GeoJSON concepts (Point, LineString)

---

## Core Concepts (Important)

MapLibre rendering is based on **sources** and **layers**:

- **Source** → Provides data (GeoJSON)
- **Layer** → Defines how data is rendered (line, circle, symbol)
- **Compose UI** → Used for overlays, sheets, dialogs (not map drawing)

> ❗ Compose UI elements cannot be drawn directly inside map layers  
> Use **CircleLayer / LineLayer** for performance  
> Use **ViewAnnotation** for custom UI markers

---

# 1. Single Route with Circle Stops (Clickable)

### Goal
- Draw a colored route line (10.dp)
- Show white circle dots along the route
- When a dot is clicked, show additional info (e.g. Stop name)

---

## 1.1 Route Line Source

```kotlin
val routeSource = rememberGeoJsonSource(
    GeoJsonData.Features(
        listOf(
            Feature.fromGeometry(
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(151.21, -33.86),
                        Point.fromLngLat(151.23, -33.87),
                        Point.fromLngLat(151.25, -33.88),
                    )
                )
            )
        )
    )
)
````

---

## 1.2 Route Line Layer

```kotlin
LineLayer(
    id = "route-line",
    source = routeSource,
    color = const(Color(0xFF1E88E5)),
    width = const(10.dp),
    cap = const(LineCap.Round),
    join = const(LineJoin.Round),
)
```

---

## 1.3 Stops Source (Circle Dots)

Each stop is a **Point feature** with metadata.

```kotlin
val stopsSource = rememberGeoJsonSource(
    GeoJsonData.Features(
        listOf(
            Feature.fromGeometry(Point.fromLngLat(151.21, -33.86))
                .apply { addStringProperty("stopName", "Stop A") },

            Feature.fromGeometry(Point.fromLngLat(151.23, -33.87))
                .apply { addStringProperty("stopName", "Stop B") },

            Feature.fromGeometry(Point.fromLngLat(151.25, -33.88))
                .apply { addStringProperty("stopName", "Stop C") },
        )
    )
)
```

---

## 1.4 Circle Layer with Click Handling

```kotlin
CircleLayer(
    id = "route-stops",
    source = stopsSource,
    radius = const(6.dp),
    color = const(Color.White),
    strokeColor = const(Color(0xFF1E88E5)),
    strokeWidth = const(2.dp),
    onClick = { features ->
        val stopName = features.first()
            .getStringProperty("stopName")

        // Trigger UI state update
        println("Clicked stop: $stopName")
        ClickResult.Consume
    }
)
```

---

## 1.5 Showing Additional Info (Compose UI)

Use normal Compose state to show details.

```kotlin
var selectedStop by remember { mutableStateOf<String?>(null) }
```

```kotlin
onClick = { features ->
    selectedStop = features.first().getStringProperty("stopName")
    ClickResult.Consume
}
```

```kotlin
selectedStop?.let {
    Text(
        text = it,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .background(Color.Black)
            .padding(12.dp),
        color = Color.White
    )
}
```

---

# 2. Many Stops (≈50) with Colored or Custom Markers

### Goal

* Display many stops efficiently
* Each stop is clickable
* Optional: custom UI marker

---

## 2.1 Efficient Stops (Recommended)

For 50+ stops, always prefer **CircleLayer**.

```kotlin
CircleLayer(
    id = "all-stops",
    source = stopsSource,
    radius = const(5.dp),
    color = const(Color(0xFF4CAF50)),
    strokeColor = const(Color.White),
    strokeWidth = const(1.dp),
)
```

✔ Fast
✔ GPU rendered
✔ Scales to hundreds of markers

---

## 2.2 Custom Marker (ViewAnnotation)

Use **only for selected or highlighted stops**.

```kotlin
ViewAnnotation(
    key = "selected-stop",
    point = Point.fromLngLat(151.23, -33.87)
) {
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("Central Station", color = Color.Black)
    }
}
```

⚠ Do **not** use ViewAnnotations for all 50 stops

---

## 2.3 Recommended Hybrid Pattern

| Purpose       | Technique            |
| ------------- | -------------------- |
| All stops     | `CircleLayer`        |
| Selected stop | `ViewAnnotation`     |
| Stop details  | BottomSheet / Dialog |

---

# 3. Multiple Routes (Train → Bus → Ferry)

### Goal

* Multiple connected routes
* Different colors per transport mode
* White stop circles on each route

Example:

* **T1 Train** (Blue)
* **Bus** (Green)
* **Ferry** (Orange)

---

## 3.1 Separate Sources per Route

```kotlin
val trainRouteSource = rememberGeoJsonSource(...)
val busRouteSource = rememberGeoJsonSource(...)
```

---

## 3.2 Multiple Line Layers

```kotlin
LineLayer(
    id = "t1-train-line",
    source = trainRouteSource,
    color = const(Color(0xFF1565C0)),
    width = const(10.dp),
)

LineLayer(
    id = "bus-line",
    source = busRouteSource,
    color = const(Color(0xFF2E7D32)),
    width = const(8.dp),
)
```

---

## 3.3 Stops Per Route

```kotlin
CircleLayer(
    id = "train-stops",
    source = trainStopsSource,
    color = const(Color.White),
    strokeColor = const(Color(0xFF1565C0)),
)

CircleLayer(
    id = "bus-stops",
    source = busStopsSource,
    color = const(Color.White),
    strokeColor = const(Color(0xFF2E7D32)),
)
```

---

## 3.4 Transfer Stops (Shared)

At interchange points (e.g. Train → Bus):

```kotlin
CircleLayer(
    id = "transfer-stop",
    source = transferStopsSource,
    radius = const(8.dp),
    color = const(Color.White),
    strokeColor = const(Color.Black),
    strokeWidth = const(3.dp),
)
```

---

## Recommended Layer Order

```text
Base map
↓
Route lines
↓
Route stops
↓
Transfer stops
↓
Selected stop ViewAnnotation
```

Use `Anchor.Above("road_motorway")` if needed.

---

# Best Practices Summary

✔ Use **LineLayer** for routes
✔ Use **CircleLayer** for stops
✔ Use **ViewAnnotation** only for highlighted items
✔ Store metadata in GeoJSON properties
✔ Handle UI in Compose, not in layers

---

## Mental Model

> **MapLibre layers draw the map**
> **Compose shows information about the map**

This separation keeps your app fast, scalable, and maintainable.

---

## Next Extensions

* Zoom-based stop visibility
* Animated selected stop
* Route filtering
* Accessibility focus on selected stops

---
