package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter2 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 2 to 3")
        sqlDriver.execute(
            identifier = null,
            sql = """
                    CREATE TABLE IF NOT EXISTS NswStops (
                        stopId TEXT PRIMARY KEY,
                        stopName TEXT NOT NULL,
                        stopLat REAL NOT NULL,
                        stopLon REAL NOT NULL
                    );

                    CREATE TABLE IF NOT EXISTS NswStopProductClass (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        stopId TEXT NOT NULL,
                        productClass INTEGER NOT NULL,
                        FOREIGN KEY (stopId) REFERENCES NswStops(stopId)
                    );
                """.trimIndent(),
            parameters = 0,
        )
    }
}
