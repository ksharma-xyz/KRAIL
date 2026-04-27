package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor

private val suggestions = listOf(
    "🏠" to "Home",
    "💼" to "Work",
    "🏋" to "Gym",
    "🏫" to "School",
    "☕" to "Cafe",
    "❤️" to "Favourite",
)

@Composable
fun AddLabelBottomSheet(
    onDismiss: () -> Unit,
    onPickStop: (emoji: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    var name by rememberSaveable { mutableStateOf("") }
    var selectedSuggestionIndex by rememberSaveable { mutableIntStateOf(-1) }
    val emoji = if (selectedSuggestionIndex >= 0) suggestions[selectedSuggestionIndex].first else "📍"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(bottom = dim.spacingXXL),
        ) {
            Text(
                text = "Name this place",
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(dim.spacingM))

            Text(
                text = "Pick a suggestion or type your own name.",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(dim.spacingL))

            // Suggestion chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = dim.pageHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(suggestions, key = { _, pair -> pair.second }) { index, (chipEmoji, chipName) ->
                    val isSelected = selectedSuggestionIndex == index
                    val shape = RoundedCornerShape(dim.radiusFull)
                    Row(
                        modifier = Modifier
                            .clip(shape)
                            .then(
                                if (isSelected) {
                                    Modifier.background(themeColor(), shape)
                                } else {
                                    Modifier.border(
                                        dim.strokeThin,
                                        KrailTheme.colors.onSurface.copy(alpha = 0.2f),
                                        shape,
                                    )
                                },
                            )
                            .klickable {
                                selectedSuggestionIndex = index
                                name = chipName
                            }
                            .padding(
                                horizontal = dim.chipHorizontalPadding,
                                vertical = dim.chipVerticalPadding,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = chipEmoji,
                            style = KrailTheme.typography.labelLarge,
                            color = if (isSelected) KrailTheme.colors.surface else KrailTheme.colors.onSurface,
                        )
                        Text(
                            text = chipName,
                            style = KrailTheme.typography.labelLarge,
                            color = if (isSelected) KrailTheme.colors.surface else KrailTheme.colors.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dim.spacingL))

            // Name text field
            TextField(
                placeholder = "e.g. Home, Gym, School…",
                initialText = name,
                onTextChange = { text ->
                    name = text.toString()
                    if (text.isNotBlank()) {
                        val matchedIndex =
                            suggestions.indexOfFirst { it.second.equals(text.toString(), ignoreCase = true) }
                        selectedSuggestionIndex = matchedIndex
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dim.pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(dim.spacingXL))

            Button(
                onClick = { onPickStop(emoji, name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dim.pageHorizontalPadding),
            ) {
                Text(text = "Pick a stop")
            }
        }
    }
}

// region Previews

@Preview(name = "1. Empty state")
@Composable
private fun PreviewAddLabelBottomSheet_Empty() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AddLabelBottomSheet(
            onDismiss = {},
            onPickStop = { _, _ -> },
        )
    }
}

@Preview(name = "2. Metro — suggestion selected")
@Composable
private fun PreviewAddLabelBottomSheet_Selected() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        AddLabelBottomSheet(
            onDismiss = {},
            onPickStop = { _, _ -> },
        )
    }
}

// endregion
