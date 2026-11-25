package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

/** From this zoom a stop shows its mode's letter. Further out the letters crowd into each other, so it is a dot. */
internal const val MODE_LETTERS_MIN_ZOOM = 14.5f

internal val StopModeMarkerSize = 29.dp
private val StopModeLetterSize = 16.dp

/**
 * The mode a stop's marker stands for: of the stop's [modes], the one first in the order stop search ranks them, trains
 * and metro first and buses after the rest, so a station with a bus stand is still marked as a station.
 */
internal fun markerModeOf(modes: List<TransportMode>): TransportMode? = modes.minByOrNull { it.searchPriority }

/** The letter on [mode]'s marker, the one [xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon] draws. */
internal fun markerLetterOf(mode: TransportMode): String = NswTransportConfig.nameFor(mode).take(1).uppercase()

/** [mode]'s transport icon as a map marker: its letter in white on a circle of its colour, with a white edge. */
@Composable
internal fun rememberStopModeMarkerPainter(mode: TransportMode): Painter = rememberMapLetterPainter(
    letter = markerLetterOf(mode),
    fill = NswTransportConfig.colorFor(mode).hexToComposeColor(),
    letterSize = StopModeLetterSize,
)

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewStopModeMarkers() {
    PreviewTheme {
        val dim = KrailTheme.dimensions
        FlowRow(
            modifier = Modifier.padding(dim.spacingXL),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
        ) {
            TransportMode.all.forEach { mode ->
                Image(
                    painter = rememberStopModeMarkerPainter(mode),
                    contentDescription = null,
                    modifier = Modifier.size(StopModeMarkerSize),
                )
            }
        }
    }
}

// endregion
