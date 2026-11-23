package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SAVED_TRIPS_COLUMN_MAX_WIDTH
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// The To and From block (docs/MAP_HOME_UX.md rules 35 and 36) in every state a rider sees, one
// above the other: To edited, From edited, and neither. The edited bar is taj's dark text field and
// the other is taj's text field button. The narrow and wide previews sit in the
// `timetable.narrow` and `timetable.wide` packages, which their own snapshot tests scan, and draw
// the same frame. The 320 dp one at 2x font is the frame where long names end in an ellipsis.

private val previewToStop = StopItem(stopName = "Town Hall Station", stopId = "200070")
private val previewFromStop = StopItem(stopName = "Central Station", stopId = "200060")
private val previewLongStop = StopItem(stopName = "Sydney Olympic Park Station", stopId = "2127")

/** The frame every canvas draws. */
@Composable
internal fun ToFromBlockStates() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        // On the theme colour, as the search row draws it, so the white bar reads as a bar. Centred
        // and capped at the saved trips column (rule 45), so a wide window shows the block at the
        // width it really has and not stretched across the canvas.
        val themeColorHex by LocalThemeColor.current
        Box(
            modifier = Modifier.fillMaxWidth().background(themeColorHex.hexToComposeColor()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = SAVED_TRIPS_COLUMN_MAX_WIDTH).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ToFromBlockPreviewBlock(editing = ToFromField.To, to = previewToStop)
                ToFromBlockPreviewBlock(editing = ToFromField.From, to = previewToStop)
                ToFromBlockPreviewBlock(editing = null, to = previewLongStop)
            }
        }
    }
}

@Composable
private fun ToFromBlockPreviewBlock(editing: ToFromField?, to: StopItem) {
    // The dark bar holds the text the rider has typed, so it shows that and not its stop.
    val typed = rememberTextFieldState("Cen")
    ToFromBlock(
        toStopItem = to,
        fromStopItem = previewFromStop,
        onToClick = {},
        onFromClick = {},
        onGoClick = {},
        editing = editing,
        editor = editing?.let { ToFromEditor(state = typed) },
        toLeading = { PreviewSlotButton(Icons.AutoMirrored.Filled.ArrowBack) },
        toTrailing = { PreviewSlotButton(Icons.Default.Close) },
    )
}

/** Stands in for the back arrow and the X the Search Stop screen puts in the To bar's end columns. */
@Composable
private fun PreviewSlotButton(icon: ImageVector) {
    val barContent = LocalContentColor.current
    RoundIconButton(onClick = {}, color = Color.Transparent) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(barContent),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** A phone: the bars take the whole width less the 16 dp padding, on 411 dp. */
@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewToFromBlockPhone() {
    ToFromBlockStates()
}
