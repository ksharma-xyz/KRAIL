package xyz.ksharma.krail.trip.planner.ui.searchstop

// The mode controls of the Search Stop map state and the box at the top: which of them are drawn, and what moves the
// mode block open or shut. Every decision is a pure function, so each is pinned without a layout.

/** Whether the mode button and its block are drawn at all, drawn and hidden for now, or not part of the screen. */
internal enum class ModeControls {
    /** Not there: the list state, or a map that is not ready. Nothing is composed. */
    Absent,

    /** Composed, but hidden with a short fade and scale: a stop card is open, or the location permission is not granted. */
    Hidden,

    /** Drawn. */
    Shown,
}

/**
 * Where the mode controls are, in one place. They belong to the map state of a map that is ready ([ModeControls.Absent]
 * otherwise, never with the keyboard up). Within it they are shown only while the location permission is granted
 * ([locationGranted], whether the pill under the box asks or sends the rider to Settings), and are hidden while a stop
 * card is open ([stopCardOpen]). The rider's mode selection is not part of this: it is the map's and is kept while the
 * button is hidden.
 */
internal fun modeControlsFor(
    mode: SearchStopMode,
    mapReady: Boolean,
    locationGranted: Boolean = true,
    stopCardOpen: Boolean = false,
): ModeControls = when {
    mode != SearchStopMode.Map || !mapReady -> ModeControls.Absent
    !locationGranted || stopCardOpen -> ModeControls.Hidden
    else -> ModeControls.Shown
}

/** The mode button and its block are drawn: the map state of a ready map, with the location allowed and no card open. */
internal fun modeControlsShown(
    mode: SearchStopMode,
    mapReady: Boolean,
    locationGranted: Boolean = true,
    stopCardOpen: Boolean = false,
): Boolean = modeControlsFor(mode, mapReady, locationGranted, stopCardOpen) == ModeControls.Shown

/** The button shows its active look while any mode is switched off ([allClasses] is every mode the filter offers). */
internal fun modeFilterActive(allClasses: List<Int>, selected: Set<Int>): Boolean =
    allClasses.any { it !in selected }

/** What moves the mode block open or shut. */
internal enum class ModeBlockEvent { ButtonTapped, MapTapped, LeftMapState, ControlsHidden }

/**
 * Open on a tap of the button when shut and shut by the button again, a tap on the map, leaving the map state, or the
 * button going away because a stop card opened. A card that was open when the button went is shut and stays shut when the
 * button comes back, so the rider returns to the map as they left it, with the block closed.
 */
internal fun reduceModeBlock(open: Boolean, event: ModeBlockEvent): Boolean = when (event) {
    ModeBlockEvent.ButtonTapped -> !open
    ModeBlockEvent.MapTapped, ModeBlockEvent.LeftMapState, ModeBlockEvent.ControlsHidden -> false
}

/**
 * The event that shuts the mode block when the button goes away ([ModeControls.Hidden]: a stop card opened or the
 * location permission is not granted), or null while the button is shown or not part of the screen.
 */
internal fun modeBlockEventFor(controls: ModeControls): ModeBlockEvent? =
    if (controls == ModeControls.Hidden) ModeBlockEvent.ControlsHidden else null

/**
 * Whether the box at the top (the group card or the bare pill, and the location chip under it) is hidden: while the
 * Departure Board of an open stop card is expanded ([boardExpanded], which is false with no card open) in the map state, which
 * hands its room to the card. It comes back when the board is collapsed or the card is dismissed, and it is never hidden in
 * the list state, which is that box and the list.
 */
internal fun topBoxHiddenFor(mode: SearchStopMode, boardExpanded: Boolean): Boolean =
    mode == SearchStopMode.Map && boardExpanded
