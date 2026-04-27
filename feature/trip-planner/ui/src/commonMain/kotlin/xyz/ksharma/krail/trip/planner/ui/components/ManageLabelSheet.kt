package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.Res
import app.krail.taj.resources.ic_location
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
 * Sheet shown when the user long-presses a label pill. Lets them clear the current
 * stop or delete the label entirely.
 */
@Composable
fun ManageLabelSheet(
    label: StopLabel,
    onClearStop: () -> Unit,
    onDeleteLabel: () -> Unit,
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
            // Header: themed pill showing current label state
            ManageHeaderPill(label = label, modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))

            if (label.isSet) {
                Spacer(modifier = Modifier.height(dim.spacingXS))
                Text(
                    text = label.stopName.orEmpty(),
                    style = KrailTheme.typography.bodyMedium,
                    color = KrailTheme.colors.softLabel,
                    modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                )
            }

            Spacer(modifier = Modifier.height(dim.spacingL))

            if (label.isSet) {
                ActionRow(
                    text = "Clear stop",
                    onClick = {
                        onClearStop()
                        onDismiss()
                    },
                )
            }

            ActionRow(
                text = "Delete label",
                onClick = {
                    onDeleteLabel()
                    onDismiss()
                },
                emphasised = true,
            )
        }
    }
}

@Composable
private fun ManageHeaderPill(
    label: StopLabel,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val themeColor = themeColor()
    val icon = stopLabelIcon(label.label) ?: Res.drawable.ic_location
    Row(
        modifier = modifier
            .clip(shape)
            .background(themeColor, shape)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label.label,
            style = KrailTheme.typography.titleMedium,
            color = KrailTheme.colors.surface,
        )
    }
}

@Composable
private fun ActionRow(
    text: String,
    onClick: () -> Unit,
    emphasised: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .klickable(onClick = onClick)
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = KrailTheme.typography.titleMedium,
            color = if (emphasised) KrailTheme.colors.error else KrailTheme.colors.onSurface,
        )
    }
}

// region Previews

@Preview(name = "1. Manage set label")
@Composable
private fun PreviewManageLabelSheet_Set() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        ManageLabelSheet(
            label = StopLabel(
                emoji = "🏠",
                label = "Home",
                stopId = "2000001",
                stopName = "Central Station",
            ),
            onClearStop = {},
            onDeleteLabel = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "2. Manage unset label")
@Composable
private fun PreviewManageLabelSheet_Unset() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        ManageLabelSheet(
            label = StopLabel(emoji = "🏋", label = "Gym"),
            onClearStop = {},
            onDeleteLabel = {},
            onDismiss = {},
        )
    }
}

// endregion
