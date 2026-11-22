package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Save as and Go here on the floating stop card, on real nodes (`docs/MAP_HOME_UX.md`, Stop card, and rules
 * 13, 21 and 22): the Save as pills first and the primary action under them, whole items that wrap and are
 * never squeezed at 320 dp and 200 percent text, every target at least 48 dp, a filled pill that is a status
 * and not a control, and a card that keeps its height when a label is assigned.
 *
 * Each test runs in a window of its own size at density 1, so a dp is a pixel. The board is closed, which is
 * how a pin opens the card, so nothing here waits on the network.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ACTIONS_TEST_SDK], manifest = Config.NONE)
class StopCardActionsLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var picked = 0
    private val saved = mutableListOf<Pair<String, String>>()

    private var role = PickRole.Destination
    private var labels: ImmutableList<StopLabel> by mutableStateOf(persistentListOf(HOME, WORK))

    // region the words and the order

    @Test
    @Config(qualifiers = PHONE)
    fun `a destination search reads Go here under the Save as pills`() {
        showCard()

        composeRule.onNodeWithTag(StopCard.PICK).assertHasClickAction()
        composeRule.onAllNodesWithTag(StopCard.PICK).assertCountEquals(1)
        assertTrue(bounds(StopCard.PICK).top >= bounds(StopCard.SAVE_AS).bottom, "Go here sits under the pills.")
        assertEquals("Go here", textOf(StopCard.PICK))
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a start search reads Start here`() {
        role = PickRole.Start

        showCard()

        assertEquals("Start here", textOf(StopCard.PICK))
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `choosing a stop for a label reads Select stop and offers no Save as`() {
        role = PickRole.Choose

        showCard()

        assertEquals("Select stop", textOf(StopCard.PICK))
        composeRule.onAllNodesWithTag(StopCard.SAVE_AS).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a stop the stops table does not know still offers Go here`() {
        showCard(stop = UNKNOWN_STOP)

        assertEquals("Go here", textOf(StopCard.PICK))
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `go here is one tap and hands the stop to the host once`() {
        showCard()

        composeRule.onNodeWithTag(StopCard.PICK).performClick()

        assertEquals(1, picked)
    }

    // endregion

    // region Save as

    @Test
    @Config(qualifiers = PHONE)
    fun `an unset label is an outlined pill worded Save as, 48 dp tall, and a tap gives it to this stop`() {
        showCard()

        val home = bounds(StopCard.saveAsPill("Home"))
        assertNear("pill height", 48.dp, home.height)
        composeRule.onNodeWithTag(StopCard.saveAsPill("Home")).assertHasClickAction().performClick()
        assertEquals(listOf("Home" to CENTRAL.stopId), saved)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the label a stop carries is one filled pill that is not a control`() {
        labels = persistentListOf(HOME.copy(stopId = CENTRAL.stopId, stopName = CENTRAL.stopName), WORK)

        showCard()

        composeRule.onAllNodesWithTag(StopCard.saveAsPill("Work")).assertCountEquals(0)
        val pill = composeRule.onNodeWithTag(StopCard.saveAsPill("Home"))
        pill.assertHasNoClickAction()
        assertEquals(
            "Saved as Home",
            pill.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single(),
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a label set on another stop is not offered, so there is no pill that reassigns it`() {
        labels = persistentListOf(HOME.copy(stopId = "999", stopName = "Elsewhere"), WORK)

        showCard()

        composeRule.onAllNodesWithTag(StopCard.saveAsPill("Home")).assertCountEquals(0)
        composeRule.onNodeWithTag(StopCard.saveAsPill("Work")).assertHasClickAction()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `no labels to offer leaves no Save as row and the card keeps Go here`() {
        labels = persistentListOf()

        showCard()

        composeRule.onAllNodesWithTag(StopCard.SAVE_AS).assertCountEquals(0)
        composeRule.onNodeWithTag(StopCard.PICK).assertHasClickAction()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `assigning a label keeps the card as tall as it was`() = assertHeightKeptWhenAssigned()

    @Test
    @Config(qualifiers = "w320dp-h900dp")
    fun `assigning a label at 320 dp and 200 percent text keeps the card as tall as it was`() =
        assertHeightKeptWhenAssigned(fontScale = 2f)

    private fun assertHeightKeptWhenAssigned(fontScale: Float = 1f) {
        showCard(fontScale = fontScale, onSaveAs = { label, stop ->
            labels = labels.map {
                if (it.label == label.label) it.copy(stopId = stop.stopId, stopName = stop.stopName) else it
            }.toImmutableList()
        })
        val before = bounds(StopCard.CARD).height

        composeRule.onNodeWithTag(StopCard.saveAsPill("Home")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(StopCard.saveAsPill("Home")).assertHasNoClickAction()
        assertNear("card height after assigning", before, bounds(StopCard.CARD).height)
    }

    // endregion

    // region narrow, large text and wide

    @Test
    @Config(qualifiers = "w320dp-h900dp")
    fun `at 320 dp and 200 percent text every pill is whole inside the card and none overlaps another`() {
        labels = persistentListOf(HOME, WORK, GYM)

        showCard(fontScale = 2f)

        val card = bounds(StopCard.CARD)
        val pills = listOf("Home", "Work", "Gym").map { bounds(StopCard.saveAsPill(it)) }
        pills.forEach { pill ->
            assertTrue(
                pill.left >= card.left - TOLERANCE && pill.right <= card.right + TOLERANCE,
                "A pill is cut: $pill in $card",
            )
            assertTrue(pill.height >= 48.dp - TOLERANCE, "A pill is under 48 dp: ${pill.height}")
        }
        pills.forEachIndexed { i, a ->
            pills.drop(i + 1).forEach { b ->
                val apart = a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top
                assertTrue(apart, "Two pills overlap: $a and $b")
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h900dp")
    fun `at 320 dp and 200 percent text Go here is the card's full width and under every pill`() {
        showCard(fontScale = 2f)

        val pick = bounds(StopCard.PICK)
        val row = bounds(StopCard.SAVE_AS)
        assertNear("Go here width", row.width, pick.width)
        assertTrue(pick.top >= row.bottom - TOLERANCE, "Go here sits under the pills.")
        assertTrue(pick.height >= 48.dp - TOLERANCE, "Go here is ${pick.height} tall.")
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp")
    fun `in a wide window the pills sit inside the one column card`() {
        showCard()

        val card = bounds(StopCard.CARD)
        val row = bounds(StopCard.SAVE_AS)
        assertTrue(card.width <= 528.dp + TOLERANCE, "The card is ${card.width} wide.")
        assertTrue(row.left >= card.left && row.right <= card.right, "The pills run outside the card.")
    }

    // endregion

    private fun showCard(
        stop: NearbyStopFeature = CENTRAL,
        fontScale: Float = 1f,
        onSaveAs: (StopLabel, StopItem) -> Unit = { label, item -> saved += label.label to item.stopId },
    ) {
        composeRule.setContent {
            WithFontScale(fontScale) {
                PreviewTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        StopCardPlacement(topReserve = 0.dp, onHeight = {}) {
                            StopCard(
                                stop = stop,
                                departures = DeparturesState(),
                                onEvent = {},
                                onClose = {},
                                actions = {
                                    StopCardPickActions(
                                        stop = StopItem(stopId = stop.stopId, stopName = stop.stopName),
                                        options = StopCardOptions(
                                            pickRole = role,
                                            labels = labels,
                                            onSaveAs = onSaveAs,
                                        ),
                                        onPick = { picked++ },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    /** The words on a control, merged with its children, which is what a screen reader reads. */
    private fun textOf(tag: String): String =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE.value,
            "$what should be $expected but is $actual.",
        )
    }

    private companion object {
        const val PHONE = "w411dp-h891dp"
        val TOLERANCE = 1.5.dp

        val HOME = StopLabel(emoji = "H", label = "Home")
        val WORK = StopLabel(emoji = "W", label = "Work")
        val GYM = StopLabel(emoji = "G", label = "Gym")

        val CENTRAL = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train, TransportMode.Bus),
        )

        /** A stop with no modes, as one the local table has no row for is drawn. */
        val UNKNOWN_STOP = NearbyStopFeature(
            stopId = "999000",
            stopName = "Unlisted Stop",
            position = LatLng(-33.88, 151.20),
            transportModes = persistentListOf(),
        )
    }
}

private const val ACTIONS_TEST_SDK = 34
