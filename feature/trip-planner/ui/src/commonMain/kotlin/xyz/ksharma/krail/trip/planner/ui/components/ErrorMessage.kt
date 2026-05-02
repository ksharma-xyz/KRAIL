package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.core.snapshot.ScreenshotTest

@Composable
fun ErrorMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    emoji: String = "🐶",
    actionData: ActionData? = null,
    filledButton: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    val themeColor by LocalThemeColor.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = dim.spacingXXXXL),
    ) {
        Text(
            text = emoji,
            style = KrailTheme.typography.headlineLarge.copy(fontSize = 64.sp),
            modifier = Modifier.padding(bottom = dim.spacingXXXL),
        )
        Text(
            text = title,
            style = KrailTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dim.pageHorizontalPadding),
        )
        Text(
            text = message,
            style = KrailTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dim.spacingL)
                .padding(horizontal = dim.pageHorizontalPadding),
        )

        actionData?.let {
            if (filledButton) {
                Button(
                    onClick = actionData.onActionClick,
                    modifier = Modifier.padding(
                        vertical = dim.spacingXL,
                        horizontal = dim.pageHorizontalPadding,
                    ),
                ) {
                    Text(text = actionData.actionText)
                }
            } else {
                TextButton(
                    dimensions = ButtonDefaults.largeButtonSize(),
                    onClick = actionData.onActionClick,
                    modifier = Modifier.padding(
                        vertical = dim.spacingXL,
                        horizontal = dim.pageHorizontalPadding,
                    ),
                ) {
                    Text(
                        text = actionData.actionText,
                        color = themeColor.hexToComposeColor(),
                    )
                }
            }
        }
    }
}

data class ActionData(
    val actionText: String,
    val onActionClick: () -> Unit,
)

// region Preview
@ScreenshotTest
@Preview
@Composable
private fun PreviewErrorMessage() {
    val themeColor = remember { mutableStateOf(NswTransportMode.Ferry.colorCode) }
    CompositionLocalProvider(LocalThemeColor provides themeColor) {
        PreviewTheme {
            ErrorMessage(
                title = "Eh! That's not looking right mate.",
                message = "Let's try again.",
                actionData = ActionData(
                    actionText = "Retry",
                    onActionClick = {},
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KrailTheme.colors.surface),
            )
        }
    }
}

// endregion
