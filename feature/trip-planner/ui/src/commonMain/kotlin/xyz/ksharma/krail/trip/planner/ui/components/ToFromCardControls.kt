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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_reverse
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

// The controls of the Search Stop card's right column (`docs/MAP_HOME_UX.md`, The box): the bare swap glyph, the go circle and the
// clear X. They share one vertical axis, 36 dp from the card's right edge. The swap is the only one that moves.

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

/**
 * The go circle: the theme colour with the app's arrow in the colour taj gives that fill. It is there only while [shown] (both
 * stops chosen), absent and not disabled otherwise. It arrives scaling from nothing to [MapMotion.GO_PEAK_SCALE] by
 * [MapMotion.GO_PEAK_MS] and back to 1 by [MapMotion.GO_ENTER_MS] while it fades in over [MapMotion.GO_FADE_IN_MS], and it leaves
 * in [MapMotion.GO_LEAVE_MS] with its scale 1 minus p squared and its alpha 1 minus p. The scale is drawn on the 40 dp circle and
 * the 48 dp box round it never changes size. The box takes no tap until the scale reaches [MapMotion.GO_TAP_SCALE] on the way in
 * and none from the first frame of the way out, and it consumes the touch meanwhile so it never reaches the map. Plain, with no
 * frame between, with reduced motion. The slot is the To row's end, so the card is the same height with the circle and without it.
 */
@Composable
internal fun GoCircleSlot(shown: Boolean, onClick: () -> Unit) {
    val reduceMotion = isReduceMotionEnabled()
    val scale = remember { Animatable(if (shown) 1f else 0f) }
    val fade = remember { Animatable(if (shown) 1f else 0f) }
    var present by remember { mutableStateOf(shown) }
    val currentShown by rememberUpdatedState(shown)
    val currentClick by rememberUpdatedState(onClick)
    LaunchedEffect(shown, reduceMotion) {
        if (shown) {
            present = true
            if (reduceMotion) {
                scale.snapTo(1f)
                fade.snapTo(1f)
            } else {
                coroutineScope {
                    launch { fade.animateTo(1f, goAppearFadeSpec()) }
                    launch {
                        scale.animateTo(MapMotion.GO_PEAK_SCALE, goGrowSpec())
                        scale.animateTo(1f, goSettleSpec())
                    }
                }
            }
        } else {
            if (reduceMotion) {
                scale.snapTo(0f)
                fade.snapTo(0f)
            } else {
                coroutineScope {
                    launch { fade.animateTo(0f, goLeaveFadeSpec()) }
                    launch { scale.animateTo(0f, goLeaveScaleSpec()) }
                }
            }
            present = false
        }
    }
    if (!present) return
    val fill = mapActiveChipColors().containerColor
    CircleTargetButton(
        contentDescription = "Show trips",
        onClick = {
            if (currentShown && scale.value >= MapMotion.GO_TAP_SCALE) currentClick()
        },
        circle = fill,
        tag = TripPlannerTestTags.SEARCH_ROW_SEARCH,
        circleTag = TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE,
        circleLayer = {
            scaleX = scale.value
            scaleY = scale.value
            alpha = fade.value
        },
    ) {
        Image(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            colorFilter = ColorFilter.tint(getForegroundColor(fill)),
            modifier = Modifier.size(KrailTheme.dimensions.iconM),
        )
    }
}

/**
 * The X in a row's end slot while its field holds text: a bare cross, no circle, `iconM` (24 dp) across like the search glyph
 * of a field and in the card's text colour, in a 48 dp target that consumes the touch (see [ClearGlyphButton]). It clears the field
 * and does nothing else.
 */
@Composable
internal fun ClearCross(state: TextFieldState) {
    ClearGlyphButton(
        contentDescription = "Clear",
        onClick = { state.clearText() },
        glyph = mapCardColors().onTint,
        glyphSize = KrailTheme.dimensions.iconM,
        tag = TripPlannerTestTags.SEARCH_STOP_CLEAR,
        glyphTag = TripPlannerTestTags.SEARCH_STOP_CLEAR_GLYPH,
    )
}
