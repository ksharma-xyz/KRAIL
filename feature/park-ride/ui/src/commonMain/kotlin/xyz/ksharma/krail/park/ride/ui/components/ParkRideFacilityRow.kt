package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * One selectable Park & Ride facility in the picker.
 *
 * The whole row is the tap target — a per-row "Add" button would stack a column of identical
 * buttons down the screen. The trailing [ParkRideAddToggle] shows state rather than being its
 * own control, so it carries no click of its own.
 *
 * Carries the same `P` identity as the home Park & Ride card, not a transport-mode letter,
 * so a facility looks the same wherever it appears.
 */
@Composable
fun ParkRideFacilityRow(
    facilityName: String,
    added: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    stopName: String? = null,
    // Slot so callers in other modules can pass the exact badge their surface already uses —
    // the picker passes trip-planner's `ParkRideIcon`, so a station looks identical in the
    // picker and on the home Park & Ride card.
    icon: @Composable () -> Unit = { ParkAndRideIcon() },
    showDivider: Boolean = true,
) {
    val dim = KrailTheme.dimensions

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // klickable sits before padding so the ripple covers the whole row width
                // rather than just the padded content box.
                .klickable(onClick = onClick)
                .padding(horizontal = dim.pageHorizontalPadding, vertical = RowVerticalPadding)
                // mergeDescendants so the badge, name, subtitle and toggle are announced as
                // one row instead of four separate nodes. State goes in stateDescription
                // rather than being concatenated into the label, so a screen reader reports
                // it as the control's state and not as part of its name.
                .semantics(mergeDescendants = true) {
                    role = Role.Checkbox
                    stateDescription = if (added) "Added" else "Not added"
                },
            horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dim.spacingXXS),
            ) {
                // No line cap on either line: at large font scales a clamped station name is
                // unreadable, and a taller row beats a name the rider cannot finish.
                Text(
                    text = facilityName,
                    style = KrailTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                )

                if (!stopName.isNullOrBlank() && stopName != facilityName) {
                    Text(
                        text = stopName,
                        style = KrailTheme.typography.bodySmall,
                        color = KrailTheme.colors.secondaryLabel,
                    )
                }
            }

            ParkRideAddToggle(added = added)
        }

        // Aligned to the page gutter, so the rule starts level with the badge and the list
        // reads as one column rather than an indented sub-list.
        if (showDivider) {
            Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
        }
    }
}

@PreviewComponent
@Composable
private fun ParkRideFacilityRowPreview() {
    PreviewTheme {
        ParkRideFacilityRow(
            facilityName = "Gordon Henry St (north)",
            stopName = "Gordon Station",
            added = false,
            onClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun ParkRideFacilityRowAddedPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink) {
        ParkRideFacilityRow(
            facilityName = "Revesby",
            stopName = "Revesby Station",
            added = true,
            onClick = {},
        )
    }
}

// A touch taller than spacingL so each row reads as its own block rather than a dense list.
private val RowVerticalPadding = 20.dp
