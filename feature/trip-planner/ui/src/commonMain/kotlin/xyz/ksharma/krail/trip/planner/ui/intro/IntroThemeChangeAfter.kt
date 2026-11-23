package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color

/**
 * The run for [request] that carries on from [previous], [timeMs] into it. It starts exactly where [previous] had got to, so nothing
 * jumps: the colours begin at the blend [previous] had reached, the circle it was growing is left where it was, under the new one,
 * the headline that was most to be seen leaves from the opacity and the place it had, the emoji starts at the scale it had and the
 * emoji it showed, and the badge pressed before carries on from how far it had got. A [previous] that is over is just a start from
 * its end.
 */
internal fun themeChangeSpecAfter(
    previous: ThemeChangeSpec,
    timeMs: Float,
    request: ThemeChangeRequest,
): ThemeChangeSpec {
    val over = timeMs >= previous.durationMs
    val time = timeMs.coerceIn(0f, previous.durationMs)
    val fromColour = previous.colourAt(time)
    val fromText = previous.textAt(time)
    return ThemeChangeSpec(
        reduceMotion = request.reduceMotion,
        target = request.target,
        colours = ThemeChangeColours(
            fromColour = fromColour,
            toColour = request.target.colour,
            fromText = fromText,
            toText = request.toText,
            fromGround = groundBelow(previous, time, over),
            toGround = request.toGround,
        ),
        wash = ThemeChangeWash(request.origin, frozenAfter(previous, time, over)),
        headline = headlineAfter(previous, time, request.headline),
        taps = ThemeChangeTaps(
            tapped = request.tapped,
            previous = if (previous.reduceMotion) -1 else previous.taps.tapped,
            previousElapsedMs = time,
        ),
        buttonAtMs = if (previous.buttonAtMs <= time) 0f else themeButtonAtMs(request.reduceMotion),
    )
}

// The colour of the whole window under the new circle: the previous run's own ground when it is over, or when it left a circle that
// has covered everything, that circle's colour, and otherwise the ground that circle was growing over.
private fun groundBelow(previous: ThemeChangeSpec, time: Float, over: Boolean): Color = when {
    over -> previous.colours.toGround
    previous.reduceMotion -> exactBlend(previous.colours.fromGround, previous.colours.toGround, themeColour(time, true))
    themeWash(time) >= 1f -> previous.colours.toGround
    else -> previous.colours.fromGround
}

// The circles the previous run left, which are its own frozen ones and the one it was growing, each where it had got to. A circle
// that had covered everything leaves nothing under it, and only the last few are kept.
private fun frozenAfter(previous: ThemeChangeSpec, time: Float, over: Boolean): List<ThemeWashLayer> {
    if (over || previous.reduceMotion || themeWash(time) >= 1f) return emptyList()
    val growing = ThemeWashLayer(previous.colours.toGround, previous.wash.origin, themeWash(time))
    return (previous.wash.frozen + growing).takeLast(THEME_WASH_FROZEN_MAX)
}

// The headline that is most to be seen at [time], leaving from how it looked, and the emoji as it is.
private fun headlineAfter(previous: ThemeChangeSpec, time: Float, incoming: HeadlineContent): ThemeChangeHeadline {
    val old = previous.headline
    val reduce = previous.reduceMotion
    val leaving = old.outgoing?.let { it to outgoingLooks(old.outgoingLooks, time, reduce) }
    val arriving = old.incoming to incomingLooks(old.incoming.lines.size, time, reduce)
    val shown = if (leaving != null && leaving.second.visibility > arriving.second.visibility) leaving else arriving
    return ThemeChangeHeadline(
        incoming = incoming,
        outgoing = shown.first,
        outgoingLooks = shown.second,
        emojiBefore = previous.emojiAt(time),
        emojiScaleAtStart = themeEmojiScale(time, old.emojiScaleAtStart, reduce),
    )
}
