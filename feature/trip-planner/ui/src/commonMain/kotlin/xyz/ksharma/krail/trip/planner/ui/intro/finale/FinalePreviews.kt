package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

// The finale at the moments that matter, each one the frame the timeline gives at that time, drawn in the colour of
// the theme the rider chose. A preview and a snapshot are one still frame, never a running animation. `PreviewScreen`
// records each in light and dark and at 100 and 200 percent text, and the 320 dp ones are in `narrow`.

internal const val PREVIEW_ENTERING_MS = 300L
internal const val PREVIEW_HOLD_MS = 1050L
internal const val PREVIEW_BLENDED_MS = 2000L
internal const val PREVIEW_FLOODING_MS = 2250L
internal const val PREVIEW_FADING_MS = 2850L

/** The finale [elapsedMs] in, in [style]'s colour. */
@Composable
internal fun PreviewFinaleFrame(elapsedMs: Long, style: KrailThemeStyle = KrailThemeStyle.Train) {
    PreviewTheme(themeStyle = style) {
        val frame = remember(elapsedMs) { mutableStateOf(finaleFrameAt(elapsedMs, reduceMotion = false)) }
        FinaleOverlay(
            frame = frame,
            themeColour = style.hexColorCode.hexToComposeColor(),
            groundColour = KrailTheme.colors.surface,
            onTap = {},
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleEntering() = PreviewFinaleFrame(PREVIEW_ENTERING_MS)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleHold() = PreviewFinaleFrame(PREVIEW_HOLD_MS)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleBlendedToTheme() = PreviewFinaleFrame(PREVIEW_BLENDED_MS)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleMidFlood() = PreviewFinaleFrame(PREVIEW_FLOODING_MS)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleFading() = PreviewFinaleFrame(PREVIEW_FADING_MS)

// The settled wordmark in each of the four themes the Intro offers.
@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleBlendedMetro() = PreviewFinaleFrame(PREVIEW_BLENDED_MS, KrailThemeStyle.Metro)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleBlendedBus() = PreviewFinaleFrame(PREVIEW_BLENDED_MS, KrailThemeStyle.Bus)

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewFinaleBlendedFerry() = PreviewFinaleFrame(PREVIEW_BLENDED_MS, KrailThemeStyle.Ferry)
