package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.CloseIcon
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * The close X of the map screens: taj's [RoundIconButton] drawn as a [CIRCLE_DIAMETER] circle in [circle] with the cross in
 * [glyph] inside it, so an X is never a bare glyph. The caller picks the two colours from existing tokens so the circle shows
 * on whatever it sits on, and the cross is 4.5 to 1 or better on the circle.
 *
 * The finger target is a real [TARGET] box around the circle that is itself the clickable, not the touch slop Compose adds
 * around a small clickable, which loses to the map under a control that floats over it (a tap on the map outranks that
 * slop). [tag] is on that box, and the circle is tagged [circleTag]. A screen reader hears one button, [contentDescription].
 *
 * [glyphSize] is a step of the icon scale, `KrailTheme.dimensions.iconM` or `iconL`, and the cross is half of it.
 */
@Composable
internal fun CloseCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    circle: Color,
    glyph: Color,
    glyphSize: Dp,
    modifier: Modifier = Modifier,
    tag: String? = null,
    circleTag: String? = null,
) {
    CircleTargetButton(
        contentDescription = contentDescription,
        onClick = onClick,
        circle = circle,
        modifier = modifier,
        tag = tag,
        circleTag = circleTag,
    ) {
        Image(
            imageVector = CloseIcon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(glyph),
            modifier = Modifier.size(glyphSize),
        )
    }
}

/**
 * A [CIRCLE_DIAMETER] circle in [circle] with [glyph] drawn in it, inside a real [TARGET] box that is itself the clickable.
 * It is the pattern of [CloseCircleButton], for any round control that floats over the map (the go circle of the Search Stop
 * card): the target is a box that consumes the touch, not the slop Compose adds around a small clickable, so a tap in the 48 dp
 * reaches the control and never the map under it. [tag] is on that box, [circleTag] on the circle. A screen reader hears one
 * button, [contentDescription].
 */
@Composable
internal fun CircleTargetButton(
    contentDescription: String,
    onClick: () -> Unit,
    circle: Color,
    modifier: Modifier = Modifier,
    tag: String? = null,
    circleTag: String? = null,
    glyph: @Composable () -> Unit,
) {
    TargetBox(contentDescription = contentDescription, onClick = onClick, modifier = modifier, tag = tag) {
        Box(
            modifier = Modifier
                .size(CIRCLE_DIAMETER)
                .then(if (circleTag != null) Modifier.testTag(circleTag) else Modifier),
        ) {
            // The round button is the circle and the glyph. The box round it is the one a screen reader finds, so the
            // button's own semantics are cleared and the name is said once.
            RoundIconButton(
                onClick = onClick,
                color = circle,
                modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
            ) {
                glyph()
            }
        }
    }
}

/**
 * The real [TARGET] box of a control that floats over the map: it is itself the clickable and consumes the touch, so a tap
 * anywhere in its 48 dp reaches the control and never the map under it (a tap on the map outranks the touch slop Compose adds
 * around a small clickable). [indication] is the press feedback, the box's own ripple unless a caller that draws a bare glyph
 * asks for a round one. The content is centred in it. A screen reader hears one button, [contentDescription], and [tag] is on the
 * box.
 */
@Composable
internal fun TargetBox(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    indication: Indication? = LocalIndication.current,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(TARGET)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = indication,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** The circle of an X, the same size as the go circle beside it in a search pill. */
internal val CIRCLE_DIAMETER = 40.dp

/** The finger target round the circle, the 48 dp of rule 21. */
internal val TARGET = 48.dp

/** The circle and cross a soft card draws: the app's surface for the circle and its text colour for the cross. */
@Composable
internal fun softCloseCircle(): Pair<Color, Color> = KrailTheme.colors.surface to KrailTheme.colors.onSurface
