package xyz.ksharma.krail.core.transport.nsw

import xyz.ksharma.krail.core.transport.TransportMode

/**
 * NSW convenience aliases for [TransportMode].
 *
 * [TransportMode] is the canonical type everywhere in state and UI.
 * This object lets NSW-specific call sites (mappers, tests, previews) keep
 * `NswTransportMode.Train` syntax, which reads clearly and signals "NSW data".
 * [fromProductClass] is also here because product-class codes are a TfNSW concept.
 *
 * **Melbourne note**: colorCode / name baked into [TransportMode] are NSW values.
 * When Melbourne is added, create a `VicTransportMode` object with Vic data.
 * Extract a common interface *at that point* only if both city types must coexist
 * in the same runtime context (e.g., search results from multiple cities).
 */
object NswTransportMode {
    val Train get() = TransportMode.Train
    val Metro get() = TransportMode.Metro
    val Bus get() = TransportMode.Bus
    val LightRail get() = TransportMode.LightRail
    val Ferry get() = TransportMode.Ferry
    val Coach get() = TransportMode.Coach

    val all: List<TransportMode> get() = TransportMode.all

    fun fromProductClass(productClass: Int): TransportMode? =
        TransportMode.fromProductClass(productClass)

    /**
     * Resolves [TransportMode] from TfNSW API `transportation.iconId`.
     * More precise than [fromProductClass] — distinguishes Sydney Trains (iconId 1) from
     * NSW Trains (iconId 2/3), both of which share productClass = 1.
     * Returns null for modes without GTFS-RT coverage (coaches, school buses, private services).
     */
    @Suppress("MagicNumber")
    fun fromIconId(iconId: Long?): TransportMode? = when (iconId?.toInt()) {
        1, 19        -> Train      // Sydney Trains + temporary trains
        2, 3         -> Train      // Intercity + Regional Trains (NSW Trains feed)
        24           -> Metro
        13, 20, 21   -> LightRail // CBD & SE / temporary / Newcastle Light Rail
        4, 5, 6, 9,
        14, 15, 23,
        31, 32, 33,
        34, 35, 36,
        37, 38       -> Bus
        10, 11, 12,
        18           -> Ferry
        7, 22        -> Coach
        else         -> null
    }
}
