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
internal const val THEME_ANOTHER = "No, show me another"
internal const val THEME_CONFIRM = "Yes, this one"
internal const val STOP_SEARCH = "No, search"
internal const val STOP_CHOOSE = "Yes, this one"
internal const val TRIP_NOT_NOW = "Not now"
internal const val TRIP_SAVE = "Yes, save it"

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
 * The decision bar of a screen: always two buttons, the negative answer on the left and the positive
 * one on the right. The right one is the primary and is the only place the theme colour is drawn.
 * Only the right one is ever disabled.
 */
internal data class IntroBar(
    val left: IntroBarAction,
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
 * The theme screen has its own bar, see [themeBar], and so have the destination and origin screens, which
 * share [stopBar]. The confirm screen shares one temporary bar until the unit that builds it gives it its own.
 */
internal fun introBar(state: IntroState, readiness: StopsReadiness): IntroBar? = when (state.step) {
    IntroStep.THEME -> themeBar(state)
    IntroStep.PARK_RIDE -> parkRideBar(state)
    IntroStep.DESTINATION, IntroStep.ORIGIN -> stopBar(state)
    IntroStep.CONFIRM -> tripBar(state, readiness)
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
 * The theme screen. The left button moves to the next theme, so it is always available and the first press
 * both starts the choice and shows a theme, and the right button confirms the theme on show. The rider must
 * pick a colour to go on, so the right one is off until a theme is selected. Neither has a compact form: at a
 * large text size, or a narrow window, the bar stacks the two whole buttons instead.
 */
private fun themeBar(state: IntroState) = IntroBar(
    left = IntroBarAction(THEME_ANOTHER, IntroUiEvent.ShowAnotherTheme),
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
 * TEMPORARY, for the confirm screen until it has its own bar. The trip is
 * saved from the picks if there is a complete pair and otherwise nothing is written, and the Intro ends
 * either way. The save waits for the stops data to be Ready or Failed, as the confirm screen will, and
 * a trip already written needs no wait.
 *
 * Outcome analytics are completed with the confirm screen: `skipped` then means the rider answered No
 * here, and today it still means no complete trip was picked (see [IntroState.toOutcome]).
 */
private fun tripBar(state: IntroState, readiness: StopsReadiness) = IntroBar(
    left = IntroBarAction(TRIP_NOT_NOW, IntroUiEvent.FinishWithoutTrip),
    right = IntroBarAction(TRIP_SAVE, IntroUiEvent.FinishPressed),
    rightEnabled = state.saved || readiness.isUsable(),
)

// Only stops that are still loading hold the save back. A failed load is no different from a ready one,
// because the picks carry their own id and name, so a trip never needs the stops table.
private fun StopsReadiness.isUsable(): Boolean = when (this) {
    StopsReadiness.Unknown, StopsReadiness.Loading -> false
    is StopsReadiness.Failed, is StopsReadiness.Ready -> true
}
