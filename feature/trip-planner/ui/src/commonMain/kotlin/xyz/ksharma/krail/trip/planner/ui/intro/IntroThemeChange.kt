package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.trip.planner.ui.intro.finale.floodRadiusPx

// Animations at normal speed, whatever the system's animation scale is.
private object FullSpeed : MotionDurationScale {
    override val scaleFactor: Float = 1f
}

// The four badges of the colour screen, the most a tap can come from.
private const val BADGE_SLOTS = 4

/**
 * The sequence that plays when a badge is tapped, for the screens under [IntroThemeBlend] to draw. It is `null` outside the Intro's
 * shell, such as in a preview, where nothing plays and the screen is simply in the state it was given.
 */
internal val LocalIntroThemeChange = staticCompositionLocalOf<IntroThemeChange?> { null }

/**
 * The state of the theme change sequence: the run that is on ([spec]) and its one clock ([clock]), in milliseconds since the
 * tap. Everything that moves reads the clock inside a draw or layer lambda, so a frame redraws and does not compose or lay out, and
 * the few things that change what is composed, the words and the button, are read from [spec] and from values derived from the clock
 * that only change when something does. A new tap starts a new run from where this one had got to ([play]), so nothing jumps.
 *
 * It also keeps where each badge is, so the wash can grow from the one that was tapped.
 */
@Stable
internal class IntroThemeChange(initial: ThemeChangeSpec) {

    var spec: ThemeChangeSpec by mutableStateOf(initial)
        private set

    val clock = Animatable(initial.durationMs)

    private val revealed = derivedStateOf { clock.value >= spec.buttonAtMs }
    private val running = derivedStateOf { clock.value < spec.durationMs }
    private val emojiIsNew = derivedStateOf { themeEmojiIsNew(clock.value, spec.reduceMotion) }

    private var root: LayoutCoordinates? = null
    private val badges = arrayOfNulls<LayoutCoordinates>(BADGE_SLOTS)
    private var pendingTap = -1
    private var pendingOrigin = Offset.Unspecified

    /** The coordinates of the window the wash is drawn in. */
    fun attachRoot(coordinates: LayoutCoordinates) {
        root = coordinates
    }

    /** The coordinates of badge [index], so a tap on it can say where the wash starts. */
    fun attachBadge(index: Int, coordinates: LayoutCoordinates) {
        if (index in badges.indices) badges[index] = coordinates
    }

    /** Badge [index] was tapped: remembers it, and the middle of it in the window, for the sequence the pick starts. */
    fun noteTap(index: Int) {
        pendingTap = index
        val badge = badges.getOrNull(index)
        val window = root
        pendingOrigin = if (badge != null && window != null && badge.isAttached && window.isAttached) {
            window.localPositionOf(badge, badge.size.toSize().center)
        } else {
            Offset.Unspecified
        }
    }

    /** The tap the next sequence is for, as the badge and where it was, and forgets it. The middle of the window when there was none. */
    fun takeTap(): Pair<Int, Offset> {
        val origin = if (pendingOrigin.isSpecified) pendingOrigin else windowCentre()
        val tap = pendingTap to origin
        pendingTap = -1
        pendingOrigin = Offset.Unspecified
        return tap
    }

    private fun windowCentre(): Offset = root?.takeIf { it.isAttached }?.size?.toSize()?.center ?: Offset.Zero

    /**
     * Plays the run for [request] from where the one that was on had got to, and is over when the clock is. A tap in the middle of a
     * run cancels this and starts the next, which begins at the state this had reached.
     */
    suspend fun play(request: ThemeChangeRequest) {
        spec = themeChangeSpecAfter(spec, clock.value, request)
        clock.snapTo(0f)
        val run = spec
        // The plain cross-fade of reduced motion is the whole of what the rider asked to be left with, and Android's own switch for it
        // sets the animation scale to 0, which would make it a snap, so it runs at normal speed whatever that scale is.
        if (run.reduceMotion) withContext(FullSpeed) { animateClock(run) } else animateClock(run)
        spec = run.copy(headline = run.headline.copy(outgoing = null))
    }

    private suspend fun animateClock(run: ThemeChangeSpec) {
        clock.animateTo(run.durationMs, tween(durationMillis = run.durationMs.toInt(), easing = LinearEasing))
    }

    /** Whether the run has not finished. */
    val isRunning: Boolean get() = running.value

    /** Whether the button is there: always, once it has been, and from [ThemeChangeSpec.buttonAtMs] on the pick that shows it first. */
    val buttonRevealed: Boolean get() = revealed.value

    /** Whether the emoji is the new one. It only changes while the emoji is at its smallest. */
    val emojiShowsNew: Boolean get() = emojiIsNew.value

    /**
     * Draws the ground of the window, the wash, into the scope's whole size. [settled] is the ground that stays once it is over, or
     * null for none, where the plain surface behind shows. Nothing here allocates.
     */
    fun drawGround(scope: DrawScope, settled: Color?) {
        val s = spec
        val time = clock.value
        if (time >= s.durationMs) {
            if (settled != null) scope.drawRect(settled)
            return
        }
        s.forEachGroundLayer(time) { colour, origin, share -> scope.drawWash(colour, origin, share) }
    }
}

// One layer of the ground: the whole window at 1, a circle from [origin] that is that share of the way to covering it before.
private fun DrawScope.drawWash(colour: Color, origin: Offset, share: Float) {
    if (share >= 1f) {
        drawRect(colour)
        return
    }
    if (share <= 0f) return
    val centre = if (origin.isSpecified) origin else center
    drawCircle(colour, radius = floodRadiusPx(share, size.width, size.height, centre.x, centre.y), center = centre)
}
