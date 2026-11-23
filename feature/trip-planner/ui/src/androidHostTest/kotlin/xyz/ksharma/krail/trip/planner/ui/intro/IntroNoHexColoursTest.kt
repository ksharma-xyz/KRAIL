package xyz.ksharma.krail.trip.planner.ui.intro

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * No colour is typed into an Intro source. Every colour is a token of the app's theme or of taj, or a colour taj derives from one
 * (`introAccentInk`, `asThemeInk`), so a change of a theme or of a ground reaches the Intro. This reads the Intro sources, the way
 * `IntroEmojiGuardTest` does, and fails on a hex colour written as a number (`0xFF...`) or as text (`#RRGGBB`) in any of them.
 */
class IntroNoHexColoursTest {

    private val introSources: List<File> by lazy {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, INTRO_PATH) }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot find $INTRO_PATH above ${File("").absolutePath}")
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun `the Intro sources are found, so this guard cannot pass by looking at nothing`() {
        assertTrue(introSources.size >= MIN_SOURCES, "found only ${introSources.map { it.name }}")
    }

    @Test
    fun `no Intro source has a hex colour typed into it`() {
        val offenders = introSources.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                HEX_COLOUR.find(line)?.let { "${file.name}:${index + 1} has ${it.value}" }
            }
        }

        assertTrue(offenders.isEmpty(), "Colours come from the theme, never typed: $offenders")
    }

    @Test
    fun `the pattern finds the two ways a colour is typed and not a plain word`() {
        listOf("Color(0xFF5AB031)", "val c = 0xff00b5ef", "\"#009B77\"", "\"#E0218A\"").forEach {
            assertTrue(HEX_COLOUR.containsMatchIn(it), "\"$it\" was not found")
        }
        listOf("Color.White", "issue #123", "0x1F", "\"#tag\"").forEach {
            assertTrue(!HEX_COLOUR.containsMatchIn(it), "\"$it\" was found")
        }
    }

    private companion object {
        const val INTRO_PATH =
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/intro"
        const val MIN_SOURCES = 6
        val HEX_COLOUR = Regex("0[xX][0-9A-Fa-f]{6,8}\\b|\"#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?\"")
    }
}
