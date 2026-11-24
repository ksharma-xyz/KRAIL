package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.ui.test.onNodeWithContentDescription
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.discover.state.Button
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.discover.state.DiscoverState
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverScreen

/**
 * Discover on every device profile (`docs/testing/DEVICE_PROFILES.md`). Its title bar, filter chips and cards stand inside the
 * safe area on every one, in the phone layout and in the wide one. The cards carry no image, so nothing reaches the network.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class DiscoverDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private val state = DiscoverState(
        discoverCardsList = persistentListOf(
            card("travel", DiscoverCardType.Travel, Button.Cta(label = "Plan it", url = "https://example.com/travel")),
            card("events", DiscoverCardType.Events, Button.Share),
            card("food", DiscoverCardType.Food),
        ),
        sortedDiscoverCardTypes = persistentListOf(
            DiscoverCardType.Travel,
            DiscoverCardType.Events,
            DiscoverCardType.Food,
        ),
        selectedType = DiscoverCardType.Travel,
    )

    @Test
    fun `Discover keeps every control inside the safe area on every device`() =
        matrix.forEachDeviceProfile(screen = "DiscoverScreen") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                DiscoverScreen(
                    state = state,
                    onBackClick = {},
                    onAppSocialLinkClicked = {},
                    onPartnerSocialLinkClicked = { _, _, _ -> },
                    onCtaClicked = { _, _, _ -> },
                    onShareClick = { _, _ -> },
                    onCardSeen = {},
                    resetAllSeenCards = {},
                    onChipSelected = {},
                )
            }
            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithContentDescription("Back"))
        }

    private fun card(id: String, type: DiscoverCardType, vararg buttons: Button) = DiscoverState.DiscoverUiModel(
        cardId = id,
        title = "Discover $id",
        description = "A short description of the $id card.",
        imageList = persistentListOf(),
        type = type,
        buttons = persistentListOf(*buttons),
    )
}
