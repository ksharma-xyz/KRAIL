package xyz.ksharma.krail.trip.planner.ui.intro

/** What an event does to the keyboard and the search field's focus, which only the screen can do. */
internal enum class IntroKeyboardAction {
    /** Focus the search field and show the keyboard. */
    Open,

    /** Hide the keyboard and let go of the focus, because the rider has chosen or left the screen. */
    Close,

    /** Leave both as they are. */
    None,
}

/**
 * The keyboard's part in [this] event. No search opens the field, and every way off a stop screen, a choice and
 * Back, closes it, so the keyboard never stays up over the next screen. Everything else leaves it alone.
 */
internal fun IntroUiEvent.keyboardAction(): IntroKeyboardAction = when (this) {
    IntroUiEvent.FocusSearch -> IntroKeyboardAction.Open
    is IntroUiEvent.ChooseStop, IntroUiEvent.UseHighlightedStop, IntroUiEvent.Back -> IntroKeyboardAction.Close
    else -> IntroKeyboardAction.None
}
