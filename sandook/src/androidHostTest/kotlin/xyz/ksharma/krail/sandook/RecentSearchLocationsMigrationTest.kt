package xyz.ksharma.krail.sandook

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecentSearchLocationsMigrationTest {

    @Test
    fun `migration copies visible legacy recents then removes the legacy table`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        createVersionTenTables(driver)
        insertLegacyRecent(driver)

        KrailSandook.Schema.migrate(
            driver = driver,
            oldVersion = VERSION_TEN,
            newVersion = KrailSandook.Schema.version,
        )

        assertEquals(
            listOf(
                "200060",
                "Central Station",
                "TRANSIT_STOP",
                null,
                "1,2",
                "2026-07-13 09:30:00",
            ),
            driver.migratedLocation(),
        )
        assertEquals(
            0L,
            driver.longValue(
                """
                SELECT COUNT(*)
                FROM sqlite_master
                WHERE type = 'table' AND name = 'RecentSearchStops'
                """.trimIndent(),
            ),
        )

        driver.close()
    }

    private fun createVersionTenTables(driver: JdbcSqliteDriver) {
        driver.execute(
            identifier = null,
            sql = "CREATE TABLE RecentSearchStops(stopId TEXT NOT NULL PRIMARY KEY, timestamp TEXT)",
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = "CREATE TABLE NswStops(stopId TEXT NOT NULL PRIMARY KEY, stopName TEXT NOT NULL)",
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = "CREATE TABLE NswStopProductClass(stopId TEXT NOT NULL, productClass INTEGER NOT NULL)",
            parameters = 0,
        )
    }

    private fun insertLegacyRecent(driver: JdbcSqliteDriver) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO NswStops VALUES ('200060', 'Central Station')",
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = "INSERT INTO NswStopProductClass VALUES ('200060', 1), ('200060', 2)",
            parameters = 0,
        )
        driver.execute(
            identifier = null,
            sql = "INSERT INTO RecentSearchStops VALUES ('200060', '2026-07-13 09:30:00')",
            parameters = 0,
        )
    }

    private fun JdbcSqliteDriver.migratedLocation(): List<String?> =
        executeQuery(
            identifier = null,
            sql = """
                SELECT locationId, displayName, kind, addressType, productClasses, timestamp
                FROM RecentSearchLocations
            """.trimIndent(),
            mapper = { cursor ->
                check(cursor.next().value)
                QueryResult.Value(List(LOCATION_COLUMN_COUNT, cursor::getString))
            },
            parameters = 0,
        ).value

    private fun JdbcSqliteDriver.longValue(sql: String): Long =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                check(cursor.next().value)
                QueryResult.Value(cursor.getLong(0) ?: error("Expected a count"))
            },
            parameters = 0,
        ).value

    private companion object {
        const val LOCATION_COLUMN_COUNT = 6
        const val VERSION_TEN = 10L
    }
}
