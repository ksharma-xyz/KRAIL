package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import xyz.ksharma.krail.park.ride.ui.components.CarIcon
import xyz.ksharma.krail.park.ride.ui.components.DirectionsButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.krailRipple
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark

/**
 * A line of a Park & Ride tile that keeps its box when it has nothing to say. With [present] false
 * the content is still measured, so the tile is the same height, but it is drawn clear and hidden
 * from a screen reader: a placeholder is never read out as if it were a number.
 */
@Composable
internal fun ReservedLine(
    present: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = if (present) modifier else modifier.alpha(0f).clearAndSetSemantics {}) { content() }
}

/**
 * The shell every Park & Ride tile and card on the home shares, the saved trip card's own corners on the one
 * fixed dark ([FloatingDark.container]), opaque and the same in light and dark app modes, because the map under
 * it is always light. What is drawn on it is [FloatingContent]. The padding is the caller's, because the closed
 * tile centres its lines in the room it has.
 */
@Composable
internal fun Modifier.tileShell(): Modifier =
    clip(CardShape).background(color = FloatingDark.container, shape = CardShape)

/**
 * The three lines of a closed Park & Ride tile: the free spots across the station's car parks as a big
 * number with "spots" small beside it, then the car icon and the station name, then the time of the oldest
 * reading. Drawn into the caller's column so the home row's tile and the one-time suggestion card on home
 * read the same figures the same way and cannot drift.
 *
 * With no reading yet, or a failed first fetch, the number and the time keep their line boxes invisible, so
 * the tile is the same height whether its numbers are loading, loaded or failed and shows the station name
 * alone, never an error.
 */
@Composable
internal fun ColumnScope.ClosedTileFigures(tile: ParkRideTile) {
    ReservedLine(present = tile.totalFreeSpots != null) {
        SpotsLine(spots = tile.totalFreeSpots ?: PLACEHOLDER_SPOTS)
    }
    StationLine(name = tile.stationName)
    ReservedLine(present = tile.timeText != null) {
        Text(
            text = "Last updated at\u00A0${tile.timeText ?: PLACEHOLDER_TIME}",
            style = KrailTheme.typography.bodySmall,
            color = FloatingDark.softContent,
        )
    }
}

private const val PLACEHOLDER_SPOTS = 0

// Non-breaking, so the clear line box is as wide as a real time, which never breaks inside itself.
private const val PLACEHOLDER_TIME = "12:00\u00A0AM"

/** The big number with "spots" small beside it, on one baseline, wrapping whole items when short. */
@Composable
internal fun SpotsLine(spots: Int, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(dim.spacingXS)) {
        Text(
            text = "$spots",
            style = KrailTheme.typography.headlineMedium,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = "spots",
            style = KrailTheme.typography.bodyMedium,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/**
 * The type of every station and car park name in Park & Ride: `displaySmall` at the normal weight, the
 * name style of the Park & Ride card the home has always had, so a name reads the same on a closed tile,
 * an open tile, the pin card and the old card.
 */
@Composable
internal fun parkRideNameStyle(): TextStyle = KrailTheme.typography.displaySmall.copy(fontWeight = FontWeight.Normal)

/**
 * The car icon and the station's name. Whole items: when the name is too long to sit beside the
 * icon it goes under it, and it is never squeezed or cut. The name is in [parkRideNameStyle]. The
 * [large] form is the open tile's header, with a bigger icon, and at least 48 dp tall so it is a
 * touch target when it closes the tile.
 */
@Composable
internal fun StationLine(name: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = if (large) modifier.heightIn(min = dim.textFieldHeight) else modifier,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        CarIcon(modifier = Modifier.size(if (large) dim.iconM else dim.iconXS))
        Text(text = name, style = parkRideNameStyle())
    }
}

/**
 * One car park of an open tile: its name, its numbers and time through the shared
 * [CarParkFigures], and its Directions when it has a position of its own. A car park with no
 * reading yet shows its name alone and never an error.
 *
 * With [holdsHeightWhileLoading] a car park with no reading yet keeps the room its numbers and its
 * Directions will take, drawn clear and hidden from a screen reader, so the block is the same height
 * before and after its reading lands (rule 22). The home tile is drawn as it was, without it. The map
 * card sets it, because there the readings arrive after the card has opened.
 */
@Composable
internal fun CarParkBlockContent(
    carPark: CarParkBlock,
    onDirectionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    holdsHeightWhileLoading: Boolean = false,
) {
    val dim = KrailTheme.dimensions
    val waiting = holdsHeightWhileLoading && carPark.freeSpots == null
    Column(
        modifier = modifier.testTag(TripPlannerTestTags.parkRideCarPark(carPark.facilityId)),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        Text(text = carPark.name, style = parkRideNameStyle())
        if (carPark.freeSpots != null) {
            CarParkFigures(
                spotsAvailable = carPark.freeSpots,
                percentageFull = carPark.percentFull,
                timeText = carPark.timeText,
                secondaryColor = FloatingDark.softContent,
            )
        } else if (waiting) {
            ReservedLine(present = false) {
                CarParkFigures(
                    spotsAvailable = 0,
                    percentageFull = 0,
                    timeText = RESERVED_TIME,
                    secondaryColor = FloatingDark.softContent,
                )
            }
        }
        if (carPark.directionsTo != null) {
            DirectionsButton(
                carParkName = carPark.name,
                onClick = onDirectionsClick,
                modifier = Modifier.testTag(TripPlannerTestTags.parkRideDirections(carPark.facilityId)),
            )
        } else if (waiting) {
            ReservedLine(present = false) { Spacer(modifier = Modifier.size(dim.textFieldHeight)) }
        }
    }
}

private const val RESERVED_TIME = "12:00 AM"

/**
 * A closed tile's touch: a tap opens it and a long press begins reorder mode, so it is the card's
 * own [combinedClickable] with the app's ripple, as the saved trip card has. taj's `klickable` has
 * no long press, and the long press is the one way into reorder mode. With no [onClick] the tile is
 * not touchable at all, which is how reorder mode keeps the whole block one drag target.
 */
@Composable
internal fun Modifier.tileClick(label: String, onClick: (() -> Unit)?, onLongClick: (() -> Unit)?): Modifier {
    if (onClick == null) return this
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .combinedClickable(
            interactionSource = interactionSource,
            indication = krailRipple(color = themeDecorColor()),
            role = Role.Button,
            onLongClick = onLongClick,
            onClick = onClick,
        )
        .semantics { onClick(label = label, action = null) }
}
