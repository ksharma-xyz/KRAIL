package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter7 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 7 to 8")
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS NswStops (
                    stopId TEXT PRIMARY KEY,
                    stopName TEXT NOT NULL,
                    stopLat REAL NOT NULL,
                    stopLon REAL NOT NULL,
                    parentStopId TEXT
                );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
