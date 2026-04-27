package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.Res
import app.krail.taj.resources.ic_location
import org.jetbrains.compose.resources.painterResource
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
    "Home",
    "Work",
    "Gym",
    "School",
    "Cafe",
    "Favourite",
)

@Composable
fun AddLabelBottomSheet(
    stopName: String,
    onDismiss: () -> Unit,
    onSave: (emoji: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    var name by rememberSaveable { mutableStateOf("") }
    var selectedSuggestionIndex by rememberSaveable { mutableIntStateOf(-1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = dim.spacingXXL),
        ) {
            Text(
                text = "Give a nickname to",
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(dim.spacingXS))

            // Stop name in a themed chip completing the heading sentence
            val stopChipShape = RoundedCornerShape(dim.radiusFull)
            Row(
                modifier = Modifier
                    .padding(horizontal = dim.pageHorizontalPadding)
                    .clip(stopChipShape)
                    .background(themeColor(), stopChipShape)
                    .padding(
                        horizontal = dim.chipHorizontalPadding,
                        vertical = dim.chipVerticalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_location),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stopName,
                    style = KrailTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = KrailTheme.colors.surface,
                )
            }

            Spacer(modifier = Modifier.height(dim.spacingL))

            // Live label preview — animates as user types
            if (name.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = dim.pageHorizontalPadding)
                        .padding(bottom = dim.spacingM),
                    horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Preview:",
                        style = KrailTheme.typography.bodySmall,
                        color = KrailTheme.colors.softLabel,
                    )
                    AnimatedContent(
                        targetState = name,
                        transitionSpec = {
                            fadeIn() + slideInVertically { -it / 2 } togetherWith
                                fadeOut() + slideOutVertically { it / 2 }
                        },
                        label = "labelPreview",
                    ) { previewName ->
                        val previewShape = RoundedCornerShape(dim.radiusFull)
                        Row(
                            modifier = Modifier
                                .clip(previewShape)
                                .background(themeColor(), previewShape)
                                .padding(
                                    horizontal = dim.chipHorizontalPadding,
                                    vertical = dim.chipVerticalPadding,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_location),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = previewName,
                                style = KrailTheme.typography.labelLarge,
                                color = KrailTheme.colors.surface,
                            )
                        }
                    }
                }
            } else {
                // Reserve space so layout doesn't jump when preview appears
                Spacer(modifier = Modifier.height(dim.spacingXXL))
            }

            Text(
                text = "Pick a suggestion or type your own.",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(dim.spacingM))

            // Suggestion chips — selecting one fills the text field
            LazyRow(
                contentPadding = PaddingValues(horizontal = dim.pageHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(suggestions, key = { _, s -> s }) { index, chipName ->
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
                                        KrailTheme.colors.outlineSubtle,
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
                            text = chipName,
                            style = KrailTheme.typography.labelLarge,
                            color = if (isSelected) KrailTheme.colors.surface else KrailTheme.colors.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dim.spacingL))

            // key(selectedSuggestionIndex) forces TextField to recompose (and pick up
            // the chip's name as initialText) when a suggestion is tapped.
            key(selectedSuggestionIndex) {
                TextField(
                    placeholder = "e.g. Home, Gym, School…",
                    initialText = name,
                    onTextChange = { text ->
                        name = text.toString()
                        if (text.isNotBlank()) {
                            val matchedIndex =
                                suggestions.indexOfFirst { it.equals(text.toString(), ignoreCase = true) }
                            selectedSuggestionIndex = matchedIndex
                        } else {
                            selectedSuggestionIndex = -1
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dim.pageHorizontalPadding),
                )
            }

            Spacer(modifier = Modifier.height(dim.spacingXL))

            Button(
                onClick = { onSave("📍", name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dim.pageHorizontalPadding),
            ) {
                Text(text = "Save")
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
            stopName = "Central Station",
            onDismiss = {},
            onSave = { _, _ -> },
        )
    }
}

@Preview(name = "2. Metro — suggestion selected")
@Composable
private fun PreviewAddLabelBottomSheet_Selected() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        AddLabelBottomSheet(
            stopName = "Town Hall Station",
            onDismiss = {},
            onSave = { _, _ -> },
        )
    }
}

// endregion
