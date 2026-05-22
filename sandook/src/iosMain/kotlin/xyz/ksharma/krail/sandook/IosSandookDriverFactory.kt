package xyz.ksharma.krail.sandook

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.JournalMode
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter1
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter2
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter3
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter4
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter5
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter6
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter7
import xyz.ksharma.krail.sandook.migrations.SandookMigrationAfter8

class IosSandookDriverFactory : SandookDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = KrailSandook.Schema,
            name = "krailSandook.db",
            onConfiguration = { config ->
                // First-launch GTFS imports run multi-second write transactions. sqliter's
                // default 5s busy timeout is shorter than those inserts, so a competing writer
                // (e.g. theme selection) times out and crashes with SQLITE_BUSY. Raise the
                // timeout well past worst-case insert time and keep WAL explicit so readers
                // never block writers. The real fix for cross-pool contention is the single
                // shared driver in the DI module; this is defence-in-depth.
                config.copy(
                    journalMode = JournalMode.WAL,
                    extendedConfig = config.extendedConfig.copy(
                        busyTimeout = BUSY_TIMEOUT_MS,
                    ),
                )
            },
            callbacks = getMigrationCallbacks(),
        )
    }

    @Suppress("MagicNumber")
    private fun getMigrationCallbacks(): Array<AfterVersion> = arrayOf(
        AfterVersion(1) { SandookMigrationAfter1.migrate(it) },
        AfterVersion(2) { SandookMigrationAfter2.migrate(it) },
        AfterVersion(3) { SandookMigrationAfter3.migrate(it) },
        AfterVersion(4) { SandookMigrationAfter4.migrate(it) },
        AfterVersion(5) { SandookMigrationAfter5.migrate(it) },
        AfterVersion(6) { SandookMigrationAfter6.migrate(it) },
        AfterVersion(7) { SandookMigrationAfter7.migrate(it) },
        AfterVersion(8) { SandookMigrationAfter8.migrate(it) },
    )

    private companion object {
        // Generous timeout (30s) so a slow first-launch GTFS insert never starves another writer.
        const val BUSY_TIMEOUT_MS = 30_000
    }
}
