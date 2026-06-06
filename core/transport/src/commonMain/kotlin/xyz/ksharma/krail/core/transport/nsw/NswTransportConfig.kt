package xyz.ksharma.krail.core.transport.nsw

import xyz.ksharma.krail.core.transport.TransportConfig
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.TransportModeSortOrder

/**
 * NSW implementation of [TransportConfig].
 *
 * [TransportMode] carries its own [TransportMode.colorCode] and [TransportMode.name],
 * so [colorFor] and [nameFor] simply delegate. [productClassFor] reads [TransportMode.productClass].
 */
object NswTransportConfig : TransportConfig {

    // School Bus (class 11) is not a TransportMode subclass — it shares Bus UI and API behaviour.
    // Mapping it here means all callers get TransportMode.Bus automatically; no per-callsite guard needed.
    // Add future product-class aliases here if another class maps to an existing mode.
    override fun modeFromProductClass(productClass: Int): TransportMode? =
        if (productClass == TransportMode.SCHOOL_BUS_PRODUCT_CLASS) {
            TransportMode.Bus
        } else {
            TransportMode.fromProductClass(productClass)
        }

    override fun colorFor(mode: TransportMode): String = mode.colorCode

    override fun nameFor(mode: TransportMode): String = mode.name

    override fun productClassFor(mode: TransportMode): Int = mode.productClass

    override fun sortedModes(order: TransportModeSortOrder): List<TransportMode> =
        when (order) {
            TransportModeSortOrder.NAME -> TransportMode.all.sortedBy { it.name }
            TransportModeSortOrder.PRIORITY -> TransportMode.all.sortedBy { it.priority }
            TransportModeSortOrder.PRODUCT_CLASS -> TransportMode.all.sortedBy { it.productClass }
        }

    override fun allProductClasses(): Set<Int> =
        TransportMode.all.map { it.productClass }.toSet()

    override fun lineColor(lineKey: String): String? =
        NswTransportLine.entries.firstOrNull { it.key == lineKey }?.hexColor

    /**
     * Resolves the human-readable direction label shown on departure boards and leg views.
     *
     * NSW convention:
     *  - **Train / Metro**: `"towards <destination>"` — the API [description] is the full
     *    route label (e.g. "Emu Plains or Richmond to City") which is unhelpful on a board;
     *    [destinationName] (the actual terminus) is far more useful.
     *  - **All other modes**: [description] already contains a concise route label
     *    (e.g. "Seven Hills to Rouse Hill Station via Norwest") — use it directly.
     *
     * @param productClass NSW product class int (1 = Train, 2 = Metro, 5 = Bus, etc.).
     * @param destinationName The terminus stop name from `transportation.destination.name`.
     * @param description     The route description from `transportation.description`.
     * @return A display-ready string, or null if both inputs are null.
     */
    fun resolveServiceDisplayText(
        productClass: Int?,
        destinationName: String?,
        description: String?,
    ): String? = when (productClass) {
        TransportMode.Train.productClass,
        TransportMode.Metro.productClass,
        -> destinationName?.let { "towards $it" } ?: description
        else -> description ?: destinationName
    }
}
