package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize

// The soft shadow under a badge, and the least a badge is in each direction, so it is always a full touch target.
private val BadgeShadow = 6.dp
private val MinTouchTarget = 48.dp

/**
 * One theme badge. It is the app's own `TransportModeIcon` for the choice's transport mode, used as it is: the
 * circle in the mode's colour, the mode's first letter in white, and the white ring, all drawn by that component.
 * Nothing is passed to it that picks a colour, so the letter is white on every badge, which is the rule for a mode
 * icon everywhere in the app. It is drawn at the component's extra large size, `TransportModeIconSize.XLarge`, twice
 * the size it was, and grows with the text as the component does.
 *
 * The icon has no click of its own, so the badge is a box the size of the icon with the app's own click modifier
 * (`klickable`, the ripple every taj button has) on a circle laid over it. The box is at least 48 dp each way, so
 * it is the touch target, and it is what [frame] moves. The taj `RoundIconButton` is a fixed 48 dp circle that clips what
 * it holds, so it could not carry a badge this size.
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
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
            size = TransportModeIconSize.XLarge,
            displayBorder = true,
            modifier = Modifier.shadow(elevation = BadgeShadow, shape = CircleShape, clip = false),
        )
        // The tap and its ripple, in the circle of the badge, drawn over the icon.
        Box(modifier = Modifier.matchParentSize().clip(CircleShape).klickable(onClick = onClick))
    }
}
