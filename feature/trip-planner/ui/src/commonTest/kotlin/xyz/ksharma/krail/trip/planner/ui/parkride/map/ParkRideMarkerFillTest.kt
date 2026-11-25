package xyz.ksharma.krail.trip.planner.ui.parkride.map

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.contrastRatio
import kotlin.test.Test
import kotlin.test.assertTrue

class ParkRideMarkerFillTest {

    @Test
    fun `the white P stands out on the marker in every theme`() {
        KrailThemeStyle.entries.forEach { style ->
            val ratio = Color.White.contrastRatio(parkRideMarkerFill(style.hexColorCode.hexToComposeColor()))

            assertTrue(ratio >= ContrastAnalyzer.UI_COMPONENT_CONTRAST_AA, "${style.name} gives $ratio")
        }
    }
}
