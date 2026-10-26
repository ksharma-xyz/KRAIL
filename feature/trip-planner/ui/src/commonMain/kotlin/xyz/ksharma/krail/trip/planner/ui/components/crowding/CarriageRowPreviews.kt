package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

// region Previews

@Composable
private fun PreviewSection(levels: List<SeatLevel>, sentence: String?, modifier: Modifier = Modifier) {
    PreviewTheme {
        CarriageSection(
            cars = CarriageLevels(levels),
            sentence = sentence,
            modifier = modifier.padding(KrailTheme.dimensions.spacingL),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_Carriages_BothEnds_EightCars() {
    PreviewSection(
        levels = listOf(SeatLevel.SEATS_AVAILABLE) + List(6) { SeatLevel.FEW_SEATS } + SeatLevel.SEATS_AVAILABLE,
        sentence = "Seats expected at both ends.",
    )
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_Carriages_Middle_FourCars() {
    PreviewSection(
        levels = listOf(
            SeatLevel.STANDING_ONLY,
            SeatLevel.SEATS_AVAILABLE,
            SeatLevel.SEATS_AVAILABLE,
            SeatLevel.FEW_SEATS,
        ),
        sentence = "Seats in the middle of the train.",
    )
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_Carriages_StandingEverywhere() {
    PreviewSection(levels = List(8) { SeatLevel.STANDING_ONLY }, sentence = "Standing only, all along the train.")
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_Carriages_OneEndNoSentence() {
    // An end is quieter but ends cannot be named yet: the row draws, the sentence does not.
    PreviewSection(
        levels = listOf(SeatLevel.SEATS_AVAILABLE) + List(7) { SeatLevel.STANDING_ONLY },
        sentence = null,
    )
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_Carriages_Narrow_ThreeBlocks() {
    PreviewSection(
        levels = listOf(SeatLevel.SEATS_AVAILABLE) + List(6) { SeatLevel.FEW_SEATS } + SeatLevel.SEATS_AVAILABLE,
        sentence = "Seats expected at both ends.",
        modifier = Modifier.width(NARROW_PREVIEW_WIDTH),
    )
}

private val NARROW_PREVIEW_WIDTH: Dp = 160.dp

// endregion
