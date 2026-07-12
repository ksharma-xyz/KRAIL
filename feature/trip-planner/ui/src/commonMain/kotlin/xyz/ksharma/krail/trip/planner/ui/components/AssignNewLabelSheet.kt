package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Only ever opened from the "+ New label" chip inside the expanded
 * `StopLabelAssignRow` — the stop is always known, so there's no "no stop yet" branch,
 * no suggestion chips. Name → Save and assign → the label is created AND this stop
 * assigned to it in one step.
 *
 * Title and stop name both use titleLarge/onSurface, its mode roundel(s) underneath,
 * a divider, then the "Label name" field (its own titleMedium/onSurface caption,
 * matching the weight of Manage's "Rename" caption) — the one field this flow needs.
 * Duplicate names are blocked with `ConfirmLabelActionSheet` (see [existingLabelNames]),
 * which can appear stacked on top of this one.
 *
 * Content is split out into [AssignNewLabelSheetContent] because `ModalBottomSheet`
 * renders via a real `Dialog`/`Popup`, which the IDE's static preview surface can't
 * show — previews below call the content composable directly instead.
 *
 * [existingLabelNames] blocks Save on a duplicate (case-insensitive, same
 * `normaliseLabelName` the ViewModel dedupes with) with a [ConfirmLabelActionSheet]
 * instead of silently no-oping — the ViewModel's own dedupe check is a silent no-op
 * by design, so without this the sheet would close claiming success while nothing
 * was actually created.
 */
@Composable
fun AssignNewLabelSheet(
    stopName: String,
    transportModeSet: List<TransportMode>,
    existingLabelNames: ImmutableList<String>,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        AssignNewLabelSheetContent(
            stopName = stopName,
            transportModeSet = transportModeSet,
            existingLabelNames = existingLabelNames,
            onSave = onSave,
        )
    }
}

@Composable
internal fun AssignNewLabelSheetContent(
    stopName: String,
    transportModeSet: List<TransportMode>,
    onSave: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    existingLabelNames: ImmutableList<String> = persistentListOf(),
) {
    val dim = KrailTheme.dimensions
    val textFieldState = rememberTextFieldState("")
    val cleanedName by remember {
        derivedStateOf { normaliseLabelName(textFieldState.text.toString()) }
    }
    var duplicateName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            // 2x+ font scale can push content taller than the sheet's available
            // height — scroll instead of clipping.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingL),
    ) {
        Text(
            text = "Assign a label to this stop",
            style = KrailTheme.typography.headlineMedium,
            color = KrailTheme.colors.onSurface,
        )

        Spacer(modifier = Modifier.height(dim.spacingXXL))

        Text(
            text = stopName,
            style = KrailTheme.typography.titleLarge,
            color = KrailTheme.colors.onSurface,
        )

        Spacer(modifier = Modifier.height(dim.spacingS))

        Row(horizontalArrangement = Arrangement.spacedBy(dim.spacingM)) {
            transportModeSet.forEach { mode ->
                TransportModeIcon(transportMode = mode, size = TransportModeIconSize.Small)
            }
        }

        Spacer(modifier = Modifier.height(dim.spacingXXL))
        Divider()
        Spacer(modifier = Modifier.height(dim.spacingL))

        Text(
            text = "Label name",
            style = KrailTheme.typography.titleMedium,
            color = KrailTheme.colors.onSurface,
        )

        Spacer(modifier = Modifier.height(dim.spacingS))

        TextField(
            placeholder = "e.g. Home, Gym, School…",
            state = textFieldState,
            // Blocks emoji/punctuation at keystroke time rather than silently stripping
            // them later — keeps what the user sees in sync with the normalised name
            // that CreateLabel actually stores under.
            filter = { input ->
                input.filter { c ->
                    c.isLetterOrDigit() || c.isWhitespace() || c == '-' || c == '_' || c == '\''
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(dim.spacingL))

        Button(
            onClick = {
                if (existingLabelNames.any { normaliseLabelName(it).equals(cleanedName, ignoreCase = true) }) {
                    duplicateName = cleanedName
                } else {
                    onSave(cleanedName)
                }
            },
            enabled = cleanedName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Save and assign")
        }
    }

    val blockedName = duplicateName
    if (blockedName != null) {
        ConfirmLabelActionSheet(
            title = "\"$blockedName\" already exists",
            message = "Choose a different name for this label.",
            primaryText = "OK",
            onPrimary = { duplicateName = null },
            onCancel = { duplicateName = null },
        )
    }
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewAssignNewLabelSheetContent_Empty() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        AssignNewLabelSheetContent(
            stopName = "Bondi Beach",
            transportModeSet = listOf(TransportMode.Bus),
            onSave = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewAssignNewLabelSheetContent_MultiMode() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AssignNewLabelSheetContent(
            stopName = "Central Station",
            transportModeSet = listOf(TransportMode.Train, TransportMode.LightRail),
            onSave = {},
        )
    }
}

// endregion
