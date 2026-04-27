package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Warning sheet for label assignment conflicts. The same component handles both
 * directions of the 1:1 invariant: a stop already saved against another label, or a
 * label already pointing at another stop.
 */
@Composable
fun LabelConflictSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = dim.pageHorizontalPadding)
                .padding(bottom = dim.spacingXXL),
        ) {
            Text(
                text = title,
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
            )

            Spacer(modifier = Modifier.height(dim.spacingS))

            Text(
                text = message,
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
            )

            Spacer(modifier = Modifier.height(dim.spacingXL))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingM, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text(text = "Cancel", color = KrailTheme.colors.onSurface)
                }
                TextButton(onClick = onConfirm) {
                    Text(text = confirmLabel, color = KrailTheme.colors.error)
                }
            }
        }
    }
}

// region Previews

@Preview(name = "Stop already saved")
@Composable
private fun PreviewLabelConflictSheet_StopSide() {
    PreviewTheme {
        LabelConflictSheet(
            title = "Already saved",
            message = "Central Station is currently saved as Library. Move it to Hospital?",
            confirmLabel = "Move",
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Label already in use")
@Composable
private fun PreviewLabelConflictSheet_LabelSide() {
    PreviewTheme {
        LabelConflictSheet(
            title = "Already in use",
            message = "Hospital is currently saved as Town Hall. Replace with Central Station?",
            confirmLabel = "Replace",
            onConfirm = {},
            onCancel = {},
        )
    }
}

// endregion
