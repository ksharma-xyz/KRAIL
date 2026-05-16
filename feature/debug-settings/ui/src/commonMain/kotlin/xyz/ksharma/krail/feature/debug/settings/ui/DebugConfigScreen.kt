package xyz.ksharma.krail.feature.debug.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Top-level Debug Config screen, list of category tiles. Routed in from
 * Settings via the "Debug Config" row (which is gated on
 * `appInfo.isDebug`, so this screen never appears in release builds).
 *
 * Currently has just the Network tile. The list-of-tiles pattern is kept
 * so future debug categories (logs, analytics overrides, force-crash, etc.)
 * slot in cleanly without rewriting this screen.
 */
@Composable
fun DebugConfigScreen(
    isProEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNetworkClick: () -> Unit = {},
    onTogglePro: (Boolean) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { Text(text = "Debug Config") },
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = "header") {
                    DebugConfigHeader()
                }
                item(key = "tile-network") {
                    DebugConfigTile(
                        title = "Network",
                        subtitle = "Pick where BFF-eligible calls are routed.",
                        onClick = onNetworkClick,
                    )
                }
                item(key = "tile-pro-toggle") {
                    DebugProToggleRow(
                        isProEnabled = isProEnabled,
                        onToggle = onTogglePro,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugConfigHeader() {
    val dim = KrailTheme.dimensions
    Text(
        text = "Debug builds only. Settings persist across launches.",
        style = KrailTheme.typography.bodyLarge,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
    )
}

@Composable
internal fun DebugConfigTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .klickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = title, style = KrailTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = KrailTheme.typography.body,
                    color = KrailTheme.colors.softLabel,
                )
            }
        }
        Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
    }
}

@Composable
private fun DebugProToggleRow(
    isProEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "KRAIL Pro", style = KrailTheme.typography.bodyLarge)
                Text(
                    text = "Force Pro active for UI testing. No real IAP.",
                    style = KrailTheme.typography.body,
                    color = KrailTheme.colors.softLabel,
                )
            }
            Spacer(modifier = Modifier.padding(start = dim.spacingM))
            Switch(
                checked = isProEnabled,
                onCheckedChange = onToggle,
            )
        }
        Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
    }
}

@PreviewScreen
@Composable
private fun DebugConfigScreenPreview() {
    PreviewTheme {
        DebugConfigScreen()
    }
}
