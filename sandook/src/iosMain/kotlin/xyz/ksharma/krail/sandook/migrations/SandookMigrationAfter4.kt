package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter4 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 4 to 5")
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE NSWParkRide (
                    facilityId TEXT NOT NULL PRIMARY KEY,
                    spotsAvailable INTEGER NOT NULL,
                    totalSpots INTEGER NOT NULL,
                    facilityName TEXT NOT NULL,
                    percentageFull INTEGER NOT NULL,
                    stopId TEXT NOT NULL,
                    timeText TEXT NOT NULL,
                    suburb TEXT NOT NULL,
                    address TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL
                );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
