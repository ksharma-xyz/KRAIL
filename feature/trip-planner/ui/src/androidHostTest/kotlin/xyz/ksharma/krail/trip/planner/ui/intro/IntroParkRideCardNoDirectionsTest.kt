package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.toImmutableSet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The Intro's copy of the Park & Ride card only shows the numbers. It has no Directions, even for car parks whose position is
 * known, which is when the home card does offer it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroParkRideCardNoDirectionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `car parks with a position have no Directions on the Intro card`() {
        val station = assertIs<IntroCardSlot.Station>(introCardSlot(PreviewTallCardContentState))
        val positioned = station.copy(
            card = station.card.copy(
                facilities = station.card.facilities
                    .map { it.copy(latitude = -33.69, longitude = 150.9) }
                    .toImmutableSet(),
            ),
        )

        composeRule.setContent { PreviewTheme { IntroParkRideCardSlot(slot = positioned) } }
        composeRule.waitForIdle()

        val firstCarPark = positioned.card.facilities.first().facilityName
        assertTrue(
            composeRule.onAllNodesWithText(firstCarPark, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
            "the open card is on screen",
        )
        composeRule
            .onAllNodes(hasContentDescription("Directions to", substring = true), useUnmergedTree = true)
            .assertCountEquals(0)
    }
}
