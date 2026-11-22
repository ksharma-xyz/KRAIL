package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text

/**
 * The one small status button that floats on the map: the location notice on home and the location chip on
 * Search Stop. It is taj's filled [Button] as it ships, at [ButtonDefaults.extraSmallButtonSize] (the smaller of
 * taj's sizes, whose text is the normal weight), with the fixed dark and fixed light passed through the button's
 * own colours parameter ([FloatingDark.buttonColors]), so it reads on the light map in both app modes. It has
 * no look of its own: the touch target is [minimumInteractiveComponentSize] (48 dp) and the text wraps inside
 * the button, never beside it.
 */
@Composable
internal fun FloatingStatusButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = FloatingDark.buttonColors(),
        dimensions = ButtonDefaults.extraSmallButtonSize(),
        // The tag comes before the size, so the node a screen reader and a test find is the whole 48 dp target.
        modifier = modifier
            .testTag(tag)
            .minimumInteractiveComponentSize(),
    ) {
        Text(text = text)
    }
}
