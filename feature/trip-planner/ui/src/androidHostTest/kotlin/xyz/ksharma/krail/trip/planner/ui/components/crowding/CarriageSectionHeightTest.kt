package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The carriage row is the same height whatever it shows: every level, four or eight cars, and
 * merged into three blocks. Levels change the fill inside each box, never the row, so a refresh
 * that moves a car from Seats to Standing cannot shift the card under the rider's thumb.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class CarriageSectionHeightTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rowHeightIsTheSameForEveryLevelCarCountAndWidth() {
        var cars by mutableStateOf(CarriageLevels(List(8) { SeatLevel.SEATS_AVAILABLE }))
        var width by mutableStateOf(WIDE)
        composeRule.setContent {
            PreviewTheme {
                CarriageRow(cars = cars, ink = Color.Black, modifier = Modifier.width(width))
            }
        }
        val heights = mutableListOf<Dp>()
        fun measure() {
            composeRule.waitForIdle()
            heights += composeRule.onNodeWithTag(CARRIAGE_ROW_TAG, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
                .height
        }

        measure()
        cars = CarriageLevels(List(8) { SeatLevel.STANDING_ONLY })
        measure()
        cars = CarriageLevels(listOf(SeatLevel.FEW_SEATS, SeatLevel.SEATS_AVAILABLE, SeatLevel.FEW_SEATS, SeatLevel.STANDING_ONLY))
        measure()
        width = NARROW
        measure() // three blocks

        assertTrue(heights.all { it > 0.dp })
        assertEquals(1, heights.distinct().size, "heights differ: $heights")
    }

    private companion object {
        val WIDE = 360.dp
        val NARROW = 120.dp
    }
}
