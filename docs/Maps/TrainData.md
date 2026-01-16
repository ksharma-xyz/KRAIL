The Static GTFS Files
This is the standard way transit apps work. You parse these files and store them in a local SQLite database (using Room) or a bundled JSON.

routes.txt: Map route_short_name (T1) to route_id.

trips.txt: Find all trips associated with that route_id.

stop_times.txt: This is the "Gold Mine." It links a trip_id to a stop_id and defines the sequence (order of stops).

stops.txt: Map stop_id to stop_name, lat, and lon.

2. The Logic to find Stops by Route ID
   If you have a route_id, here is the pseudo-logic to populate your MapDisplay object:

Find a Representative Trip: A route has many trips (e.g., T1 North Shore vs T1 Western). Pick the "longest" trip or a specific direction for that route_id from trips.txt.

Get Stop IDs: Query stop_times.txt for all entries where trip_id == {selected_trip}. Sort them by stop_sequence.

Get Coordinates: Join those Stop IDs with stops.txt to get names and LatLng.

3. Implementation: Mapping to your Model
   To convert this data into your MapUiState.Ready class, you can create a repository function. Here is how you would map the GTFS data into your existing RouteFeature and StopFeature classes:

Kotlin

// Example Repository function
fun getRouteDetails(routeId: String): MapDisplay {
// 1. Fetch points for the GeoJson line (You said you already have this)
val routePoints = geoJsonRepository.getPointsForRoute(routeId)

    // 2. Fetch stops for this route (Querying your GTFS source)
    // This would be a JOIN between stop_times and stops
    val rawStops = gtfsDatabase.getStopsForRoute(routeId) 

    return MapDisplay(
        routes = listOf(
            RouteFeature(
                id = routeId,
                colorHex = "#F35F23", // You can map this from route_color in GTFS
                points = routePoints.map { LatLng(it.lat, it.lon) }
            )
        ),
        stops = rawStops.map { stop ->
            StopFeature(
                stopId = stop.id,
                stopName = stop.name,
                lineId = routeId,
                position = LatLng(stop.lat, stop.lon)
            )
        }
    )
}