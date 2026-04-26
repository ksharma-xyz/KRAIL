package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

@Composable
fun StopLabelPillRow(
    stopLabels: ImmutableList<StopLabel>,
    onLabelClick: (StopItem) -> Unit,
    onUnsetLabelClick: (StopLabel) -> Unit,
    onAddLabelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(items = stopLabels, key = { it.label }) { label ->
            if (label.isSet) {
                SetLabelPill(
                    label = label,
                    onClick = { label.toStopItem()?.let(onLabelClick) },
                )
            } else {
                UnsetLabelPill(
                    label = label,
                    onClick = { onUnsetLabelClick(label) },
                )
            }
        }

        item(key = "add-label") {
            AddLabelPill(onClick = onAddLabelClick)
        }
    }
}

@Composable
private fun SetLabelPill(
    label: StopLabel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val themeColor = themeColor()

    Row(
        modifier = modifier
            .clip(shape)
            .background(color = themeColor, shape = shape)
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides KrailTheme.colors.surface,
            LocalTextStyle provides KrailTheme.typography.labelLarge,
        ) {
            Text(text = label.emoji)
            Text(text = label.label)
        }
    }
}

@Composable
private fun UnsetLabelPill(
    label: StopLabel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)

    Row(
        modifier = modifier
            .clip(shape)
            .border(
                width = dim.strokeThin,
                color = KrailTheme.colors.onSurface.copy(alpha = 0.3f),
                shape = shape,
            )
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides KrailTheme.colors.onSurface.copy(alpha = 0.5f),
            LocalTextStyle provides KrailTheme.typography.labelLarge,
        ) {
            Text(text = label.emoji)
            Text(text = label.label)
        }
    }
}

@Composable
private fun AddLabelPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)

    Row(
        modifier = modifier
            .clip(shape)
            .border(
                width = dim.strokeThin,
                color = KrailTheme.colors.onSurface.copy(alpha = 0.3f),
                shape = shape,
            )
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides KrailTheme.colors.onSurface.copy(alpha = 0.5f),
            LocalTextStyle provides KrailTheme.typography.labelLarge,
        ) {
            Text(text = "+ Add")
        }
    }
}

// region Previews

@Preview(name = "1. Light — set labels")
@Composable
private fun PreviewStopLabelPillRow_Set() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        StopLabelPillRow(
            stopLabels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home", stopId = "2000001", stopName = "Central Station"),
                StopLabel(emoji = "💼", label = "Work", stopId = "2000002", stopName = "Town Hall"),
            ),
            onLabelClick = {},
            onUnsetLabelClick = {},
            onAddLabelClick = {},
            modifier = Modifier.padding(KrailTheme.dimensions.spacingXL),
        )
    }
}

@Preview(name = "2. Light — unset labels")
@Composable
private fun PreviewStopLabelPillRow_Unset() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        StopLabelPillRow(
            stopLabels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home"),
                StopLabel(emoji = "💼", label = "Work"),
            ),
            onLabelClick = {},
            onUnsetLabelClick = {},
            onAddLabelClick = {},
            modifier = Modifier.padding(KrailTheme.dimensions.spacingXL),
        )
    }
}

@Preview(name = "3. Light — mixed (Home set, Work unset)")
@Composable
private fun PreviewStopLabelPillRow_Mixed() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        StopLabelPillRow(
            stopLabels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home", stopId = "2000001", stopName = "Central Station"),
                StopLabel(emoji = "💼", label = "Work"),
                StopLabel(emoji = "🏋", label = "Gym", stopId = "2000003", stopName = "Town Hall Station"),
            ),
            onLabelClick = {},
            onUnsetLabelClick = {},
            onAddLabelClick = {},
            modifier = Modifier.padding(KrailTheme.dimensions.spacingXL),
        )
    }
}

// endregion
