package xyz.ksharma.krail.trip.planner.ui.alerts

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Identity of a service alert row.
 *
 * NSW sends distinct alerts under one heading, and the list was once keyed on the heading
 * alone. Compose rejects a duplicate key outright, so the second alert with a repeated
 * heading took the Service Alerts sheet down with
 * "Key ... was already used. If you are using LazyColumn/Row please make sure you provide a
 * unique key for each item." Rendering the screen is the assertion: these tests fail by
 * throwing, not by comparing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class ServiceAlertScreenIdentityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeTestRule) }

    private val duplicateHeadings = persistentSetOf(
        ServiceAlert(heading = "Trackwork", message = "Buses replace trains on the T1."),
        ServiceAlert(heading = "Trackwork", message = "Buses replace trains on the T8."),
        ServiceAlert(heading = "Trackwork", message = "Buses replace trains on the T9."),
    )

    @Test
    fun `alerts sharing a heading all render instead of crashing`() {
        composeTestRule.setContent {
            PreviewTheme {
                ServiceAlertScreen(serviceAlerts = duplicateHeadings)
            }
        }

        composeTestRule.onAllNodesWithText("Trackwork").assertCountEquals(3)
    }

    @Test
    fun `expanding one alert does not expand its same-heading siblings`() {
        // Expansion was keyed on hashCode(), so identity was an Int over unbounded text.
        // It is now the same string the list is keyed on.
        composeTestRule.setContent {
            PreviewTheme {
                ServiceAlertScreen(serviceAlerts = duplicateHeadings)
            }
        }

        composeTestRule.onAllNodesWithText("Trackwork")[1].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Buses replace trains on the T8.").assertExists()
        composeTestRule.onAllNodesWithText("Buses replace trains on the T1.").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Buses replace trains on the T9.").assertCountEquals(0)
    }

    @Test
    fun `a single alert still renders`() {
        composeTestRule.setContent {
            PreviewTheme {
                ServiceAlertScreen(
                    serviceAlerts = persistentSetOf(
                        ServiceAlert(heading = "Lift outage", message = "Use the ramp."),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Lift outage").assertExists()
    }

    @Test
    fun `alertId separates alerts that share a heading`() {
        val (first, second) = duplicateHeadings.toList()

        assertNotEquals(first.alertId, second.alertId)
    }

    @Test
    fun `alertId is stable across equal instances`() {
        // The key has to survive a state emission that rebuilds the alert objects.
        val alert = ServiceAlert(heading = "Trackwork", message = "Buses replace trains.")
        val rebuilt = ServiceAlert(heading = "Trackwork", message = "Buses replace trains.")

        assertEquals(alert.alertId, rebuilt.alertId)
    }

    @Test
    fun `the expanded alert survives activity recreation`() {
        // expandedAlertId changed from Int? to String?, and rememberSaveable only restores a
        // type the saver understands. Rotation is the path a rider takes to this.
        restorationTester.setContent {
            PreviewTheme {
                ServiceAlertScreen(serviceAlerts = duplicateHeadings)
            }
        }

        composeTestRule.onAllNodesWithText("Trackwork")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Buses replace trains on the T8.").assertExists()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Buses replace trains on the T8.").assertExists()
        composeTestRule.onAllNodesWithText("Buses replace trains on the T1.").assertCountEquals(0)
    }
}
