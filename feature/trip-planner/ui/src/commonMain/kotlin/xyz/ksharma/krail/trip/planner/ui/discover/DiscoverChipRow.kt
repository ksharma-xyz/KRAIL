package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun DiscoverChipRow(
    chipTypes: Set<DiscoverCardType>,
    selectedType: DiscoverCardType,
    onChipSelected: (DiscoverCardType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(
            items = chipTypes.toList(),
            key = { type ->
                type.displayName
            },
        ) { type ->
            DiscoverChip(
                type = type,
                selected = type == selectedType,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    onChipSelected(type)
                }
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun DiscoverChipRowPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        DiscoverChipRow(
            chipTypes = setOf(
                DiscoverCardType.Travel,
                DiscoverCardType.Events,
                DiscoverCardType.Food,
                DiscoverCardType.Sports,
            ),
            selectedType = DiscoverCardType.Travel,
            onChipSelected = { }
        )
    }
}

// endregion Previews
