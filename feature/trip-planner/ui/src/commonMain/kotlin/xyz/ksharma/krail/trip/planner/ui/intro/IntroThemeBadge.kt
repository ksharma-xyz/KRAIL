package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor

// The soft shadow under a badge.
private val BadgeShadow = 6.dp

/**
 * One theme badge, copied from the pills in the website's footer: a circle in the theme's colour with its letter
 * in the app's heaviest display style, a white ring and a soft shadow. It is a taj `RoundIconButton` sized and
 * ringed by modifiers, because that button has a content slot and no border of its own.
 *
 * The colour is read from the theme itself, and the letter takes whichever of the app's light and dark text
 * colours reads clearly on it. The badge is 64 dp at the default text size and grows with the text to at most
 * 96 dp, so the letter always fits, and it is well over the 48 dp a touch target needs.
 *
 * [frame] says where the badge is. It is read inside `graphicsLayer`, so a frame that changes every tick moves
 * the badge without composing or laying anything out again. For a screen reader the badge is one button named
 * for its theme, with whether it is selected, and the letter is hidden from it because the name says the same.
 */
@Composable
internal fun IntroThemeBadge(
    choice: IntroThemeChoice,
    selected: Boolean,
    ringWidth: Dp,
    frame: () -> BadgeFrame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = remember(choice) { choice.style.hexColorCode.hexToComposeColor() }
    val letterColor = remember(fill) { getForegroundColor(fill) }
    val size = badgeSizeDp(LocalDensity.current.fontScale).dp

    RoundIconButton(
        onClick = onClick,
        color = fill,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val pose = frame()
                translationX = pose.xDp.dp.toPx()
                translationY = pose.yDp.dp.toPx()
                rotationZ = pose.rotationDegrees
                scaleX = pose.scale
                scaleY = pose.scale
            }
            .shadow(elevation = BadgeShadow, shape = CircleShape, clip = false)
            .border(width = ringWidth, color = Color.White, shape = CircleShape)
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
        Text(
            text = choice.letter,
            style = KrailTheme.typography.introTagline,
            color = letterColor,
        )
    }
}
