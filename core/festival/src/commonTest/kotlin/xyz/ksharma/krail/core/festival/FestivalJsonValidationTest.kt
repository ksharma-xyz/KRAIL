package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.FestivalData
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Comprehensive JSON validation tests for festival configurations.
 * These tests validate that a festival JSON configuration works correctly
 * across multiple scenarios and edge cases.
 *
 * Use these tests to validate your festival JSON before deploying to production.
 */
class FestivalJsonValidationTest {

    private lateinit var fakeFlag: FakeFlag
    private lateinit var manager: RealFestivalManager

    @BeforeTest
    fun setup() {
        fakeFlag = FakeFlag()
        manager = RealFestivalManager(fakeFlag)
    }

    /**
     * Comprehensive test to validate a festival JSON configuration.
     * This test validates the JSON against multiple test scenarios and provides detailed feedback.
     *
     * To use this test:
     * 1. Replace the testFestivalJson variable with your JSON string
     * 2. Or read from a file (see commented example below)
     * 3. Run the test to see validation results
     */
    @Test
    fun testValidateFestivalJson_comprehensive() {
        // To read from a file instead, replace the testFestivalJson initialization above
        // with code to read your JSON file

        val validationResult = validateFestivalJson(prodJsonTest)

        // Print validation results
        println(validationResult.getReport())

        // Assert that validation passed
        assertTrue(
            validationResult.isValid,
            "Festival JSON validation failed. See report:\n${validationResult.getReport()}"
        )
    }

    /**
     * Example test for a specific JSON configuration.
     * You can duplicate this test and modify the JSON to test different configurations.
     */
    @Test
    fun testValidateFestivalJson_example() {
        val myFestivalJson = """
        {
            "confirmedDates": [],
            "variableDates": [
                {
                    "type": "DIWALI_2024",
                    "startDate": "2024-11-01",
                    "endDate": "2024-11-05",
                    "emojiList": ["🪔", "✨"],
                    "greeting": "Happy Diwali!"
                }
            ]
        }
        """.trimIndent()

        val result = validateFestivalJson(myFestivalJson)
        println(result.getReport())
        assertTrue(result.isValid, result.getReport())
    }

    /**
     * Test for validating production festival JSON.
     * Replace with your actual production JSON to validate before deployment.
     */
    @Test
    fun testValidateFestivalJson_production() {
        // TODO: Replace this with your actual production JSON
        val productionFestivalJson = """
        {
            "confirmedDates": [
                {
                    "type": "NEW_YEAR",
                    "month": 1,
                    "day": 1,
                    "emojiList": ["🎉", "🥳", "🎊"],
                    "greeting": "Happy New Year! 🎉"
                },
                {
                    "type": "AUSTRALIA_DAY",
                    "month": 1,
                    "day": 26,
                    "emojiList": ["🇦🇺"],
                    "greeting": "Happy Australia Day!"
                },
                {
                    "type": "VALENTINES_DAY",
                    "month": 2,
                    "day": 14,
                    "emojiList": ["❤️", "💕", "💖"],
                    "greeting": "Happy Valentine's Day!"
                },
                {
                    "type": "HALLOWEEN",
                    "month": 10,
                    "day": 31,
                    "emojiList": ["🎃", "👻", "🦇"],
                    "greeting": "Happy Halloween!"
                },
                {
                    "type": "CHRISTMAS",
                    "month": 12,
                    "day": 25,
                    "emojiList": ["🎄", "🎅", "🎁"],
                    "greeting": "Merry Christmas!"
                }
            ],
            "variableDates": []
        }
        """.trimIndent()

        val result = validateFestivalJson(productionFestivalJson)
        println(result.getReport())
        assertTrue(result.isValid, result.getReport())
    }

    // ========== Validation Helper Functions ==========

    private fun validateFestivalJson(jsonString: String): ValidationResult {
        val results = mutableListOf<TestResult>()

        println("\n🔍 Festival JSON Validation")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        // Test 1: JSON parsing
        val parseResult = testJsonParsing(jsonString)
        results.add(parseResult)
        if (!parseResult.passed) {
            println("❌ ${parseResult.testName}: ${parseResult.message}")
            return ValidationResult(false, results)
        }
        println("✅ ${parseResult.testName}")

        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(jsonString))
        val localManager = RealFestivalManager(fakeFlag)

        // Test 2: Date format validation
        val dateFormatResult = testDateFormatValidation(jsonString)
        results.add(dateFormatResult)
        println("${if (dateFormatResult.passed) "✅" else "❌"} ${dateFormatResult.testName}")
        if (!dateFormatResult.passed) println("   ${dateFormatResult.message}")

        // Test 3: Date range validation
        val rangeResult = testDateRangeValidation(localManager, jsonString)
        results.add(rangeResult)
        println("${if (rangeResult.passed) "✅" else "❌"} ${rangeResult.testName}")
        if (!rangeResult.passed) println("   ${rangeResult.message}")

        // Test 4: Emoji validation
        val emojiResult = testEmojiValidation(jsonString)
        results.add(emojiResult)
        println("${if (emojiResult.passed) "✅" else "❌"} ${emojiResult.testName}")

        // Test 5: Overlapping festivals detection
        val overlapResult = testOverlappingFestivals(jsonString)
        results.add(overlapResult)
        println("${if (overlapResult.passed) "✅" else "⚠️"} ${overlapResult.testName}")
        if (overlapResult.message.contains("overlap(s)")) {
            println(overlapResult.message)
        }

        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return ValidationResult(results.all { it.passed }, results)
    }

    private fun testJsonParsing(jsonString: String): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            TestResult(
                testName = "JSON Parsing (${data.confirmedDates.size} fixed, ${data.variableDates.size} variable)",
                passed = true,
                message = ""
            )
        } catch (e: Exception) {
            TestResult(
                testName = "JSON Parsing",
                passed = false,
                message = "Failed to parse: ${e.message}"
            )
        }
    }

    private fun testDateFormatValidation(jsonString: String): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            val errors = mutableListOf<String>()

            // Validate fixed date ranges
            data.confirmedDates.forEach { festival ->
                if (festival.month !in 1..12) {
                    errors.add("${festival.type}: Invalid month ${festival.month}")
                }
                if (festival.day !in 1..31) {
                    errors.add("${festival.type}: Invalid day ${festival.day}")
                }
                // Try to create actual date to catch invalid day/month combinations
                try {
                    LocalDate(2024, festival.month, festival.day)
                } catch (_: Exception) {
                    errors.add("${festival.type}: Invalid date ${festival.month}/${festival.day}")
                }
            }

            // Validate variable date formats and parse-ability
            data.variableDates.forEach { festival ->
                try {
                    val start = LocalDate.parse(festival.startDate)
                    val end = LocalDate.parse(festival.endDate)
                    if (start > end) {
                        errors.add("${festival.type}: Start date after end date")
                    }
                } catch (e: Exception) {
                    errors.add("${festival.type}: Invalid date format - ${e.message}")
                }
            }

            if (errors.isEmpty()) {
                TestResult(
                    testName = "Date Format Validation",
                    passed = true,
                    message = ""
                )
            } else {
                TestResult(
                    testName = "Date Format Validation",
                    passed = false,
                    message = errors.joinToString("\n   ")
                )
            }
        } catch (e: Exception) {
            TestResult(
                testName = "Date Format Validation",
                passed = false,
                message = "Error: ${e.message}"
            )
        }
    }

    private fun testDateRangeValidation(
        manager: RealFestivalManager,
        jsonString: String
    ): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            val errors = mutableListOf<String>()

            data.variableDates.forEach { festival ->
                try {
                    val startDate = LocalDate.parse(festival.startDate)
                    val endDate = LocalDate.parse(festival.endDate)

                    // Test boundary: day before start
                    val dayBefore = LocalDate.fromEpochDays(startDate.toEpochDays() - 1)
                    val beforeResult = manager.festivalOnDate(dayBefore)
                    if (beforeResult?.greeting == festival.greeting) {
                        errors.add("${festival.type}: Found on day before start ($dayBefore)")
                    }

                    // Test boundary: day after end
                    val dayAfter = LocalDate.fromEpochDays(endDate.toEpochDays() + 1)
                    val afterResult = manager.festivalOnDate(dayAfter)
                    if (afterResult?.greeting == festival.greeting) {
                        errors.add("${festival.type}: Found on day after end ($dayAfter)")
                    }
                } catch (_: Exception) {
                    // Skip invalid dates (already caught in date format validation)
                }
            }

            if (errors.isEmpty()) {
                TestResult(
                    testName = "Date Range Boundaries",
                    passed = true,
                    message = ""
                )
            } else {
                TestResult(
                    testName = "Date Range Boundaries",
                    passed = false,
                    message = errors.joinToString("\n   ")
                )
            }
        } catch (_: Exception) {
            TestResult(
                testName = "Date Range Boundaries",
                passed = false,
                message = "Error validating date ranges"
            )
        }
    }

    private fun testEmojiValidation(jsonString: String): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            val missing = mutableListOf<String>()

            data.confirmedDates.forEach { if (it.emojiList.isEmpty()) missing.add(it.type) }
            data.variableDates.forEach { if (it.emojiList.isEmpty()) missing.add(it.type) }

            if (missing.isEmpty()) {
                val total = data.confirmedDates.sumOf { it.emojiList.size } +
                        data.variableDates.sumOf { it.emojiList.size }
                TestResult(
                    testName = "Emoji Validation ($total emojis)",
                    passed = true,
                    message = ""
                )
            } else {
                TestResult(
                    testName = "Emoji Validation",
                    passed = false,
                    message = "Missing emojis: ${missing.joinToString(", ")}"
                )
            }
        } catch (e: Exception) {
            TestResult(
                testName = "Emoji Validation",
                passed = false,
                message = "Error: ${e.message}"
            )
        }
    }

    private fun testOverlappingFestivals(jsonString: String): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            val overlaps = mutableListOf<String>()

            // Check fixed vs variable overlaps
            data.confirmedDates.forEach { fixed ->
                data.variableDates.forEach { variable ->
                    val startDate = runCatching { LocalDate.parse(variable.startDate) }.getOrNull()
                    val endDate = runCatching { LocalDate.parse(variable.endDate) }.getOrNull()

                    if (startDate != null && endDate != null) {
                        var currentDate: LocalDate = startDate
                        while (currentDate <= endDate) {
                            if (currentDate.month.number == fixed.month && currentDate.day == fixed.day) {
                                overlaps.add("   ${currentDate}: ${fixed.type} vs ${variable.type} → Fixed wins")
                                break
                            }
                            currentDate = LocalDate.fromEpochDays(currentDate.toEpochDays() + 1)
                        }
                    }
                }
            }

            // Check variable vs variable overlaps
            for (i in data.variableDates.indices) {
                for (j in (i + 1) until data.variableDates.size) {
                    val var1 = data.variableDates[i]
                    val var2 = data.variableDates[j]

                    val start1 = runCatching { LocalDate.parse(var1.startDate) }.getOrNull()
                    val end1 = runCatching { LocalDate.parse(var1.endDate) }.getOrNull()
                    val start2 = runCatching { LocalDate.parse(var2.startDate) }.getOrNull()
                    val end2 = runCatching { LocalDate.parse(var2.endDate) }.getOrNull()

                    if (start1 != null && end1 != null && start2 != null && end2 != null) {
                        val hasOverlap = !(end1 < start2 || end2 < start1)
                        if (hasOverlap) {
                            val overlapStart = maxOf(start1, start2)
                            val overlapEnd = minOf(end1, end2)
                            overlaps.add("   ${overlapStart} to ${overlapEnd}: ${var1.type} vs ${var2.type} → First wins")
                        }
                    }
                }
            }

            if (overlaps.isEmpty()) {
                TestResult(
                    testName = "Overlap Detection",
                    passed = true,
                    message = ""
                )
            } else {
                val summary = buildString {
                    appendLine("\n   Found ${overlaps.size} overlap(s):")
                    append(overlaps.joinToString("\n"))
                    appendLine("\n   Note: Fixed dates always win over variable dates")
                    append("         First variable date wins over later ones")
                }
                TestResult(
                    testName = "Overlap Detection",
                    passed = true, // Informational, not a failure
                    message = summary
                )
            }
        } catch (e: Exception) {
            TestResult(
                testName = "Overlap Detection",
                passed = false,
                message = "Error: ${e.message}"
            )
        }
    }

    // ========== Data Classes for Validation Results ==========

    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val message: String
    )

    data class ValidationResult(
        val isValid: Boolean,
        val testResults: List<TestResult>
    ) {
        fun getReport(): String {
            val passed = testResults.count { it.passed }
            val total = testResults.size
            val status = if (isValid) "✅ PASSED" else "❌ FAILED"

            val failures = testResults.filter { !it.passed }
            return if (failures.isEmpty()) {
                "$status ($passed/$total tests)"
            } else {
                buildString {
                    appendLine("$status ($passed/$total tests)")
                    appendLine()
                    failures.forEach {
                        appendLine("${it.testName}:")
                        appendLine(it.message)
                    }
                }
            }
        }
    }

    private class FakeFlag : Flag {
        private val flagValues = mutableMapOf<String, FlagValue>()
        fun setFlagValue(key: String, value: FlagValue) {
            flagValues[key] = value
        }

        override fun getFlagValue(key: String): FlagValue {
            return flagValues[key] ?: FlagValue.BooleanValue(false)
        }
    }

    val prodJsonTest = """
        {
          "confirmedDates": [
            {
              "type": "NEW_YEAR",
              "month": 1,
              "day": 1,
              "emojiList": [
                "🎉"
              ],
              "greeting": "Happy New Year"
            },
            {
              "type": "AUSTRALIA_DAY",
              "month": 1,
              "day": 26,
              "emojiList": [
                "🇦🇺",
                "🎉",
                "🎆"
              ],
              "greeting": "Australia Day"
            },
            {
              "type": "ROSE_DAY",
              "month": 2,
              "day": 7,
              "emojiList": [
                "🌹"
              ],
              "greeting": "Rose Day"
            },
            {
              "type": "PROPOSE_DAY",
              "month": 2,
              "day": 8,
              "emojiList": [
                "💍",
                "💞",
                "💌"
              ],
              "greeting": "Propose Day"
            },
            {
              "type": "CHOCOLATE_DAY",
              "month": 2,
              "day": 9,
              "emojiList": [
                "🍫",
                "💞"
              ],
              "greeting": "Chocolate Day"
            },
            {
              "type": "TEDDY_DAY",
              "month": 2,
              "day": 10,
              "emojiList": [
                "🧸"
              ],
              "greeting": "Teddy Day"
            },
            {
              "type": "PROMISE_DAY",
              "month": 2,
              "day": 11,
              "emojiList": [
                "🤝"
              ],
              "greeting": "Promise Day"
            },
            {
              "type": "HUG_DAY",
              "month": 2,
              "day": 12,
              "emojiList": [
                "🤗"
              ],
              "greeting": "Hug Day"
            },
            {
              "type": "KISS_DAY",
              "month": 2,
              "day": 13,
              "emojiList": [
                "😘"
              ],
              "greeting": "Kiss Day"
            },
            {
              "type": "VALENTINES_DAY",
              "month": 2,
              "day": 14,
              "emojiList": [
                "❤️",
                "🌹"
              ],
              "greeting": "Love is in the air"
            },
            {
              "type": "POKEMON_DAY",
              "month": 2,
              "day": 27,
              "emojiList": [
                "🎮",
                "🧢",
                "⚡️"
              ],
              "greeting": "Gotta Catch 'Em All"
            },
            {
              "type": "WOMENS_DAY",
              "month": 3,
              "day": 8,
              "emojiList": [
                "💜",
                "♀️",
                "👩",
                "👩‍🚀",
                "👩‍🚒",
                "👩‍✈️"
              ],
              "greeting": "International Women's Day"
            },
            {
              "type": "BARBIE_DAY",
              "month": 3,
              "day": 9,
              "emojiList": [
                "👠",
                "💖",
                "👗",
                "🎀"
              ],
              "greeting": "Be anything. Go everywhere. \uD83D\uDC96"
            },
            {
              "type": "MARIO_DAY",
              "month": 3,
              "day": 10,
              "emojiList": [
                "🍄",
                "🎮"
              ],
              "greeting": "It's-a me, Mario! Let's-a go! \uD83C\uDF44"
            },
            {
              "type": "PI_DAY",
              "month": 3,
              "day": 14,
              "emojiList": [
                "🥧",
                "π"
              ],
              "greeting": "Pi Day"
            },
            {
              "type": "ANZAC_DAY",
              "month": 4,
              "day": 25,
              "emojiList": [
                "🌺",
                "🇦🇺",
                "🎖️"
              ],
              "greeting": "Lest we forget"
            },
            {
              "type": "HARRY_POTTER_DAY",
              "month": 5,
              "day": 2,
              "emojiList": [
                "🪄",
                "🧙‍♂️",
                "⚡️"
              ],
              "greeting": "Hop on Platform 9¾ — Magic Awaits!"
            },
            {
              "type": "STAR_WARS_DAY",
              "month": 5,
              "day": 4,
              "emojiList": [
                "🌌",
                "🚀",
                "⚔️",
                "🪐"
              ],
              "greeting": "May the Force Ride With You"
            },
            {
              "type": "NURSES_DAY",
              "month": 5,
              "day": 12,
              "emojiList": [
                "🏥",
                "🩺"
              ],
              "greeting": "Nurses Day"
            },
            {
              "type": "A11Y_DAY",
              "month": 5,
              "day": 16,
              "emojiList": [
                "♿️"
              ],
              "greeting": "Global Accessibility Awareness Day"
            },
            {
              "type": "WORLD_ENVIRONMENT_DAY",
              "month": 6,
              "day": 5,
              "emojiList": [
                "🌍",
                "🌱",
                "🌳"
              ],
              "greeting": "World Environment Day"
            },
            {
              "type": "WORLD_OCEANS_DAY",
              "month": 6,
              "day": 8,
              "emojiList": [
                "🌊",
                "🐬",
                "🐠"
              ],
              "greeting": "World Oceans Day"
            },
            {
              "type": "INTERNATIONAL_YOGA_DAY",
              "month": 6,
              "day": 21,
              "emojiList": [
                "🧘",
                "🧘‍♀️",
                "🧘‍♂️",
                "🕉️"
              ],
              "greeting": "International Yoga Day"
            },
            {
              "type": "WORLD_EMOJI_DAY",
              "month": 7,
              "day": 17,
              "emojiList": [
                "😎",
                "🤩",
                "🔥"
              ],
              "greeting": "World Emoji Day"
            },
            {
              "type": "FRIENDSHIP_DAY",
              "month": 8,
              "day": 3,
              "emojiList": [
                "🤝",
                "💛"
              ],
              "greeting": "Friendship Day"
            },
            {
              "type": "INTERNATIONAL_CAT_DAY",
              "month": 8,
              "day": 8,
              "emojiList": [
                "😸",
                "😻"
              ],
              "greeting": "Purrfect day to ride"
            },
            {
              "type": "INTERNATIONAL_DOG_DAY",
              "month": 8,
              "day": 26,
              "emojiList": [
                "🐶",
                "🐾",
                "🦴"
              ],
              "greeting": "International Dog Day"
            },
            {
              "type": "TEACHERS_DAY",
              "month": 9,
              "day": 5,
              "emojiList": [
                "👩‍🏫",
                "👨‍🏫",
                "📚"
              ],
              "greeting": "Happy Teachers' Day"
            },
            {
              "type": "ENGINEERS_DAY",
              "month": 9,
              "day": 15,
              "emojiList": [
                "⚙️",
                "🔧"
              ],
              "greeting": "Engineers Day"
            },
            {
              "type": "PEACE_DAY",
              "month": 9,
              "day": 21,
              "emojiList": [
                "☮️",
                "✌️"
              ],
              "greeting": "International Peace Day"
            },
            {
              "type": "HOBBIT_DAY",
              "month": 9,
              "day": 22,
              "emojiList": [
                "🧙",
                "🍄",
                "🌋"
              ],
              "greeting": "Happy Hobbit Day"
            },
            {
              "type": "MENTAL_HEALTH_DAY",
              "month": 10,
              "day": 10,
              "emojiList": [
                "🧠"
              ],
              "greeting": "World Mental Health Day"
            },
            {
              "type": "HALLOWEEN",
              "month": 10,
              "day": 31,
              "emojiList": [
                "🎃",
                "👻"
              ],
              "greeting": "Spooktacular vibes only!"
            },
            {
              "type": "REMEMBRANCE_DAY",
              "month": 11,
              "day": 11,
              "emojiList": ["🌺", "🕊️", "🇦🇺"],
              "greeting": "Remembrance Day — Lest we forget"
            },
            {
              "type": "WORLD_KINDNESS_DAY",
              "month": 11,
              "day": 13,
              "emojiList": ["🤝", "💖", "🌍"],
              "greeting": "World Kindness Day"
            },
            {
              "type": "MENS_DAY",
              "month": 11,
              "day": 19,
              "emojiList": [
                "💙",
                "♂️",
                "🚹",
                "👨‍🚒",
                "👨‍🌾",
                "👨‍🚀"
              ],
              "greeting": "International Men's Day"
            },
            {
              "type": "TAYLOR_SWIFT_DAY",
              "month": 12,
              "day": 13,
              "emojiList": [
                "🎤",
                "🎶",
                "🩷"
              ],
              "greeting": "Happy Taylor Swift Day"
            },
            {
              "type": "CHRISTMAS",
              "month": 12,
              "day": 25,
              "emojiList": [
                "🎄",
                "🎅"
              ],
              "greeting": "Merry Christmas"
            },
            {
              "type": "NEW_YEAR_EVE",
              "month": 12,
              "day": 31,
              "emojiList": [
                "🎆"
              ],
              "greeting": "New Year's Eve"
            }
          ],
          "variableDates": [
            {
              "type": "EASTER",
              "startDate": "2026-04-05",
              "endDate": "2026-04-05",
              "emojiList": [
                "🐰",
                "🐣",
                "🥚"
              ],
              "greeting": "Happy Easter"
            },
            {
              "type": "EID",
              "startDate": "2026-03-19",
              "endDate": "2026-03-20",
              "emojiList": [
                "🌙",
                "🕌"
              ],
              "greeting": "Eid Mubarak!"
            },
            {
              "type": "CHINESE_NEW_YEAR",
              "startDate": "2026-02-17",
              "endDate": "2026-03-03",
              "emojiList": [
                "🧧"
              ],
              "greeting": "Chinese New Year"
            },
            {
              "type": "VIVID_SYDNEY",
              "startDate": "2026-05-22",
              "endDate": "2026-06-13",
              "emojiList": [
                "🎆",
                "🌈",
                "🌟",
                "✨"
              ],
              "greeting": "Vivid Sydney"
            },
            {
              "type": "MARDI_GRAS",
              "startDate": "2026-02-13",
              "endDate": "2026-03-01",
              "emojiList": [
                "🏳️‍🌈",
                "🪩",
                "🌈"
              ],
              "greeting": "Happy Mardi Gras"
            },
            {
              "type": "NAIDOC_WEEK",
              "startDate": "2026-07-05",
              "endDate": "2026-07-12",
              "emojiList": [
                "🖤",
                "💛",
                "❤️"
              ],
              "greeting": "NAIDOC Week"
            },
            {
              "type": "HOLI",
              "startDate": "2026-03-04",
              "endDate": "2026-03-04",
              "emojiList": [
                "🌈",
                "🫧",
                "🎈"
              ],
              "greeting": "Happy Holi"
            },
            {
              "type": "MELBOURNE_CUP",
              "startDate": "2026-11-03",
              "endDate": "2026-11-03",
              "emojiList": [
                "🐎",
                "💸"
              ],
              "greeting": "Melbourne Cup Day"
            },
            {
              "type": "RECONCILIATION_WEEK",
              "startDate": "2026-05-27",
              "endDate": "2026-06-03",
              "emojiList": [
                "🪃",
                "🖤",
                "💛",
                "❤️"
              ],
              "greeting": "Reconciliation Week"
            },
            {
              "type": "DIWALI",
              "startDate": "2026-11-08",
              "endDate": "2026-11-08",
              "emojiList": [
                "🪔",
                "🎆"
              ],
              "greeting": "Happy Diwali"
            }
          ]
        }
    """.trimIndent()
}
