package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The camera-seeding and location-button behaviour every full map surface shares. It was extracted
 * from `SearchStopMap` precisely so a second map could not drift into a different permission flow,
 * so the branch each caller depends on is what these tests hold.
 */
class MapLocationHelpersTest {

    @Test
    fun `GIVEN no known location WHEN seeding the camera THEN it starts on the Sydney default`() {
        val position = initialCameraPosition(knownLocation = null)

        assertEquals(NearbyStopsConfig.DEFAULT_CENTER_LAT, position.target.latitude)
        assertEquals(NearbyStopsConfig.DEFAULT_CENTER_LON, position.target.longitude)
        assertEquals(NearbyStopsConfig.DEFAULT_ZOOM, position.zoom)
    }

    @Test
    fun `GIVEN a known location WHEN seeding the camera THEN re-entry does not flash at the default`() {
        val position = initialCameraPosition(LatLng(latitude = -33.75, longitude = 151.05))

        assertEquals(-33.75, position.target.latitude)
        assertEquals(151.05, position.target.longitude)
        assertEquals(UserLocationConfig.AUTO_CENTER_ZOOM, position.zoom)
    }

    @Test
    fun `GIVEN a coordinate WHEN converted for MapLibre THEN latitude and longitude stay put`() {
        val position = LatLng(latitude = -33.87, longitude = 151.21).toPosition()

        assertEquals(-33.87, position.latitude)
        assertEquals(151.21, position.longitude)
    }

    @Test
    fun `GIVEN no fix and a hard denial WHEN the location button is tapped THEN the denial is surfaced`() =
        krailRunTest {
            val manager = FakeUserLocationManager(PermissionStatus.Denied(canAskAgain = false))
            var denied: PermissionStatus? = null
            var requested = false

            handleLocationButtonClick(
                userLoc = null,
                cameraState = CameraState(CameraPosition()),
                userLocationManager = manager,
                onPermissionDenied = { denied = it },
                onRequestPermission = { requested = true },
            )

            assertEquals(PermissionStatus.Denied(canAskAgain = false), denied)
            // A denial routes to settings; asking again would show nothing on iOS and on a
            // twice-denied Android.
            assertEquals(false, requested)
        }

    @Test
    fun `GIVEN no fix and an unasked permission WHEN the button is tapped THEN the rider is asked`() =
        krailRunTest {
            val manager = FakeUserLocationManager(PermissionStatus.NotDetermined)
            var denied: PermissionStatus? = null
            var requested = false

            handleLocationButtonClick(
                userLoc = null,
                cameraState = CameraState(CameraPosition()),
                userLocationManager = manager,
                onPermissionDenied = { denied = it },
                onRequestPermission = { requested = true },
            )

            assertNull(denied)
            assertEquals(true, requested)
        }

    @Test
    fun `GIVEN a known fix WHEN the button is tapped THEN it recentres without re-asking for permission`() =
        krailRunTest {
            val manager = FakeUserLocationManager(PermissionStatus.Denied(canAskAgain = false))
            var touchedPermissionFlow = false

            // animateTo parks until a real map attaches, which never happens off-device — the
            // assertion is that the permission flow is not reached at all.
            val job = launch {
                handleLocationButtonClick(
                    userLoc = LatLng(latitude = -33.87, longitude = 151.21),
                    cameraState = CameraState(CameraPosition()),
                    userLocationManager = manager,
                    onPermissionDenied = { touchedPermissionFlow = true },
                    onRequestPermission = { touchedPermissionFlow = true },
                )
            }
            runCurrent()

            assertEquals(0, manager.permissionChecks)
            assertEquals(false, touchedPermissionFlow)
            job.cancel()
        }
}
