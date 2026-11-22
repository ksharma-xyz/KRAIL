package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// Where the screens that use the list start in it. The origin screen lands on an entry of the list, and the theme
// screen, before a choice, on the first one, the scooter, a neutral ride that is no mode. The origin's place is a
// number of its own and not the screen's position in the order, so putting a screen before it moves nothing.
private const val ORIGIN_EMOJI_INDEX = 8
private const val NEUTRAL_RIDE_INDEX = 0

/**
 * The emoji of [step]. The destination screen has the pin, the confirm screen the star and the Park & Ride screen
 * a car. The theme screen has the transport emoji of the theme chosen, [selectedThemeId]: the train, the metro, the
 * bus or the ferry, and with no choice yet, or an id the Intro does not offer, the scooter from the app's own non
 * festival list, [FestivalManager.commonEmojiList], the one the loading emoji uses, never the alarm clock. Those
 * seven are the operator approved exceptions in `IntroApprovedEmoji.kt`. The origin screen has an entry of the list.
 * No emoji is typed into any other Intro code, and no festival emoji is used, so the Intro never changes with the date.
 *
 * It is a pure function of the step, the choice and the list. The origin screen takes the entry at its fixed place,
 * wrapping round the end, so a list that grows or shrinks still gives a valid entry, and an empty list gives an
 * empty string, which the headline draws as nothing. A chosen theme never depends on the list.
 */
internal fun introEmojiFor(
    step: IntroStep,
    selectedThemeId: Int? = null,
    emojis: List<String> = FestivalManager.commonEmojiList,
): String = when (step) {
    IntroStep.THEME -> themeEmoji(selectedThemeId) ?: emojis.getOrNull(NEUTRAL_RIDE_INDEX).orEmpty()
    IntroStep.PARK_RIDE -> INTRO_CAR_EMOJI
    IntroStep.DESTINATION -> INTRO_PIN_EMOJI
    IntroStep.ORIGIN -> if (emojis.isEmpty()) "" else emojis[ORIGIN_EMOJI_INDEX % emojis.size]
    IntroStep.CONFIRM -> INTRO_STAR_EMOJI
}

/** The transport emoji of the theme with id [themeId], or null for no choice and for a theme the Intro does not offer. */
internal fun themeEmoji(themeId: Int?): String? = when (themeId) {
    KrailThemeStyle.Train.id -> INTRO_TRAIN_EMOJI
    KrailThemeStyle.Metro.id -> INTRO_METRO_EMOJI
    KrailThemeStyle.Bus.id -> INTRO_BUS_EMOJI
    KrailThemeStyle.Ferry.id -> INTRO_FERRY_EMOJI
    else -> null
}
