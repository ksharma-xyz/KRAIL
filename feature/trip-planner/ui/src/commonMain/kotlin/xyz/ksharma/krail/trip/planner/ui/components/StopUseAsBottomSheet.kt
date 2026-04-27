package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

@Composable
fun StopUseAsBottomSheet(
    stopItem: StopItem,
    onDismiss: () -> Unit,
    onUseAsFrom: (StopItem) -> Unit,
    onUseAsTo: (StopItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = dim.pageHorizontalPadding)
                .padding(bottom = dim.spacingXXL),
        ) {
            Text(
                text = stopItem.stopName,
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
            )

            Text(
                text = "Use this stop as…",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = dim.spacingXS),
            )

            Spacer(modifier = Modifier.height(dim.spacingXL))

            Button(
                onClick = { onUseAsTo(stopItem) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Going here")
            }

            Spacer(modifier = Modifier.height(dim.spacingM))

            TextButton(
                onClick = { onUseAsFrom(stopItem) },
                dimensions = ButtonDefaults.largeButtonSize(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Starting from here",
                    color = KrailTheme.colors.onSurface,
                )
            }
        }
    }
}

// region Previews

@Preview(name = "1. Train theme")
@Composable
private fun PreviewStopUseAsBottomSheet_Train() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        StopUseAsBottomSheet(
            stopItem = StopItem(stopId = "2000001", stopName = "Central Station"),
            onDismiss = {},
            onUseAsFrom = {},
            onUseAsTo = {},
        )
    }
}

@Preview(name = "2. Bus theme — long name")
@Composable
private fun PreviewStopUseAsBottomSheet_LongName() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        StopUseAsBottomSheet(
            stopItem = StopItem(
                stopId = "2000099",
                stopName = "Parramatta Station, Smith Street Platform 1",
            ),
            onDismiss = {},
            onUseAsFrom = {},
            onUseAsTo = {},
        )
    }
}

// endregion
