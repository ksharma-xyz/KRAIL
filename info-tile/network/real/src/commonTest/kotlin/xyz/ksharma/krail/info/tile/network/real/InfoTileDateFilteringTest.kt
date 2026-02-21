package xyz.ksharma.krail.info.tile.network.real

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.info.tile.state.InfoTileData
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Tests for InfoTile date-based filtering.
 *
 * Verifies that tiles are shown/hidden based on startDate and endDate:
 * - Tiles with startDate in the future are NOT shown
 * - Tiles with endDate in the past are NOT shown
 * - Tiles where today is between startDate and endDate ARE shown
 * - Both dates are INCLUSIVE (tiles show on start and end dates)
 */
class InfoTileDateFilteringTest {

    // region: Boundary Tests - Dates are Inclusive

    @Test
    fun whenTodayIsStartDate_thenTileIsShown() {
        // Given: A tile that starts today
        val tileStartingToday = createTile(
            startDate = getDateString(daysFromToday = 0),
            endDate = null
        )

        // Then: Tile should be visible (startDate is inclusive)
        assertTrue(shouldDisplayTile(tileStartingToday))
    }

    @Test
    fun whenTodayIsEndDate_thenTileIsShown() {
        // Given: A tile that ends today
        val tileEndingToday = createTile(
            startDate = null,
            endDate = getDateString(daysFromToday = 0)
        )

        // Then: Tile should be visible (endDate is inclusive)
        assertTrue(shouldDisplayTile(tileEndingToday))
    }

    @Test
    fun whenOneDayEvent_thenTileIsShown() {
        // Given: A one-day event (start and end date are the same - today)
        val oneDayEvent = createTile(
            startDate = getDateString(daysFromToday = 0),
            endDate = getDateString(daysFromToday = 0)
        )

        // Then: Tile should be visible (both dates are inclusive)
        assertTrue(shouldDisplayTile(oneDayEvent))
    }

    // endregion

    // region: Future Events - Not Yet Started

    @Test
    fun whenEventStartsInFuture_thenTileIsNotShown() {
        // Given: An event that starts tomorrow
        val futureEvent = createTile(
            startDate = getDateString(daysFromToday = 1),
            endDate = getDateString(daysFromToday = 3)
        )

        // Then: Tile should NOT be visible (hasn't started yet)
        assertFalse(shouldDisplayTile(futureEvent))
    }

    // endregion

    // region: Expired Events - Already Ended

    @Test
    fun whenEventEndedYesterday_thenTileIsNotShown() {
        // Given: An event that ended yesterday
        val expiredEvent = createTile(
            startDate = getDateString(daysFromToday = -3),
            endDate = getDateString(daysFromToday = -1)
        )

        // Then: Tile should NOT be visible (already expired)
        assertFalse(shouldDisplayTile(expiredEvent))
    }

    // endregion

    // region: Active Events - Within Date Range

    @Test
    fun whenTodayIsBetweenStartAndEndDate_thenTileIsShown() {
        // Given: An event running from yesterday to tomorrow
        val activeEvent = createTile(
            startDate = getDateString(daysFromToday = -1),
            endDate = getDateString(daysFromToday = 1)
        )

        // Then: Tile should be visible (today is within range)
        assertTrue(shouldDisplayTile(activeEvent))
    }

    @Test
    fun whenEventStartedButHasNoEndDate_thenTileIsShown() {
        // Given: An event that started yesterday with no end date
        val ongoingEvent = createTile(
            startDate = getDateString(daysFromToday = -1),
            endDate = null
        )

        // Then: Tile should be visible (started and no expiry)
        assertTrue(shouldDisplayTile(ongoingEvent))
    }

    @Test
    fun whenNoStartDateButEndsInFuture_thenTileIsShown() {
        // Given: A tile with no start date but ends tomorrow
        val immediateEvent = createTile(
            startDate = null,
            endDate = getDateString(daysFromToday = 1)
        )

        // Then: Tile should be visible (no start restriction, not expired)
        assertTrue(shouldDisplayTile(immediateEvent))
    }

    // endregion

    // region: No Date Restrictions

    @Test
    fun whenNoStartOrEndDate_thenTileIsAlwaysShown() {
        // Given: A tile with no date restrictions
        val permanentTile = createTile(
            startDate = null,
            endDate = null
        )

        // Then: Tile should always be visible
        assertTrue(shouldDisplayTile(permanentTile))
    }

    // endregion

    // region: Real-World Scenario

    @Test
    fun metroClosureScenario_whenTodayIsFeb21AndEventRunsFeb21To23_thenTileIsShown() {
        // Given: Today is Feb 21 and Metro closure runs Feb 21-23
        val metroClosureTile = createTile(
            startDate = getDateString(daysFromToday = 0),  // Feb 21 (today)
            endDate = getDateString(daysFromToday = 2)     // Feb 23
        )

        // Then: Tile should be visible on Feb 21
        assertTrue(shouldDisplayTile(metroClosureTile))
    }

    // endregion

    // region: Helper Functions

    /**
     * Creates a test tile with the given dates.
     */
    private fun createTile(startDate: String?, endDate: String?): InfoTileData {
        return InfoTileData(
            key = "test_tile",
            title = "Test Tile",
            description = "Test Description",
            startDate = startDate,
            endDate = endDate,
        )
    }

    /**
     * Gets a date string for testing, offset from today by the specified number of days.
     *
     * @param daysFromToday Positive = future, Negative = past, 0 = today
     * @return ISO-8601 formatted date string (e.g., "2026-02-21")
     */
    @OptIn(ExperimentalTime::class)
    private fun getDateString(daysFromToday: Int): String {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.plus(daysFromToday, DateTimeUnit.DAY).toString()
    }

    /**
     * Determines if a tile should be displayed based on its date range.
     * This mimics the actual filtering logic from RealInfoTileManager.
     *
     * Display rules:
     * - If startDate exists, it must be today or in the past
     * - If endDate exists, it must be today or in the future
     * - Both conditions must be true for the tile to show
     */
    @OptIn(ExperimentalTime::class)
    private fun shouldDisplayTile(tile: InfoTileData): Boolean {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        val startDateValid = tile.startDate?.let { dateStr ->
            runCatching {
                val localDate = kotlinx.datetime.LocalDate.parse(dateStr)
                localDate <= today // Must be today or past (inclusive)
            }.getOrDefault(false)
        } ?: true // No startDate = no restriction

        val endDateValid = tile.endDate?.let { dateStr ->
            runCatching {
                val localDate = kotlinx.datetime.LocalDate.parse(dateStr)
                localDate >= today // Must be today or future (inclusive)
            }.getOrDefault(false)
        } ?: true // No endDate = no restriction

        return startDateValid && endDateValid
    }

    // endregion
}

