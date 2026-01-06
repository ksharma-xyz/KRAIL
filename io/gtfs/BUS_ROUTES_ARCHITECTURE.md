# NSW Bus Routes Architecture

This document explains how NSW Bus Routes data flows through the KRAIL app, from protobuf files to UI display.

## Table of Contents
- [Overview](#overview)
- [Database Schema](#database-schema)
- [Data Flow](#data-flow)
- [Search Implementation](#search-implementation)
- [Performance Optimizations](#performance-optimizations)
- [How to Update Bus Routes Data](#how-to-update-bus-routes-data)

---

## Overview

The bus routes feature allows users to search for bus routes by route number (e.g., "702") and see all trips/directions for that route with their stops.

### Key Components:
- **Data Source**: Protobuf file (`NSW_BUSES_ROUTES.pb`)
- **Storage**: SQLite database via SQLDelight
- **Manager**: `NswBusRoutesManager` - handles data insertion
- **Search**: `RealStopResultsManager` - handles route search queries
- **UI**: Trip search results displayed in search screen

---

## Database Schema

### Table Relationships:

```
NswBusRouteGroups (Route "702")
    ↓ (1-to-many)
NswBusRouteVariants (Different networks operating "702")
    ↓ (1-to-many)
NswBusTripOptions (Specific trips/directions on route)
    ↓ (1-to-many)
NswBusTripStops (Ordered stops for each trip)
    ↓ (references)
NswStops (Stop details)
```

### Tables:

#### 1. NswBusRouteGroups
Groups routes by their short name (e.g., "702", "333", "M30")

| Column | Type | Description |
|--------|------|-------------|
| routeShortName | TEXT (PK) | Route number (e.g., "702") |

#### 2. NswBusRouteVariants
Different variants of the same route (e.g., different operators or networks)

| Column | Type | Description |
|--------|------|-------------|
| routeId | TEXT (PK) | Unique variant ID (e.g., "2504_702") |
| routeShortName | TEXT (FK) | References NswBusRouteGroups |
| routeName | TEXT | Full route name (e.g., "Sydney Buses Network 702") |

#### 3. NswBusTripOptions
Individual trips on a route (each has a unique direction/headsign)

| Column | Type | Description |
|--------|------|-------------|
| tripId | TEXT (PK) | **Unique identifier for this trip** |
| routeId | TEXT (FK) | References NswBusRouteVariants |
| headsign | TEXT | Direction/destination (e.g., "Blacktown to Parramatta") |

**Important:** Multiple trips can have the same headsign! This happens when:
- Same route runs at different times of day
- Express vs. Local services (same destination, different stops)
- Weekday vs. Weekend schedules

#### 4. NswBusTripStops
Ordered list of stops for each trip

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER (PK) | Auto-increment ID |
| tripId | TEXT (FK) | References NswBusTripOptions |
| stopId | TEXT (FK) | References NswStops |
| stopSequence | INTEGER | Order of stop (0, 1, 2, ...) |

---

## Data Flow

### Complete Journey: From routeShortName to UI Display

#### Example: User searches for "702"

```
┌─────────────────────────────────────────────────────────────────────┐
│ Step 1: Get all variants for route "702"                           │
└─────────────────────────────────────────────────────────────────────┘

Input: routeShortName = "702"
    ↓
Query: selectRouteVariantsByShortName("702")
    ↓
Result: List<NswBusRouteVariants>
[
  NswBusRouteVariants(
    routeId="2504_702",
    routeShortName="702",
    routeName="Sydney Buses Network 702"
  ),
  NswBusRouteVariants(
    routeId="2505_702",
    routeShortName="702",
    routeName="Another Network 702"
  )
]


┌─────────────────────────────────────────────────────────────────────┐
│ Step 2: Batch query - Get ALL trips for ALL variants               │
│         Performance Optimization: 1 query instead of N queries!     │
└─────────────────────────────────────────────────────────────────────┘

Extract route IDs: allRouteIds = ["2504_702", "2505_702"]
    ↓
Query: selectTripsByRouteIds(["2504_702", "2505_702"])  ✅ SINGLE QUERY!
    ↓
Result: List<NswBusTripOptions>
[
  NswBusTripOptions(
    tripId="trip_1",
    routeId="2504_702",
    headsign="Blacktown to Parramatta"
  ),
  NswBusTripOptions(
    tripId="trip_2",
    routeId="2504_702",
    headsign="Parramatta to Blacktown"
  ),
  NswBusTripOptions(
    tripId="trip_3",
    routeId="2504_702",
    headsign="Blacktown to Parramatta"  // Same headsign, different trip!
  ),
  NswBusTripOptions(
    tripId="trip_4",
    routeId="2505_702",
    headsign="Mayfield to Warabrook"
  ),
  NswBusTripOptions(
    tripId="trip_5",
    routeId="2505_702",
    headsign="Warabrook to Mayfield"
  )
]


┌─────────────────────────────────────────────────────────────────────┐
│ Step 3: Create lookup map for O(1) variant access                  │
└─────────────────────────────────────────────────────────────────────┘

variantsByRouteId = variants.associateBy { it.routeId }
    ↓
Map {
  "2504_702" -> NswBusRouteVariants(...),
  "2505_702" -> NswBusRouteVariants(...)
}


┌─────────────────────────────────────────────────────────────────────┐
│ Step 4: Transform each trip to UI Trip object                      │
│         Each tripId becomes ONE search result                       │
└─────────────────────────────────────────────────────────────────────┘

For EACH trip in allTrips:
    ↓
  Get variant from map: variantsByRouteId[trip.routeId]
    ↓
  Fetch stops: selectStopsByTripId(trip.tripId)
    ↓
  Create Trip UI object:
    SearchStopState.SearchResult.Trip(
      tripId = trip.tripId,           // Unique PRIMARY KEY
      routeShortName = "702",
      headsign = trip.headsign,       // Display property
      stops = [...ordered stops...],
      transportMode = Bus
    )


┌─────────────────────────────────────────────────────────────────────┐
│ Final Output: List<SearchStopState.SearchResult.Trip>              │
│               Each tripId = One search result in UI                 │
└─────────────────────────────────────────────────────────────────────┘

[
  Trip(tripId="trip_1", headsign="Blacktown to Parramatta", ...),
  Trip(tripId="trip_2", headsign="Parramatta to Blacktown", ...),
  Trip(tripId="trip_3", headsign="Blacktown to Parramatta", ...),  // ✅ Also shown!
  Trip(tripId="trip_4", headsign="Mayfield to Warabrook", ...),
  Trip(tripId="trip_5", headsign="Warabrook to Mayfield", ...)
]

```

## Glossary

- **Route Short Name**: The route number (e.g., "702", "M30")
- **Route Variant**: Different versions of same route (different operators/networks)
- **Trip**: A specific journey on a route in one direction
- **Headsign**: The destination/direction text (e.g., "Blacktown to Parramatta")
- **Trip ID**: Unique identifier for each trip (PRIMARY KEY)
- **Stop Sequence**: Order of stops (0, 1, 2, ...) preserving correct route path

---
