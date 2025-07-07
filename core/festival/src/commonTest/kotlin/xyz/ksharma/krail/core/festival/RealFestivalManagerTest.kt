package xyz.ksharma.krail.core.festival

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.festival.model.Festival
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        val festivals = listOf(
            Festival(
                startDate = "2024-06-01",
                endDate = "2024-06-10",
                emojiList = listOf("🎉"),
                description = "Test Festival"
            )
        )
        val json = Json.encodeToString(festivals)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))

        val result = manager.getFestivals()
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Test Festival", result[0].description)
        assertEquals(listOf("🎉"), result[0].emojiList)
    }

    @Test
    fun testFestivalToday_returnsFestivalIfTodayInRange() {
        val today = LocalDate.parse("2024-06-05")
        val festivals = listOf(
            Festival(
                startDate = "2024-06-01",
                endDate = "2024-06-10",
                emojiList = listOf("🎉"),
                description = "Ongoing Festival"
            ),
            Festival(
                startDate = "2024-07-01",
                endDate = "2024-07-10",
                emojiList = listOf("🎊"),
                description = "Future Festival"
            )
        )
        val json = Json.encodeToString(festivals)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))

        val result = manager.festivalOnDate(today)
        assertNotNull(result)
        assertEquals("Ongoing Festival", result.description)
    }

    @Test
    fun testFestivalToday_returnsNullIfNoFestivalToday() {
        val festivals = listOf(
            Festival(
                startDate = "2024-01-01",
                endDate = "2024-01-10",
                emojiList = listOf("❄️"),
                description = "Past Festival"
            )
        )
        val json = Json.encodeToString(festivals)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))

        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    @Test
    fun testGetFestivals_returnsNullOnInvalidJson() {
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue("not a json"))
        val result = manager.getFestivals()
        assertNull(result)
    }

    // Empty festival list
    @Test
    fun testFestivalOnDate_withEmptyFestivalList_returnsNull() {
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue("[]"))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    // Festival with invalid date format
    @Test
    fun testFestivalOnDate_withInvalidDateFormat_ignoresFestival() {
        val festivals = listOf(
            Festival("invalid-date", "2024-06-10", listOf("🎉"), "Bad Start Date"),
            Festival("2024-06-01", "invalid-date", listOf("🎊"), "Bad End Date")
        )
        val json = Json.encodeToString(festivals)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    // Festival with startDate after endDate
    @Test
    fun testFestivalOnDate_withStartDateAfterEndDate_ignoresFestival() {
        val festivals = listOf(
            Festival("2024-06-10", "2024-06-01", listOf("❌"), "Invalid Range")
        )
        val json = Json.encodeToString(festivals)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNull(result)
    }

    // Multiple festivals on the same date (returns the first)
    @Test
    fun testFestivalOnDate_multipleFestivalsOnSameDate_returnsFirst() {
        val festivals = listOf(
            Festival("2024-06-01", "2024-06-10", listOf("🎉"), "First Festival"),
            Festival("2024-06-01", "2024-06-10", listOf("🎊"), "Second Festival")
        )
        val json = Json.encodeToString(festivals)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNotNull(result)
        assertEquals("First Festival", result.description)
    }

    // Festival with only one day (startDate == endDate)
    @Test
    fun testFestivalOnDate_singleDayFestival_foundOnThatDay() {
        val festivals = listOf(
            Festival("2024-06-05", "2024-06-05", listOf("🥳"), "One Day Festival")
        )
        val json = Json.encodeToString(festivals)
        fakeFlag.setFlagValue(FlagKeys.FESTIVALS.key, FlagValue.JsonValue(json))
        val result = manager.festivalOnDate(LocalDate.parse("2024-06-05"))
        assertNotNull(result)
        assertEquals("One Day Festival", result.description)
    }

    // Fallback to default config when no flag is set
    @Test
    fun testGetFestivals_fallbackToDefaultConfig() {
        // Do not set any flag value, should use default config (which is "[]")
        val result = manager.getFestivals()
        assertNotNull(result)
        assertEquals(0, result.size)
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