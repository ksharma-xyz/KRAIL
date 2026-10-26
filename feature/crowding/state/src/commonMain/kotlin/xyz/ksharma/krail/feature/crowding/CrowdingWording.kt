package xyz.ksharma.krail.feature.crowding

/**
 * What KRAIL says about seats. [sentence] sits above the carriage row; [tag] is the short form
 * beside a departure time. Both come from the same reading so they can never disagree.
 */
data class SeatAdvice(val sentence: String?, val tag: String)

/** A later service named the way the station board names it: line, clock time, platform. */
data class NextService(
    val line: String,
    val departureTime: String,
    val level: SeatLevel,
    val certainty: Certainty,
    val platform: String? = null,
)

/**
 * Every rider-facing crowding line. The wording table in
 * docs/investigations/CROWDING_DESIGN_REVIEW.md is the source of truth; a row there that is not
 * a test in CrowdingWordingTest is a bug.
 *
 * Rules carried here: "seats", never "room"; no car numbers; inform, never instruct; one
 * certainty word (live "has", forecast "expected" or "should", never "will").
 */
object CrowdingWording {

    /**
     * Gate on "at the back" and "at the front". Off until the platform check in
     * TFNSW_OCCUPANCY_DATA.md proves which end the feed's first car is. Turning it on is this
     * one line; the wording and tests already exist.
     */
    const val END_WORDING_ENABLED = false

    const val NIGHT_LINE = "It's late: wait in the blue Nightsafe area to board near the guard."

    private const val NIGHT_STARTS_AT_HOUR = 21
    private const val NIGHT_ENDS_AT_HOUR = 5

    /** After about 9pm KRAIL stops pointing riders at the emptiest car (NSW Police advice). */
    fun isNight(hourOfDay: Int): Boolean =
        hourOfDay >= NIGHT_STARTS_AT_HOUR || hourOfDay < NIGHT_ENDS_AT_HOUR

    /** The tag for a whole-train level, when there are no carriages to place the seats. */
    fun levelTag(level: SeatLevel): String = when (level) {
        SeatLevel.SEATS_AVAILABLE -> "Seats available"
        SeatLevel.FEW_SEATS -> "Few seats"
        SeatLevel.STANDING_ONLY -> "Standing only"
    }

    /**
     * Advice from a carriage row. Null when the only thing worth saying names an end and
     * [endWordingEnabled] is off: the caller then falls back to the whole-train [levelTag].
     */
    fun forCarriages(
        cars: CarriageLevels,
        certainty: Certainty,
        isNight: Boolean = false,
        endWordingEnabled: Boolean = END_WORDING_ENABLED,
    ): SeatAdvice? {
        val level = cars.quietestZoneLevel()
        val place = if (isNight) null else placeFor(cars.quietestZones(), endWordingEnabled)
        return when {
            isNight -> SeatAdvice(sentence = NIGHT_LINE, tag = levelTag(level))
            place == null -> null
            place == Place.ALL_ALONG -> SeatAdvice(allAlongSentence(level, certainty), levelTag(level))
            else -> SeatAdvice(zoneSentence(level, certainty, place), zoneTag(level, place))
        }
    }

    /**
     * "Want a seat? The next T1, at 8:59am from Platform 1, has seats available and is due at
     * Central at 9:26am." Named like the station board: line, clock time, platform. The service
     * gets the verb and the rider chooses. Arrival is "due at", never "by".
     */
    fun nextTrain(service: NextService, destination: String, arrivalTime: String): String {
        val from = service.platform?.let { " from $it" }.orEmpty()
        return "Want a seat? The next ${service.line}, at ${service.departureTime}$from, " +
            "${serviceHas(service)} and is due at $destination at $arrivalTime."
    }

    /** "Want a seat? The next 333, at 9:04am, has seats available." */
    fun nextBus(service: NextService): String =
        "Want a seat? The next ${service.line}, at ${service.departureTime}, ${serviceHas(service)}."

    private enum class Place(val words: String) {
        BACK("at the back"),
        FRONT("at the front"),
        BOTH_ENDS("at both ends"),
        MIDDLE("in the middle"),
        ALL_ALONG("all along"),
    }

    private fun placeFor(zones: Set<Zone>, endWordingEnabled: Boolean): Place? = when {
        zones.size == Zone.entries.size -> Place.ALL_ALONG
        zones == setOf(Zone.BACK, Zone.FRONT) -> Place.BOTH_ENDS
        // Middle plus one end: the middle is true either way and does not depend on the gate.
        Zone.MIDDLE in zones -> Place.MIDDLE
        !endWordingEnabled -> null
        zones == setOf(Zone.BACK) -> Place.BACK
        else -> Place.FRONT
    }

    private fun allAlongSentence(level: SeatLevel, certainty: Certainty): String =
        when (certainty) {
            Certainty.LIVE -> when (level) {
                SeatLevel.SEATS_AVAILABLE -> "Seats available all along the train."
                SeatLevel.FEW_SEATS -> "A few seats left along the train."
                SeatLevel.STANDING_ONLY -> "Standing only, all along the train."
            }
            Certainty.FORECAST -> when (level) {
                SeatLevel.SEATS_AVAILABLE -> "Seats expected all along the train."
                SeatLevel.FEW_SEATS -> "A few seats expected along the train."
                SeatLevel.STANDING_ONLY -> "Standing only expected all along the train."
            }
        }

    // A zone is only named when it is quieter than another, so it is never Standing only.
    private fun zoneSentence(level: SeatLevel, certainty: Certainty, place: Place): String {
        val seats = level == SeatLevel.SEATS_AVAILABLE
        return when (certainty) {
            Certainty.LIVE ->
                if (seats) "Seats ${place.words} of the train." else "A few seats left ${place.words}."
            Certainty.FORECAST ->
                if (seats) "Seats expected ${place.words}." else "A few seats expected ${place.words}."
        }
    }

    private fun zoneTag(level: SeatLevel, place: Place): String =
        if (level == SeatLevel.SEATS_AVAILABLE) "Seats ${place.words}" else "Few seats ${place.words}"

    private fun serviceHas(service: NextService): String {
        val seats = if (service.level == SeatLevel.SEATS_AVAILABLE) "seats available" else "a few seats left"
        return when (service.certainty) {
            Certainty.LIVE -> "has $seats"
            Certainty.FORECAST -> "should have $seats"
        }
    }
}
