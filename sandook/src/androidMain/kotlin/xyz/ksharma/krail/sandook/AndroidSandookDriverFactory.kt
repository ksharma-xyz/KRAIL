package xyz.ksharma.krail.sandook

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidSandookDriverFactory(private val context: Context) : SandookDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = KrailSandook.Schema,
            context = context,
            name = "krailSandook.db",
            callback = SandookCallback(KrailSandook.Schema)
        )
    }
}
