package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.savedtrips.CarParkBlock
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile

// The suggestion card in every state the home stack can show it in. Each is a @PreviewScreen, so it is drawn
// in light and dark at normal and 200 percent text, the 320 dp frame included, where the two buttons stack.

private const val TALLAWONG_STOP = "2155384"
private const val PREVIEW_TOTAL = 155
private const val PREVIEW_PERCENT_FULL = 60
private const val PREVIEW_TIME = "8:05 AM"

private fun tile(total: Int?, time: String?) = ParkRideTile(
    stationId = TALLAWONG_STOP,
    stationName = "Tallawong Station",
    stopIds = persistentListOf(TALLAWONG_STOP),
    totalFreeSpots = total,
    timeText = time,
    carParks = persistentListOf(
        CarParkBlock(
            facilityId = "26",
            name = "Tallawong P1",
            freeSpots = total,
            percentFull = total?.let { PREVIEW_PERCENT_FULL },
            timeText = time,
            directionsTo = null,
        ),
    ),
)

@PreviewScreen
@Composable
private fun PreviewParkRideSuggestionWithNumbers() = CardPreview(tile(PREVIEW_TOTAL, PREVIEW_TIME))

@PreviewScreen
@Composable
private fun PreviewParkRideSuggestionNameAlone() = CardPreview(tile(total = null, time = null))

@PreviewScreen
@Composable
private fun PreviewParkRideSuggestionOnTheStack() {
    PreviewTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(KrailTheme.colors.surface),
        ) {
            ParkRideSuggestionItem(
                slot = ParkRideSuggestionSlot(
                    tile = tile(PREVIEW_TOTAL, PREVIEW_TIME),
                    onShown = {},
                    onAddToHome = {},
                    onNotNow = {},
                ),
            )
        }
    }
}

@Composable
private fun CardPreview(tile: ParkRideTile) {
    PreviewTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KrailTheme.colors.surface)
                .padding(KrailTheme.dimensions.pageHorizontalPadding),
        ) {
            ParkRideSuggestionCard(tile = tile, onAddToHome = {}, onNotNow = {})
        }
    }
}
