package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.FestivalData
import xyz.ksharma.krail.core.festival.model.FixedDateFestival
import xyz.ksharma.krail.core.festival.model.VariableDateFestival
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
            "Festival JSON validation failed. See report:\n${validationResult.getReport()}",
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
        // Stands in for the shipped festival JSON. Paste the real file in here to validate
        // it before a release; the assertions below do not care which JSON they are given.
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
                message = "",
            )
        } catch (e: Exception) {
            TestResult(
                testName = "JSON Parsing",
                passed = false,
                message = "Failed to parse: ${e.message}",
            )
        }
    }

    private fun testDateFormatValidation(jsonString: String): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            val errors = mutableListOf<String>()

            // Validate fixed date ranges
            data.confirmedDates.forEach { festival -> errors += fixedDateErrors(festival) }

            // Validate variable date formats and parse-ability
            data.variableDates.forEach { festival -> errors += variableDateErrors(festival) }

            if (errors.isEmpty()) {
                TestResult(
                    testName = "Date Format Validation",
                    passed = true,
                    message = "",
                )
            } else {
                TestResult(
                    testName = "Date Format Validation",
                    passed = false,
                    message = errors.joinToString("\n   "),
                )
            }
        } catch (e: Exception) {
            TestResult(
                testName = "Date Format Validation",
                passed = false,
                message = "Error: ${e.message}",
            )
        }
    }

    private fun testDateRangeValidation(
        manager: RealFestivalManager,
        jsonString: String,
    ): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            val errors = mutableListOf<String>()

            data.variableDates.forEach { festival ->
                errors += boundaryErrors(manager, festival)
            }

            if (errors.isEmpty()) {
                TestResult(
                    testName = "Date Range Boundaries",
                    passed = true,
                    message = "",
                )
            } else {
                TestResult(
                    testName = "Date Range Boundaries",
                    passed = false,
                    message = errors.joinToString("\n   "),
                )
            }
        } catch (_: Exception) {
            TestResult(
                testName = "Date Range Boundaries",
                passed = false,
                message = "Error validating date ranges",
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
                    message = "",
                )
            } else {
                TestResult(
                    testName = "Emoji Validation",
                    passed = false,
                    message = "Missing emojis: ${missing.joinToString(", ")}",
                )
            }
        } catch (e: Exception) {
            TestResult(
                testName = "Emoji Validation",
                passed = false,
                message = "Error: ${e.message}",
            )
        }
    }

    private fun testOverlappingFestivals(jsonString: String): TestResult {
        return try {
            val data = Json.decodeFromString<FestivalData>(jsonString)
            val overlaps = mutableListOf<String>()

            overlaps += fixedVersusVariableOverlaps(data)
            overlaps += variableVersusVariableOverlaps(data)

            if (overlaps.isEmpty()) {
                TestResult(
                    testName = "Overlap Detection",
                    passed = true,
                    message = "",
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
                    message = summary,
                )
            }
        } catch (e: Exception) {
            TestResult(
                testName = "Overlap Detection",
                passed = false,
                message = "Error: ${e.message}",
            )
        }
    }

    // ========== Data Classes for Validation Results ==========

    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val message: String,
    )

    data class ValidationResult(
        val isValid: Boolean,
        val testResults: List<TestResult>,
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

/**
 * The per-festival rules the shipped festival JSON has to satisfy, and the overlap sweep.
 *
 * Top-level rather than members of the test class on purpose: as members, every branch in here
 * counted towards the complexity of the validators that call them, and those validators are the
 * part a reader needs to follow.
 */
private fun fixedDateErrors(festival: FixedDateFestival): List<String> = buildList {
    if (festival.month !in MONTHS) add("${festival.type}: Invalid month ${festival.month}")
    if (festival.day !in DAYS) add("${festival.type}: Invalid day ${festival.day}")

    // Building the date is what catches a day/month pair that is individually in range and
    // still not a date, like the 31st of February.
    runCatching { LocalDate(LEAP_YEAR, festival.month, festival.day) }
        .onFailure { add("${festival.type}: Invalid date ${festival.month}/${festival.day}") }
}

private fun variableDateErrors(festival: VariableDateFestival): List<String> = buildList {
    runCatching { LocalDate.parse(festival.startDate) to LocalDate.parse(festival.endDate) }
        .onSuccess { (start, end) ->
            if (start > end) add("${festival.type}: Start date after end date")
        }
        .onFailure { add("${festival.type}: Invalid date format - ${it.message}") }
}

private fun boundaryErrors(
    manager: RealFestivalManager,
    festival: VariableDateFestival,
): List<String> = buildList {
    // An unparseable range is not reported again here; testDateFormatValidation owns that.
    val range = festival.parsedRange() ?: return@buildList

    val dayBefore = LocalDate.fromEpochDays(range.first.toEpochDays() - 1)
    if (manager.festivalOnDate(dayBefore)?.greeting == festival.greeting) {
        add("${festival.type}: Found on day before start ($dayBefore)")
    }

    val dayAfter = LocalDate.fromEpochDays(range.second.toEpochDays() + 1)
    if (manager.festivalOnDate(dayAfter)?.greeting == festival.greeting) {
        add("${festival.type}: Found on day after end ($dayAfter)")
    }
}

private fun fixedVersusVariableOverlaps(data: FestivalData): List<String> = buildList {
    data.confirmedDates.forEach { fixed ->
        data.variableDates.forEach { variable ->
            val hit = variable.datesOrEmpty()
                .firstOrNull { it.month.number == fixed.month && it.day == fixed.day }
            if (hit != null) {
                add("   $hit: ${fixed.type} vs ${variable.type}, fixed wins")
            }
        }
    }
}

private fun variableVersusVariableOverlaps(data: FestivalData): List<String> = buildList {
    data.variableDates.indices.forEach { i ->
        ((i + 1) until data.variableDates.size).forEach { j ->
            val first = data.variableDates[i]
            val second = data.variableDates[j]
            add(overlapBetween(first, second) ?: return@forEach)
        }
    }
}

private fun overlapBetween(first: VariableDateFestival, second: VariableDateFestival): String? {
    val one = first.parsedRange() ?: return null
    val two = second.parsedRange() ?: return null
    val overlapStart = maxOf(one.first, two.first)
    val overlapEnd = minOf(one.second, two.second)
    return if (overlapStart <= overlapEnd) {
        "   $overlapStart to $overlapEnd: ${first.type} vs ${second.type}, first wins"
    } else {
        null
    }
}

/** Start and end as dates, or null if either does not parse. */
private fun VariableDateFestival.parsedRange(): Pair<LocalDate, LocalDate>? =
    runCatching { LocalDate.parse(startDate) to LocalDate.parse(endDate) }.getOrNull()

private fun VariableDateFestival.datesOrEmpty(): Sequence<LocalDate> {
    val range = parsedRange() ?: return emptySequence()
    return generateSequence(range.first) { LocalDate.fromEpochDays(it.toEpochDays() + 1) }
        .takeWhile { it <= range.second }
}

private val MONTHS = 1..12
private val DAYS = 1..31

// Any leap year will do: the point is to accept 29 February as a fixed festival date.
private const val LEAP_YEAR = 2024
