package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter3 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 3 to 4")
        sqlDriver.execute(
            identifier = null,
            sql = """
                   CREATE TABLE IF NOT EXISTS KrailPref (
                       key TEXT PRIMARY KEY NOT NULL,
                       int_value INTEGER,
                       string_value TEXT,
                       bool_value INTEGER,
                       float_value REAL
                   );
                """.trimIndent(),
            parameters = 0,
        )
    }
}
