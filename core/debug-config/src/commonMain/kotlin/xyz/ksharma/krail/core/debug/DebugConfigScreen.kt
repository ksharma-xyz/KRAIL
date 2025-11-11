package xyz.ksharma.krail.core.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.ksharma.krail.core.network.EnvironmentType
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor

/**
 * Debug Configuration Screen.
 * This screen should only be accessible in debug builds.
 */
@Composable
fun DebugConfigScreen(
    viewModel: DebugConfigViewModel,
    onNavigateBack: () -> Unit,
) {
    val currentEnvironment by viewModel.currentEnvironment.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter),
        ) {
            // Title Bar
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onNavigateBack,
                title = {
                    Text(
                        text = "🛠️ Debug Config",
                        style = KrailTheme.typography.title,
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .klickable(onClick = { viewModel.resetToDefaults() })
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Reset",
                            style = KrailTheme.typography.body,
                            color = KrailTheme.colors.error,
                        )
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = KrailTheme.colors.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "⚠️ DEBUG MODE ONLY\nThese settings are not available in production builds.",
                            style = KrailTheme.typography.body,
                            color = KrailTheme.colors.error,
                        )
                    }
                }

                // Environment Selection
                item {
                    EnvironmentSelectionSection(
                        currentEnvironment = currentEnvironment,
                        onEnvironmentSelected = { viewModel.setEnvironment(it) }
                    )
                }

                // Information Section
                item {
                    InfoSection(currentEnvironment = currentEnvironment)
                }

                // Dangerous Actions Section
                item {
                    DangerousActionsSection(
                        onClearAllData = { showClearDataDialog = true }
                    )
                }
            }
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        ClearDataDialog(
            onConfirm = {
                viewModel.clearAllAppData()
                showClearDataDialog = false
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
}

@Composable
private fun EnvironmentSelectionSection(
    currentEnvironment: EnvironmentType,
    onEnvironmentSelected: (EnvironmentType) -> Unit,
) {
    SectionCard(title = "🌐 API Environment") {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnvironmentType.entries.forEach { env ->
                EnvironmentOption(
                    environment = env,
                    isSelected = currentEnvironment == env,
                    onClick = { onEnvironmentSelected(env) }
                )
            }
        }
    }
}

@Composable
private fun EnvironmentOption(
    environment: EnvironmentType,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isSelected)
                    themeBackgroundColor()
                else
                    KrailTheme.colors.surface.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    themeColor()
                else
                    KrailTheme.colors.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .klickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = environment.displayName,
                    style = KrailTheme.typography.title,
                    color = if (isSelected)
                        themeColor()
                    else
                        KrailTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (environment) {
                        EnvironmentType.PRODUCTION -> "Uses city-specific production API"
                        EnvironmentType.LOCAL_BFF -> "http://10.0.2.2:8080"
                    },
                    style = KrailTheme.typography.body,
                    color = KrailTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Text(
                    text = "✓",
                    style = KrailTheme.typography.headlineMedium,
                    color = themeColor()
                )
            }
        }
    }
}

@Composable
private fun DangerousActionsSection(
    onClearAllData: () -> Unit,
) {
    SectionCard(title = "⚠️ Dangerous Actions") {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "These actions cannot be undone!",
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.error
            )

            Button(
                onClick = onClearAllData,
                modifier = Modifier.fillMaxWidth(),
                colors = xyz.ksharma.krail.taj.components.ButtonDefaults.alertButtonColors(),
            ) {
                Text("🗑️ Clear All App Data")
            }

            Text(
                text = "This will delete all saved trips, preferences, and cached data. NSW stops will be preserved.",
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun InfoSection(currentEnvironment: EnvironmentType) {
    SectionCard(title = "ℹ️ Current Configuration") {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow(label = "Environment", value = currentEnvironment.displayName)
            InfoRow(label = "City", value = "Sydney") // TODO: Make dynamic when multi-city is added
            InfoRow(label = "Build Type", value = "DEBUG")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = KrailTheme.typography.body,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = KrailTheme.colors.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = KrailTheme.colors.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = KrailTheme.typography.titleLarge,
            )
            content()
        }
    }
}

@Composable
private fun ClearDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = KrailTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚠️ Clear All App Data?",
                    style = KrailTheme.typography.titleLarge,
                )

                Text(
                    text = "This will delete:\n" +
                           "• Saved trips\n" +
                           "• Theme preferences\n" +
                           "• Service alerts cache\n" +
                           "• All app preferences\n\n" +
                           "NSW stops data will be preserved.\n\n" +
                           "This action cannot be undone!",
                    style = KrailTheme.typography.body,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = xyz.ksharma.krail.taj.components.ButtonDefaults.subtleButtonColors(),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = xyz.ksharma.krail.taj.components.ButtonDefaults.alertButtonColors(),
                    ) {
                        Text("Clear All Data")
                    }
                }
            }
        }
    }
}

