package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter4 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 4 to 5")
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS NSWParkRideFacilityDetail (
                    facilityId TEXT NOT NULL PRIMARY KEY,
                    spotsAvailable INTEGER NOT NULL,
                    totalSpots INTEGER NOT NULL,
                    facilityName TEXT NOT NULL,
                    percentageFull INTEGER NOT NULL,
                    stopId TEXT NOT NULL,
                    stopName TEXT NOT NULL,
                    timeText TEXT NOT NULL,
                    suburb TEXT NOT NULL,
                    address TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    timestamp INTEGER NOT NULL
                );
                
                CREATE TABLE IF NOT EXISTS SavedParkRide (
                    stopId TEXT NOT NULL,
                    facilityId TEXT NOT NULL,
                    source TEXT NOT NULL,
                    PRIMARY KEY (stopId, facilityId)
                );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
