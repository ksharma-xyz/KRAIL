package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * Sheet shown when the user taps the "save" button on a stop result row.
 * Lets the user pick which label to assign the stop to, or create a new label.
 */
@Composable
fun SaveStopAsLabelSheet(
    stopName: String,
    labels: ImmutableList<StopLabel>,
    onLabelClick: (StopLabel) -> Unit,
    onCreateNewLabel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = dim.spacingXXL),
        ) {
            Text(
                text = "Save as a label",
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(dim.spacingXS))

            Text(
                text = stopName,
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(dim.spacingL))

            LazyRow(
                contentPadding = PaddingValues(horizontal = dim.pageHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(items = labels, key = { it.label }) { label ->
                    LabelChoiceChip(label = label, onClick = { onLabelClick(label) })
                }
                item(key = "__new_label__") {
                    NewLabelChip(onClick = onCreateNewLabel)
                }
            }
        }
    }
}

@Composable
private fun LabelChoiceChip(
    label: StopLabel,
    onClick: () -> Unit,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val themeColor = themeColor()
    val icon = stopLabelIcon(label.label) ?: Res.drawable.ic_location_on
    // Solid chip when the label already has a stop; outlined when it doesn't, since
    // tapping an outlined chip is what attaches this stop to that empty label.
    val isSet = label.isSet
    val contentColor = if (isSet) KrailTheme.colors.surface else themeColor

    Row(
        modifier = Modifier
            .clip(shape)
            .then(
                if (isSet) {
                    Modifier.background(themeColor, shape)
                } else {
                    Modifier.border(width = dim.strokeThin, color = themeColor, shape = shape)
                },
            )
            .klickable(onClick = onClick)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = Modifier.size(LABEL_CHIP_ICON_SIZE),
        )
        Text(
            text = label.label,
            style = KrailTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

@Composable
private fun NewLabelChip(onClick: () -> Unit) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    Row(
        modifier = Modifier
            .clip(shape)
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
            text = "+ New label",
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.softLabel,
        )
    }
}

// region Previews

@Preview(name = "1. Save sheet")
@Composable
private fun PreviewSaveStopAsLabelSheet() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SaveStopAsLabelSheet(
            stopName = "Central Station",
            labels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home"),
                StopLabel(emoji = "💼", label = "Work", stopId = "1", stopName = "Town Hall"),
                StopLabel(emoji = "🏋", label = "Gym"),
            ),
            onLabelClick = {},
            onCreateNewLabel = {},
            onDismiss = {},
        )
    }
}

// endregion

private val LABEL_CHIP_ICON_SIZE = 14.dp
