package xyz.ksharma.krail.sandook

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedParkRideSourceMigrationTest {

    @Test
    fun `migration keeps existing rows and lets both sources hold the same facility`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        createVersionElevenTable(driver)
        driver.insertSavedParkRide(source = SAVED_TRIP_SOURCE)

        KrailSandook.Schema.migrate(
            driver = driver,
            oldVersion = VERSION_ELEVEN,
            newVersion = KrailSandook.Schema.version,
        )

        // The pre-migration row survives untouched.
        assertEquals(1L, driver.savedParkRideCount())

        // The same stop + facility can now also be held by the user-added source,
        // instead of replacing the saved-trip row.
        driver.insertSavedParkRide(source = USER_SOURCE)
        assertEquals(2L, driver.savedParkRideCount())

        // Removing the user-added row leaves the saved-trip row intact.
        driver.execute(
            identifier = null,
            sql = "DELETE FROM SavedParkRide WHERE stopId = ? AND facilityId = ? AND source = ?",
            parameters = 3,
        ) {
            bindString(0, STOP_ID)
            bindString(1, FACILITY_ID)
            bindString(2, USER_SOURCE)
        }
        assertEquals(1L, driver.savedParkRideCount())
        assertEquals(SAVED_TRIP_SOURCE, driver.remainingSource())

        driver.close()
    }

    /** The SavedParkRide shape as of schema version 11, keyed without `source`. */
    private fun createVersionElevenTable(driver: JdbcSqliteDriver) {
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE SavedParkRide (
                    stopId TEXT NOT NULL,
                    facilityId TEXT NOT NULL,
                    stopName TEXT NOT NULL,
                    facilityName TEXT NOT NULL,
                    source TEXT NOT NULL,
                    PRIMARY KEY (stopId, facilityId)
                )
            """.trimIndent(),
            parameters = 0,
        )
    }

    private fun JdbcSqliteDriver.insertSavedParkRide(source: String) {
        execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO SavedParkRide VALUES (?, ?, ?, ?, ?)",
            parameters = 5,
        ) {
            bindString(0, STOP_ID)
            bindString(1, FACILITY_ID)
            bindString(2, "Gordon Station")
            bindString(3, "Gordon Henry St (north)")
            bindString(4, source)
        }
    }

    private fun JdbcSqliteDriver.savedParkRideCount(): Long =
        executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM SavedParkRide",
            mapper = { cursor ->
                check(cursor.next().value)
                QueryResult.Value(cursor.getLong(0) ?: error("Expected a count"))
            },
            parameters = 0,
        ).value

    private fun JdbcSqliteDriver.remainingSource(): String =
        executeQuery(
            identifier = null,
            sql = "SELECT source FROM SavedParkRide",
            mapper = { cursor ->
                check(cursor.next().value)
                QueryResult.Value(cursor.getString(0) ?: error("Expected a source"))
            },
            parameters = 0,
        ).value

    private companion object {
        const val VERSION_ELEVEN = 11L
        const val STOP_ID = "207210"
        const val FACILITY_ID = "6"
        const val SAVED_TRIP_SOURCE = "saved_trip"
        const val USER_SOURCE = "user"
    }
}
