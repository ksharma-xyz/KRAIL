# Implementation Plan: GTFS Hierarchical Stops & Route Search

## Overview

This plan details the changes required to support:

1. **Hierarchical Stops:** Consuming a new Parent-Child stop format while maintaining existing
   search behavior.
2. **Route Search:** Ingesting route data (Proto/JSON) and enabling search by route number (e.g., "
   700") to show route variants and their stops.

## 1. Database Schema Changes

### A. Modify `NswStops` Table

We need to store both Parent stops (interchanges) and Child stops (platforms/stands) in the same
table to allow ID-based lookup for both. We add a `parentId` column to distinguish them.

**File:** `sandook/src/commonMain/sqldelight/xyz/ksharma/krail/sandook/NswStops.sq`

**Changes:**

1. **Migration:** Add `parentId` column.
    * *Strategy:* Since the source data format is changing completely, we recommend a **Destructive
      Migration** (Drop & Recreate) for the `NswStops` table on the first run with the new data
      version.
2. **Schema:**
   ```sql
   CREATE TABLE IF NOT EXISTS NswStops (
       stopId TEXT PRIMARY KEY,
       stopName TEXT NOT NULL,
       stopLat REAL NOT NULL,
       stopLon REAL NOT NULL,
       parentId TEXT -- NULL for Parents, populated for Children
   );
   ```
3. **Queries:**
    * `insertStop`: Update to accept `parentId`.
    * `selectProductClassesForStop`: Update logic to prioritize Parents during name search.
      ```sql
      -- If searching by Name, prefer Parents (parentId IS NULL).
      -- If searching by ID, return exact match.
      SELECT ... WHERE (stopId = :query) OR (stopName LIKE :queryPattern AND parentId IS NULL)
      ```
    * `selectStopsByIds`: New query to fetch multiple stops (for Route details).
      ```sql
      SELECT * FROM NswStops WHERE stopId IN ?;
      ```

### B. Create `NswRoutes` Table

New table to store route information. We will store the route group data as a BLOB (Protobuf bytes)
or Text (JSON) to avoid over-normalizing, as the query pattern is simple (Search by Short Name ->
Get Full Object).

**File:** `sandook/src/commonMain/sqldelight/xyz/ksharma/krail/sandook/NswRoutes.sq` (New File)

**Schema:**

```sql
CREATE TABLE IF NOT EXISTS NswRoutes (
    routeShortName TEXT PRIMARY KEY, -- e.g. "700", "M52"
    routeData BLOB NOT NULL -- Serialized NswBusRouteGroup (Proto) or JSON string
);

insertRoute:
INSERT OR REPLACE INTO NswRoutes(routeShortName, routeData) VALUES (?, ?);

searchRoutes:
SELECT * FROM NswRoutes 
WHERE routeShortName LIKE :query || '%' 
ORDER BY length(routeShortName), routeShortName 
LIMIT 20;

deleteAll:
DELETE FROM NswRoutes;
```

## 2. Repository Layer (`Sandook`)

**File:** `sandook/src/commonMain/kotlin/xyz/ksharma/krail/sandook/Sandook.kt` & `RealSandook.kt`

1. **Stops:**
    * Update `insertNswStop` signature to include `parentId: String?`.
    * Add `selectStopsByIds(stopIds: List<String>): List<Stop>` to resolve trip stops.
2. **Routes:**
    * Add `insertRoute(shortName: String, data: ByteArray)`.
    * Add `searchRoutes(query: String): List<RouteGroup>`.
    * *Note:* The repository should handle the deserialization of `routeData` BLOB into the domain
      model (e.g., `NswBusRouteGroup`).

## 3. Data Ingestion Logic

**Location:** Wherever `NSW_STOPS.json` and the new `NSW_ROUTES.json/proto` are parsed (likely a
`SyncWorker` or `Repository`).

1. **Stops Ingestion:**
    * Parse the new Hierarchical JSON/Proto.
    * **Parent:** Insert with `parentId = null`.
    * **Children:** Iterate `children` array and insert each with `parentId = parent.id`.
2. **Routes Ingestion:**
    * Parse `NswBusRouteList`.
    * Iterate `routes` (Groups).
    * Insert each Group: `routeShortName` = `group.routeShortName`, `routeData` =
      `group.toByteArray()` (or JSON string).

## 4. Search & UI Logic

**Location:** `StopResultsManager` or `SearchUseCase`

1. **Unified Search:**
    * When user types a query (e.g., "700"):
        * **Async 1:** Query `NswStops` (Name match).
        * **Async 2:** Query `NswRoutes` (Short Name match).
    * Combine results.
        * *Heuristic:* If query looks like a route number (digits/alphanumeric short), prioritize
          Route results.
2. **Route Detail Interaction:**
    * User clicks a Route result (e.g., "700").
    * UI shows list of Variants (e.g., "Blacktown to Parramatta").
    * User clicks a Variant.
    * App retrieves `stop_ids` from the Variant object.
    * App calls `sandook.selectStopsByIds(stop_ids)`.
    * App displays the list of stops with names/locations resolved from the DB.

## 5. Backward Compatibility & Migration

* **Database:**
    * The `NswStops` table schema change is **not** backward compatible.
    * **Action:** In `Database.Schema`, increment version. In the migration callback (or
      `RealSandook` init), if the data source has changed to the new format, execute
      `nswStopsQueries.clearNswStopsTable()` (or drop table) to ensure clean state.
* **Server:**
    * Ensure the new data files are served from new endpoints or versioned filenames to prevent
      crashing old app versions.

## 6. Proto Definitions

Ensure the `.proto` files are added to the project and code generation is configured (Wire or
Protobuf Gradle Plugin).

* `NswBusRouteList`
* `NswBusRouteGroup`
* `NswBusRouteVariant`
* `NswBusTripOption`

