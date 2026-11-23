package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.max
import kotlin.math.min

/**
 * Where an Intro screen puts the middle zone, the thing it asks about, under the headline. [Under] starts it straight
 * under the headline, for a middle that changes height while it is used. [CentredInRoom] centres it in the room
 * between the headline and the bar. [CentredOnWindow] centres it on the vertical middle of the whole window, so it
 * sits at half the screen height whatever the headline above it takes, and is only moved when it would overlap the
 * headline or the bar.
 */
internal enum class MiddlePlacement { Under, CentredInRoom, CentredOnWindow }

/** The placement for the two flags a screen can give its frame. The window wins when both are set. */
internal fun middlePlacement(centreMiddle: Boolean, centreOnWindow: Boolean): MiddlePlacement = when {
    centreOnWindow -> MiddlePlacement.CentredOnWindow
    centreMiddle -> MiddlePlacement.CentredInRoom
    else -> MiddlePlacement.Under
}

/**
 * The vertical middle of the window, in pixels from the top of the frame, for a frame that is [zone] pixels tall and
 * sits [topInset] pixels below the top of the window and [bottomInset] above its bottom (the system bars, a cutout).
 * The window is the frame and both insets, so its middle is half way down that, less the top inset.
 */
internal fun windowMiddleInZone(zone: Int, topInset: Int, bottomInset: Int): Int = (zone + bottomInset - topInset) / 2

/**
 * The top of a middle zone [middle] pixels tall that is centred on [windowMiddle], in pixels from the top of the frame,
 * and kept out of the way of what is above and below it.
 *
 * - The middle is never higher than [headlineBottom], the bottom of the headline zone, which already holds the gap
 *   under the headline. That limit wins when there is not room for both, and the screen scrolls.
 * - The middle never ends lower than [barTop] less [bottomGap], one gap above the bar's slot, which the bar's own
 *   room keeps whether or not the bar is shown. When it is higher than [headlineBottom] allows, the headline's limit wins.
 *
 * Nothing here depends on the words of the headline, only on its height, which the screen keeps the same for every
 * headline it can show, so the middle does not move when the words change.
 */
internal fun windowCentredMiddleTop(
    windowMiddle: Int,
    middle: Int,
    headlineBottom: Int,
    barTop: Int,
    bottomGap: Int,
): Int {
    val ideal = windowMiddle - middle / 2
    val lowest = barTop - bottomGap - middle
    return max(headlineBottom, min(ideal, lowest))
}
