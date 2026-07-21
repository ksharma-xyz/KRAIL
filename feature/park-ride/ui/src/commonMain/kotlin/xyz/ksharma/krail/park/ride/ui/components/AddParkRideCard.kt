package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * The always-present entry point into the Park & Ride picker.
 *
 * Deliberately an action, not an empty state: when a user has no Park & Ride stations this is
 * the only thing in the section, so it has to offer the next step rather than report that
 * there is nothing to show. It keeps the same shape once cards exist, where [label] becomes
 * the "add another" wording.
 *
 * Deliberately has no container: filling it the same way as [ParkRideCard] made an action
 * look identical to a card carrying live parking data. A plain row keeps the filled card
 * treatment meaning "this holds data", and this meaning "this does something".
 */
@Composable
fun AddParkRideCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Clip before klickable: the ripple takes the clip bounds, so without this the
            // touch feedback is a full-width rectangle on a row that has no container.
            .clip(RoundedCornerShape(percent = PILL_CORNER_PERCENT))
            .klickable(onClick = onClick)
            .padding(horizontal = dim.spacingL, vertical = dim.spacingML)
            // One node: the ring is decorative, the label is the whole meaning.
            .semantics(mergeDescendants = true) { role = Role.Button },
        horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParkRideAddToggle(added = false)

        Text(
            text = label,
            style = KrailTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@PreviewComponent
@Composable
private fun AddParkRideCardPreview() {
    PreviewTheme {
        AddParkRideCard(label = "Add Park & Ride", onClick = {})
    }
}

@PreviewComponent
@Composable
private fun AddParkRideCardAnotherPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AddParkRideCard(label = "Add another", onClick = {})
    }
}

private const val PILL_CORNER_PERCENT = 50
