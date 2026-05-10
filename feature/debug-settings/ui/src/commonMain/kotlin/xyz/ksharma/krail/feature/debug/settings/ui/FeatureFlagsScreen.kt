package xyz.ksharma.krail.feature.debug.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/**
 * Picker for the single Firebase RC rollout flag `enable_proto_bff`.
 * Selection persists immediately on tap; no save button.
 */
@Composable
fun FeatureFlagsScreen(
    selected: FlagOverride,
    liveBffEnabled: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSelect: (FlagOverride) -> Unit = {},
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
                title = { Text(text = "Feature Flags") },
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = "header") {
                    FeatureFlagsHeader()
                }
                item(key = "row-follow-rc") {
                    NetworkRadioRow(
                        title = "Follow Firebase RC",
                        subtitle = "Currently: $liveBffEnabled",
                        selected = selected == FlagOverride.FOLLOW_RC,
                        enabled = true,
                        onClick = { onSelect(FlagOverride.FOLLOW_RC) },
                    )
                }
                item(key = "row-force-on") {
                    NetworkRadioRow(
                        title = "Force ON",
                        subtitle = "Always route through BFF.",
                        selected = selected == FlagOverride.FORCE_ON,
                        enabled = true,
                        onClick = { onSelect(FlagOverride.FORCE_ON) },
                    )
                }
                item(key = "row-force-off") {
                    NetworkRadioRow(
                        title = "Force OFF",
                        subtitle = "Always hit NSW direct.",
                        selected = selected == FlagOverride.FORCE_OFF,
                        enabled = true,
                        onClick = { onSelect(FlagOverride.FORCE_OFF) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureFlagsHeader() {
    val dim = KrailTheme.dimensions
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingXL),
    ) {
        Text(
            text = "enable_proto_bff",
            style = KrailTheme.typography.title,
        )
        Text(
            text = "Routes traffic through BFF when on; NSW direct when off.",
            style = KrailTheme.typography.bodyLarge,
            color = KrailTheme.colors.softLabel,
        )
    }
}

@PreviewScreen
@Composable
private fun FeatureFlagsScreenPreview() {
    PreviewTheme {
        FeatureFlagsScreen(
            selected = FlagOverride.FOLLOW_RC,
            liveBffEnabled = false,
        )
    }
}
