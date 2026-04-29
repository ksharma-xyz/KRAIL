package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.ic_info
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
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import krail.feature.trip_planner.ui.generated.resources.Res as TripPlannerRes

@Suppress("LongMethod")
// The sheet is one continuous flow (title → optional stop chip → preview pill →
// suggestion chips → text field → save) so users see the whole "give me a name"
// surface at once. Each section is small but they share the parent Column's
// vertical rhythm; extracting per-section composables would push the spacing
// owner state up here and add boilerplate without payoff.
@Composable
fun AddLabelBottomSheet(
    stopName: String?,
    existingLabels: ImmutableList<StopLabel>,
    onDismiss: () -> Unit,
    onSave: (emoji: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    var name by rememberSaveable { mutableStateOf("") }
    var selectedSuggestionIndex by rememberSaveable { mutableIntStateOf(-1) }

    val availableSuggestions = remember(existingLabels) {
        stopLabelSuggestions.filter { (suggestionName, _) ->
            existingLabels.none { labelNamesMatch(it.label, suggestionName) }
        }
    }
    val cleanedName = remember(name) { normaliseLabelName(name) }
    val isDuplicate = remember(cleanedName, existingLabels) {
        cleanedName.isNotBlank() &&
            existingLabels.any { labelNamesMatch(it.label, cleanedName) }
    }
    val canSave = cleanedName.isNotBlank() && !isDuplicate
    var showInfoText by rememberSaveable { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(dim.spacingL),
        ) {
            // ─── Title (with info-toggle when no stop is attached) ───────────
            if (stopName != null) {
                Text(
                    text = "Give a nickname to",
                    style = KrailTheme.typography.headlineMedium,
                    color = KrailTheme.colors.onSurface,
                    modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                )
                StopChip(
                    stopName = stopName,
                    modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                )
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dim.spacingS),
                ) {
                    Text(
                        text = "Add a new label",
                        style = KrailTheme.typography.headlineMedium,
                        color = KrailTheme.colors.onSurface,
                    )
                    Image(
                        painter = painterResource(TripPlannerRes.drawable.ic_info),
                        contentDescription = if (showInfoText) {
                            "Hide label help"
                        } else {
                            "Show label help"
                        },
                        colorFilter = ColorFilter.tint(KrailTheme.colors.label),
                        modifier = Modifier
                            .size(dim.spacingXXL)
                            .clip(CircleShape)
                            .klickable { showInfoText = !showInfoText }
                            .padding(dim.spacingXS),
                    )
                }

                // animateContentSize keeps the height collapse smooth when the
                // user toggles the help text on/off.
                Column(modifier = Modifier.animateContentSize()) {
                    if (showInfoText) {
                        Spacer(modifier = Modifier.height(dim.spacingS))
                        Text(
                            text = "Labels are quick shortcuts to your favourite stops " +
                                "like Home, Work, or Gym, anywhere you ride to often." +
                                "\n\nPick a name now, then tap the ⭐ next to a stop " +
                                "in your search results to attach it.",
                            style = KrailTheme.typography.bodySmall,
                            color = KrailTheme.colors.label,
                            modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                        )
                    }
                }
            }

            // ─── Preview ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(dim.spacingS),
            ) {
                SectionHeading(text = "Preview")
                LabelPreviewPill(name = cleanedName.ifBlank { "Your label" })
            }

            // ─── Suggestions ──────────────────────────────────────────────────
            if (availableSuggestions.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dim.spacingS),
                ) {
                    SectionHeading(
                        text = "Suggestions",
                        modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = dim.pageHorizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        itemsIndexed(
                            availableSuggestions,
                            key = { _, pair -> pair.first },
                        ) { index, (chipName, icon) ->
                            val isSelected = selectedSuggestionIndex == index
                            val shape = RoundedCornerShape(dim.radiusFull)
                            val contentColor = if (isSelected) {
                                KrailTheme.colors.surface
                            } else {
                                KrailTheme.colors.onSurface
                            }
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
                                Image(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(contentColor),
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = chipName,
                                    style = KrailTheme.typography.labelLarge,
                                    color = contentColor,
                                )
                            }
                        }
                    }
                }
            }

            // ─── Name field ───────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
            ) {
                SectionHeading(text = "Name")
                key(selectedSuggestionIndex) {
                    TextField(
                        placeholder = "e.g. Home, Gym, School…",
                        initialText = name,
                        onTextChange = { text ->
                            name = text.toString()
                            val cleaned = normaliseLabelName(name)
                            selectedSuggestionIndex = if (cleaned.isNotBlank()) {
                                availableSuggestions.indexOfFirst {
                                    labelNamesMatch(it.first, cleaned)
                                }
                            } else {
                                -1
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (isDuplicate) {
                    Text(
                        text = "\"$cleanedName\" is already a label.",
                        style = KrailTheme.typography.bodySmall,
                        color = KrailTheme.colors.error,
                    )
                }
            }

            // ─── Save ─────────────────────────────────────────────────────────
            Button(
                onClick = { onSave("📍", cleanedName) },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dim.pageHorizontalPadding),
            ) {
                Text(text = "Save")
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = KrailTheme.typography.titleSmall,
        color = KrailTheme.colors.softLabel,
        modifier = modifier,
    )
}

@Composable
private fun StopChip(stopName: String, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    Row(
        modifier = modifier
            .clip(shape)
            .background(themeColor(), shape)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
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
}

@Composable
private fun LabelPreviewPill(name: String) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    val icon = stopLabelIcon(name) ?: Res.drawable.ic_location
    Row(
        modifier = Modifier
            .clip(shape)
            .background(themeColor(), shape)
            .padding(horizontal = dim.chipHorizontalPadding, vertical = dim.chipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = name,
            style = KrailTheme.typography.labelLarge,
            color = KrailTheme.colors.surface,
        )
    }
}

// region Previews

@Preview(name = "1. Empty")
@Composable
private fun PreviewAddLabelBottomSheet_Empty() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AddLabelBottomSheet(
            stopName = null,
            existingLabels = persistentListOf(),
            onDismiss = {},
            onSave = { _, _ -> },
        )
    }
}

@Preview(name = "2. With existing labels filtered")
@Composable
private fun PreviewAddLabelBottomSheet_FilteredSuggestions() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        AddLabelBottomSheet(
            stopName = null,
            existingLabels = persistentListOf(
                StopLabel(emoji = "🏠", label = "Home"),
                StopLabel(emoji = "💼", label = "Work"),
                StopLabel(emoji = "📍", label = "Gym"),
            ),
            onDismiss = {},
            onSave = { _, _ -> },
        )
    }
}

@Preview(name = "3. With stop context")
@Composable
private fun PreviewAddLabelBottomSheet_WithStop() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        AddLabelBottomSheet(
            stopName = "Central Station",
            existingLabels = persistentListOf(),
            onDismiss = {},
            onSave = { _, _ -> },
        )
    }
}

// endregion
