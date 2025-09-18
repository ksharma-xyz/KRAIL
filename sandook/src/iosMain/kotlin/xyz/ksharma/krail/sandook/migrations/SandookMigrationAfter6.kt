package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter6 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 6 to 7")
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS RecentSearchStops (
                    stopId TEXT NOT NULL PRIMARY KEY,
                    timestamp TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY (stopId) REFERENCES NswStops(stopId)
                );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
