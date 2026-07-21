package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.ensureMinimumContrast
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.taj.themeContentColor

private val ToggleSize = 32.dp
private val GlyphStrokeWidth = 2.dp
private val RingStrokeWidth = 1.5.dp

/**
 * The trailing affordance on a Park & Ride picker row: a `+` that flips to a check once the
 * facility is added. Drawn rather than iconified so it scales with the row and needs no
 * drawable per state.
 *
 * The disc uses the user's selected theme colour ([themeColor]); the glyph starts from
 * [themeContentColor] but is passed through [getForegroundColor], so a theme whose content
 * colour is too low-contrast against its own fill falls back to white or black instead of
 * rendering an unreadable glyph.
 */
@Composable
fun ParkRideAddToggle(
    added: Boolean,
    modifier: Modifier = Modifier,
) {
    // Not-added is an outline ring on the surface, so the row stays quiet and the accent is
    // the only colour in it. Added flips to a filled disc, which reads as "done" at a glance
    // rather than relying on telling a plus from a tick.
    val accent = themeColor().ensureMinimumContrast(background = KrailTheme.colors.surface)
    val glyphColor = if (added) {
        getForegroundColor(backgroundColor = accent, foregroundColor = themeContentColor())
    } else {
        accent
    }

    val haptic = LocalHapticFeedback.current
    // Buzz only on a real change, never on first composition and never when a row scrolls back
    // into view. A tap that changes nothing (a station a saved trip is holding) therefore
    // stays silent, which is the honest signal.
    var previousAdded by remember { mutableStateOf(added) }
    LaunchedEffect(added) {
        if (added != previousAdded) {
            previousAdded = added
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // The disc springs past its resting size on change, so the state flip is felt as well as
    // seen. Spring rather than tween: it settles naturally if toggled rapidly.
    val transition = updateTransition(targetState = added, label = "park-ride-toggle")
    val discScale by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "park-ride-toggle-pop",
    ) { isAdded -> if (isAdded) TOGGLE_ADDED_SCALE else 1f }

    Box(
        modifier = modifier
            // Purely a state indicator: the row it sits in owns the click and announces the
            // state, so announcing it again here would just repeat.
            .clearAndSetSemantics {}
            .size(ToggleSize)
            .scale(discScale)
            .clip(CircleShape)
            .then(
                if (added) {
                    Modifier.background(color = accent, shape = CircleShape)
                } else {
                    Modifier.border(width = RingStrokeWidth, color = accent, shape = CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // The plus rotates a quarter turn out as the tick rotates in, so the two glyphs read
        // as one control changing rather than two icons swapping.
        AnimatedContent(
            targetState = added,
            transitionSpec = {
                (scaleIn(initialScale = GLYPH_ENTER_SCALE) + fadeIn())
                    .togetherWith(scaleOut(targetScale = GLYPH_EXIT_SCALE) + fadeOut())
            },
            label = "park-ride-toggle-glyph",
        ) { isAdded ->
            if (isAdded) {
                CheckGlyph(color = glyphColor)
            } else {
                PlusGlyph(color = glyphColor)
            }
        }
    }
}

private const val TOGGLE_ADDED_SCALE = 1.08f
private const val GLYPH_ENTER_SCALE = 0.4f
private const val GLYPH_EXIT_SCALE = 0.4f

@Composable
private fun PlusGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(ToggleSize)) {
        val stroke = GlyphStrokeWidth.toPx()
        val inset = size.minDimension * PLUS_INSET_FRACTION
        drawLine(
            color = color,
            start = Offset(x = inset, y = size.height / 2),
            end = Offset(x = size.width - inset, y = size.height / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(x = size.width / 2, y = inset),
            end = Offset(x = size.width / 2, y = size.height - inset),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CheckGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(ToggleSize)) {
        val stroke = GlyphStrokeWidth.toPx()
        val w = size.width
        val h = size.height
        drawLine(
            color = color,
            start = Offset(x = w * CHECK_START_X, y = h * CHECK_START_Y),
            end = Offset(x = w * CHECK_VERTEX_X, y = h * CHECK_VERTEX_Y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(x = w * CHECK_VERTEX_X, y = h * CHECK_VERTEX_Y),
            end = Offset(x = w * CHECK_END_X, y = h * CHECK_END_Y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private const val PLUS_INSET_FRACTION = 0.3f
private const val CHECK_START_X = 0.30f
private const val CHECK_START_Y = 0.52f
private const val CHECK_VERTEX_X = 0.44f
private const val CHECK_VERTEX_Y = 0.66f
private const val CHECK_END_X = 0.70f
private const val CHECK_END_Y = 0.36f

@PreviewComponent
@Composable
private fun ParkRideAddTogglePreview() {
    PreviewTheme {
        Box(modifier = Modifier.background(KrailTheme.colors.surface)) {
            ParkRideAddToggle(added = false)
        }
    }
}

@PreviewComponent
@Composable
private fun ParkRideAddToggleAddedPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        Box(modifier = Modifier.background(KrailTheme.colors.surface)) {
            ParkRideAddToggle(added = true)
        }
    }
}
