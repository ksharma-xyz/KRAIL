package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

// Real station names, never a rider's trip. Shared by the phone and the 320dp previews and by the layout tests.

/** The confirm screen with a short pair of names, as the saved trip tile shows them on Saved Trips. */
internal val PreviewConfirm = IntroState(
    step = IntroStep.CONFIRM,
    startStopId = "2155384",
    startName = "Tallawong Station",
    endStopId = "200060",
    endName = "Central Station",
)

/** The confirm screen with long names, which wrap inside the tile at a large text size. */
internal val PreviewConfirmLongNames = IntroState(
    step = IntroStep.CONFIRM,
    startStopId = "203294",
    startName = "Juniors Kingsford Light Rail",
    endStopId = "206720",
    endName = "Chatswood Station, Stand A",
)

@Composable
internal fun PreviewIntroConfirmStep(state: IntroState) {
    PreviewTheme {
        IntroFlowScreen(
            state = state,
            suggestedStops = DEFAULT_SUGGESTED_STOPS,
            onEvent = { _, _ -> },
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroConfirmSettled() = PreviewIntroConfirmStep(PreviewConfirm)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroConfirmLongNames() = PreviewIntroConfirmStep(PreviewConfirmLongNames)

/**
 * The star part way through its celebration: a little past its size and tilted, with the burst a third of the way
 * out. The headline draws this at the first frames of the confirm screen, and a preview cannot play a clock.
 */
@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewIntroConfirmMidAnimation() {
    PreviewTheme {
        Text(
            text = introEmojiFor(IntroStep.CONFIRM),
            style = KrailTheme.typography.emoji,
            modifier = Modifier
                .padding(PREVIEW_BURST_ROOM)
                .introBurst(progress = { MID_BURST_PROGRESS }, color = themeDecorColor())
                .graphicsLayer {
                    scaleX = MID_STAR_SCALE
                    scaleY = MID_STAR_SCALE
                    rotationZ = MID_STAR_TILT
                },
        )
    }
}

private val PREVIEW_BURST_ROOM = 96.dp
private const val MID_BURST_PROGRESS = 0.35f
private const val MID_STAR_SCALE = 1.12f
private const val MID_STAR_TILT = -10f
