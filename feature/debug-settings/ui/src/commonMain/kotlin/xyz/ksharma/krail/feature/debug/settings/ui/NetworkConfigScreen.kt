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
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Picker for the single [NetworkSource] knob driving the resolver.
 * Selection persists immediately on tap; no save button.
 *
 * The FOLLOW_RC row's caption shows the live `enable_proto_bff` value plus
 * what it currently maps to so a developer can see at a glance what the
 * production cohort would see right now.
 */
@Composable
fun NetworkConfigScreen(
    selected: NetworkSource,
    liveBffEnabled: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSelect: (NetworkSource) -> Unit = {},
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
                item(key = "row-follow-rc") {
                    NetworkRadioRow(
                        title = "Follow Firebase RC",
                        subtitle = followRcSubtitle(liveBffEnabled),
                        selected = selected == NetworkSource.FOLLOW_RC,
                        enabled = true,
                        onClick = { onSelect(NetworkSource.FOLLOW_RC) },
                    )
                }
                item(key = "row-nsw-direct") {
                    NetworkRadioRow(
                        title = "NSW Direct",
                        subtitle = "https://api.transport.nsw.gov.au",
                        selected = selected == NetworkSource.NSW_DIRECT,
                        enabled = true,
                        onClick = { onSelect(NetworkSource.NSW_DIRECT) },
                    )
                }
                item(key = "row-bff-local") {
                    NetworkRadioRow(
                        title = "BFF Local",
                        subtitle = bffLocalSubtitle(),
                        selected = selected == NetworkSource.BFF_LOCAL,
                        enabled = KRAIL_BFF_BASE_URL.isNotBlank(),
                        onClick = { onSelect(NetworkSource.BFF_LOCAL) },
                    )
                }
                item(key = "row-bff-prod") {
                    NetworkRadioRow(
                        title = "BFF Prod",
                        subtitle = bffProdSubtitle(),
                        selected = selected == NetworkSource.BFF_PROD,
                        enabled = IS_BFF_PROD_DEPLOYED,
                        onClick = { onSelect(NetworkSource.BFF_PROD) },
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
        text = "Pick where the app's BFF-eligible requests go.",
        style = KrailTheme.typography.bodyLarge,
        color = KrailTheme.colors.softLabel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
    )
}

private fun followRcSubtitle(liveBffEnabled: Boolean): String {
    val rcLine = "enable_proto_bff is currently $liveBffEnabled"
    val mappingLine = if (liveBffEnabled) "Maps to: BFF Prod" else "Maps to: NSW Direct"
    return "$rcLine\n$mappingLine"
}

private fun bffLocalSubtitle(): String =
    if (KRAIL_BFF_BASE_URL.isNotBlank()) {
        KRAIL_BFF_BASE_URL
    } else {
        "(set krail.bffBaseUrl in local.properties)"
    }

private fun bffProdSubtitle(): String =
    if (KRAIL_BFF_PROD_BASE_URL.isNotBlank()) {
        KRAIL_BFF_PROD_BASE_URL
    } else {
        "(not deployed yet)"
    }

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
        NetworkConfigScreen(
            selected = NetworkSource.FOLLOW_RC,
            liveBffEnabled = false,
        )
    }
}
