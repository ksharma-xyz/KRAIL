package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize

// The soft shadow under a badge.
private val BadgeShadow = 6.dp

/**
 * One theme badge. It is the app's own `TransportModeIcon` for the choice's transport mode, used as it is: the
 * circle in the mode's colour, the mode's first letter in white, and the white ring, all drawn by that component.
 * Nothing is passed to it that picks a colour, so the letter is white on every badge, which is the rule for a mode
 * icon everywhere in the app. It is drawn at the component's largest size, `TransportModeIconSize.Large`, and grows
 * with the text as the component does.
 *
 * The icon has no click of its own, so a taj `RoundIconButton` with no fill of its own carries it. That button is
 * the 48 dp touch target, and it is what [frame] moves.
 *
 * [frame] says where the badge is. It is read inside `graphicsLayer`, so a frame that changes every tick moves
 * the badge without composing or laying anything out again. The chosen badge shows it with the dance, or with no
 * motion by sitting lifted, and the ring is the component's own fixed one on every badge. For a screen reader the
 * badge is one button named for its theme, with whether it is selected, and the letter is hidden from it because
 * the name says the same.
 */
@Composable
internal fun IntroThemeBadge(
    choice: IntroThemeChoice,
    selected: Boolean,
    frame: () -> BadgeFrame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoundIconButton(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier
            .graphicsLayer {
                val pose = frame()
                translationX = pose.xDp.dp.toPx()
                translationY = pose.yDp.dp.toPx()
                rotationZ = pose.rotationDegrees
                scaleX = pose.scale
                scaleY = pose.scale
            }
            // The tag comes before the clearing semantics, which clear what is inside them, tags included.
            .testTag(choice.tag)
            // One node for the badge, so a screen reader hears one button and its name, not the button and its
            // letter. It does what a tap does, and says whether it is selected.
            .clearAndSetSemantics {
                contentDescription = choice.name
                role = Role.Button
                this.selected = selected
                onClick {
                    onClick()
                    true
                }
            },
    ) {
        TransportModeIcon(
            transportMode = choice.mode,
            size = TransportModeIconSize.Large,
            displayBorder = true,
            modifier = Modifier.shadow(elevation = BadgeShadow, shape = CircleShape, clip = false),
        )
    }
}
