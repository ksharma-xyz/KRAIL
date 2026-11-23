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

/**
 * What the theme screen's headline says: its [lines] and the one with the key word, the [spoken] text a screen reader reads as the
 * heading, and whether the lines are a question with a hero ([heroLayout]) or a tagline whose lines are all the same.
 */
internal data class IntroThemeHeadline(
    val lines: List<String>,
    val emphasisIndex: Int,
    val spoken: String = introHeadlinePhrase(lines),
    val heroLayout: Boolean = false,
)

/**
 * Before a choice, the question as it is drawn, in three lines. Each line is at most 16 characters, and the headline draws them
 * in capitals, the hero `transport mode` ([PICK_THEME_HERO_INDEX]) as large as the room lets it be and the other two as whispers
 * ([planHeroHeadline]). It is the short form of the question, and what is spoken is [PICK_THEME_HEADING].
 */
internal val PICK_THEME_HEADLINE = listOf("Your favourite", "transport mode", "in Sydney?")

/** The line of [PICK_THEME_HEADLINE] that is the hero. */
internal const val PICK_THEME_HERO_INDEX = 1

/** The whole question, which a screen reader reads as the heading, and is the text a test finds. It is not the drawn lines joined. */
internal const val PICK_THEME_HEADING = "What's your favourite transport mode in Sydney?"

/**
 * The headline for the theme chosen, [selectedId]: the question before a choice, and after one that
 * theme's tagline, in its own lines with the last one heaviest.
 */
internal fun introThemeHeadline(selectedId: Int?): IntroThemeHeadline {
    val choice = themeChoiceFor(selectedId)
        ?: return IntroThemeHeadline(
            lines = PICK_THEME_HEADLINE,
            emphasisIndex = PICK_THEME_HERO_INDEX,
            spoken = PICK_THEME_HEADING,
            heroLayout = true,
        )
    val lines = taglineLines(choice.style.tagLine)
    return IntroThemeHeadline(lines, emphasisIndex = lines.lastIndex)
}

/** The lines of each theme's tagline, which are drawn at the ordinary headline size, unlike the question. */
internal fun themeTaglineVariants(): List<List<String>> = introThemeChoices().map { taglineLines(it.style.tagLine) }

/** The theme colour as hex, for the app to switch to, or null for an id the Intro does not offer. */
internal fun themeHexFor(themeId: Int?): String? = themeChoiceFor(themeId)?.style?.hexColorCode

/**
 * The colour the finale floods the screen with: the theme the rider chose, and the app's default theme when none
 * was chosen or the id is not one the Intro offers. The first screen makes a choice before anything else, so the default
 * is only a safe answer and not a path a rider takes.
 */
internal fun finaleThemeHex(themeId: Int?): String = themeHexFor(themeId) ?: DEFAULT_THEME_STYLE.hexColorCode
