package xyz.ksharma.krail.core.maps.data.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import xyz.ksharma.aagya.permission.AppPermission
import xyz.ksharma.aagya.permission.PermissionResult
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.aagya.permission.data.PermissionController
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationConfig
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.dhruva.location.data.LocationTracker
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent.Outcome
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the app reads the location permission as, when it asks for it, and what it reports of the answer. The app
 * asks about precise location alone. Android grants approximate separately, and only the report looks at it.
 */
class UserLocationManagerImplTest {

    private val permissions = StubPermissions()
    private val tracker = StubTracker()
    private val recorder = KeptRecorder()
    private val manager = UserLocationManagerImpl(permissions, tracker, recorder)

    // region reading the permission

    @Test
    fun `precise location is location allowed`() = runTest {
        permissions.allowPrecise()

        assertEquals(PermissionStatus.Granted, manager.checkPermissionStatus())
    }

    @Test
    fun `the status is what precise location says, whatever approximate says`() = runTest {
        permissions.coarse = PermissionStatus.Granted

        assertEquals(PermissionStatus.NotDetermined, manager.checkPermissionStatus())

        permissions.fine = PermissionStatus.Denied(canAskAgain = true)

        assertEquals(PermissionStatus.Denied(canAskAgain = true), manager.checkPermissionStatus())
    }

    @Test
    fun `a refusal the system has forgotten is still reported as denied, and only reported`() = runTest {
        recorder.wasRefused = true

        assertEquals(PermissionStatus.Denied(canAskAgain = false), manager.reportedPermissionStatus())
        assertEquals(PermissionStatus.NotDetermined, manager.checkPermissionStatus())
    }

    @Test
    fun `a rider never asked is reported as not asked`() = runTest {
        assertEquals(PermissionStatus.NotDetermined, manager.reportedPermissionStatus())
    }

    @Test
    fun `allowing it later clears the remembered refusal`() = runTest {
        recorder.wasRefused = true
        permissions.allowPrecise()

        assertEquals(PermissionStatus.Granted, manager.reportedPermissionStatus())
        assertFalse(recorder.wasRefused)
    }

    // endregion

    // region asking, and the one report for each answer

    @Test
    fun `a precise answer is reported as granted from where it was asked`() = runTest {
        permissions.answer = {
            allowPrecise()
            PermissionResult.Granted
        }

        assertTrue(manager.getCurrentLocation(Source.MAP_BUTTON).isSuccess)

        assertEquals(listOf(Answer(Source.MAP_BUTTON, Outcome.GRANTED, null)), recorder.answers)
        assertFalse(recorder.wasRefused)
    }

    @Test
    fun `an approximate answer is reported as approximate and gives the app no location`() = runTest {
        permissions.answer = {
            coarse = PermissionStatus.Granted
            PermissionResult.Denied(canAskAgain = true)
        }

        val result = manager.getCurrentLocation(Source.INTRO)

        assertIs<LocationError.PermissionDenied>(result.exceptionOrNull())
        assertEquals(0, tracker.fixes)
        assertEquals(listOf(Answer(Source.INTRO, Outcome.GRANTED_APPROXIMATE, true)), recorder.answers)
        assertTrue(recorder.wasRefused)
    }

    @Test
    fun `a refusal is reported with whether the system will ask again, and is remembered`() = runTest {
        permissions.answer = { PermissionResult.Denied(canAskAgain = false) }

        val result = manager.getCurrentLocation(Source.SEARCH_STOP_PILL)

        assertIs<LocationError.PermissionDenied>(result.exceptionOrNull())
        assertEquals(listOf(Answer(Source.SEARCH_STOP_PILL, Outcome.DENIED, false)), recorder.answers)
        assertTrue(recorder.wasRefused)
        assertEquals(0, tracker.fixes)
    }

    @Test
    fun `a dismissed prompt is reported as cancelled`() = runTest {
        permissions.answer = { PermissionResult.Cancelled }

        manager.getCurrentLocation(Source.HOME_PILL)

        assertEquals(listOf(Answer(Source.HOME_PILL, Outcome.CANCELLED, null)), recorder.answers)
        assertTrue(recorder.wasRefused)
    }

    @Test
    fun `a request that was never made is not reported`() = runTest {
        permissions.answer = { PermissionResult.PolicyExhausted }

        val result = manager.getCurrentLocation(Source.INTRO)

        assertIs<LocationError.PermissionDenied>(result.exceptionOrNull())
        assertEquals(emptyList(), recorder.answers)
        assertFalse(recorder.wasRefused)
    }

    @Test
    fun `a rider who has already answered is not asked and nothing is reported`() = runTest {
        permissions.fine = PermissionStatus.Denied(canAskAgain = true)
        manager.getCurrentLocation(Source.MAP_BUTTON)

        permissions.allowPrecise()
        manager.getCurrentLocation(Source.MAP_BUTTON)

        assertEquals(0, permissions.requests)
        assertEquals(emptyList(), recorder.answers)
    }

    @Test
    fun `tracking with no request source asks nothing`() = runTest {
        assertFailsWith<LocationError.PermissionDenied> { manager.locationUpdates(requestSource = null).first() }

        assertEquals(0, permissions.requests)
    }

    @Test
    fun `tracking started by a tap asks and reports from that tap`() = runTest {
        permissions.answer = {
            allowPrecise()
            PermissionResult.Granted
        }

        manager.locationUpdates(requestSource = Source.PARK_RIDE_MAP).first()

        assertEquals(listOf(Source.PARK_RIDE_MAP), recorder.answers.map { it.source })
    }

    // endregion

    private data class Answer(val source: Source, val outcome: Outcome, val canAskAgain: Boolean?)

    private class KeptRecorder : LocationPermissionRecorder {
        override var wasRefused: Boolean = false
        val answers = mutableListOf<Answer>()

        override fun onRequestAnswered(source: Source, outcome: Outcome, canAskAgain: Boolean?) {
            answers += Answer(source, outcome, canAskAgain)
        }
    }

    private class StubPermissions : PermissionController {
        var fine: PermissionStatus = PermissionStatus.NotDetermined
        var coarse: PermissionStatus = PermissionStatus.NotDetermined
        var answer: StubPermissions.() -> PermissionResult = { error("no prompt was expected") }
        var requests = 0
            private set

        fun allowPrecise() {
            fine = PermissionStatus.Granted
            coarse = PermissionStatus.Granted
        }

        override suspend fun requestPermission(permission: AppPermission): PermissionResult {
            requests++
            return answer()
        }

        override suspend fun checkPermissionStatus(permission: AppPermission): PermissionStatus =
            when (permission) {
                AppPermission.Location.Fine -> fine
                AppPermission.Location.Coarse -> coarse
                else -> error("not a location permission: $permission")
            }

        override suspend fun wasPermissionRequested(permission: AppPermission): Boolean = requests > 0

        override fun openAppSettings() = Unit
    }

    private class StubTracker : LocationTracker {
        private val here = Location(latitude = -33.8688, longitude = 151.2093, timestamp = 0L)
        var fixes = 0
            private set

        override suspend fun getCurrentLocation(timeoutMs: Long): Location {
            fixes++
            return here
        }

        override fun startTracking(config: LocationConfig): Flow<Location> = flowOf(here)

        override fun stopTracking() = Unit

        override suspend fun isLocationEnabled(): Boolean = true
    }
}
