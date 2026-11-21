package xyz.ksharma.krail.sandook

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.ksharma.krail.sandook.db.KrailSandook
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * What the stops and bus routes inserts rely on, against real SQLite and not a fake: clearing the
 * tables inside the insert transaction means a reader keeps the old rows until the commit, and a
 * failed insert undoes the clear.
 *
 * The reader is a second connection to the same file in WAL mode. An in-memory database would not
 * do: it is one connection, so the reader would see the writer's own uncommitted rows.
 *
 * What this proves, and what it does not. It proves the behaviour on the JDBC test driver. The app
 * sets WAL explicitly on iOS (`JournalMode.WAL` in `IosSandookDriverFactory`) and on Android
 * (`enableWriteAheadLogging()` in `SandookCallback.onOpen`, passed in by
 * `AndroidSandookDriverFactory`), and the `-wal` and `-shm` files have been seen next to the
 * database on a real Android phone. This test does not run the Android or iOS drivers themselves.
 */
@RunWith(RobolectricTestRunner::class)
class NswTablesReplaceTransactionTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var writerDriver: JdbcSqliteDriver
    private lateinit var readerDriver: JdbcSqliteDriver
    private lateinit var writer: Sandook
    private lateinit var reader: Sandook
    private lateinit var writerBus: NswBusRoutesSandook
    private lateinit var readerBus: NswBusRoutesSandook

    @Before
    fun setup() {
        val url = "jdbc:sqlite:${folder.newFile("krail.db").absolutePath}"
        val wal = Properties().apply { setProperty("journal_mode", "WAL") }
        writerDriver = JdbcSqliteDriver(url, wal)
        KrailSandook.Schema.create(writerDriver)
        readerDriver = JdbcSqliteDriver(url, wal)
        assertEquals("wal", journalMode(readerDriver), "the test must run in the mode production uses")
        val writerDb = KrailSandook(writerDriver)
        val readerDb = KrailSandook(readerDriver)
        writer = RealSandook(writerDb, Dispatchers.Unconfined)
        reader = RealSandook(readerDb, Dispatchers.Unconfined)
        writerBus = RealNswBusRoutesSandook(writerDb)
        readerBus = RealNswBusRoutesSandook(readerDb)

        listOf("OLD1", "OLD2").forEach { id ->
            writer.insertNswStop(id, "Old $id", 0.0, 0.0, isParent = true)
            writer.insertNswStopProductClass(id, productClass = 1)
            writerBus.insertBusRouteGroup(id)
        }
    }

    private fun journalMode(driver: JdbcSqliteDriver): String? = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA journal_mode",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
        parameters = 0,
    ).value

    @After
    fun tearDown() {
        writerDriver.close()
        readerDriver.close()
    }

    @Test
    fun `a reader keeps the old stops until the replacing transaction commits`() {
        var seenMidway = -1

        writer.insertTransaction {
            writer.clearNswStopsTable()
            writer.clearNswProductClassTable()
            writer.insertNswStop("NEW1", "New 1", 0.0, 0.0, isParent = true)
            seenMidway = reader.stopsCount()
        }

        assertEquals(2, seenMidway, "the old rows must stay readable until the commit")
        assertEquals(1, reader.stopsCount())
    }

    @Test
    fun `a clear committed on its own empties the table for readers at once`() {
        // The layout this change moved away from. It proves the reader above can see the difference.
        writer.clearNswStopsTable()

        assertEquals(0, reader.stopsCount())
    }

    @Test
    fun `a failure inside the transaction undoes the clear`() {
        assertFailsWith<IllegalStateException> {
            writer.insertTransaction {
                writer.clearNswStopsTable()
                writer.clearNswProductClassTable()
                writer.insertNswStop("NEW1", "New 1", 0.0, 0.0, isParent = true)
                error("insert failed")
            }
        }

        assertEquals(2, writer.stopsCount())
        assertEquals(2, reader.stopsCount())
        assertEquals(2, writer.productClassCount())
    }

    @Test
    fun `bus routes keep the old rows for a reader until commit`() {
        var seenMidway = -1

        writerBus.insertTransaction {
            writerBus.clearNswBusRoutesData()
            writerBus.insertBusRouteGroup("NEW1")
            seenMidway = readerBus.busRouteGroupsCount()
        }

        assertEquals(2, seenMidway)
        assertEquals(1, readerBus.busRouteGroupsCount())
    }

    @Test
    fun `a failed bus routes insert undoes its clear`() {
        assertFailsWith<IllegalStateException> {
            writerBus.insertTransaction {
                writerBus.clearNswBusRoutesData()
                error("insert failed")
            }
        }

        assertEquals(2, writerBus.busRouteGroupsCount())
        assertEquals(2, readerBus.busRouteGroupsCount())
    }
}
