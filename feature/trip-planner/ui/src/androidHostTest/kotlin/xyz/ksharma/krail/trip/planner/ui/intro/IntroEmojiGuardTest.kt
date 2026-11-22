package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.festival.FestivalManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Intro draws only emoji the operator approved: the entries of the app's non festival list,
 * [FestivalManager.commonEmojiList], and the pin and the star, which are declared once in
 * `IntroApprovedEmoji.kt`. Nothing is typed into any other Intro source, and nothing festival is asked
 * for, so no screen changes with the date. This reads the Intro sources, the way `IntroNeverReviewsTest`
 * does, and fails on an emoji outside that set wherever it is written, as a character or as an escape.
 */
class IntroEmojiGuardTest {

    private val introSources: List<File> by lazy {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, INTRO_PATH) }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot find $INTRO_PATH above ${File("").absolutePath}")
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private val allowed = FestivalManager.commonEmojiList + INTRO_PIN_EMOJI + INTRO_STAR_EMOJI

    @Test
    fun `the Intro sources are found, so this guard cannot pass by looking at nothing`() {
        assertTrue(introSources.size >= MIN_SOURCES, "found only ${introSources.map { it.name }}")
    }

    @Test
    fun `every emoji in an Intro source is on the approved list or is the pin or the star`() {
        val offenders = introSources.flatMap { file ->
            emojiRuns(file.readText()).filter { it !in allowed }.map { "${file.name} has ${it.describe()}" }
        }

        assertTrue(offenders.isEmpty(), "Only the approved emoji may be used in the Intro: $offenders")
    }

    @Test
    fun `the pin and the star are written out in one file only`() {
        val exceptions = setOf(INTRO_PIN_EMOJI, INTRO_STAR_EMOJI)

        val files = introSources.filter { file -> emojiRuns(file.readText()).any { it in exceptions } }

        assertEquals(listOf("IntroApprovedEmoji.kt"), files.map { it.name })
    }

    @Test
    fun `no Intro source asks for a festival emoji`() {
        val offenders = introSources.flatMap { file ->
            val text = file.readText()
            FESTIVAL_CALLS.filter { it in text }.map { "${file.name} uses $it" }
        }

        assertTrue(offenders.isEmpty(), "The Intro must not use a festival emoji: $offenders")
    }

    @Test
    fun `the pin and the star are the code points the operator approved`() {
        assertEquals(PIN_CODE_POINT, INTRO_PIN_EMOJI.codePointAt(0))
        assertEquals(INTRO_PIN_EMOJI.length, Character.charCount(PIN_CODE_POINT))
        assertEquals(STAR_CODE_POINT, INTRO_STAR_EMOJI.codePointAt(0))
        assertEquals(INTRO_STAR_EMOJI.length, Character.charCount(STAR_CODE_POINT))
    }

    @Test
    fun `the scan finds an emoji written as a character and as an escape, so it cannot miss one`() {
        val car = String(Character.toChars(CAR_CODE_POINT))
        val escaped = "\\uD83D" + "\\uDE97"

        assertEquals(listOf(car), emojiRuns("val a = \"$car\""))
        assertEquals(listOf(car), emojiRuns("val a = \"$escaped\""))
        assertEquals(emptyList(), emojiRuns("val a = \"Driving to the station\" // a plain line"))
    }

    // Escapes are turned into the characters they stand for, then each run of emoji code points is one emoji.
    private fun emojiRuns(source: String): List<String> {
        val text = ESCAPE.replace(source) { it.groupValues[1].toInt(HEX).toChar().toString() }
        val runs = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (codePoint.isEmojiPart()) {
                current.appendCodePoint(codePoint)
            } else if (current.isNotEmpty()) {
                runs += current.toString()
                current.clear()
            }
            index += Character.charCount(codePoint)
        }
        if (current.isNotEmpty()) runs += current.toString()
        return runs
    }

    private fun Int.isEmojiPart(): Boolean = EMOJI_RANGES.any { this in it } || this == ZERO_WIDTH_JOINER

    private fun String.describe(): String {
        val codePoints = mutableListOf<Int>()
        var index = 0
        while (index < length) {
            val codePoint = codePointAt(index)
            codePoints += codePoint
            index += Character.charCount(codePoint)
        }
        return codePoints.joinToString(separator = "+", prefix = "U+") { it.toString(HEX).uppercase() }
    }

    private companion object {
        const val INTRO_PATH =
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/intro"
        const val MIN_SOURCES = 6
        const val HEX = 16
        const val PIN_CODE_POINT = 0x1F4CD
        const val STAR_CODE_POINT = 0x2B50
        const val CAR_CODE_POINT = 0x1F697
        const val ZERO_WIDTH_JOINER = 0x200D
        val ESCAPE = Regex("""\\u([0-9A-Fa-f]{4})""")

        // Pictographs and symbols, the clock block, miscellaneous symbols and dingbats, the star block,
        // variation selectors and keycaps. Plain text and the other punctuation the repo bans are outside it.
        val EMOJI_RANGES = listOf(
            0x1F000..0x1FAFF,
            0x2300..0x23FF,
            0x2600..0x27BF,
            0x2B00..0x2BFF,
            0xFE00..0xFE0F,
            0x20E3..0x20E3,
        )

        val FESTIVAL_CALLS = listOf("festivalOnDate", "emojiForDate", "Festival.emojiList", ".emojiList")
    }
}
