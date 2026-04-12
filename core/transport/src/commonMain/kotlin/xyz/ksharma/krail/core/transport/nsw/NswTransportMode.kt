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
}
