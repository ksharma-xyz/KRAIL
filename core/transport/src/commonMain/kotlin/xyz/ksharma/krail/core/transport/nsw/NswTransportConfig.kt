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

    override fun modeFromProductClass(productClass: Int): TransportMode? =
        TransportMode.fromProductClass(productClass)

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
}
