package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideLookup
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision

/** The one preference key the Intro's progress lives under, as one serialised string. */
internal const val INTRO_PROGRESS_KEY = "KEY_INTRO_PROGRESS"

/** The shape of the saved text. A text of any other version is ignored, so the Intro starts again. */
internal const val INTRO_PROGRESS_VERSION = 1

/**
 * What the Intro keeps on the device so a rider who swipes the app away or force stops it carries on where they were:
 * the screen, the theme picked, the Park & Ride answer and the station added, the two stops and the answer on the
 * confirm screen. A stop is its id and its name and nothing else.
 *
 * It never holds a coordinate, what was typed into search, the stations near the rider, a car park or anything
 * else that says where the rider is. It is cleared when the Intro finishes and when it is replayed from Settings.
 */
@Serializable
internal data class IntroProgress(
    val version: Int,
    val step: IntroStep,
    val themeId: Int? = null,
    val parkRide: ParkRideAnswer = ParkRideAnswer.ASK,
    val locationResult: LocationResult = LocationResult.NOT_ASKED,
    val parkRideLookup: ParkRideLookup = ParkRideLookup.NOT_LOOKED,
    val added: Boolean = false,
    val station: ProgressStop? = null,
    val start: ProgressStop? = null,
    val end: ProgressStop? = null,
    val startFromParkRide: Boolean = false,
    val decision: TripDecision? = null,
    val usedSearch: Boolean = false,
    val saved: Boolean = false,
)

/** A stop kept as its id and its name. */
@Serializable
internal data class ProgressStop(val id: String, val name: String)

/**
 * The progress of this state. Until the rider leaves the Park & Ride screen its question is still open, whatever was
 * tapped or looked up on it, so it is kept as unanswered and the rider is asked again.
 */
internal fun IntroState.toProgress(): IntroProgress {
    val questionOpen = step == IntroStep.PARK_RIDE && !addedParkRide
    return IntroProgress(
        version = INTRO_PROGRESS_VERSION,
        step = step,
        themeId = selectedThemeId,
        parkRide = if (questionOpen) ParkRideAnswer.ASK else parkRideAnswer,
        locationResult = if (questionOpen) LocationResult.NOT_ASKED else locationResult,
        parkRideLookup = if (questionOpen) ParkRideLookup.NOT_LOOKED else parkRideLookup,
        added = addedParkRide,
        station = addedStation(),
        start = stopOf(startStopId, startName),
        end = stopOf(endStopId, endName),
        startFromParkRide = startFromParkRide,
        decision = tripDecision,
        usedSearch = usedSearch,
        saved = saved,
    )
}

private fun IntroState.addedStation(): ProgressStop? {
    val card = (parkRideCard as? ParkRideCardPhase.Card)?.station
    return if (addedParkRide && card != null) ProgressStop(card.stationId, card.stationName) else null
}

private fun stopOf(id: String?, name: String?): ProgressStop? =
    if (id != null && name != null) ProgressStop(id, name) else null

/**
 * The state the Intro opens with. A station added before is a card with its name and no car parks, and nothing is
 * worked out again: the Park & Ride question is already behind the rider.
 */
internal fun IntroProgress.toState(): IntroState = IntroState(
    step = step,
    parkRideAnswer = parkRide,
    locationResult = locationResult,
    parkRideLookup = parkRideLookup,
    parkRideCard = station?.let { stop ->
        ParkRideCardPhase.Card(
            ParkRideCardStation(
                stationId = stop.id,
                stationName = stop.name,
                availability = ParkRideAvailabilityState.Unavailable,
            ),
        )
    } ?: ParkRideCardPhase.None,
    startStopId = start?.id,
    startName = start?.name,
    endStopId = end?.id,
    endName = end?.name,
    startFromParkRide = startFromParkRide,
    addedParkRide = added,
    usedSearch = usedSearch,
    saved = saved,
    selectedThemeId = themeId,
    tripDecision = decision,
)

/** The progress as the text that is saved. */
internal fun IntroProgress.toText(): String = Json.encodeToString(IntroProgress.serializer(), this)

/**
 * The progress saved in [text], or null when there is none, when it cannot be read, or when it is of a version this
 * build does not know, newer or older, so a value from another shape is never half used.
 */
internal fun introProgressFromText(text: String?): IntroProgress? {
    if (text.isNullOrEmpty()) return null
    val decoded = runCatching { Json.decodeFromString(IntroProgress.serializer(), text) }.getOrNull()
    return decoded?.takeIf { it.version == INTRO_PROGRESS_VERSION }
}
