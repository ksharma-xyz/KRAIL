package xyz.ksharma.krail.core.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.network.EnvironmentType
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor

/**
 * Floating debug button that opens the debug configuration screen.
 * This should only be visible in debug builds.
 *
 * Usage: Add this to your screen composable in debug builds.
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     YourScreenContent()
 *     DebugFloatingButton()
 * }
 * ```
 */
@Composable
fun DebugFloatingButton(
    modifier: Modifier = Modifier,
) {
    var showDebugScreen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .systemBarsPadding()
                .size(56.dp)
                .clip(CircleShape)
                .background(themeColor())
                .klickable(onClick = { showDebugScreen = true }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛠️",
                style = KrailTheme.typography.titleLarge,
                color = getForegroundColor(themeColor()),
            )
        }
    }

    if (showDebugScreen) {
        DebugConfigDialog(
            onDismiss = { showDebugScreen = false }
        )
    }
}

/**
 * Debug configuration dialog.
 */
@Composable
fun DebugConfigDialog(
    onDismiss: () -> Unit,
) {
    val viewModel: DebugConfigViewModel = koinViewModel()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KrailTheme.colors.surface)
        ) {
            DebugConfigScreen(
                viewModel = viewModel,
                onNavigateBack = onDismiss
            )
        }
    }
}
