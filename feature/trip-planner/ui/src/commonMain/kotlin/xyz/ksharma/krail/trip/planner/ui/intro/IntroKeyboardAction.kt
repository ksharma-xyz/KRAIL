package xyz.ksharma.krail.trip.planner.ui.intro

/** What an event does to the keyboard and the search field's focus, which only the screen can do. */
internal enum class IntroKeyboardAction {
    /** Hide the keyboard and let go of the focus, because the rider has chosen. */
    Close,

    /** Leave both as they are. */
    None,
}

/**
 * The keyboard's part in [this] event. The field opens the keyboard itself when it is tapped, so nothing here opens it.
 * Selecting a stop and confirming one close it, so the list and the button are in view and the keyboard never stays up
 * over the next screen. Everything else leaves it alone.
 */
internal fun IntroUiEvent.keyboardAction(): IntroKeyboardAction = when (this) {
    is IntroUiEvent.SelectStop, IntroUiEvent.ConfirmStop -> IntroKeyboardAction.Close
    else -> IntroKeyboardAction.None
}
