package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// Where the screens that use the list start in it. The Park & Ride screen lands on a transport emoji, and
// each of those screens is one entry on from the one before it.
private const val EMOJI_OFFSET = 5

/**
 * The emoji of [step]. The destination screen has the pin and the confirm screen the star, the two
 * operator approved exceptions in [INTRO_PIN_EMOJI] and [INTRO_STAR_EMOJI]. Every other screen has an
 * entry of the app's own non festival list, [FestivalManager.commonEmojiList], the one the loading emoji
 * uses. No emoji is typed into any other Intro code, and no festival emoji is used, so the Intro never
 * changes with the date.
 *
 * It is a pure function of the step and the list. The screens that use the list take the entry at their
 * place in the order, wrapping round the end, so the theme and Park & Ride screens, which sit side by side,
 * never share one while the list has two or more entries, and a list that grows or shrinks still gives a
 * valid entry for every screen. The pin and the star sit between them and the rest, so no two screens in a
 * row ever share an emoji. An empty list gives an empty string, which the headline draws as nothing.
 */
internal fun introEmojiFor(
    step: IntroStep,
    emojis: List<String> = FestivalManager.commonEmojiList,
): String = when (step) {
    IntroStep.DESTINATION -> INTRO_PIN_EMOJI
    IntroStep.CONFIRM -> INTRO_STAR_EMOJI
    IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.ORIGIN ->
        if (emojis.isEmpty()) "" else emojis[(step.ordinal + EMOJI_OFFSET) % emojis.size]
}
