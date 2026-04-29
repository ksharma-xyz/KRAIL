package xyz.ksharma.krail.sandook.migrations

import app.cash.sqldelight.db.SqlDriver
import xyz.ksharma.krail.core.log.log

internal object SandookMigrationAfter8 : SandookMigration {

    override fun migrate(sqlDriver: SqlDriver) {
        log("Upgrading database from version 8 to 9")
        // No-op here as migration happened as per 8.sqm file already
    }
}
