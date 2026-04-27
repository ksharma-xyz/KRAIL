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
 * Warning sheet shown when the user tries to save a stop as a label, but that stop is
 * already attached to a different label. Confirming moves the stop; cancelling does
 * nothing.
 */
@Composable
fun LabelConflictSheet(
    stopName: String,
    currentLabel: String,
    targetLabel: String,
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
                text = "Already saved",
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
            )

            Spacer(modifier = Modifier.height(dim.spacingS))

            Text(
                text = "$stopName is currently saved as $currentLabel. " +
                    "Move it to $targetLabel?",
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
                    Text(text = "Move", color = KrailTheme.colors.error)
                }
            }
        }
    }
}

// region Previews

@Preview(name = "Label conflict warning")
@Composable
private fun PreviewLabelConflictSheet() {
    PreviewTheme {
        LabelConflictSheet(
            stopName = "Central Station",
            currentLabel = "Library",
            targetLabel = "Hospital",
            onConfirm = {},
            onCancel = {},
        )
    }
}

// endregion
