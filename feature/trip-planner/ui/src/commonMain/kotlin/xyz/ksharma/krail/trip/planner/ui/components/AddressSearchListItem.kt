package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Row for an address/POI hit from the remote `stop_finder` search. Deliberately mirrors
 * [StopSearchListItem]'s padding/spacing so both row types read as the same list, but
 * carries its own leading pin icon + a subtitle line (address type) instead of transport
 * mode icons + star, since neither concept applies to a raw address.
 */
@Composable
fun AddressSearchListItem(
    displayName: String,
    addressType: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = dim.spacingM, horizontal = dim.pageHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_location_on),
            contentDescription = null,
            colorFilter = ColorFilter.tint(textColor),
            modifier = Modifier.size(TransportModeIconSize.Small.dpSize),
        )
        Column(verticalArrangement = Arrangement.spacedBy(dim.spacingXS)) {
            Text(
                text = displayName,
                color = textColor,
                style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = addressTypeLabel(addressType),
                color = KrailTheme.colors.softLabel,
                style = KrailTheme.typography.bodySmall,
            )
        }
    }
}

private fun addressTypeLabel(addressType: String): String = when (addressType) {
    "singlehouse" -> "Address"
    "street" -> "Street"
    "poi" -> "Point of interest"
    else -> "Place"
}

// region Preview

@PreviewComponent
@Composable
private fun AddressSearchListItemPreview() {
    PreviewTheme {
        AddressSearchListItem(
            displayName = "123 Example St, Sydney",
            addressType = "singlehouse",
            textColor = KrailTheme.colors.onSurface,
        )
    }
}

// endregion
