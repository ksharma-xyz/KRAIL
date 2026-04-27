package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

@Composable
fun StopSearchListItem(
    stopName: String,
    stopId: String,
    transportModeSet: ImmutableSet<TransportMode>,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: (StopItem) -> Unit = {},
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
            ) {
                transportModeSet.forEach { mode ->
                    TransportModeIcon(transportMode = mode, size = TransportModeIconSize.Small)
                }
            }
        }
        if (onSaveAsLabel != null) {
            SaveAsLabelButton(
                onClick = { onSaveAsLabel(StopItem(stopId = stopId, stopName = stopName)) },
            )
        }
    }
}

@Composable
private fun SaveAsLabelButton(onClick: () -> Unit) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(KrailTheme.colors.surface, shape)
            .border(
                width = dim.strokeThin,
                color = KrailTheme.colors.outlineSubtle,
                shape = shape,
            )
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+ Save",
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.softLabel,
        )
    }
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
