package xyz.ksharma.krail.sandook

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import xyz.ksharma.krail.sandook.db.KrailSandook
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end name matching against real SQLite, not a fake.
 *
 * The bug these guard: NSW GTFS names are punctuated ("Wollongong Central, Burelli St") and
 * riders are not. The old predicate matched the raw query as one contiguous substring, so
 * typing the name without its comma returned "No match found" while typing it *with* the
 * comma worked. Asserting on the pattern string alone would not have caught that — only
 * running it through SQLite's own LIKE does.
 */
@RunWith(RobolectricTestRunner::class)
class NswStopsNameSearchTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var stops: NswStopsSandook

    @Before
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KrailSandook.Schema.create(driver)
        val sandook = KrailSandook(driver)
        stops = RealNswStopsSandook(sandook)

        listOf(
            "200060" to "Wollongong Central, Burelli St",
            "200061" to "Wollongong Central, Burelli St, Stand A",
            "200062" to "Wollongong Central, Burelli St, Stand B",
            "200070" to "Wollongong Station, Station St",
            "200080" to "Central Station, Eddy Ave, Stand C",
            "200090" to "Burwood Station, Burwood Rd",
            "200100" to "100% Fun Park, George St",
        ).forEach { (id, name) ->
            stops.insertStop(stopId = id, stopName = name, stopLat = 0.0, stopLon = 0.0, isParent = 1)
        }
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun search(query: String): List<String> =
        stops.selectProductClassesForStop(stopId = query, stopName = query).map { it.stopName }

    @Test
    fun `query without the comma finds a comma-punctuated stop`() {
        // The reported bug, verbatim: this returned nothing and the screen said "No match found".
        val results = search("wollongong central burell")

        assertTrue(
            results.any { it == "Wollongong Central, Burelli St" },
            "Expected the Burelli St stop, got: $results",
        )
    }

    @Test
    fun `query with and without the comma return identical results`() {
        // Punctuation the rider happens to type must not change what they are shown.
        assertEquals(
            search("wollongong central, burell").toSet(),
            search("wollongong central burell").toSet(),
        )
    }

    @Test
    fun `all stands of a stop are returned, not just the parent`() {
        val results = search("wollongong central burelli")

        assertEquals(
            setOf(
                "Wollongong Central, Burelli St",
                "Wollongong Central, Burelli St, Stand A",
                "Wollongong Central, Burelli St, Stand B",
            ),
            results.toSet(),
        )
    }

    @Test
    fun `matching stays order-preserving so tokens in the wrong order do not match`() {
        // Reordered tokens are the fuzzy ranker's job. If this predicate matched them it
        // would also match half the state, which is the failure mode it exists to avoid.
        assertTrue(search("burelli wollongong").isEmpty())
    }

    @Test
    fun `single-token query behaves exactly as before`() {
        val results = search("burwood")

        assertEquals(listOf("Burwood Station, Burwood Rd"), results)
    }

    @Test
    fun `a typed percent is a literal, not a wildcard`() {
        // Without ESCAPE, "%" matched every stop in the state.
        val results = search("100%")

        assertEquals(listOf("100% Fun Park, George St"), results)
    }

    @Test
    fun `a typed underscore is a literal, not a single-character wildcard`() {
        // "_" would otherwise match any one character, so this would surface Burwood.
        assertTrue(search("burwoo_").isEmpty())
    }

    @Test
    fun `exact stopId match still works`() {
        val results = stops.selectProductClassesForStop(stopId = "200070", stopName = "zzzz nothing")

        assertEquals(listOf("Wollongong Station, Station St"), results.map { it.stopName })
    }
}
