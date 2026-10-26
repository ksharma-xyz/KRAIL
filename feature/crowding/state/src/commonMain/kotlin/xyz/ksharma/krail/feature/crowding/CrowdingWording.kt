package xyz.ksharma.krail.feature.crowding

/**
 * What KRAIL says about seats. [sentence] sits above the carriage row; [tag] is the short form
 * beside a departure time. Both come from the same reading so they can never disagree.
 */
data class SeatAdvice(val sentence: String?, val tag: String)

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

    const val NIGHT_LINE = "It's late. Wait in the blue Nightsafe area to board near the guard."

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
     * The tag beside a departure time. Worked out from the cars when they are known, so the tag
     * and the sentence above the carriage row never disagree; otherwise from the whole-train
     * [level]. Null when there is nothing to say, which draws no tag.
     */
    fun tagFor(
        level: SeatLevel?,
        cars: CarriageLevels? = null,
        certainty: Certainty = Certainty.FORECAST,
        isNight: Boolean = false,
    ): String? {
        val fromCars = cars?.let { forCarriages(it, certainty, isNight)?.tag }
        val fallbackLevel = level ?: cars?.quietestZoneLevel()
        return fromCars ?: fallbackLevel?.let(::levelTag)
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
}
