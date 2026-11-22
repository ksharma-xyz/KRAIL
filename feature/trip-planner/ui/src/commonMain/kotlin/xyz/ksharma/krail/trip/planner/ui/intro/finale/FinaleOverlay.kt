package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.testTag
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

/**
 * The finale as one full screen layer: the backdrop, the wordmark in the middle, and the colour that floods out
 * of it. It draws [frame] and nothing else, so everything it does is a function of one value, and it is the
 * layer that sits above the app, never one that holds it.
 *
 * - The backdrop and the flood are `drawBehind`, read when drawing: the ground [groundColour] fades in, and
 *   [themeColour] grows from the middle as a circle to the farthest corner, and at full cover is a plain rectangle,
 *   so no pixel is left uncovered. There is no offscreen layer here, and nothing under this layer is wrapped.
 * - The wordmark is inside the system bars and the page's side padding, and is the only thing that is not the
 *   backdrop.
 * - It swallows every touch, so nothing behind it can be pressed. A tap anywhere is [onTap], which skips the
 *   finale. It is a gesture on the layer and not a control.
 * - A screen reader hears one phrase, [FINALE_ACCESSIBILITY_LABEL], once as the layer appears, and not the letters.
 */
@Composable
internal fun FinaleOverlay(
    frame: State<FinaleFrame>,
    themeColour: Color,
    groundColour: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics {
                contentDescription = FINALE_ACCESSIBILITY_LABEL
                liveRegion = LiveRegionMode.Polite
                testTag = TripPlannerTestTags.INTRO_FINALE
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) }
            .drawBehind { drawBackdrop(frame.value, themeColour, groundColour) },
        contentAlignment = Alignment.Center,
    ) {
        FinaleWordmark(
            frame = frame,
            themeColour = themeColour,
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding),
        )
    }
}
