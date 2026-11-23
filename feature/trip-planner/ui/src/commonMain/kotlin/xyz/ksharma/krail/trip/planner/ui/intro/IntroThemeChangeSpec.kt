package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// What one run of the theme change sequence is made of, and how a second tap carries on from where the first had got to.
// All of it is plain values and pure functions of the clock, so a table holds it, and the drawing only reads it.

/** The theme the sequence moves to: its id (null for none), the hex the app is switched to and the colour that hex is. */
internal data class ThemeChangeTarget(val themeId: Int?, val hex: String, val colour: Color)

/**
 * The colours at the two ends. [fromColour] and [toColour] are the theme colour, which is what the button and everything that reads
 * the theme colour blend; [fromText] and [toText] are the headline's text; [fromGround] and [toGround] are the colour of the
 * window's ground, the wash. They all move on the one blend ([themeColour]).
 */
internal data class ThemeChangeColours(
    val fromColour: Color,
    val toColour: Color,
    val fromText: Color,
    val toText: Color,
    val fromGround: Color,
    val toGround: Color,
)

/** A circle of ground colour that an earlier tap had grown to [share] of the way to covering the window, and left where it was. */
internal data class ThemeWashLayer(val colour: Color, val origin: Offset, val share: Float)

/** The wash: where it grows from, and the circles of earlier taps that were cut short and are still under it. */
internal data class ThemeChangeWash(val origin: Offset, val frozen: List<ThemeWashLayer>)

/**
 * The words: the headline that arrives, the one that leaves (null when nothing is leaving) with how its lines look as the sequence
 * starts, the emoji that is shown until it changes, and the emoji's scale as the sequence starts.
 */
internal data class ThemeChangeHeadline(
    val incoming: HeadlineContent,
    val outgoing: HeadlineContent?,
    val outgoingLooks: LineLooks,
    val emojiBefore: String,
    val emojiScaleAtStart: Float,
)

/** The badge that was tapped, -1 for none, and the one tapped before it with how long ago that was as this sequence starts. */
internal data class ThemeChangeTaps(val tapped: Int, val previous: Int, val previousElapsedMs: Float)

/**
 * One run of the sequence. Everything in it is fixed when the run starts, and the clock, in milliseconds since then, is the only
 * thing that moves. [buttonAtMs] is when the button shows, which is 0 when it was already there.
 */
internal data class ThemeChangeSpec(
    val reduceMotion: Boolean,
    val target: ThemeChangeTarget,
    val colours: ThemeChangeColours,
    val wash: ThemeChangeWash,
    val headline: ThemeChangeHeadline,
    val taps: ThemeChangeTaps,
    val buttonAtMs: Float,
) {
    /** How long this run takes. */
    val durationMs: Float get() = themeChangeDurationMs(reduceMotion)
}

/** What a tap asks for: the theme, the colours it moves to, the words, where the wash starts and which badge was tapped. */
internal data class ThemeChangeRequest(
    val reduceMotion: Boolean,
    val target: ThemeChangeTarget,
    val toText: Color,
    val toGround: Color,
    val headline: HeadlineContent,
    val tapped: Int,
    val origin: Offset,
)

/** The most circles of cut short taps kept under the wash. Four badges can be tapped no faster than this fills. */
internal const val THEME_WASH_FROZEN_MAX = 4

/** The run that is already over, showing [request]'s theme with nothing moving: the state the screen opens in. */
internal fun settledThemeChangeSpec(
    request: ThemeChangeRequest,
    ground: Color,
    text: Color,
): ThemeChangeSpec = ThemeChangeSpec(
    reduceMotion = request.reduceMotion,
    target = request.target,
    colours = ThemeChangeColours(
        fromColour = request.target.colour,
        toColour = request.target.colour,
        fromText = text,
        toText = text,
        fromGround = ground,
        toGround = ground,
    ),
    wash = ThemeChangeWash(request.origin, emptyList()),
    headline = ThemeChangeHeadline(
        incoming = request.headline,
        outgoing = null,
        outgoingLooks = LineLooks.settled(0),
        emojiBefore = request.headline.emoji,
        emojiScaleAtStart = 1f,
    ),
    taps = ThemeChangeTaps(tapped = -1, previous = -1, previousElapsedMs = 0f),
    // With no theme there is no button, so the first pick is the first time it shows. With one, it is already there.
    buttonAtMs = if (request.target.themeId == null) Float.MAX_VALUE else 0f,
)

// Exactly the start colour at 0 and exactly the end colour at 1, not a copy of either that a round trip through a colour space
// has moved a little, so a run starts where the one before stopped and settles on the theme's own colour.
internal fun exactBlend(from: Color, to: Color, progress: Float): Color = when {
    progress <= 0f -> from
    progress >= 1f -> to
    else -> lerp(from, to, progress)
}

/** The theme colour as it is [timeMs] into [this] run: exactly the end colour once the run is over. */
internal fun ThemeChangeSpec.colourAt(timeMs: Float): Color =
    exactBlend(colours.fromColour, colours.toColour, themeColour(timeMs, reduceMotion))

/** The headline's text colour at [timeMs], on the same blend as [colourAt]. */
internal fun ThemeChangeSpec.textAt(timeMs: Float): Color =
    exactBlend(colours.fromText, colours.toText, themeColour(timeMs, reduceMotion))

/** The emoji that is shown at [timeMs]. */
internal fun ThemeChangeSpec.emojiAt(timeMs: Float): String =
    if (themeEmojiIsNew(timeMs, reduceMotion)) headline.incoming.emoji else headline.emojiBefore

/** The scale of badge [index] from its press, at [timeMs]. 1 for a badge that was not tapped, and with reduced motion. */
internal fun ThemeChangeSpec.pressAt(index: Int, timeMs: Float): Float = when {
    reduceMotion -> 1f
    index == taps.tapped -> themePress(timeMs)
    index == taps.previous -> themePress(taps.previousElapsedMs + timeMs)
    else -> 1f
}

/**
 * Hands every layer of the ground at [timeMs] to [draw], bottom first, as a colour, a centre and how far it has grown to covering
 * the window, 1 being all of it. A layer of share 1 is the whole window, and with reduced motion there is just the one, the
 * colour blend of the two grounds. It allocates nothing, so a frame can call it from `drawBehind`.
 */
internal inline fun ThemeChangeSpec.forEachGroundLayer(timeMs: Float, draw: (Color, Offset, Float) -> Unit) {
    if (reduceMotion) {
        draw(exactBlend(colours.fromGround, colours.toGround, themeColour(timeMs, true)), wash.origin, 1f)
        return
    }
    draw(colours.fromGround, wash.origin, 1f)
    if (timeMs >= durationMs) {
        draw(colours.toGround, wash.origin, 1f)
        return
    }
    // An index loop, so no iterator is made on a frame.
    for (index in wash.frozen.indices) {
        val layer = wash.frozen[index]
        draw(layer.colour, layer.origin, layer.share)
    }
    draw(colours.toGround, wash.origin, themeWash(timeMs))
}
