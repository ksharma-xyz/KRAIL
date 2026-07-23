package xyz.ksharma.krail.feature.debug.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    modifier: Modifier = Modifier,
    tripTrackingEnabled: Boolean = true,
    onTripTrackingToggle: (Boolean) -> Unit = {},
    addressSearchEnabled: Boolean = false,
    onAddressSearchToggle: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNetworkClick: () -> Unit = {},
    onResetReviewClick: () -> Unit = {},
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
                item(key = "tile-trip-tracking") {
                    DebugConfigToggleTile(
                        title = "Trip Tracking",
                        subtitle = "Override TRIP_TRACKING_ENABLED RC flag.",
                        checked = tripTrackingEnabled,
                        onCheckedChange = onTripTrackingToggle,
                    )
                }
                item(key = "tile-address-search") {
                    DebugConfigToggleTile(
                        title = "Address Search",
                        subtitle = "Override SEARCH_STOP_ADDRESS_SEARCH_ENABLED RC flag.",
                        checked = addressSearchEnabled,
                        onCheckedChange = onAddressSearchToggle,
                    )
                }
                item(key = "tile-reset-review") {
                    DebugConfigTile(
                        title = "Reset in-app review",
                        subtitle = "Clear the 'already asked' count so it can fire again. Keeps the install date.",
                        onClick = onResetReviewClick,
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
internal fun DebugConfigToggleTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .klickable(onClick = { onCheckedChange(!checked) })
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = KrailTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = KrailTheme.typography.body,
                    color = KrailTheme.colors.softLabel,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
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
