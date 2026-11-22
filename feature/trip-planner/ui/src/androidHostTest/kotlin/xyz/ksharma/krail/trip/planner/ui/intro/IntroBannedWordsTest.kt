package xyz.ksharma.krail.trip.planner.ui.intro

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every string the Intro draws is held to the Intro's wording rules: no word that lets a rider skip a
 * question (Skip, Later, Next, Continue, Done), and none of a question mark, an exclamation mark, a colon,
 * an em dash, an arrow or a middle dot. The rider always answers yes or no, so a screen never words a way
 * past the question, and the copy stays plain. The one exception is a question mark at the end of the last line of
 * each of the five headlines, which are complete questions: exactly those lines may end in one. The apostrophe is allowed.
 *
 * This reads the Intro sources the way `IntroNeverReviewsTest` does and checks every string literal in them,
 * so a new string is caught the day it is written, wherever it is. It also checks every string resource an
 * Intro source names. `IntroCopyTest` checks the bars and headlines the model produces.
 */
class IntroBannedWordsTest {

    private val introSources: List<File> by lazy {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, INTRO_PATH) }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot find $INTRO_PATH above ${File("").absolutePath}")
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private val stringResources: Map<String, String> by lazy {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, RESOURCES_PATH) }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot find $RESOURCES_PATH above ${File("").absolutePath}")
        root.walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .flatMap { RESOURCE.findAll(it.readText()).map { match -> match.groupValues[1] to match.groupValues[2] } }
            .toMap()
    }

    @Test
    fun `the Intro sources and their strings are found, so this guard cannot pass by looking at nothing`() {
        assertTrue(introSources.size >= MIN_SOURCES, "found only ${introSources.map { it.name }}")
        val literals = introSources.flatMap { stringLiterals(it.readText()) }
        assertTrue("Yes, show me free spots" in literals && "Not now" in literals, "the bar words were not found")
        assertTrue(stringResources.isNotEmpty(), "no string resources were found")
    }

    @Test
    fun `no string in an Intro source has a word that skips a question or a banned character`() {
        val offenders = introSources.flatMap { file ->
            stringLiterals(file.readText()).flatMap { text -> problems(text).map { "${file.name}: \"$text\" $it" } }
        }

        assertTrue(offenders.isEmpty(), "Intro wording rules broken: $offenders")
    }

    @Test
    fun `no string resource an Intro source uses has a word that skips a question or a banned character`() {
        val names = introSources.flatMap { RESOURCE_USE.findAll(it.readText()).map { match -> match.groupValues[1] } }

        val offenders = names.distinct().flatMap { name ->
            val text = stringResources[name] ?: return@flatMap listOf("$name is not in any strings.xml")
            problems(text).map { "$name: \"$text\" $it" }
        }

        assertTrue(offenders.isEmpty(), "Intro wording rules broken in a string resource: $offenders")
    }

    @Test
    fun `the check catches each banned word and each banned character, and passes plain copy`() {
        BANNED_WORDS.forEach { word ->
            assertTrue(problems("Press $word now").isNotEmpty(), "$word was not caught")
            assertTrue(problems("press ${word.lowercase()}.").isNotEmpty(), "${word.lowercase()} was not caught")
        }
        BANNED_CHARACTERS.forEach { codePoint ->
            assertTrue(problems("Plain${Char(codePoint)}words").isNotEmpty(), "U+${codePoint.toString(HEX)} not caught")
        }
        assertTrue(problems("Plain : words").isNotEmpty(), "a spaced colon was not caught")
        listOf("What's your", "Yes, show me free spots", "skipped", "Nextdoor", "Not now").forEach {
            assertEquals(emptyList(), problems(it), "plain copy $it was flagged")
        }
    }

    @Test
    fun `the reader finds a literal in code and ignores one in a comment`() {
        val source = """
            // a comment with "Skip" in it
            /** a doc with "Later" in it */
            val a = "Not now"
            val b = "Search ${'$'}{x} for"
            val c = '"'
        """.trimIndent()

        assertEquals(listOf("Not now", "Search ${'$'}{x} for"), stringLiterals(source))
    }

    private fun problems(text: String): List<String> {
        val words = BANNED_WORDS.filter { Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) }
            .map { "has the word $it" }
        val characters = BANNED_CHARACTERS
            .filter { text.contains(Char(it)) && !(it == QUESTION_MARK && text in questionLines) }
            .map { "has U+${it.toString(HEX)}" }
        val colon = if (COLON.containsMatchIn(text)) listOf("has a colon") else emptyList()
        return words + characters + colon
    }

    private fun stringLiterals(source: String): List<String> = LiteralReader(source).read()

    // The last lines of the five headlines, which are questions: the only strings that may have a question mark.
    private val questionLines: List<String> = listOf(
        PARK_RIDE_HEADLINE,
        DESTINATION_HEADLINE,
        ORIGIN_HEADLINE,
        CONFIRM_HEADLINE,
        PICK_THEME_HEADLINE,
    ).map { it.last() }

    @Test
    fun `a question mark is allowed at the end of the last line of the five headlines and nowhere else`() {
        assertEquals(
            listOf("to the station?", "you going?", "leaving from?", "trip?", "in Sydney?"),
            questionLines,
        )
        questionLines.forEach { assertEquals(emptyList(), problems(it), "$it") }
        // The exemption is the exact line, so the same words with more, or any other question, are caught.
        listOf("trip? now", "Which one?", "trip??", "Save this trip?", "Pick a colour?").forEach {
            assertTrue(problems(it).isNotEmpty(), "\"$it\" was not caught")
        }
        // The other banned characters are caught in those lines as everywhere.
        assertTrue(problems("trip?!").isNotEmpty())
    }

    private companion object {
        const val INTRO_PATH =
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/intro"
        const val RESOURCES_PATH = "feature/trip-planner/ui/src/commonMain/composeResources"
        const val MIN_SOURCES = 6
        const val HEX = 16

        val BANNED_WORDS = listOf("Skip", "Later", "Next", "Continue", "Done")

        // Written as code points so no banned character is typed into this file: a question mark, an
        // exclamation mark, an em dash, the arrows, and the middle dot.
        const val QUESTION_MARK = 0x3F
        val BANNED_CHARACTERS = listOf(QUESTION_MARK, 0x21, 0x2014, 0x2190, 0x2192, 0x21D2, 0x00B7)

        val COLON = Regex("""\s:|:\s""")
        val RESOURCE = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val RESOURCE_USE = Regex("""Res\.string\.(\w+)""")
    }
}

/**
 * Reads the string literals out of Kotlin source, without the ones in comments. A small reader, because a
 * regular expression cannot tell a quote in a comment from one in code.
 */
private class LiteralReader(private val source: String) {
    private val found = mutableListOf<String>()
    private var index = 0

    fun read(): List<String> {
        while (index < source.length) step()
        return found.filter { it.isNotBlank() }
    }

    private fun step() {
        when {
            at("//") -> index = endOf("\n", from = index, length = 0)
            at("/*") -> index = endOf("*/", from = index, length = 2)
            at("\"\"\"") -> readRaw()
            source[index] == '"' -> readString()
            source[index] == '\'' -> skipCharacter()
            else -> index++
        }
    }

    private fun at(text: String) = source.startsWith(text, index)

    // The position after [marker], or the end of the source when there is none.
    private fun endOf(marker: String, from: Int, length: Int): Int =
        source.indexOf(marker, from).let { if (it < 0) source.length else it + length }

    private fun readRaw() {
        val end = source.indexOf("\"\"\"", index + 3).let { if (it < 0) source.length else it }
        found += source.substring(index + 3, end)
        index = minOf(end + 3, source.length)
    }

    private fun readString() {
        var end = index + 1
        while (end < source.length && source[end] != '"') end += if (source[end] == '\\') 2 else 1
        found += source.substring(index + 1, minOf(end, source.length))
        index = end + 1
    }

    // A character literal such as the quote character, so its quote does not open a string.
    private fun skipCharacter() {
        index = source.indexOf('\'', index + if (source.getOrNull(index + 1) == '\\') 3 else 2) + 1
    }
}
