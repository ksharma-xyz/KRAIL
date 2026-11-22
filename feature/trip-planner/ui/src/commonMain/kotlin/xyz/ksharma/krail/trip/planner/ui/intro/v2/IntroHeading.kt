package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme

private const val STICKER_START_SCALE = 0.6f
private const val RISE_MS = 400
private val RiseDistance = 16.dp

/**
 * The heading of a step: a sticker emoji and a line of text, both in ink, in the same type as the
 * tagline the carousel used. The sticker pops and the line rises into place once when the step
 * appears. It is not repeated by a turn of the screen, because the flag is saved, and with reduced
 * motion it is already in place.
 */
@Composable
internal fun IntroHeading(emoji: String, text: String, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    // A preview shows the heading where it ends up, and so does reduced motion.
    val inPlaceAtOnce = isReduceMotionEnabled() || LocalInspectionMode.current
    var entered by rememberSaveable { mutableStateOf(inPlaceAtOnce) }
    LaunchedEffect(Unit) { entered = true }

    val rise by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = RISE_MS),
        label = "intro-heading-rise",
    )
    val pop by animateFloatAsState(
        targetValue = if (entered) 1f else STICKER_START_SCALE,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "intro-heading-pop",
    )

    Column(
        modifier = modifier.padding(top = dim.spacingXXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        Text(
            text = emoji,
            style = KrailTheme.typography.introTagline,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                }
                // A decoration. The line below says everything it would.
                .clearAndSetSemantics {},
        )
        Text(
            text = text,
            style = KrailTheme.typography.introTagline,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier
                .graphicsLayer {
                    alpha = rise
                    translationY = (1f - rise) * RiseDistance.toPx()
                }
                .semantics { heading() },
        )
    }
}
