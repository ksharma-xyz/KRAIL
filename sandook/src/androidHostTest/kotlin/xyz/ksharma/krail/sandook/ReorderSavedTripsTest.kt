package xyz.ksharma.krail.sandook

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.ksharma.krail.sandook.db.KrailSandook
import kotlin.test.assertEquals

/**
 * What Done on home's reorder mode writes, against real SQLite: the new order and the removals in
 * one call, with the sort orders counted from zero in the order given.
 */
@RunWith(RobolectricTestRunner::class)
class ReorderSavedTripsTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var sandook: Sandook

    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KrailSandook.Schema.create(driver)
        sandook = RealSandook(KrailSandook(driver), Dispatchers.Unconfined)
        listOf("A", "B", "C", "D").forEachIndexed { index, from ->
            sandook.insertOrReplaceTrip("$from->Z", from, "From $from", "Z", "To Z")
            sandook.updateSavedTripSortOrder("$from->Z", index.toLong())
        }
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun stored(): List<Pair<String, Long>> =
        sandook.selectAllTrips().sortedBy { it.sort_order }.map { it.tripId to it.sort_order }

    @Test
    fun `the given order becomes sort orders from zero`() {
        sandook.reorderSavedTrips(orderedTripIds = listOf("C->Z", "A->Z", "D->Z", "B->Z"), removedTripIds = emptySet())

        assertEquals(
            listOf("C->Z" to 0L, "A->Z" to 1L, "D->Z" to 2L, "B->Z" to 3L),
            stored(),
        )
    }

    @Test
    fun `a removed trip is deleted and the rest are counted without a gap`() {
        sandook.reorderSavedTrips(orderedTripIds = listOf("D->Z", "A->Z", "C->Z"), removedTripIds = setOf("B->Z"))

        assertEquals(listOf("D->Z" to 0L, "A->Z" to 1L, "C->Z" to 2L), stored())
    }

    @Test
    fun `no trips and no removals writes nothing`() {
        sandook.reorderSavedTrips(orderedTripIds = emptyList(), removedTripIds = emptySet())

        assertEquals(listOf("A->Z", "B->Z", "C->Z", "D->Z"), stored().map { it.first })
    }

    @Test
    fun `a removed id that is not saved, and an ordered id that is not saved, change nothing else`() {
        sandook.reorderSavedTrips(orderedTripIds = listOf("B->Z", "X->Z", "A->Z"), removedTripIds = setOf("Y->Z"))

        assertEquals(setOf("A->Z", "B->Z", "C->Z", "D->Z"), stored().map { it.first }.toSet())
        assertEquals(0L, sandook.selectTripById("B->Z")?.sort_order)
        assertEquals(2L, sandook.selectTripById("A->Z")?.sort_order)
    }
}
