package xyz.ksharma.krail.core.appstart

import xyz.ksharma.krail.core.remoteconfig.RemoteConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigValue
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.sandook.LifecycleCounter
import xyz.ksharma.krail.sandook.UserLifecycleStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `RealAppStart` is the only thing that starts the stops insert. If its call to `retry()` goes,
 * nothing fills the stops table and no other test notices, so this holds it.
 */
class RealAppStartTest {

    private val events = mutableListOf<String>()
    private val readiness = FakeAppDataReadiness(StopsReadiness.Unknown, onRetry = { events += RETRY })
    private val remoteConfig = RecordingRemoteConfig(events)
    private val lifecycle = RecordingUserLifecycleStore(events)

    @Test
    fun `start stamps the install then calls retry once and sets up remote config`() = krailRunTest {
        RealAppStart(
            coroutineScope = this,
            remoteConfig = remoteConfig,
            appDataReadiness = readiness,
            userLifecycleStore = lifecycle,
        ).start()
        runCurrent()

        assertEquals(1, readiness.retryCallCount)
        assertEquals(1, events.count { it == SETUP })
        assertTrue(
            events.indexOf(FIRST_INSTALL) in 0 until events.indexOf(RETRY),
            "the install age must be stamped before retry: $events",
        )
    }

    private class RecordingRemoteConfig(private val events: MutableList<String>) : RemoteConfig {
        override fun getConfigValue(key: String): RemoteConfigValue = error("not used by RealAppStart")
        override fun setup() {
            events += SETUP
        }
    }

    private class RecordingUserLifecycleStore(private val events: MutableList<String>) : UserLifecycleStore {
        override fun recordFirstInstallIfAbsent() {
            events += FIRST_INSTALL
        }

        override fun recordActiveDay(): Long {
            events += ACTIVE_DAY
            return 1L
        }

        override fun firstInstallAtMillis(): Long? = null
        override fun daysSinceFirstInstall(): Long? = null
        override fun increment(counter: LifecycleCounter): Long = 0L
        override fun count(counter: LifecycleCounter): Long = 0L
        override fun lastAtMillis(counter: LifecycleCounter): Long? = null
        override fun millisSinceLast(counter: LifecycleCounter): Long? = null
        override fun reset(counter: LifecycleCounter) = Unit
    }

    private companion object {
        const val RETRY = "retry"
        const val SETUP = "setup"
        const val FIRST_INSTALL = "recordFirstInstallIfAbsent"
        const val ACTIVE_DAY = "recordActiveDay"
    }
}
