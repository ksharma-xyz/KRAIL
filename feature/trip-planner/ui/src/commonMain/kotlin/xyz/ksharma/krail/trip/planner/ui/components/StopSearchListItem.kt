package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_stop_label_airport
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import app.krail.taj.resources.Res as TajRes

@Composable
fun StopSearchListItem(
    stopName: String,
    stopId: String,
    transportModeSet: ImmutableSet<TransportMode>,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: (StopItem) -> Unit = {},
    isSaved: Boolean = false,
    onSaveAsLabel: ((StopItem) -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                onClick(StopItem(stopId = stopId, stopName = stopName))
            }
            .padding(vertical = dim.spacingM, horizontal = dim.spacingXXL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
        ) {
            Text(
                text = stopName,
                color = textColor,
                style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                transportModeSet.forEach { mode ->
                    TransportModeIcon(transportMode = mode, size = TransportModeIconSize.Small)
                }
                if (stopName.contains("airport", ignoreCase = true)) {
                    Image(
                        painter = painterResource(TajRes.drawable.ic_stop_label_airport),
                        contentDescription = "Airport",
                        colorFilter = ColorFilter.tint(KrailTheme.colors.label),
                        modifier = Modifier.size(TransportModeIconSize.Small.dpSize),
                    )
                }
            }
        }
        if (onSaveAsLabel != null) {
            SaveAsLabelStar(
                isSaved = isSaved,
                onClick = { onSaveAsLabel(StopItem(stopId = stopId, stopName = stopName)) },
            )
        }
    }
}

@Composable
private fun SaveAsLabelStar(isSaved: Boolean, onClick: () -> Unit) {
    val icon = if (isSaved) Res.drawable.ic_star_filled else Res.drawable.ic_star
    val tint = if (isSaved) themeColor() else KrailTheme.colors.softLabel
    Image(
        painter = painterResource(icon),
        contentDescription = if (isSaved) "Saved as label" else "Save as label",
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .klickable(onClick = onClick)
            .padding(6.dp),
    )
}

// region Preview

@Preview
@Composable
private fun StopSearchListItemPreview() {
    PreviewTheme {
        StopSearchListItem(
            stopId = "123",
            stopName = "Stop Name",
            transportModeSet = persistentSetOf(
                TransportMode.Bus,
                TransportMode.LightRail,
            ),
            textColor = KrailTheme.colors.onSurface,
        )
    }
}

@Preview
@Composable
private fun StopSearchListItemLongNamePreview() {
    PreviewTheme {
        StopSearchListItem(
            stopId = "123",
            stopName = "This is a very long stop name that should wrap to the next line",
            transportModeSet = persistentSetOf(
                TransportMode.Train,
                TransportMode.Ferry,
            ),
            textColor = KrailTheme.colors.onSurface,
        )
    }
}

// endregion
