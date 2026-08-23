package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// region Previews
//
// The pair that matters is the two action-slot states. They must be the same shape: Search
// stays in the lower slot in both, and only the button above it changes.

private val previewFromStop = StopItem(stopName = "Central Station", stopId = "200060")
private val previewToStop = StopItem(stopName = "Town Hall Station", stopId = "200070")

@PreviewComponent
@Composable
private fun PreviewSearchStopRowAiAvailable() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            fromStopItem = previewFromStop,
            toStopItem = previewToStop,
            isExpanded = true,
            isAiSearchAvailable = true,
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSearchStopRowAiUnavailable() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            fromStopItem = previewFromStop,
            toStopItem = previewToStop,
            isExpanded = true,
            isAiSearchAvailable = false,
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSearchStopRowEmptyFields() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            isExpanded = true,
            isAiSearchAvailable = false,
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSearchStopRowCollapsed() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SearchStopRow(
            fromButtonClick = {},
            toButtonClick = {},
            isExpanded = false,
        )
    }
}

// endregion
