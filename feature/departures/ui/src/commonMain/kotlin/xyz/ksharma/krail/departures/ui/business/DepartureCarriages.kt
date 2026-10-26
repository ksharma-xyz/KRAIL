package xyz.ksharma.krail.departures.ui.business

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.feature.crowding.Certainty
import xyz.ksharma.krail.feature.crowding.CrowdingWording

private const val TRAIN_MODE_NAME = "Train"

/** True when the board shows a train the trip updates feed could describe. */
internal fun DeparturesState.wantsCarriages(): Boolean =
    (departures + previousDepartures).any { it.transportModeName == TRAIN_MODE_NAME && it.realtimeTripId != null }

/**
 * Attaches forecast cars, sentence and tag to every departure the [index] covers. Applied to
 * every state the board polls in, so a refreshed list keeps its cars.
 */
internal fun DeparturesState.withCarriages(index: CarriageIndex): DeparturesState {
    if (index.tripCount == 0) return this
    return copy(
        departures = departures.withCarriages(index),
        previousDepartures = previousDepartures.withCarriages(index),
    )
}

private fun ImmutableList<StopDeparture>.withCarriages(index: CarriageIndex): ImmutableList<StopDeparture> =
    map { departure ->
        val cars = index.at(departure.realtimeTripId, departure.platformStopId)
            ?: return@map departure.copy(carriages = null, carriageSentence = null, carriageTag = null)
        val isNight = runCatching {
            CrowdingWording.isNight(departure.departureUtcDateTime.utcToLocalDateTimeAEST().hour)
        }.getOrDefault(false)
        val advice = CrowdingWording.forCarriages(cars, Certainty.FORECAST, isNight)
        departure.copy(carriages = cars, carriageSentence = advice?.sentence, carriageTag = advice?.tag)
    }.toImmutableList()
