package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_reverse
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.theme.krailRipple
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

// The controls of the Search Stop card's right column (`docs/MAP_HOME_UX.md`, The box): the bare swap glyph, the go circle and the
// clear X. They share one vertical axis, 36 dp from the card's right edge. The swap is the only one that moves.

/**
 * The swap's motion, held by the card. [lift] is how far the swap has glided from the divider level (0) up to the From row's
 * centre line (1), and [fade] is its alpha. Both are animations read in the draw phase, so a frame of either redraws and
 * does not recompose. [present] is whether the swap is in the tree at all: it stays while it fades out and then leaves.
 */
@Stable
internal class SwapMotion(
    val lift: Animatable<Float, *>,
    val fade: Animatable<Float, *>,
    present: Boolean,
) {
    var present by mutableStateOf(present)

    /** A tap is answered by what is under it at that instant: from [MapMotion.SWAP_TAP_ALPHA] while it fades in, never while it leaves. */
    fun takesTaps(shown: Boolean): Boolean = shown && fade.value >= MapMotion.SWAP_TAP_ALPHA
}

/**
 * The swap's motion for [shown] (at least one stop chosen and no X on the card) and [lifted] (both stops chosen, so the go
 * circle is in the To row and the swap has made room). It fades in or out in [MapMotion.SWAP_FADE_MS] with no move, and glides
 * between its two heights in [MapMotion.SWAP_GLIDE_MS], each from wherever it is. Reduced motion jumps in one frame.
 */
@Composable
internal fun rememberSwapMotion(shown: Boolean, lifted: Boolean): SwapMotion {
    val reduceMotion = isReduceMotionEnabled()
    val motion = remember { SwapMotion(Animatable(if (lifted) 1f else 0f), Animatable(if (shown) 1f else 0f), shown) }
    LaunchedEffect(lifted, reduceMotion) {
        val target = if (lifted) 1f else 0f
        if (reduceMotion) motion.lift.snapTo(target) else if (motion.lift.value != target) motion.lift.animateTo(target, swapGlideSpec())
    }
    LaunchedEffect(shown, reduceMotion) {
        val target = if (shown) 1f else 0f
        if (shown) motion.present = true
        if (reduceMotion) motion.fade.snapTo(target) else if (motion.fade.value != target) motion.fade.animateTo(target, swapFadeSpec())
        if (!shown) motion.present = false
    }
    return motion
}

/**
 * The swap: the reverse glyph, 24 dp in the card's text colour, bare. No circle, no fill, no ring: it just appears and sits there,
 * inside an invisible 48 dp box that consumes the touch so a tap in it reaches the swap and never the map under it. A tap
 * reverses From and To ([onSwap]) and the glyph makes a half turn, 180 degrees in [MapMotion.SWAP_TURN_MS], plain with reduced
 * motion. It looks the same at 0 and 180 degrees, so the turn is a gesture and not a state. A screen reader hears Swap From and To.
 *
 * It is drawn at the divider level and the card moves it with [motion] (see `ToFromCardRows`); it is faded by [SwapMotion.fade]
 * in a layer, so the box moves with the glyph and a tap on the visible glyph always lands on it.
 */
@Composable
internal fun SwapControl(onSwap: () -> Unit, shown: Boolean, motion: SwapMotion, modifier: Modifier = Modifier) {
    val ink = mapCardColors().onTint
    val grid = rememberCardGrid()
    var turned by rememberSaveable { mutableStateOf(false) }
    val currentSwap by rememberUpdatedState(onSwap)
    val currentShown by rememberUpdatedState(shown)
    val reduceMotion = isReduceMotionEnabled()
    val angle by animateFloatAsState(
        targetValue = if (turned) SWAP_HALF_TURN else 0f,
        animationSpec = if (reduceMotion) snap() else tween(MapMotion.SWAP_TURN_MS, easing = FastOutSlowInEasing),
        label = "swapGlyphTurn",
    )
    TargetBox(
        contentDescription = "Swap From and To",
        onClick = {
            if (motion.takesTaps(currentShown)) {
                turned = !turned
                currentSwap()
            }
        },
        modifier = modifier,
        tag = TripPlannerTestTags.SEARCH_ROW_REVERSE,
        indication = krailRippleOnGlyph(grid.swapGlyph),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_reverse),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ink),
            modifier = Modifier
                .size(grid.swapGlyph)
                .graphicsLayer { rotationZ = angle }
                .testTag(TripPlannerTestTags.SEARCH_ROW_SWAP_GLYPH),
        )
    }
}

private const val SWAP_HALF_TURN = 180f

/** The press feedback of the bare glyph: a round ripple the glyph's own size, in the theme's decor colour, not the 48 dp square. */
@Composable
private fun krailRippleOnGlyph(radius: Dp) = krailRipple(bounded = false, radius = radius, color = themeDecorColor())

/**
 * The go circle: the theme colour with the app's arrow in the colour taj gives that fill. It is there only while [shown] (both
 * stops chosen), absent and not disabled otherwise, arriving by [mapGoCircleIn] and leaving by [mapVanishOut], and it takes no
 * touch while it is on its way out. The slot is the To row's end, so the card is the same height with it and without it.
 */
@Composable
internal fun GoCircleSlot(shown: Boolean, onClick: () -> Unit) {
    val reduceMotion = isReduceMotionEnabled()
    MapControlVisibility(
        visible = shown,
        enter = mapGoCircleIn(reduceMotion),
        exit = mapVanishOut(reduceMotion),
    ) {
        val fill = mapActiveChipColors().containerColor
        CircleTargetButton(
            contentDescription = "Show trips",
            onClick = onClick,
            circle = fill,
            tag = TripPlannerTestTags.SEARCH_ROW_SEARCH,
            circleTag = TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE,
        ) {
            Image(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                colorFilter = ColorFilter.tint(getForegroundColor(fill)),
                modifier = Modifier.size(KrailTheme.dimensions.iconM),
            )
        }
    }
}

/** The X in a row's end slot while its field holds text: it clears the field and does nothing else. */
@Composable
internal fun ClearCircle(state: TextFieldState) {
    val fill = md_theme_light_softLabel
    CloseCircleButton(
        contentDescription = "Clear",
        onClick = { state.clearText() },
        circle = fill,
        glyph = getForegroundColor(fill),
        glyphSize = KrailTheme.dimensions.iconL,
        tag = TripPlannerTestTags.SEARCH_STOP_CLEAR,
        circleTag = TripPlannerTestTags.SEARCH_STOP_CLEAR_CIRCLE,
    )
}
