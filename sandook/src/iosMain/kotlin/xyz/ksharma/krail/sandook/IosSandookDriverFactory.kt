package xyz.ksharma.krail.sandook

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter1
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter2
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter3
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter4
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter5
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter6

class IosSandookDriverFactory : SandookDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = KrailSandook.Schema,
            name = "krailSandook.db",
            callbacks = getMigrationCallbacks(),
        )
    }

    private fun getMigrationCallbacks(): Array<AfterVersion> = arrayOf(
        AfterVersion(1) { SandookMigrationAfter1.migrate(it) },
        AfterVersion(2) { SandookMigrationAfter2.migrate(it) },
        AfterVersion(3) { SandookMigrationAfter3.migrate(it) },
        AfterVersion(4) { SandookMigrationAfter4.migrate(it) },
        AfterVersion(5) { SandookMigrationAfter5.migrate(it) },
        AfterVersion(6) { SandookMigrationAfter6.migrate(it) },
    )
}
