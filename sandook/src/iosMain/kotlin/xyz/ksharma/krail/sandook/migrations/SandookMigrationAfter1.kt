package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter1 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 1 to 2")
        sqlDriver.execute(
            identifier = null,
            sql = """
                        CREATE TABLE IF NOT EXISTS ServiceAlertsTable (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            journeyId TEXT NOT NULL,
                            heading TEXT NOT NULL,
                            message TEXT NOT NULL
                        );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
