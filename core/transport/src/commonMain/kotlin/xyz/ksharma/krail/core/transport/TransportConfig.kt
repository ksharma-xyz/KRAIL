package xyz.ksharma.krail.core.transport

/**
 * City-agnostic contract for resolving all transport display data from raw API codes.
 *
 * Each supported city provides its own implementation:
 *   - NSW  → [xyz.ksharma.krail.core.transport.nsw.NswTransportConfig]
 *   - (future) Melbourne → VicTransportConfig
 */
interface TransportConfig {

    /** Returns the [TransportMode] matching an API product/route-type code, or null. */
    fun modeFromProductClass(productClass: Int): TransportMode?

    /** Hex colour for the given mode in this city's branding. */
    fun colorFor(mode: TransportMode): String

    /** Human-readable name for the given mode in this city (e.g. "Train", "Tram"). */
    fun nameFor(mode: TransportMode): String

    /** API product/route-type code for the given mode in this city. */
    fun productClassFor(mode: TransportMode): Int

    /** All modes supported by this city, sorted by [order]. */
    fun sortedModes(order: TransportModeSortOrder = TransportModeSortOrder.PRIORITY): List<TransportMode>

    /** All API product-class codes supported by this city. */
    fun allProductClasses(): Set<Int>

    /** Hex colour for a named line key (e.g. "T1", "F2"), or null if not in this city's catalogue. */
    fun lineColor(lineKey: String): String?
}
