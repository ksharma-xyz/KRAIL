package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverChipDefaults.RowContentPadding

@Composable
fun DiscoverChipRow(
    chipTypes: ImmutableList<DiscoverCardType>,
    selectedType: DiscoverCardType,
    onChipSelected: (DiscoverCardType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RowContentPadding)
    ) {
        items(
            items = chipTypes,
            key = { type ->
                type.displayName
            },
        ) { type ->
            DiscoverChip(
                type = type,
                selected = type == selectedType,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clickable(
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
            ).toImmutableList(),
            selectedType = DiscoverCardType.Travel,
            onChipSelected = { }
        )
    }
}

// endregion Previews
