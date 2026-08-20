package xyz.ksharma.krail.sandook

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.ksharma.krail.sandook.db.KrailSandook
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class SavedTripIdentityMigrationTest {

    @Test
    fun `migration merges duplicate stop pairs and canonicalizes their IDs`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        createVersionThirteenTable(driver)
        driver.insertLegacyTrip(
            tripId = "200060-200080",
            fromStopId = "200060",
            fromStopName = "Old Central",
            toStopId = "200080",
            toStopName = "Wynyard Station",
            timestamp = "2026-08-06 09:07:19",
            sortOrder = 4,
        )
        driver.insertLegacyTrip(
            tripId = "200060200080",
            fromStopId = "200060",
            fromStopName = "Central Station",
            toStopId = "200080",
            toStopName = "Wynyard Station",
            timestamp = "2026-08-08 11:28:46",
            sortOrder = 0,
        )
        driver.insertLegacyTrip(
            tripId = "200070-200060",
            fromStopId = "200070",
            fromStopName = "Town Hall Station",
            toStopId = "200060",
            toStopName = "Central Station",
            timestamp = "2026-08-06 09:07:19",
            sortOrder = 1,
        )

        KrailSandook.Schema.migrate(
            driver = driver,
            oldVersion = VERSION_THIRTEEN,
            newVersion = KrailSandook.Schema.version,
        )

        val trips = KrailSandook(driver).krailSandookQueries.selectAllTrips().executeAsList()
        assertEquals(2, trips.size)
        val centralToWynyard = trips.single {
            it.fromStopId == "200060" && it.toStopId == "200080"
        }
        assertEquals("200060->200080", centralToWynyard.tripId)
        assertEquals("Central Station", centralToWynyard.fromStopName)
        assertEquals(0L, centralToWynyard.sort_order)
        assertEquals(
            setOf("200060->200080", "200070->200060"),
            trips.map { it.tripId }.toSet(),
        )
        assertNonCanonicalTripIdRejected(driver)

        driver.close()
    }

    @Test
    fun `migration discards a leftover staging table before retrying`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        createVersionThirteenTable(driver)
        driver.insertLegacyTrip(
            tripId = "200060200080",
            fromStopId = "200060",
            fromStopName = "Central Station",
            toStopId = "200080",
            toStopName = "Wynyard Station",
            timestamp = "2026-08-08 11:28:46",
            sortOrder = 2,
        )
        driver.execute(
            identifier = null,
            sql = "CREATE TABLE SavedTripByStops (stale TEXT NOT NULL)",
            parameters = 0,
        )

        KrailSandook.Schema.migrate(
            driver = driver,
            oldVersion = VERSION_THIRTEEN,
            newVersion = KrailSandook.Schema.version,
        )

        val trip = KrailSandook(driver).krailSandookQueries.selectAllTrips().executeAsOne()
        assertEquals("200060->200080", trip.tripId)
        assertEquals("Central Station", trip.fromStopName)
        assertEquals(2L, trip.sort_order)
        assertNonCanonicalTripIdRejected(driver)

        driver.close()
    }

    @Test
    fun `store normalizes a mismatched caller ID before writing`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KrailSandook.Schema.create(driver)
        val store = RealSandook(KrailSandook(driver), Dispatchers.Unconfined)

        store.insertOrReplaceTrip(
            tripId = "200060-200080",
            fromStopId = "200060",
            fromStopName = "Central Station",
            toStopId = "200080",
            toStopName = "Wynyard Station",
        )

        assertNotNull(store.selectTripById("200060->200080"))
        assertNull(store.selectTripById("200060-200080"))
        assertNonCanonicalTripIdRejected(driver)

        driver.close()
    }

    private fun assertNonCanonicalTripIdRejected(driver: JdbcSqliteDriver) {
        assertFails {
            driver.insertLegacyTrip(
                tripId = "300000-400000",
                fromStopId = "300000",
                fromStopName = "From Station",
                toStopId = "400000",
                toStopName = "To Station",
                timestamp = "2026-08-08 12:00:00",
                sortOrder = 0,
            )
        }
    }

    private fun createVersionThirteenTable(driver: JdbcSqliteDriver) {
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE SavedTrip (
                    tripId TEXT PRIMARY KEY,
                    fromStopId TEXT NOT NULL,
                    fromStopName TEXT NOT NULL,
                    toStopId TEXT NOT NULL,
                    toStopName TEXT NOT NULL,
                    timestamp TEXT DEFAULT (datetime('now')),
                    sort_order INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent(),
            parameters = 0,
        )
    }

    private fun JdbcSqliteDriver.insertLegacyTrip(
        tripId: String,
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
        timestamp: String,
        sortOrder: Long,
    ) {
        execute(
            identifier = null,
            sql = "INSERT INTO SavedTrip VALUES (?, ?, ?, ?, ?, ?, ?)",
            parameters = 7,
        ) {
            bindString(0, tripId)
            bindString(1, fromStopId)
            bindString(2, fromStopName)
            bindString(3, toStopId)
            bindString(4, toStopName)
            bindString(5, timestamp)
            bindLong(6, sortOrder)
        }
    }

    private companion object {
        const val VERSION_THIRTEEN = 13L
    }
}
