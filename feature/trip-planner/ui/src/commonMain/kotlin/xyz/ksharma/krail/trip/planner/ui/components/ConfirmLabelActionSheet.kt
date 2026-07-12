package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Restyled replacement for `LabelConflictSheet`. Only Delete uses this now — Replace
 * and row-level Remove were dropped from the stop-search row (assigning a label to a
 * stop is a one-way, no-confirm action there; changing/removing only happens in
 * Manage, where Remove-assignment fires instantly and only Delete needs a confirm).
 * Kept generic (title/message/primary) rather than hardcoded to Delete's copy in case
 * Manage ever needs a second confirm — see `STOP_LABEL_UX_REDESIGN_PROPOSAL.md`.
 *
 * Content is split out into [ConfirmLabelActionSheetContent] because
 * `ModalBottomSheet` renders via a real `Dialog`/`Popup`, which the IDE's static
 * preview surface can't show — previews below call the content composable directly.
 */
@Composable
fun ConfirmLabelActionSheet(
    title: String,
    primaryText: String,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    primaryIsDestructive: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        ConfirmLabelActionSheetContent(
            title = title,
            message = message,
            primaryText = primaryText,
            primaryIsDestructive = primaryIsDestructive,
            onPrimary = onPrimary,
            onCancel = onCancel,
        )
    }
}

@Composable
internal fun ConfirmLabelActionSheetContent(
    title: String,
    primaryText: String,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    primaryIsDestructive: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // 2x+ font scale can push content taller than the sheet's available
            // height — scroll instead of clipping.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
    ) {
        Text(
            text = title,
            style = KrailTheme.typography.headlineMedium,
            color = KrailTheme.colors.onSurface,
        )

        if (message != null) {
            Spacer(modifier = Modifier.height(dim.spacingM))
            Text(
                text = message,
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
            )
        }

        Spacer(modifier = Modifier.height(dim.spacingXXXXL))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SubtleButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Cancel")
            }

            Button(
                onClick = onPrimary,
                modifier = Modifier.weight(1f),
                colors = if (primaryIsDestructive) {
                    ButtonDefaults.buttonColors(
                        customContainerColor = KrailTheme.colors.error,
                        customContentColor = KrailTheme.colors.surface,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(text = primaryText)
            }
        }
    }
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewConfirmLabelActionSheetContent_DeleteLabel() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        ConfirmLabelActionSheetContent(
            title = "Delete \"Gym\" label?",
            message = "This action will permanently remove the label.",
            primaryText = "Delete",
            primaryIsDestructive = true,
            onPrimary = {},
            onCancel = {},
        )
    }
}

// endregion
