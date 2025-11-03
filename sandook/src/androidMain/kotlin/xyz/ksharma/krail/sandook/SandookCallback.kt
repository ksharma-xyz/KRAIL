package xyz.ksharma.krail.sandook

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import xyz.ksharma.krail.core.log.log

class SandookCallback(
    private val schema: SqlSchema<QueryResult.Value<Unit>>,
) : AndroidSqliteDriver.Callback(schema) {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        log("Creating database: ${db.version}")
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        log("Opening database: ${db.version}")
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        super.onConfigure(db)
        log("Configuring database: ${db.version}")
    }

    @Suppress("MagicNumber")
    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        super.onUpgrade(db, oldVersion, newVersion)
        log(
            "Upgrading database from version $oldVersion to $newVersion",
        )

        // Log specific migrations for better debugging
        when {
            oldVersion < 8 && newVersion >= 8 -> {
                log("Migration: Added isFromStopValid and isToStopValid columns to SavedTrip table")
            }
        }
    }
}
