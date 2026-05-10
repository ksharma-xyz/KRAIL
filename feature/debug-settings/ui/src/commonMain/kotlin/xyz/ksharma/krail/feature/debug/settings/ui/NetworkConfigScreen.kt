package xyz.ksharma.krail.feature.debug.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.network.IS_BFF_PROD_DEPLOYED
import xyz.ksharma.krail.core.network.KRAIL_BFF_BASE_URL
import xyz.ksharma.krail.core.network.KRAIL_BFF_PROD_BASE_URL
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Picker for which BFF deployment to hit when the BFF path is active.
 * Selection persists immediately on tap; no save button.
 */
@Composable
fun NetworkConfigScreen(
    selected: NetworkTarget,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSelect: (NetworkTarget) -> Unit = {},
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
                title = { Text(text = "Network") },
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = "header") {
                    NetworkConfigHeader()
                }
                item(key = "row-bff-local") {
                    NetworkRadioRow(
                        title = "BFF Local",
                        subtitle = bffLocalSubtitle(),
                        selected = selected == NetworkTarget.BFF_LOCAL,
                        enabled = true,
                        onClick = { onSelect(NetworkTarget.BFF_LOCAL) },
                    )
                }
                item(key = "row-bff-prod") {
                    NetworkRadioRow(
                        title = "BFF Production",
                        subtitle = bffProdSubtitle(),
                        selected = selected == NetworkTarget.BFF_PROD,
                        enabled = IS_BFF_PROD_DEPLOYED,
                        onClick = { onSelect(NetworkTarget.BFF_PROD) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkConfigHeader() {
    val dim = KrailTheme.dimensions
    Text(
        text = "Active when BFF is on. Pick which BFF deployment to hit.",
        style = KrailTheme.typography.bodyLarge,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
    )
}

private fun bffLocalSubtitle(): String =
    if (KRAIL_BFF_BASE_URL.isNotBlank()) KRAIL_BFF_BASE_URL else "(not set in local.properties)"

private fun bffProdSubtitle(): String =
    if (KRAIL_BFF_PROD_BASE_URL.isNotBlank()) KRAIL_BFF_PROD_BASE_URL else "(not deployed)"

@Composable
internal fun NetworkRadioRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .klickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioIndicator(selected = selected, enabled = enabled)
            Column(modifier = Modifier.padding(start = dim.spacingXL)) {
                Text(
                    text = title,
                    style = KrailTheme.typography.bodyLarge,
                    color = if (enabled) null else KrailTheme.colors.softLabel,
                )
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
private fun RadioIndicator(selected: Boolean, enabled: Boolean) {
    val ring = if (enabled) KrailTheme.colors.onSurface else KrailTheme.colors.softLabel
    Box(
        modifier = Modifier
            .size(RADIO_OUTER_SIZE)
            .clip(CircleShape)
            .border(width = RADIO_BORDER_WIDTH, color = ring, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(RADIO_INNER_SIZE)
                    .clip(CircleShape)
                    .background(color = ring),
            )
        }
    }
}

private val RADIO_OUTER_SIZE = 22.dp
private val RADIO_INNER_SIZE = 12.dp
private val RADIO_BORDER_WIDTH = 2.dp

@PreviewScreen
@Composable
private fun NetworkConfigScreenPreview() {
    PreviewTheme {
        NetworkConfigScreen(selected = NetworkTarget.BFF_LOCAL)
    }
}
