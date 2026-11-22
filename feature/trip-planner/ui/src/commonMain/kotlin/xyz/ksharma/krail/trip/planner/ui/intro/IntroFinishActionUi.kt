package xyz.ksharma.krail.trip.planner.ui.intro

/**
 * What the bottom button says. [IntroFinishAction.PickBothStops] reads Save and start because that
 * is what the button becomes once both stops are picked, and it is disabled until then.
 */
internal fun IntroFinishAction.label(): String = when (this) {
    IntroFinishAction.Next -> "Next"
    IntroFinishAction.GettingReady -> "Getting ready"
    IntroFinishAction.PickBothStops,
    IntroFinishAction.SaveAndStart,
    -> "Save and start"
    IntroFinishAction.SkipForNow -> "Skip for now"
    IntroFinishAction.LetsKrail -> "Let's KRAIL"
}

/** Only the two waiting states hold the button back. Every other action can be pressed. */
internal fun IntroFinishAction.isEnabled(): Boolean = when (this) {
    IntroFinishAction.GettingReady,
    IntroFinishAction.PickBothStops,
    -> false
    IntroFinishAction.Next,
    IntroFinishAction.SaveAndStart,
    IntroFinishAction.SkipForNow,
    IntroFinishAction.LetsKrail,
    -> true
}
