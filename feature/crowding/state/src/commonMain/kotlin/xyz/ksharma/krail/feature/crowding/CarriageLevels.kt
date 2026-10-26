package xyz.ksharma.krail.feature.crowding

/** A third of the train. Riders find a spot on a platform this way, never by car number. */
enum class Zone { BACK, MIDDLE, FRONT }

/**
 * One level per carriage, in feed order, which is drawn left to right as back to front.
 *
 * Which real end the feed's first car is has not been checked on a platform yet
 * (TFNSW_OCCUPANCY_DATA.md, "Platform check"), so no wording may name an end while the
 * end-wording gate is off. "Both ends" and "middle" are symmetric and
 * safe either way.
 */
data class CarriageLevels(val levels: List<SeatLevel>) {

    val size: Int get() = levels.size

    /**
     * The busiest car in each zone. Null under [MIN_CARS_FOR_ZONES], where zones mean nothing.
     *
     * An end is the end car, not a third: one car per end up to 11 cars (8 cars split 1/6/1,
     * 4 split 1/2/1). Equal thirds were tried first and failed on a real 26 October reading,
     * where only the two end cars of a Waratah had seats: thirds pair each end car with a fuller
     * neighbour and the train read "a few seats along the train", hiding exactly the pattern the
     * field results support. Ends stay the same size so "both ends" is symmetric.
     */
    fun zoneLevels(): Map<Zone, SeatLevel>? {
        if (size < MIN_CARS_FOR_ZONES) return null
        val end = maxOf(1, size / CARS_PER_END_CAR)
        return mapOf(
            Zone.BACK to levels.subList(0, end).max(),
            Zone.MIDDLE to levels.subList(end, size - end).max(),
            Zone.FRONT to levels.subList(size - end, size).max(),
        )
    }

    /** The zones whose busiest car is quietest. All three when the train is even. */
    fun quietestZones(): Set<Zone> {
        val zones = zoneLevels() ?: return Zone.entries.toSet()
        val best = zones.values.min()
        return zones.filterValues { it == best }.keys
    }

    /** The level the rider gets by walking to the quietest zone. */
    fun quietestZoneLevel(): SeatLevel = zoneLevels()?.values?.min() ?: levels.max()

    companion object {
        private const val MIN_CARS_FOR_ZONES = 3
        private const val CARS_PER_END_CAR = 6

        /**
         * Builds the row from `position to level` pairs as the feed sends them.
         *
         * Sydney Trains numbers cars from 1 and Metro from 0, so positions are only used to sort
         * and the row is re-sequenced from that order. A train reports every car or none, so a
         * car without a level means the reading is incomplete and the whole row is dropped.
         */
        fun fromPositions(byPosition: Map<Int, SeatLevel?>): CarriageLevels? {
            if (byPosition.isEmpty()) return null
            val ordered = byPosition.entries.sortedBy { it.key }.map { it.value ?: return null }
            return CarriageLevels(ordered)
        }
    }
}
