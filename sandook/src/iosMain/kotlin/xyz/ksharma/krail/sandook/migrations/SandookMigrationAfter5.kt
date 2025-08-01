package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter5 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 5 to 6")
        sqlDriver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS DiscoverCardSeen (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cardId TEXT NOT NULL UNIQUE
                );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
