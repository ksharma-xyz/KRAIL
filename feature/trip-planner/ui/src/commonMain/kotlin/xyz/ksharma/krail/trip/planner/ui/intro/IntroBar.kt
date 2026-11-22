package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.L_FONT_SCALE_THRESHOLD
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase

// The words on the decision bar. Every one is an answer, never a way past a question, so none of them
// is Skip, Later, Next, Continue or Done. IntroBannedWordsTest holds that for every string the Intro draws.
internal const val PARK_RIDE_NO = "No thanks"
internal const val PARK_RIDE_NO_COMPACT = "No"
internal const val PARK_RIDE_YES = "Yes, show me free spots"
internal const val PARK_RIDE_YES_COMPACT = "Yes"
internal const val PARK_RIDE_NOT_NOW = "Not now"
internal const val PARK_RIDE_ADD = "Add to home"
internal const val THEME_CONFIRM = "Yes, this one"
internal const val STOP_SEARCH = "No, search"
internal const val STOP_CHOOSE = "Yes, this one"
internal const val CONFIRM_NOT_NOW = "Not now"
internal const val CONFIRM_SAVE = "Yes, save it"

/**
 * One button of the decision bar. [label] is the whole phrase, which a screen reader reads at every
 * text size. [compactLabel] is what shows instead above [L_FONT_SCALE_THRESHOLD], the one place the
 * Intro shortens its words: a whole phrase does not fit beside another at a large text size, and the
 * answer is still the same. A label with no compact form shows whole at every size.
 */
internal data class IntroBarAction(
    val label: String,
    val event: IntroUiEvent,
    val compactLabel: String? = null,
)

/**
 * The decision bar of a screen: two buttons, the negative answer on the left and the positive
 * one on the right. The right one is the primary and is the only place the theme colour is drawn.
 * Only the right one is ever disabled.
 *
 * The theme screen is the one exception, by the owner's ruling: it asks for a colour and offers no way to say no,
 * so [left] is null there and the bar is the primary alone. Every other screen has both. `IntroBarTest` pins that.
 */
internal data class IntroBar(
    val left: IntroBarAction?,
    val right: IntroBarAction,
    val rightEnabled: Boolean = true,
)

/** What the button says at [fontScale]: the compact form above the large text threshold, otherwise the whole phrase. */
internal fun IntroBarAction.visibleLabel(fontScale: Float): String =
    if (fontScale > L_FONT_SCALE_THRESHOLD) compactLabel ?: label else label

/**
 * What the bar of the screen [state] is on says and does, or null when the screen asks nothing and
 * shows no bar. A table over the step, so a new step is a compile error here and not a screen with the
 * wrong bar. [readiness] is the stops data, which only the screens that end in saving a trip wait on.
 *
 * Each screen has its own bar: [themeBar], [parkRideBar], [stopBar] for the destination and origin screens, which
 * share one, and [confirmBar].
 */
internal fun introBar(state: IntroState, readiness: StopsReadiness): IntroBar? = when (state.step) {
    IntroStep.THEME -> themeBar(state)
    IntroStep.PARK_RIDE -> parkRideBar(state)
    IntroStep.DESTINATION, IntroStep.ORIGIN -> stopBar(state)
    IntroStep.CONFIRM -> confirmBar(state, readiness)
}

/**
 * The destination and origin screens. The left button opens the search field and never chooses or moves on, so
 * it is always on, and the right button takes the highlighted row, which is the first one listed. When the typed
 * search found no stop nothing is highlighted, so the right button is off until the rider types something that
 * matches or clears the field.
 */
private fun stopBar(state: IntroState) = IntroBar(
    left = IntroBarAction(STOP_SEARCH, IntroUiEvent.FocusSearch),
    right = IntroBarAction(STOP_CHOOSE, IntroUiEvent.UseHighlightedStop),
    rightEnabled = state.search != IntroSearchPhase.NoMatch,
)

/**
 * The theme screen: the one button on the Intro with nothing beside it. A theme is chosen by tapping its badge,
 * there is no answer of no, and the rider must pick a colour to go on, so the single button is off until a theme
 * is selected. It is a full width taj button the height of the two button bar, so nothing moves when it turns on.
 * Its words are the whole phrase at every text size, and a screen reader hears the same words.
 */
private fun themeBar(state: IntroState) = IntroBar(
    left = null,
    right = IntroBarAction(THEME_CONFIRM, IntroUiEvent.ConfirmTheme),
    rightEnabled = state.selectedThemeId != null,
)

/**
 * Asking, then deciding on the card. Locating and a card still waiting for its numbers keep the same
 * bar with the right button disabled, so the bar never changes size or jumps, and the left button
 * stays live so the rider can always say no while they wait. A screen with nothing to decide has no bar
 * and the flow has already moved on.
 */
private fun parkRideBar(state: IntroState): IntroBar? = when (val card = state.parkRideCard) {
    ParkRideCardPhase.None -> if (state.parkRideAnswer == ParkRideAnswer.YES) null else askBar()
    ParkRideCardPhase.NoStationNear -> null
    is ParkRideCardPhase.Locating -> addBar(addEnabled = false)
    is ParkRideCardPhase.Card -> addBar(addEnabled = card.station.availability != ParkRideAvailabilityState.Loading)
}

private fun askBar() = IntroBar(
    left = IntroBarAction(PARK_RIDE_NO, IntroUiEvent.AnswerParkRide(yes = false), PARK_RIDE_NO_COMPACT),
    right = IntroBarAction(PARK_RIDE_YES, IntroUiEvent.AnswerParkRide(yes = true), PARK_RIDE_YES_COMPACT),
)

private fun addBar(addEnabled: Boolean) = IntroBar(
    left = IntroBarAction(PARK_RIDE_NOT_NOW, IntroUiEvent.DeclineParkRide),
    right = IntroBarAction(PARK_RIDE_ADD, IntroUiEvent.AddParkRideAndContinue),
    rightEnabled = addEnabled,
)

/**
 * The confirm screen. Not now is the only way to leave without saving, and Yes, save it writes the trip the rider
 * chose and ends the Intro. The save waits for the stops data to be Ready or Failed, as it always has, so the right
 * button is off while it is still loading, and a trip already written needs no wait.
 */
private fun confirmBar(state: IntroState, readiness: StopsReadiness) = IntroBar(
    left = IntroBarAction(CONFIRM_NOT_NOW, IntroUiEvent.FinishWithoutTrip),
    right = IntroBarAction(CONFIRM_SAVE, IntroUiEvent.FinishPressed),
    rightEnabled = state.saved || readiness.isUsable(),
)

// Only stops that are still loading hold the save back. A failed load is no different from a ready one,
// because the picks carry their own id and name, so a trip never needs the stops table.
private fun StopsReadiness.isUsable(): Boolean = when (this) {
    StopsReadiness.Unknown, StopsReadiness.Loading -> false
    is StopsReadiness.Failed, is StopsReadiness.Ready -> true
}
