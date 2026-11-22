package xyz.ksharma.krail.trip.planner.ui.intro

// How the headline's lines leave and arrive in the theme change sequence: the old ones out, the new ones in line by line.

/** The old headline eases up and fades out over the first third of the sequence. */
internal const val THEME_OUT_MS = 233f

/** How far a line moves as it leaves and as it arrives, in dp. Up is negative. */
internal const val THEME_RISE_DP = 8f

// The new tagline starts when the old one has gone, line by line, [THEME_LINE_STAGGER_MS] apart, each over [THEME_LINE_IN_MS].
internal const val THEME_LINE_IN_START_MS = THEME_OUT_MS
internal const val THEME_LINE_STAGGER_MS = 60f
internal const val THEME_LINE_IN_MS = 280f

/** How far the old headline has gone, 0 to 1: it eases up and fades over [THEME_OUT_MS], or over the cross-fade with reduced motion. */
internal fun themeOutgoing(timeMs: Float, reduceMotion: Boolean): Float =
    if (reduceMotion) themeShare(timeMs, 0f, THEME_CHANGE_REDUCED_MS) else easeOutCubic(themeShare(timeMs, 0f, THEME_OUT_MS))

/**
 * How far line [index] of the new headline has come in, 0 to 1. With motion each line starts [THEME_LINE_STAGGER_MS] after the one
 * before, from [THEME_LINE_IN_START_MS], so the old headline is gone before the first of them shows. With reduced motion every line
 * cross-fades with the old one over the one short window.
 */
internal fun themeIncomingLine(timeMs: Float, index: Int, reduceMotion: Boolean): Float {
    if (reduceMotion) return themeShare(timeMs, 0f, THEME_CHANGE_REDUCED_MS)
    val start = THEME_LINE_IN_START_MS + index * THEME_LINE_STAGGER_MS
    return easeOutCubic(themeShare(timeMs, start, start + THEME_LINE_IN_MS))
}

/** A leaving line's opacity, from [alphaAtStart] to 0 as [outgoing] goes from 0 to 1. */
internal fun themeOutgoingAlpha(alphaAtStart: Float, outgoing: Float): Float = alphaAtStart * (1f - outgoing)

/** A leaving line's rise in dp, from [riseAtStartDp] up to [THEME_RISE_DP] above its place, and none with reduced motion. */
internal fun themeOutgoingRiseDp(riseAtStartDp: Float, outgoing: Float, reduceMotion: Boolean): Float =
    if (reduceMotion) 0f else riseAtStartDp + (-THEME_RISE_DP - riseAtStartDp) * outgoing

/** An arriving line's rise in dp: [THEME_RISE_DP] below its place at 0 and none at 1, and none with reduced motion. */
internal fun themeIncomingRiseDp(incoming: Float, reduceMotion: Boolean): Float =
    if (reduceMotion) 0f else THEME_RISE_DP * (1f - incoming)

/**
 * How the lines of one headline look: each line's opacity and how far it is moved, in dp, up negative. It is what a headline
 * is when a sequence starts, so a second tap can carry on from what the first had got to.
 */
internal data class LineLooks(val alphas: List<Float>, val risesDp: List<Float>) {
    /** How much of the headline can be seen, which is the sum of its lines' opacity. */
    val visibility: Float get() = alphas.sum()

    companion object {
        /** [count] lines in place and fully shown. */
        fun settled(count: Int): LineLooks = LineLooks(List(count) { 1f }, List(count) { 0f })
    }
}

/** The leaving headline at [timeMs], from how it looked at the start. */
internal fun outgoingLooks(start: LineLooks, timeMs: Float, reduceMotion: Boolean): LineLooks {
    val gone = themeOutgoing(timeMs, reduceMotion)
    return LineLooks(
        alphas = start.alphas.map { themeOutgoingAlpha(it, gone) },
        risesDp = start.risesDp.map { themeOutgoingRiseDp(it, gone, reduceMotion) },
    )
}

/** The arriving headline of [count] lines at [timeMs]. */
internal fun incomingLooks(count: Int, timeMs: Float, reduceMotion: Boolean): LineLooks {
    val progress = List(count) { themeIncomingLine(timeMs, it, reduceMotion) }
    return LineLooks(alphas = progress, risesDp = progress.map { themeIncomingRiseDp(it, reduceMotion) })
}

