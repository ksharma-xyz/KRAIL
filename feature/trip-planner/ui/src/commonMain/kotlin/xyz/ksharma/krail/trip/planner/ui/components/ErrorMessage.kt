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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

@Composable
fun ErrorMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    emoji: String = "\uD83D\uDC36",
    actionData: ActionData? = null,
) {
    val themeColor by LocalThemeColor.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 32.dp),
    ) {
        Text(
            text = emoji,
            style = KrailTheme.typography.headlineLarge.copy(fontSize = 64.sp),
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Text(
            text = title,
            style = KrailTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Text(
            text = message,
            style = KrailTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = KrailTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
        )

        actionData?.let {
            TextButton(
                dimensions = ButtonDefaults.largeButtonSize(),
                onClick = actionData.onActionClick,
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 16.dp),
            ) {
                Text(
                    text = actionData.actionText,
                    color = themeColor.hexToComposeColor(),
                )
            }
        }
    }
}

data class ActionData(
    val actionText: String,
    val onActionClick: () -> Unit,
)

// region Preview
@Preview
@Composable
private fun PreviewErrorMessage() {
    val themeColor = remember { mutableStateOf(TransportMode.Ferry().colorCode) }
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
