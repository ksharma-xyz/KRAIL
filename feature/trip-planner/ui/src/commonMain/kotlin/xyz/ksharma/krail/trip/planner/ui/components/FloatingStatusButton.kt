package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * The one small status button that floats on the map: the location notice on home and the location chip on
 * Search Stop. It is taj's filled [Button] as it ships, at [ButtonDefaults.extraSmallButtonSize] (the smaller of
 * taj's sizes, whose text is the normal weight), in the soft theme tint with the page's text colour that the Timetable's
 * own chips wear ([mapCardColors], which follows the app mode) and a 1 dp hairline ([mapChipEdge]) in light mode so a pale chip
 * holds against the light map. The dark tint is clear of the map on its own and has none. It has no look of its own, and its text
 * wraps inside the button, never beside it.
 *
 * The finger target is the node tagged [tag]: a real 48 dp box that is itself the clickable, not the touch slop
 * Compose adds around a small clickable, which loses to the map under it (a tap on the map outranks that slop). The pill
 * sits in its middle. A screen reader hears the one node.
 */
@Composable
internal fun FloatingStatusButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dimensions = ButtonDefaults.extraSmallButtonSize()
    Box(
        // The tag comes before the size, so the node a screen reader and a test find is the whole 48 dp target.
        modifier = modifier
            .testTag(tag)
            .heightIn(min = KrailTheme.dimensions.textFieldHeight)
            .widthIn(min = KrailTheme.dimensions.textFieldHeight)
            .semantics(mergeDescendants = true) {}
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = mapCardColors().chip,
            dimensions = dimensions,
            modifier = Modifier.mapChipEdge(dimensions.shape),
        ) {
            Text(text = text)
        }
    }
}
