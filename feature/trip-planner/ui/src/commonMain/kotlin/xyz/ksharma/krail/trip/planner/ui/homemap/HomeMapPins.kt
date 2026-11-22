package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.savedtrips.directionsTarget
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

// Which pins the home map draws and what a tap on each one opens, `docs/MAP_HOME_UX.md` rule 30 and the
// Home structure table. Every rule is a plain function so a table test drives it at its boundary with no
// map, no layout and no database.

/** A Home or Work pin: a labelled stop the rider set, drawn with the label's own emoji. */
@Stable
internal data class LabelPin(
    val labelKey: String,
    val emoji: String,
    val stopId: String,
    val stopName: String,
    val position: LatLng,
)

/**
 * A saved Park & Ride station, drawn as the `P` disc the Add Park & Ride map already draws.
 *
 * @property stationId the key the home tile is saved under, so a pin and its tile are one station.
 * @property stopIds every stop id the station's car parks sit behind, so the plain stop pin at the
 * same place can give way to the `P`.
 */
@Stable
internal data class ParkRidePin(
    val stationId: String,
    val stationName: String,
    val stopIds: ImmutableList<String>,
    val position: LatLng,
)

/** Everything the home map draws besides the rider's own dot. */
@Stable
internal data class HomePins(
    val stops: ImmutableList<NearbyStopFeature> = persistentListOf(),
    val parkRide: ImmutableList<ParkRidePin> = persistentListOf(),
    val labels: ImmutableList<LabelPin> = persistentListOf(),
)

/**
 * The Home and Work pins: every label with a stop whose position is known and on the globe.
 *
 * A label with no stop has no pin, and neither has one whose stop is missing from the local stops
 * table or sits at 0.0 and 0.0, which would draw in the ocean. A stop belongs to one label, so a
 * second label on the same stop is left out and the first keeps the pin.
 */
internal fun labelPinsFor(
    labels: List<StopLabel>,
    coordinates: Map<String, Pair<Double, Double>>,
): ImmutableList<LabelPin> = labels
    .mapNotNull { label ->
        val stopId = label.stopId ?: return@mapNotNull null
        val stopName = label.stopName ?: return@mapNotNull null
        val position = coordinates[stopId]?.let { (lat, lon) -> directionsTarget(lat, lon) }
            ?: return@mapNotNull null
        LabelPin(
            labelKey = label.label,
            emoji = label.emoji,
            stopId = stopId,
            stopName = stopName,
            position = position,
        )
    }
    .distinctBy { it.stopId }
    .toImmutableList()

/**
 * The `P` pins: one per saved station, at the station's own stop when the local stops table knows
 * it, else at the first of its car parks that carries a position of its own, else nowhere.
 *
 * A station with no position is simply not plotted. Its tile on the stack still works, which is how
 * the Park & Ride picker treats the same case.
 */
internal fun parkRidePinsFor(
    tiles: List<ParkRideTile>,
    coordinates: Map<String, Pair<Double, Double>>,
): ImmutableList<ParkRidePin> = tiles
    .mapNotNull { tile ->
        val position = tile.stopIds.firstNotNullOfOrNull { stopId ->
            coordinates[stopId]?.let { (lat, lon) -> directionsTarget(lat, lon) }
        } ?: tile.carParks.firstNotNullOfOrNull { it.directionsTo }
        position?.let {
            ParkRidePin(
                stationId = tile.stationId,
                stationName = tile.stationName,
                stopIds = tile.stopIds,
                position = it,
            )
        }
    }
    .distinctBy { it.stationId }
    .toImmutableList()

/** The stop ids whose positions the pins need, each once, for one local lookup. */
internal fun pinStopIdsFor(labels: List<StopLabel>, tiles: List<ParkRideTile>): List<String> =
    (labels.mapNotNull { it.stopId } + tiles.flatMap { it.stopIds }).distinct()

/**
 * What the map draws for these three sources, with one pin per place.
 *
 * A stop that is also a label pin, or a stop a Park & Ride station sits behind, is drawn once: the
 * rider's own label beats the `P`, and the `P` beats the plain stop, so two discs never sit on one
 * another and a tap has one meaning. A `P` for a station behind a labelled stop gives way to the label.
 */
internal fun homePinsFor(
    nearby: List<NearbyStopFeature>,
    parkRide: List<ParkRidePin>,
    labels: List<LabelPin>,
): HomePins {
    val labelled = labels.map { it.stopId }.toSet()
    val stations = parkRide.filter { pin -> pin.stopIds.none { it in labelled } }
    val stationStops = stations.flatMap { it.stopIds }.toSet()
    return HomePins(
        stops = nearby.filter { it.stopId !in labelled && it.stopId !in stationStops }.toImmutableList(),
        parkRide = stations.toImmutableList(),
        labels = labels.toImmutableList(),
    )
}

/**
 * The map state with its stops replaced by [stops], the ones left after the labels and the `P` took
 * theirs. Anything that is not ready has no stops to replace and comes back as it was.
 */
internal fun MapUiState.withNearbyStops(stops: ImmutableList<NearbyStopFeature>): MapUiState =
    if (this is MapUiState.Ready) copy(mapDisplay = mapDisplay.copy(nearbyStops = stops)) else this

/**
 * The one character a label pin carries: the label's first letter, upper case, since a map cannot be
 * trusted to draw an emoji. A label with no letters at all still gets a character, so a pin is never blank.
 */
internal fun labelGlyph(label: String): String =
    label.trim().firstOrNull()?.uppercase() ?: BLANK_LABEL_GLYPH

private const val BLANK_LABEL_GLYPH = "?"

/** A label pin as the stop it stands for, with the modes the local table was not asked for left empty. */
internal fun LabelPin.toStopFeature(): NearbyStopFeature = NearbyStopFeature(
    stopId = stopId,
    stopName = stopName,
    position = position,
    transportModes = persistentListOf(),
)

/** What a tap on a pin opens, and the key the open card is saved under across a rotation. */
internal sealed interface HomeCard {
    /** The floating stop card with the departure board, for a stop pin or a label pin. */
    data class Stop(val stop: NearbyStopFeature) : HomeCard

    /** The floating Park & Ride card, for a `P` pin. */
    data class ParkRide(val pin: ParkRidePin) : HomeCard
}

internal fun stopCardKey(stopId: String): String = "$STOP_PREFIX$stopId"

internal fun parkRideCardKey(stationId: String): String = "$PARK_RIDE_PREFIX$stationId"

/**
 * The card [key] names, looked up again in [pins] on every draw, or null for none.
 *
 * The key is what is saved, because it is the part worth keeping: the pins themselves are rebuilt
 * from the local database. A pin that has since left the map, a stop panned out of the radius or a
 * station the rider removed, leaves the card closed instead of showing a ghost.
 */
internal fun homeCardFor(key: String?, pins: HomePins): HomeCard? = when {
    key == null -> null
    key.startsWith(STOP_PREFIX) -> {
        val stopId = key.removePrefix(STOP_PREFIX)
        val stop = pins.stops.firstOrNull { it.stopId == stopId }
            ?: pins.labels.firstOrNull { it.stopId == stopId }?.toStopFeature()
        stop?.let { HomeCard.Stop(it) }
    }
    key.startsWith(PARK_RIDE_PREFIX) -> {
        val stationId = key.removePrefix(PARK_RIDE_PREFIX)
        pins.parkRide.firstOrNull { it.stationId == stationId }?.let { HomeCard.ParkRide(it) }
    }
    else -> null
}

private const val STOP_PREFIX = "stop:"
private const val PARK_RIDE_PREFIX = "park_ride:"
