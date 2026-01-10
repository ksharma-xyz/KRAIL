package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.DividerType
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor

@Composable
fun StopSelectionRadioGroup(
    selectionType: StopSelectionType,
    onTypeSelected: (StopSelectionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val listBackgroundColor = if (selectionType == StopSelectionType.LIST) {
            getForegroundColor(themeColor())
        } else Color.Transparent
        val mapBackgroundColor = if (selectionType == StopSelectionType.MAP) {
            getForegroundColor(themeColor())
        } else Color.Transparent

        Text(
            text = "List",
            style = KrailTheme.typography.bodyLarge,
            color = if (selectionType == StopSelectionType.LIST) {
                getForegroundColor(listBackgroundColor)
            } else {
                getForegroundColor(themeColor())
            },
            modifier = Modifier
                .background(
                    color = listBackgroundColor,
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                )
                .then(
                    if (selectionType != StopSelectionType.LIST) {
                        Modifier.border(
                            width = 1.dp,
                            color = mapBackgroundColor,
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                        )
                    } else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .klickable { onTypeSelected(StopSelectionType.LIST) },
        )

        Divider(
            type = DividerType.VERTICAL,
            modifier = Modifier.height(32.dp),
        )

        Text(
            text = "Map",
            style = KrailTheme.typography.bodyLarge,
            color = if (selectionType == StopSelectionType.MAP) {
                getForegroundColor(mapBackgroundColor)
            } else {
                getForegroundColor(themeColor())
            },
            modifier = Modifier
                .background(
                    color = mapBackgroundColor,
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                )
                .then(
                    if (selectionType != StopSelectionType.MAP) {
                        Modifier.border(
                            width = 1.dp,
                            color = listBackgroundColor,
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                        )
                    } else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .klickable { onTypeSelected(StopSelectionType.MAP) },
        )
    }
}

enum class StopSelectionType {
    LIST,
    MAP
}

// region Previews

@Preview
@Composable
private fun StopSelectionRadioGroupPreview() {
    PreviewTheme {
        StopSelectionRadioGroup(
            selectionType = StopSelectionType.LIST,
            onTypeSelected = { _ -> },
        )
    }
}

// endregion