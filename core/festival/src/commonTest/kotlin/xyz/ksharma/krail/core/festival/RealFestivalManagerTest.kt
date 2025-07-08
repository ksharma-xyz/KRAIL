package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.*
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import kotlin.test.*

class RealFestivalManagerTest {

    private lateinit var fakeFlag: FakeFlag
    private lateinit var manager: RealFestivalManager

    @BeforeTest
    fun setup() {
        fakeFlag = FakeFlag()
        manager = RealFestivalManager(fakeFlag)
    }

    @Test
    fun testGetFestivals_returnsFestivals() {
        val data = FestivalData(
            confirmedDates = listOf(
                FixedDateFestival(
                    type = "TEST_FIXED",
                    month = 6,
                    day = 5,
                    emojiList = listOf("🎉"),
                    greeting = "Fixed Festival"
                )
            ),
            variableDates = listOf(
                VariableDateFestival(
                    type = "TEST_VARIABLE",
                    startDate = "2024-06-01",
                    endDate = "2024-06-10",
                    emojiList = listOf("🎊"),
                    greeting = "Variable Festival"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))

        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNotNull(result)
        // Should match fixed date first
        assertTrue(result is FixedDateFestival)
        assertEquals("Fixed Festival", result.greeting)
    }

    @Test
    fun testFestivalToday_returnsVariableFestivalIfTodayInRange() {
        val data = FestivalData(
            confirmedDates = emptyList(),
            variableDates = listOf(
                VariableDateFestival(
                    type = "ONGOING",
                    startDate = "2024-06-01",
                    endDate = "2024-06-10",
                    emojiList = listOf("🎉"),
                    greeting = "Ongoing Festival"
                ),
                VariableDateFestival(
                    type = "FUTURE",
                    startDate = "2024-07-01",
                    endDate = "2024-07-10",
                    emojiList = listOf("🎊"),
                    greeting = "Future Festival"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))

        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNotNull(result)
        assertTrue(result is VariableDateFestival)
        assertEquals("Ongoing Festival", (result as VariableDateFestival).greeting)
    }

    @Test
    fun testFestivalToday_returnsNullIfNoFestivalToday() {
        val data = FestivalData(
            confirmedDates = emptyList(),
            variableDates = listOf(
                VariableDateFestival(
                    type = "PAST",
                    startDate = "2024-01-01",
                    endDate = "2024-01-10",
                    emojiList = listOf("❄️"),
                    greeting = "Past Festival"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))

        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    @Test
    fun testGetFestivals_returnsNullOnInvalidJson() {
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue("not a json"))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    @Test
    fun testFestivalOnDate_withEmptyFestivalList_returnsNull() {
        val data = FestivalData()
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    @Test
    fun testFestivalOnDate_withInvalidDateFormat_ignoresFestival() {
        val data = FestivalData(
            variableDates = listOf(
                VariableDateFestival(
                    "BAD_START",
                    "invalid-date",
                    "2024-06-10",
                    listOf("🎉"),
                    "Bad Start Date"
                ),
                VariableDateFestival(
                    "BAD_END",
                    "2024-06-01",
                    "invalid-date",
                    listOf("🎊"),
                    "Bad End Date"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    @Test
    fun testFestivalOnDate_withStartDateAfterEndDate_ignoresFestival() {
        val data = FestivalData(
            variableDates = listOf(
                VariableDateFestival(
                    "INVALID_RANGE",
                    "2024-06-10",
                    "2024-06-01",
                    listOf("❌"),
                    "Invalid Range"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    @Test
    fun testFestivalOnDate_multipleFestivalsOnSameDate_returnsFirst() {
        val data = FestivalData(
            variableDates = listOf(
                VariableDateFestival(
                    "FIRST",
                    "2024-06-01",
                    "2024-06-10",
                    listOf("🎉"),
                    "First Festival"
                ),
                VariableDateFestival(
                    "SECOND",
                    "2024-06-01",
                    "2024-06-10",
                    listOf("🎊"),
                    "Second Festival"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNotNull(result)
        assertEquals("First Festival", (result as VariableDateFestival).greeting)
    }

    @Test
    fun testFestivalOnDate_singleDayFestival_foundOnThatDay() {
        val data = FestivalData(
            variableDates = listOf(
                VariableDateFestival(
                    "ONE_DAY",
                    "2024-06-05",
                    "2024-06-05",
                    listOf("🥳"),
                    "One Day Festival"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNotNull(result)
        assertEquals("One Day Festival", (result as VariableDateFestival).greeting)
    }

    @Test
    fun testFestivalOnDate_withMalformedDateFormat_ignoresFestival() {
        val data = FestivalData(
            variableDates = listOf(
                VariableDateFestival(
                    "MALFORMED",
                    "20205-01-1",
                    "20205-01-2",
                    listOf("❌"),
                    "Malformed Date"
                )
            )
        )
        val json = Json.encodeToString(data)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    @Test
    fun testFestivalData_withExtraUnknownField_parsesSuccessfully() {
        // JSON with extra unknown fields at both root and object level
        val jsonWithExtraField = """
        {
            "confirmedDates": [
                {
                    "type": "EXTRA_FIELD_TEST",
                    "month": 7,
                    "day": 7,
                    "emojiList": ["🎉"],
                    "greeting": "Extra Field Festival",
                    "extraField": "This is an extra field that should be ignored"
                }
            ],
            "variableDates": [
                {
                    "type": "EXTRA_VARIABLE",
                    "startDate": "2025-07-01",
                    "endDate": "2025-07-10",
                    "emojiList": ["🎊"],
                    "greeting": "Extra Variable Festival",
                    "extraField": "This is an extra field that should be ignored"
                }
            ],
            "extraField": "This is an extra field that should be ignored"
        }
    """.trimIndent()
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(jsonWithExtraField))
        // Should match the fixed date festival on 2025-07-07
        val result = manager.festivalOnDate(LocalDate.parse("2025-07-07"))
        assertNotNull(
            result,
            "Festival should not be null when date matches and extra fields are present"
        )
        assertTrue(result is FixedDateFestival)
        assertEquals("Extra Field Festival", result.greeting)
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
}
