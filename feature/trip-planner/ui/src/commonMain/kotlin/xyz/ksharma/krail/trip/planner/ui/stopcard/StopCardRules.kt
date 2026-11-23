package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.trip.planner.ui.savedtrips.columnWidthFor
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

/**
 * What a floating stop card shows, decided with no layout. A tapped map pin opens one, never a
 * bottom sheet (`docs/MAP_HOME_UX.md`, Stop card). Every rule here is a plain function so a table
 * test drives it at its boundary.
 */
internal enum class StopCardKind {
    /** A stop pin: the stop name, its modes and the departure board. */
    Stop,

    /** A Park & Ride pin: the station, its car parks and their numbers. */
    ParkRide,
}

/**
 * The parts a card is made of. [showsActionSlot] is the room at the bottom of the card for the
 * actions the card hosts, today the stop's one pick action and later Save as, Go here and Start
 * here (`docs/features/map-home/ERD.md`, branch 13). It is part of the model so filling it never
 * reshapes the card.
 */
internal data class StopCardLayout(
    val showsModes: Boolean,
    val showsBoard: Boolean,
    val showsTotal: Boolean,
    val showsActionSlot: Boolean,
)

/**
 * The parts of a card of [kind] for a stop with [modeCount] modes or a station with [carParkCount]
 * car parks.
 *
 * A stop shows its modes only when it has some and always carries the board. A Park & Ride card has
 * neither: it shows the station's numbers instead, and the station's total only when it has more
 * than one car park, because with one the car park's own block already says the same number.
 */
internal fun stopCardLayout(kind: StopCardKind, modeCount: Int, carParkCount: Int): StopCardLayout =
    when (kind) {
        StopCardKind.Stop -> StopCardLayout(
            showsModes = modeCount > 0,
            showsBoard = true,
            showsTotal = false,
            showsActionSlot = true,
        )

        StopCardKind.ParkRide -> StopCardLayout(
            showsModes = false,
            showsBoard = false,
            showsTotal = carParkCount > 1,
            showsActionSlot = true,
        )
    }

/**
 * The stop the open card is for, or null when it has none to show. The pin that was tapped is held
 * by its id, which is the part worth saving across a rotation, and looked up again in the stops the
 * map is drawing. A stop that has since left the nearby set, a filter turned on or a pan, leaves
 * the card closed rather than showing a stop the map no longer draws.
 */
internal fun nearbyStopFor(selectedStopId: String?, nearbyStops: List<NearbyStopFeature>): NearbyStopFeature? =
    selectedStopId?.let { id -> nearbyStops.firstOrNull { it.stopId == id } }

/** A card keeps a 16 dp margin to the pane's edges and 8 dp to whatever sits above or below it. */
internal val STOP_CARD_EDGE: Dp = 16.dp
internal val STOP_CARD_GAP: Dp = 8.dp

/**
 * The width of the card in a map pane [paneWidth] wide: the one column rule, so the column is at
 * most 560 dp and centred, and the card sits one 16 dp margin inside it on each side, as the cards
 * on home do. On a tablet or a foldable it is the same width as those cards. In the 480 dp pane of
 * a dual pane window it is the pane less the margins. A pane with no room gets a card with no width,
 * never a negative one (`docs/MAP_HOME_UX.md` rule 45).
 */
internal fun stopCardWidth(paneWidth: Dp): Dp =
    (columnWidthFor(paneWidth) - STOP_CARD_EDGE * 2).coerceAtLeast(0.dp)

/**
 * The tallest the card grows in a pane [paneHeight] tall, below [topReserve] (the form that floats
 * above the map) and above [bottomInset] (the navigation bar). The card's own content scrolls
 * inside what is left, so a long board never pushes the card over the form. The floor keeps a card
 * with a degenerate pane (a rotation transient) from collapsing to nothing.
 */
internal fun stopCardMaxHeight(paneHeight: Dp, topReserve: Dp, bottomInset: Dp): Dp =
    (paneHeight - topReserve - bottomInset - STOP_CARD_EDGE - STOP_CARD_GAP)
        .coerceAtLeast(STOP_CARD_MIN_HEIGHT)

internal val STOP_CARD_MIN_HEIGHT: Dp = 120.dp

/**
 * The map's own controls (the location button) sit above the card while it is open, so the card
 * never covers one. [cardHeight] is the card's measured height, zero with no card.
 */
internal fun controlsClearance(cardHeight: Dp): Dp =
    if (cardHeight > 0.dp) cardHeight + STOP_CARD_GAP else 0.dp

/**
 * The clearance the map's controls ride at: [controlsClearance] while a card is open and the controls stay
 * ([controlsHide] false), and nothing while no card is open or while the host hides the controls instead of lifting
 * them (Search Stop), so a hidden control never leaves a gap behind and comes back to where it was.
 */
internal fun controlsClearanceFor(cardHeight: Dp, cardOpen: Boolean, controlsHide: Boolean): Dp =
    if (cardOpen && !controlsHide) controlsClearance(cardHeight) else 0.dp

/**
 * Two cards are one card, with its state kept, only while they are for the same stop. The board's
 * open state, its lines chip and its filter are keyed on this, so tapping another pin starts that
 * stop's card closed and a rotation keeps this one as it was.
 */
internal fun stopCardStateKey(kind: StopCardKind, id: String): String = "${kind.name}:$id"
