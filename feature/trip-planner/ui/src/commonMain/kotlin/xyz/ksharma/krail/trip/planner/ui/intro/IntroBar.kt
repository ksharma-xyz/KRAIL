package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase

// The words on the decision bar. Every one is an answer, never a way past a question, so none of them
// is Skip, Later, Next, Continue or Done. IntroBannedWordsTest holds that for every string the Intro draws.
// The Park & Ride answers show one word at every text size, and the theme screen's button shows its name, and a screen reader
// reads the longer phrase. Each visible word is the start of its spoken phrase, so voice control that says the word finds the
// button.
internal const val PARK_RIDE_NO = "No"
internal const val PARK_RIDE_NO_SPOKEN = "No thanks"
internal const val PARK_RIDE_YES = "Yes"
internal const val PARK_RIDE_YES_SPOKEN = "Yes, show me free spots"
internal const val PARK_RIDE_NOT_NOW = "Not now"
internal const val PARK_RIDE_ADD = "Add to home"

// The one button when no station is near. OK is an answer to being told there is nothing to add, not a way past a question, and
// it is not one of the banned words. It is plain and says the same thing aloud, so voice control that says OK finds it.
internal const val PARK_RIDE_NO_STATION_OK = "OK"
internal const val THEME_CONFIRM = "Let's KRAIL"
internal const val THEME_CONFIRM_SPOKEN = "Let's KRAIL, use this colour"
internal const val STOP_CHOOSE = "Yes, this one"
internal const val CONFIRM_NOT_NOW = "Not now"
internal const val CONFIRM_SAVE = "Yes, save it"

/**
 * One button of the decision bar. [label] is what shows, at every text size. [spokenLabel] is what a screen reader
 * reads and voice control matches, which is [label] unless the button shows a shorter word than it says.
 */
internal data class IntroBarAction(
    val label: String,
    val event: IntroUiEvent,
    val spokenLabel: String = label,
)

/**
 * The decision bar of a screen: two buttons, the negative answer on the left and the positive
 * one on the right. The right one is the primary and is the only place the theme colour is drawn.
 *
 * No button of the Intro is ever drawn disabled, and there is no field here that could say so. A button is either shown
 * and live, so pressing it does something sensible whatever data is or is not ready, or it is not shown at all and keeps
 * its room. A rider who has not answered is never held on a button that cannot be pressed, and no state is a dead end.
 *
 * The screens that ask the rider to pick something, the theme and the two stop screens, are the exception: there is
 * no answer of no, so [left] is null and the bar is the primary alone. Every other screen has both. `IntroBarTest`
 * pins that.
 *
 * [shown] is false while the bar has nothing to answer yet, which is a single button on a screen where nothing has
 * been picked. The bar then draws nothing and cannot be reached, and it keeps the room it will take, so the screen
 * does not move when the button appears. A screen that has no bar at all is the null from [introBar] instead.
 *
 * [rightShown] is the same for the right button alone, while the left one stays shown and live. The Park & Ride card
 * has it false while the station is being found, so the rider can still say no and the bar does not change size when
 * Add to home appears.
 */
internal data class IntroBar(
    val left: IntroBarAction?,
    val right: IntroBarAction,
    val shown: Boolean = true,
    val rightShown: Boolean = true,
)

// How long the single button takes to fade in once the rider has picked something.
internal const val BAR_REVEAL_MS = 250

/** How long the bar takes to appear: [BAR_REVEAL_MS], and none under reduced motion, where it simply appears. */
internal fun introBarRevealMs(reduceMotion: Boolean): Int = if (reduceMotion) 0 else BAR_REVEAL_MS

/**
 * What the bar of the screen [state] is on says and does, or null when the screen asks nothing and
 * shows no bar. A table over the step, so a new step is a compile error here and not a screen with the
 * wrong bar. It reads the state and nothing else: no data that is still loading can change a button, because every
 * button does something whether or not it is ready.
 *
 * Each screen has its own bar: [themeBar], [parkRideBar], [stopBar] for the destination and origin screens, which
 * share one, and [confirmBar].
 */
internal fun introBar(state: IntroState): IntroBar? = when (state.step) {
    IntroStep.THEME -> themeBar(state)
    IntroStep.PARK_RIDE -> parkRideBar(state)
    IntroStep.DESTINATION, IntroStep.ORIGIN -> stopBar(state)
    IntroStep.CONFIRM -> confirmBar()
}

/**
 * The destination and origin screens: one button, like the theme screen. Nothing is selected when the screen opens, so
 * the button is not shown until the rider has tapped a stop, and it then confirms that stop and moves on. It keeps the
 * room it will take, so the list does not move when it appears, and it is never a disabled button.
 */
private fun stopBar(state: IntroState) = IntroBar(
    left = null,
    right = IntroBarAction(STOP_CHOOSE, IntroUiEvent.ConfirmStop),
    shown = state.selectedStop != null,
)

/**
 * The theme screen, the last: a single button with nothing beside it, which ends the Intro. A theme is chosen by
 * tapping its badge, there is no answer of no, and the rider must pick a colour to go on, so the single button is not
 * shown until a theme is selected, and then it fades in where the room for it was kept, and is live. It is a full width
 * taj button the height of the two button bar, so nothing moves when it appears. Its words are the whole phrase at every
 * text size, and a screen reader hears the same words.
 *
 * It never waits for the stops data. The press starts the finale at once, and anything that has to wait for the stops
 * waits under the colour the finale draws (`IntroFlowViewModel`), so the button is never off while the data loads.
 */
private fun themeBar(state: IntroState) = IntroBar(
    left = null,
    right = IntroBarAction(THEME_CONFIRM, IntroUiEvent.ConfirmTheme, THEME_CONFIRM_SPOKEN),
    shown = state.selectedThemeId != null,
)

/**
 * Asking, then deciding on the card. Locating, and a card still waiting for its numbers, keep the same bar, so it never
 * changes size or jumps. The left button is live throughout, so the rider can always say no while they wait. The right
 * one, Add to home, is not shown while the station is still being found, and is live as soon as a card shows, also while
 * its numbers load, because adding needs the station and not the readings. No station near, within the wider distance too,
 * is one button, [noStationBar], because the rider has been told so and moves on with an answer of their own. A screen with no
 * usable location, which has nothing to say, has no bar and the flow has already moved on.
 */
private fun parkRideBar(state: IntroState): IntroBar? = when (state.parkRideCard) {
    ParkRideCardPhase.None -> if (state.parkRideAnswer == ParkRideAnswer.YES) null else askBar()
    ParkRideCardPhase.NoStationNear -> noStationBar()
    is ParkRideCardPhase.Locating -> decideBar(addShown = false)
    is ParkRideCardPhase.Card -> decideBar(addShown = true)
}

private fun askBar() = IntroBar(
    left = IntroBarAction(PARK_RIDE_NO, IntroUiEvent.AnswerParkRide(yes = false), PARK_RIDE_NO_SPOKEN),
    right = IntroBarAction(PARK_RIDE_YES, IntroUiEvent.AnswerParkRide(yes = true), PARK_RIDE_YES_SPOKEN),
)

/**
 * No Park & Ride station near, in the 5 km first look or the 10 km second one: the card's room says so, and the one button
 * moves on to the next screen. It is live as soon as the message shows and nothing moves on without it.
 */
private fun noStationBar() = IntroBar(
    left = null,
    right = IntroBarAction(PARK_RIDE_NO_STATION_OK, IntroUiEvent.AcknowledgeNoStation),
)

private fun decideBar(addShown: Boolean) = IntroBar(
    left = IntroBarAction(PARK_RIDE_NOT_NOW, IntroUiEvent.DeclineParkRide),
    right = IntroBarAction(PARK_RIDE_ADD, IntroUiEvent.AddParkRideAndContinue),
    rightShown = addShown,
)

/**
 * The confirm screen. Not now is the only way to leave without saving, and Yes, save it keeps the trip the rider chose.
 * Both only record the answer and move on to the theme screen, where the Intro ends, so neither waits for anything.
 */
private fun confirmBar() = IntroBar(
    left = IntroBarAction(CONFIRM_NOT_NOW, IntroUiEvent.AnswerSaveTrip(yes = false)),
    right = IntroBarAction(CONFIRM_SAVE, IntroUiEvent.AnswerSaveTrip(yes = true)),
)
