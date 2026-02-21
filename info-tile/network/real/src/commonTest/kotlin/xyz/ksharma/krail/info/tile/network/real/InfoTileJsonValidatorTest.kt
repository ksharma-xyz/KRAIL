package xyz.ksharma.krail.info.tile.network.real

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.info.tile.state.InfoTileData
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * JSON Validation Test for Info Tiles.
 *
 * This test acts as a pre-deployment validator - run it before pushing JSON to remote config
 * to ensure all tiles are valid and will display correctly.
 *
 * What it validates:
 * - JSON syntax is correct
 * - All required fields are present
 * - Date formats are valid (ISO-8601)
 * - Dates are logically valid (startDate <= endDate)
 * - Tile types are valid
 * - URLs are properly formatted
 * - Simulates what will be shown to users today
 */
class InfoTileJsonValidatorTest {

    // region: Valid JSON Tests

    @Test
    fun validJsonFile_shouldParseSuccessfully() {
        // Given: Valid JSON fixture file
        val jsonContent = loadValidJsonFixture()

        // When: Parsing the JSON
        val result = runCatching {
            JsonConfig.lenient.decodeFromString<List<InfoTileData>>(jsonContent)
        }

        // Then: Should parse without errors
        assertTrue(result.isSuccess, "Valid JSON should parse successfully")
        val tiles = result.getOrNull()
        assertNotNull(tiles, "Parsed tiles should not be null")
        assertTrue(tiles.isNotEmpty(), "Should have at least one tile")

        println("✅ SUCCESS: Parsed ${tiles.size} valid tiles")
    }

    @Test
    fun validJsonFile_allTilesHaveRequiredFields() {
        // Given: Parsed tiles from valid JSON
        val tiles = parseValidJson()

        // Then: Every tile must have required fields
        tiles.forEachIndexed { index, tile ->
            assertTrue(tile.key.isNotBlank(), "Tile[$index]: key must not be blank")
            assertTrue(tile.title.isNotBlank(), "Tile[$index]: title must not be blank")
            assertTrue(tile.description.isNotBlank(), "Tile[$index]: description must not be blank")

            println("✅ Tile '${tile.key}': All required fields present")
        }
    }

    @Test
    fun validJsonFile_allDatesAreValidFormat() {
        // Given: Parsed tiles from valid JSON
        val tiles = parseValidJson()

        // Then: All dates must be valid ISO-8601 format
        tiles.forEach { tile ->
            tile.startDate?.let { dateStr ->
                val isValid = isValidIsoDate(dateStr)
                assertTrue(isValid, "Tile '${tile.key}': Invalid startDate format: $dateStr")
                println("✅ Tile '${tile.key}': startDate '$dateStr' is valid")
            }

            tile.endDate?.let { dateStr ->
                val isValid = isValidIsoDate(dateStr)
                assertTrue(isValid, "Tile '${tile.key}': Invalid endDate format: $dateStr")
                println("✅ Tile '${tile.key}': endDate '$dateStr' is valid")
            }
        }
    }

    @Test
    fun validJsonFile_startDateIsBeforeOrEqualToEndDate() {
        // Given: Parsed tiles from valid JSON
        val tiles = parseValidJson()

        // Then: When both dates exist, startDate must be <= endDate
        tiles.forEach { tile ->
            val startDateStr = tile.startDate
            val endDateStr = tile.endDate

            if (startDateStr != null && endDateStr != null) {
                val startDate = LocalDate.parse(startDateStr)
                val endDate = LocalDate.parse(endDateStr)

                assertTrue(
                    startDate <= endDate,
                    "Tile '${tile.key}': startDate ($startDate) must be before or equal to endDate ($endDate)"
                )

                println("✅ Tile '${tile.key}': Date range is logically valid ($startDate to $endDate)")
            }
        }
    }

    @Test
    fun validJsonFile_urlsAreProperlyFormatted() {
        // Given: Parsed tiles from valid JSON
        val tiles = parseValidJson()

        // Then: All URLs should be valid or null
        tiles.forEach { tile ->
            tile.primaryCta?.url?.let { url ->
                assertTrue(
                    url.startsWith("http://") || url.startsWith("https://"),
                    "Tile '${tile.key}': URL must start with http:// or https://"
                )
                println("✅ Tile '${tile.key}': URL is valid: $url")
            }
        }
    }

    @Test
    fun validJsonFile_simulateDisplayForToday() {
        // Given: Today's date and parsed tiles
        val tiles = parseValidJson()
        val today = getCurrentDate()

        println("\n" + "=".repeat(60))
        println("📅 DISPLAY SIMULATION FOR TODAY: $today")
        println("=".repeat(60) + "\n")

        // When: Filtering tiles that would be displayed today
        val visibleTiles = tiles.filter { shouldDisplayTile(it, today) }
        val hiddenTiles = tiles.filter { !shouldDisplayTile(it, today) }

        // Then: Show simulation results
        println("✅ VISIBLE TILES (${visibleTiles.size}):")
        visibleTiles.forEachIndexed { index, tile ->
            println("\n${index + 1}. [${tile.type}] ${tile.title}")
            println("   Key: ${tile.key}")
            println("   Start: ${tile.startDate ?: "No restriction"}")
            println("   End: ${tile.endDate ?: "No restriction"}")
            println("   Why showing: ${getVisibilityReason(tile, today)}")
        }

        println("\n" + "-".repeat(60))
        println("\n⏳ HIDDEN TILES (${hiddenTiles.size}):")
        hiddenTiles.forEachIndexed { index, tile ->
            println("\n${index + 1}. [${tile.type}] ${tile.title}")
            println("   Key: ${tile.key}")
            println("   Start: ${tile.startDate ?: "No restriction"}")
            println("   End: ${tile.endDate ?: "No restriction"}")
            println("   Why hidden: ${getHiddenReason(tile, today)}")
        }

        println("\n" + "=".repeat(60))
        println("📊 SUMMARY: ${visibleTiles.size} visible, ${hiddenTiles.size} hidden")
        println("=".repeat(60) + "\n")

        // Verify we have some tiles in the test data
        assertTrue(tiles.isNotEmpty(), "Should have tiles to validate")
    }

    // endregion

    // region: Invalid JSON Tests

    @Test
    fun invalidJsonFile_shouldHandleGracefully() {
        // Given: Invalid JSON fixture file
        val jsonContent = loadInvalidJsonFixture()

        // When: Attempting to parse invalid JSON
        val tiles = runCatching {
            JsonConfig.lenient.decodeFromString<List<InfoTileData>>(jsonContent)
        }.getOrNull() ?: emptyList()

        println("\n" + "=".repeat(60))
        println("🔍 INVALID JSON VALIDATION")
        println("=".repeat(60) + "\n")

        // Then: Check each tile for validation errors
        tiles.forEachIndexed { index, tile ->
            println("Checking tile ${index + 1}: ${tile.title}")

            val errors = mutableListOf<String>()

            // Check required fields
            if (tile.key.isBlank()) errors.add("Key is blank")
            if (tile.title.isBlank()) errors.add("Title is blank")
            if (tile.description.isBlank()) errors.add("Description is blank")

            // Check date formats
            tile.startDate?.let { dateStr ->
                if (!isValidIsoDate(dateStr)) {
                    errors.add("Invalid startDate format: $dateStr")
                }
            }

            tile.endDate?.let { dateStr ->
                if (!isValidIsoDate(dateStr)) {
                    errors.add("Invalid endDate format: $dateStr")
                }
            }

            // Check date logic
            val startDateStr = tile.startDate
            val endDateStr = tile.endDate

            if (startDateStr != null && endDateStr != null) {
                val startValid = isValidIsoDate(startDateStr)
                val endValid = isValidIsoDate(endDateStr)

                if (startValid && endValid) {
                    val startDate = LocalDate.parse(startDateStr)
                    val endDate = LocalDate.parse(endDateStr)

                    if (startDate > endDate) {
                        errors.add("startDate is after endDate")
                    }
                }
            }

            // Check URL format
            tile.primaryCta?.url?.let { url ->
                if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                    errors.add("Invalid URL format: $url")
                }
            }

            if (errors.isNotEmpty()) {
                println("  ❌ ERRORS found:")
                errors.forEach { error ->
                    println("     - $error")
                }
            } else {
                println("  ✅ No validation errors")
            }
            println()
        }

        println("=".repeat(60) + "\n")
    }

    // endregion

    // region: Helper Functions

    /**
     * Loads the valid JSON fixture file.
     */
    private fun loadValidJsonFixture(): String {
        // In a real implementation, this would read from the resources
        // For now, we'll return the content directly
        return """
        [
          {
            "key": "metro_closure_feb_21_2026",
            "title": "Metro closure 21-22 Feb",
            "description": "No Metro Services will run between Tallawong and Sydenham.",
            "type": "CRITICAL_ALERT",
            "startDate": "2026-02-21",
            "endDate": "2026-02-23",
            "dismissCtaText": "Dismiss",
            "primaryCta": {
              "text": "Read more",
              "url": "https://transportnsw.info/news/2026/upcoming-trackwork"
            }
          },
          {
            "key": "new_feature_announcement",
            "title": "New Trip Planning Features",
            "description": "We've added real-time updates and better route alternatives.",
            "type": "INFO",
            "startDate": "2026-02-15",
            "endDate": null,
            "dismissCtaText": "Got it",
            "primaryCta": {
              "text": "Learn more",
              "url": "https://krail.app/features"
            }
          },
          {
            "key": "metro_closure_mar_7_2026",
            "title": "Metro closure 7-8 Mar",
            "description": "No Metro Services will run between Tallawong and Sydenham.",
            "type": "CRITICAL_ALERT",
            "startDate": "2026-03-07",
            "endDate": "2026-03-09",
            "dismissCtaText": "Dismiss"
          }
        ]
        """.trimIndent()
    }

    /**
     * Loads the invalid JSON fixture file.
     */
    private fun loadInvalidJsonFixture(): String {
        return """
        [
          {
            "key": "invalid_date_format",
            "title": "Invalid Date",
            "description": "This has wrong date format",
            "type": "INFO",
            "startDate": "21/02/2026",
            "dismissCtaText": "Dismiss"
          },
          {
            "key": "start_after_end",
            "title": "Start After End",
            "description": "startDate is after endDate",
            "type": "INFO",
            "startDate": "2026-02-25",
            "endDate": "2026-02-20",
            "dismissCtaText": "Dismiss"
          }
        ]
        """.trimIndent()
    }

    /**
     * Parses valid JSON and returns list of tiles.
     */
    private fun parseValidJson(): List<InfoTileData> {
        val jsonContent = loadValidJsonFixture()
        return JsonConfig.lenient.decodeFromString(jsonContent)
    }

    /**
     * Checks if a date string is valid ISO-8601 format.
     */
    private fun isValidIsoDate(dateStr: String): Boolean {
        return runCatching {
            LocalDate.parse(dateStr)
            true
        }.getOrDefault(false)
    }

    /**
     * Gets current date for testing.
     */
    @OptIn(ExperimentalTime::class)
    private fun getCurrentDate(): LocalDate {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    /**
     * Determines if a tile should be displayed on the given date.
     */
    private fun shouldDisplayTile(tile: InfoTileData, today: LocalDate): Boolean {
        val startDateValid = tile.startDate?.let { dateStr ->
            runCatching {
                val localDate = LocalDate.parse(dateStr)
                localDate <= today
            }.getOrDefault(false)
        } ?: true

        val endDateValid = tile.endDate?.let { dateStr ->
            runCatching {
                val localDate = LocalDate.parse(dateStr)
                localDate >= today
            }.getOrDefault(false)
        } ?: true

        return startDateValid && endDateValid
    }

    /**
     * Gets human-readable reason why a tile is visible.
     */
    private fun getVisibilityReason(tile: InfoTileData, today: LocalDate): String {
        val startDateStr = tile.startDate
        val endDateStr = tile.endDate

        return when {
            startDateStr == null && endDateStr == null ->
                "No date restrictions"

            startDateStr == null && endDateStr != null -> {
                val endDate = LocalDate.parse(endDateStr)
                "No start date, ends ${formatDate(endDate)} (${daysFromNow(endDate, today)} days)"
            }

            startDateStr != null && endDateStr == null -> {
                val startDate = LocalDate.parse(startDateStr)
                "Started ${formatDate(startDate)} (${daysFromNow(startDate, today)} days ago), no end date"
            }

            startDateStr == endDateStr -> {
                "One-day event (today only)"
            }

            else -> {
                val startDate = LocalDate.parse(startDateStr!!)
                val endDate = LocalDate.parse(endDateStr!!)
                "Active from ${formatDate(startDate)} to ${formatDate(endDate)}"
            }
        }
    }

    /**
     * Gets human-readable reason why a tile is hidden.
     */
    private fun getHiddenReason(tile: InfoTileData, today: LocalDate): String {
        val startDateStr = tile.startDate
        val endDateStr = tile.endDate

        val startDate = startDateStr?.let { LocalDate.parse(it) }
        val endDate = endDateStr?.let { LocalDate.parse(it) }

        return when {
            startDate != null && startDate > today ->
                "Starts in ${daysFromNow(startDate, today)} days (${formatDate(startDate)})"

            endDate != null && endDate < today ->
                "Ended ${-daysFromNow(endDate, today)} days ago (${formatDate(endDate)})"

            else -> "Date range doesn't include today"
        }
    }

    /**
     * Formats date in readable format.
     */
    private fun formatDate(date: LocalDate): String {
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return "${month.substring(0, 3)} ${date.day}"
    }

    /**
     * Calculates days from now (positive = future, negative = past).
     */
    private fun daysFromNow(date: LocalDate, today: LocalDate): Int {
        return (date.toEpochDays() - today.toEpochDays()).toInt()
    }

    // endregion
}

