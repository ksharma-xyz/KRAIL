package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter7 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 7 to 8")
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS SavedTrip (
                    tripId TEXT PRIMARY KEY,
                    fromStopId TEXT NOT NULL,
                    fromStopName TEXT NOT NULL,
                    toStopId TEXT NOT NULL,
                    toStopName TEXT NOT NULL,
                    timestamp TEXT DEFAULT (datetime('now'))
                );

                ALTER TABLE SavedTrip ADD COLUMN isFromStopValid INTEGER NOT NULL DEFAULT 1;
                ALTER TABLE SavedTrip ADD COLUMN isToStopValid INTEGER NOT NULL DEFAULT 1;
            """.trimIndent(),
            parameters = 0,
        )
        log("Added isFromStopValid and isToStopValid columns to SavedTrip table")
    }
}
