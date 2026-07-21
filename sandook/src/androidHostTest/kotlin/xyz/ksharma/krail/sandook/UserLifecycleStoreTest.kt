package xyz.ksharma.krail.sandook

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class UserLifecycleStoreTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var sandook: KrailSandook
    private var now: Long = DAY_ZERO

    private lateinit var store: UserLifecycleStore

    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KrailSandook.Schema.create(driver)
        sandook = KrailSandook(driver)
        store = RealUserLifecycleStore(
            queries = sandook.userLifecycleQueries,
            preferences = RealSandookPreferences(sandook.appPreferencesQueries),
            nowMillis = { now },
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `first install is unset until recorded`() {
        assertNull(store.firstInstallAtMillis())
        assertNull(store.daysSinceFirstInstall())
    }

    @Test
    fun `first install is stamped once and never overwritten`() {
        store.recordFirstInstallIfAbsent()
        assertEquals(DAY_ZERO, store.firstInstallAtMillis())

        now = DAY_ZERO + MILLIS_PER_DAY * 5
        store.recordFirstInstallIfAbsent()

        assertEquals(DAY_ZERO, store.firstInstallAtMillis())
    }

    @Test
    fun `days since first install counts whole elapsed days`() {
        store.recordFirstInstallIfAbsent()
        assertEquals(0L, store.daysSinceFirstInstall())

        // Just short of a full day still reads as day 0.
        now = DAY_ZERO + MILLIS_PER_DAY - 1
        assertEquals(0L, store.daysSinceFirstInstall())

        now = DAY_ZERO + MILLIS_PER_DAY * 3
        assertEquals(3L, store.daysSinceFirstInstall())
    }

    @Test
    fun `days since first install clamps a backwards device clock to zero`() {
        store.recordFirstInstallIfAbsent()

        now = DAY_ZERO - MILLIS_PER_DAY * 2

        assertEquals(0L, store.daysSinceFirstInstall())
    }

    @Test
    fun `counter starts at zero with no last seen time`() {
        assertEquals(0L, store.count(LifecycleCounter.SAVED_TRIP_OPEN))
        assertNull(store.lastAtMillis(LifecycleCounter.SAVED_TRIP_OPEN))
    }

    @Test
    fun `increment returns the running total and stamps the last seen time`() {
        assertEquals(1L, store.increment(LifecycleCounter.SAVED_TRIP_OPEN))
        assertEquals(DAY_ZERO, store.lastAtMillis(LifecycleCounter.SAVED_TRIP_OPEN))

        now = DAY_ZERO + MILLIS_PER_DAY
        assertEquals(2L, store.increment(LifecycleCounter.SAVED_TRIP_OPEN))
        assertEquals(3L, store.increment(LifecycleCounter.SAVED_TRIP_OPEN))

        assertEquals(3L, store.count(LifecycleCounter.SAVED_TRIP_OPEN))
        assertEquals(DAY_ZERO + MILLIS_PER_DAY, store.lastAtMillis(LifecycleCounter.SAVED_TRIP_OPEN))
    }

    @Test
    fun `counters and install date survive reopening the database`() {
        store.recordFirstInstallIfAbsent()
        store.increment(LifecycleCounter.SAVED_TRIP_OPEN)
        store.increment(LifecycleCounter.SAVED_TRIP_OPEN)

        // A second store over the same driver stands in for a relaunch after an app update:
        // nothing is held in memory, every value is read back from the database.
        val reopened = RealUserLifecycleStore(
            queries = KrailSandook(driver).userLifecycleQueries,
            preferences = RealSandookPreferences(KrailSandook(driver).appPreferencesQueries),
            nowMillis = { now },
        )

        assertEquals(DAY_ZERO, reopened.firstInstallAtMillis())
        assertEquals(2L, reopened.count(LifecycleCounter.SAVED_TRIP_OPEN))
    }

    @Test
    fun `migration creates the counter table for an upgrading user`() {
        // Fresh installs get the table from Schema.create, which the tests above cover.
        // Users upgrading from a shipped build arrive through migrate() instead, and this
        // is the path that would silently leave them without the table if the migration
        // were misnumbered.
        val upgradeDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KrailSandook.Schema.migrate(
            driver = upgradeDriver,
            oldVersion = SHIPPED_SCHEMA_VERSION,
            newVersion = KrailSandook.Schema.version,
        )

        val upgraded = RealUserLifecycleStore(
            queries = KrailSandook(upgradeDriver).userLifecycleQueries,
            preferences = RealSandookPreferences(KrailSandook(upgradeDriver).appPreferencesQueries),
            nowMillis = { now },
        )

        assertEquals(1L, upgraded.increment(LifecycleCounter.SAVED_TRIP_OPEN))

        upgradeDriver.close()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        /** Schema version of the last build without the user-lifecycle counters. */
        const val SHIPPED_SCHEMA_VERSION = 11L

        /** An arbitrary fixed "install" instant; tests move [now] relative to it. */
        const val DAY_ZERO = 1_700_000_000_000L
    }
}
