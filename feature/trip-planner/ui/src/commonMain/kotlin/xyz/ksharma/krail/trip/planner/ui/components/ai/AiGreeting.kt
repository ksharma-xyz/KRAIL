package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.HOME_STOP_LABEL
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelSynonyms
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.time.Clock

/**
 * The one line above the field: a journey this rider could actually ask for, right now.
 *
 * There used to be six hand-written greetings ("Morning.", "Still out?") with an example under
 * each. That was two lines saying two different things on a screen whose field already says
 * `Ask KRAIL`, and the greetings carried no information: the rider knows what time it is.
 *
 * So the copy is gone and the variety moved to where it can be true. The line is built from the
 * rider's own stops plus the calendar, which means it changes across a day and across a week
 * without anybody writing six moods, and every version of it is a sentence they could say back.
 *
 * Ordered by how much each source proves the app knows them:
 *
 * 1. **Their labels, in their own words** — `Home to Work`. This is the only place the app ever
 *    shows that typing "work" resolves to their stop; nobody finds that on their own.
 * 2. **A saved trip, as real station names** — `Seven Hills to Wynyard`. Their trip, so the
 *    stations are ones they recognise.
 * 3. **Two known stations** — the first-launch case, where anything personal would be invented.
 *
 * Recents used to sit between 2 and 3. The parameter existed, `SavedTripsScreen` never passed
 * anything to it, and the rung never ran. It is gone rather than left looking implemented.
 */
internal data class AiGreeting(val suggestion: String)

// The rider's word for a place they go on a schedule. Excluded at weekends outright: suggesting
// a commute on a Sunday is the app telling someone it has not been paying attention. Matched
// case-insensitively against the label the rider typed, so a free-text "Gym" or "Beach" is
// never caught by it.
//
// Taken from LabelSynonyms rather than listed again, so a label named "Office" counts as the
// commute for the weekend rule exactly as "Work" does. Two lists would have drifted the moment
// one of them gained a word.
private val COMMUTE_LABELS = LabelSynonyms.commuteLabels

// The label is home by definition here, so the sentence says home rather than quoting whatever
// casing the rider typed. "Get me home" is also what somebody would actually say out loud.
private const val GET_ME_HOME = "Get me home"

private const val DEFAULT_FROM = "Central"
private const val DEFAULT_TO = "Parramatta"

// A weekend rider with nothing but commute data gets somewhere people actually go at a weekend,
// rather than their own commute handed back to them on a Sunday.
private const val WEEKEND_DEFAULT_TO = "Bondi Junction"

// Two NSW station names and a time clause can run past two lines in the fixed-height slot, and
// the slot cannot grow without moving the field. Over the cap the clause goes first, then the
// whole rung.
private const val LONGEST_LINE = 38

private const val STATION_SUFFIX = " Station"

/**
 * The suggestion for a given moment, as a pure function so it can be checked without a device.
 */
internal fun suggestionFor(
    hour: Int,
    isWeekend: Boolean,
    labels: List<StopLabel>,
    savedTrips: List<Trip>,
    isSunday: Boolean = false,
): String {
    val situation = situationFor(hour = hour, isWeekend = isWeekend, isSunday = isSunday)

    val pair = labelPair(labels, situation.allowsCommute)
        ?: eligibleTrip(savedTrips, labels, situation.allowsCommute)?.let { trip ->
            trip.fromStopName.withoutStationSuffix() to trip.toStopName.withoutStationSuffix()
        }

    // Home-bound because the situation asks for it, or because the day's rules disqualified
    // everything the rider has.
    //
    // That second case is the one that matters. A rider whose only data IS the commute used to
    // fall straight past the weekend rules to two unrelated stations, so every Saturday and
    // Sunday showed strangers' stops. Falling back to the commute instead would have undone
    // the weekend rule; falling back to home uses what they have, is true whenever they are
    // out, and is the only line that shows the origin can be left out at all, since the
    // nearby-stop resolver fills it from where they are.
    val home = homeLabel(labels)
    if ((situation.shape == SuggestionShape.HomeBound || pair == null) && home != null) {
        return "$GET_ME_HOME${situation.clause}"
    }

    val resolved = pair ?: defaultPair(isWeekend)

    val (from, to) =
        if (situation.direction == Direction.Back) resolved.second to resolved.first else resolved

    // Longest first, then what to give up. The clause goes before the stops do, because a
    // journey with no time still describes their journey and a time with the wrong stops does
    // not describe anything.
    val fallbacks = listOf(
        "$from to $to${situation.clause}",
        "$from to $to",
        "$DEFAULT_FROM to $DEFAULT_TO",
    )
    return fallbacks.firstOrNull { it.length <= LONGEST_LINE } ?: "$DEFAULT_FROM to $DEFAULT_TO"
}

private fun defaultPair(isWeekend: Boolean): Pair<String, String> =
    DEFAULT_FROM to if (isWeekend) WEEKEND_DEFAULT_TO else DEFAULT_TO

/**
 * The first saved trip that is not the rider's commute in disguise.
 *
 * Excluding commute LABELS at weekends was not enough, and this is the hole it left: a rider
 * whose only saved trip is the trip to work saw exactly that suggested on a Sunday, because the
 * label rung was skipped and the saved-trip rung had no opinion about the day. The exclusion has
 * to follow the stops, not the words, since the same two platforms are the commute whether or
 * not anybody has labelled them.
 */
private fun eligibleTrip(savedTrips: List<Trip>, labels: List<StopLabel>, allowsCommute: Boolean): Trip? {
    if (allowsCommute) return savedTrips.firstOrNull()

    val commuteStopIds = labels
        .filter { it.isSet && it.label.lowercase() in COMMUTE_LABELS }
        .mapNotNull { it.stopId }
        .toSet()

    return savedTrips.firstOrNull { trip ->
        trip.fromStopId !in commuteStopIds && trip.toStopId !in commuteStopIds
    }
}

/**
 * Home plus one other place the rider has named, in their own words.
 *
 * Both halves have to be `isSet`. A label exists from the moment it is created, before any stop
 * is pinned to it, and suggesting "Home to Work" to a rider for whom typing "work" resolves to
 * nothing would be the app teaching a trick it cannot do.
 *
 * On a weekday the commute label wins if there is one, because that is the trip being taken. At
 * a weekend commute labels are dropped entirely and any other named place is used instead, which
 * is how "Home to Gym" turns up on a Saturday without anybody writing a Saturday template.
 */
private fun labelPair(labels: List<StopLabel>, allowsCommute: Boolean): Pair<String, String>? {
    val set = labels.filter { it.isSet }
    val home = set.firstOrNull { it.label.equals(HOME_STOP_LABEL, ignoreCase = true) }

    val others = set.filterNot { it.label.equals(HOME_STOP_LABEL, ignoreCase = true) }
    val eligible = if (allowsCommute) {
        others.sortedByDescending { it.label.lowercase() in COMMUTE_LABELS }
    } else {
        others.filterNot { it.label.lowercase() in COMMUTE_LABELS }
    }
    val other = eligible.firstOrNull()

    return if (home != null && other != null) home.label to other.label else null
}

/** Whether the rider has a home pinned to a stop, which is all the home-bound line needs. */
private fun homeLabel(labels: List<StopLabel>): String? =
    labels.firstOrNull { it.isSet && it.label.equals(HOME_STOP_LABEL, ignoreCase = true) }?.label

/**
 * `Seven Hills Station` reads as `Seven Hills`. Only the trailing word goes: everything else in
 * a NSW stop name is doing work, including the `Light Rail` and `Wharf` suffixes that tell two
 * otherwise identical names apart.
 */
private fun String.withoutStationSuffix(): String = removeSuffix(STATION_SUFFIX)

/**
 * The suggestion for right now.
 *
 * Remembered on the hour rather than per recomposition: nothing here changes inside one, and
 * re-reading the clock while a rider types would be work for nothing.
 */
@Composable
internal fun rememberAiGreeting(
    stopLabels: List<StopLabel>,
    savedTrips: List<Trip>,
): AiGreeting {
    val now = Clock.System.now().toLocalDateTime(TimeZone.of(SYDNEY))
    val hour = now.hour
    val isSunday = now.dayOfWeek == DayOfWeek.SUNDAY
    val isWeekend = isSunday || now.dayOfWeek == DayOfWeek.SATURDAY

    return remember(hour, isWeekend, isSunday, stopLabels, savedTrips) {
        AiGreeting(
            suggestion = suggestionFor(
                hour = hour,
                isWeekend = isWeekend,
                labels = stopLabels,
                savedTrips = savedTrips,
                isSunday = isSunday,
            ),
        )
    }
}

// Sydney, not the device zone, for the same reason the time resolver fixes it: this is a
// Sydney app, and a traveller's phone still on another clock would get the wrong day entirely.
private const val SYDNEY = "Australia/Sydney"
