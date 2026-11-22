package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

/**
 * One badge on the theme screen. [mode] is the transport mode whose own icon is the badge: its fill and its white
 * letter are the app's `TransportModeIcon`, drawn by that component with nothing passed to it, and [name] is what a
 * screen reader calls it. [tag] is the selector the Maestro flow taps. The theme colour is never kept here: it is
 * read from [style], and a test holds that it is the mode's own fill, so the badge and the recolour cannot disagree.
 */
internal data class IntroThemeChoice(
    val style: KrailThemeStyle,
    val mode: TransportMode,
    val name: String,
    val tag: String,
)

// The four transport themes, in the order the website's footer shows the same modes. Light rail and Coach
// have a colour in the app but no theme (no id and no tagline), and the two fun themes stay in Settings.
private val TransportThemeChoices = listOf(
    IntroThemeChoice(
        style = KrailThemeStyle.Train,
        mode = TransportMode.Train,
        name = "Train",
        tag = TripPlannerTestTags.INTRO_THEME_TRAIN,
    ),
    IntroThemeChoice(
        style = KrailThemeStyle.Metro,
        mode = TransportMode.Metro,
        name = "Metro",
        tag = TripPlannerTestTags.INTRO_THEME_METRO,
    ),
    IntroThemeChoice(
        style = KrailThemeStyle.Bus,
        mode = TransportMode.Bus,
        name = "Bus",
        tag = TripPlannerTestTags.INTRO_THEME_BUS,
    ),
    IntroThemeChoice(
        style = KrailThemeStyle.Ferry,
        mode = TransportMode.Ferry,
        name = "Ferry",
        tag = TripPlannerTestTags.INTRO_THEME_FERRY,
    ),
)

/** The themes the first Intro screen offers, in the order they are drawn. */
internal fun introThemeChoices(): List<IntroThemeChoice> = TransportThemeChoices

/** The choice with theme id [themeId], or null for no id and for a theme the Intro does not offer. */
internal fun themeChoiceFor(themeId: Int?): IntroThemeChoice? =
    introThemeChoices().firstOrNull { it.style.id == themeId }

/**
 * The lines of a theme's tagline, which is the app's own copy and is shown exactly as it is written. It is
 * split after the first comma into two short lines, and is one line when there is no comma. Nothing is
 * added, removed or reworded, so the lines joined with a space are the tagline again.
 */
internal fun taglineLines(tagLine: String): List<String> {
    val comma = tagLine.indexOf(',')
    if (comma < 0) return listOf(tagLine)
    return listOf(tagLine.substring(0, comma + 1), tagLine.substring(comma + 1).trim())
        .filter { it.isNotEmpty() }
}

/** What the theme screen's headline says: its lines and the one with the key word. */
internal data class IntroThemeHeadline(val lines: List<String>, val emphasisIndex: Int)

/** Before a choice, the question as a few short lines with the key word last. */
internal val PICK_THEME_HEADLINE = listOf("Pick", "your ride", "colour")

/** The whole headline as one phrase, which a screen reader reads as the heading. */
internal val PICK_THEME_HEADING = introHeadlinePhrase(PICK_THEME_HEADLINE)

/**
 * The headline for the theme chosen, [selectedId]: the question before a choice, and after one that
 * theme's tagline, in its own lines with the last one heaviest.
 */
internal fun introThemeHeadline(selectedId: Int?): IntroThemeHeadline {
    val choice = themeChoiceFor(selectedId)
        ?: return IntroThemeHeadline(PICK_THEME_HEADLINE, emphasisIndex = PICK_THEME_HEADLINE.lastIndex)
    val lines = taglineLines(choice.style.tagLine)
    return IntroThemeHeadline(lines, emphasisIndex = lines.lastIndex)
}

/**
 * Every headline the theme screen can show, each as its lines: the question before a choice and the tagline of each
 * theme. The room the screen keeps for its headline is that of the longest of these, so it is the same whichever shows.
 */
internal fun themeHeadlineVariants(): List<List<String>> =
    listOf(PICK_THEME_HEADLINE) + introThemeChoices().map { taglineLines(it.style.tagLine) }

/**
 * How many lines of room the longest of [variants] needs once drawn: each line of a headline is its own block that wraps
 * by itself, so a variant takes the sum of what its lines take, as [linesOf] measures each as drawn (in capitals), and the
 * room is the most any variant takes. At least one, so there is always a line of room.
 */
internal fun reservedHeadlineLines(variants: List<List<String>>, linesOf: (String) -> Int): Int =
    maxOf(1, variants.maxOfOrNull { lines -> lines.sumOf { linesOf(introHeadlineDisplay(it)) } } ?: 1)

/** The theme colour as hex, for the app to switch to, or null for an id the Intro does not offer. */
internal fun themeHexFor(themeId: Int?): String? = themeChoiceFor(themeId)?.style?.hexColorCode

/**
 * The colour the finale floods the screen with: the theme the rider chose, and the app's default theme when none
 * was chosen or the id is not one the Intro offers. The first screen makes a choice before anything else, so the default
 * is only a safe answer and not a path a rider takes.
 */
internal fun finaleThemeHex(themeId: Int?): String = themeHexFor(themeId) ?: DEFAULT_THEME_STYLE.hexColorCode
