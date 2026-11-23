package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget

/**
 * The place the Intro keeps a focus it is handed and did not ask for. It goes on the root of the Intro, outside every
 * control, and is the same for all five screens.
 *
 * In a window wide enough for two panes the Intro is drawn in the pane scaffold of the app's list and detail scene, and
 * that scaffold asks for the focus for the pane it shows each time it is composed, so on every start of the app and every
 * turn of the screen at that width. A pane is only a focus group, so the focus went on to the first control inside it. On
 * the destination and origin screens that is the search field, which opened the keyboard without a tap, and on the first
 * screen it was the `No thanks` button, where a key press would have answered for the rider.
 *
 * With this on the root the handed focus stops here. It draws nothing, takes no touch and is not announced, because it is
 * a bare focus target with no semantics. A tap on the field still gives the field the focus, and a move of the focus by
 * keyboard goes on from here into the controls in their order, so nothing is taken out of reach.
 */
internal fun Modifier.introFocusHome(): Modifier = focusTarget()
