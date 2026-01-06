package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter7 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 7 to 8")

        // Add isParent column to existing NswStops table (table already exists in production)
        sqlDriver.execute(
            identifier = null,
            sql = "ALTER TABLE NswStops ADD COLUMN isParent INTEGER;",
            parameters = 0,
        )

        // Create NSW Bus Routes tables
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS NswBusRouteGroups (
                    routeShortName TEXT PRIMARY KEY
                );
            """.trimIndent(),
            parameters = 0,
        )

        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS NswBusRouteVariants (
                    routeId TEXT PRIMARY KEY,
                    routeShortName TEXT NOT NULL,
                    routeName TEXT NOT NULL,
                    FOREIGN KEY (routeShortName) REFERENCES NswBusRouteGroups(routeShortName)
                );
            """.trimIndent(),
            parameters = 0,
        )

        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS NswBusTripOptions (
                    tripId TEXT PRIMARY KEY,
                    routeId TEXT NOT NULL,
                    headsign TEXT NOT NULL,
                    FOREIGN KEY (routeId) REFERENCES NswBusRouteVariants(routeId)
                );
            """.trimIndent(),
            parameters = 0,
        )

        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS NswBusTripStops (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tripId TEXT NOT NULL,
                    stopId TEXT NOT NULL,
                    stopSequence INTEGER NOT NULL,
                    FOREIGN KEY (tripId) REFERENCES NswBusTripOptions(tripId),
                    FOREIGN KEY (stopId) REFERENCES NswStops(stopId)
                );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
