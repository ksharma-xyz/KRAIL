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

                CREATE TABLE IF NOT EXISTS DiscoverCardFeedback (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cardId TEXT NOT NULL UNIQUE,
                    isPositive INTEGER NOT NULL, -- 1 for positive, 0 for negative
                    timestamp INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                    isCompleted INTEGER NOT NULL DEFAULT 0 -- 1 for completed, 0 for not completed
                );
            """.trimIndent(),
            parameters = 0,
        )
    }
}
