package xyz.ksharma.krail.trip.planner.ui.state

import kotlinx.serialization.Serializable

/**
 * NSW Transport Key to icons and line codes
 * Color code resources - https://opendata.transport.nsw.gov.au/resources
 * Line Codes - https://transportnsw.info/plan/help/key-to-icons-line-codes
 *
 * Icons - https://opendata.transport.nsw.gov.au/dataset/transport-mode-symbols-and-pictograms
 * Symbols - https://opendata.transport.nsw.gov.au/data/dataset/transport-mode-symbols-and-pictograms/resource/ea95a4b5-7d7b-4196-b886-c834953d19b6?inner_span=True
 */
@Serializable
sealed class TransportMode {

    abstract val name: String
    abstract val colorCode: String
    abstract val productClass: Int

    /**
     * Priority to sort the transport modes in Search Results
     */
    abstract val priority: Int

    @Serializable
    data class Train(
        override val name: String = "Train",
        override val colorCode: String = "#F6891F",
        override val productClass: Int = 1,
        override val priority: Int = 1,
    ) : TransportMode()

    @Serializable
    data class Metro(
        override val name: String = "Metro",
        override val colorCode: String = "#009B77",
        override val productClass: Int = 2,
        override val priority: Int = 3,
    ) : TransportMode()

    @Serializable
    data class Ferry(
        override val name: String = "Ferry",
        override val colorCode: String = "#5AB031",
        override val productClass: Int = 9,
        override val priority: Int = 5,
    ) : TransportMode()

    @Serializable
    data class Bus(
        override val name: String = "Bus",
        override val colorCode: String = "#00B5EF",
        override val productClass: Int = 5,
        override val priority: Int = 2,
    ) : TransportMode()

    @Serializable
    data class LightRail(
        override val name: String = "Light Rail",
        override val colorCode: String = "#E4022D",
        override val productClass: Int = 4,
        override val priority: Int = 4,
    ) : TransportMode()

    @Serializable
    data class Coach(
        override val name: String = "Coach",
        override val colorCode: String = "#742282",
        override val productClass: Int = 7,
        override val priority: Int = 6,
    ) : TransportMode()

    /*@Serializable
    data class SchoolBus(
        override val name: String = "Bus",
        override val colorCode: String = "#F6891F",
        val productClass: Int = 11,
        val priority: Int = 7777,
    ) : TransportMode()*/

    companion object {
        private val productClassMap = listOf(
            Train(),
            Bus(),
            Metro(),
            LightRail(),
            Ferry(),
            Coach(),
        ).associateBy { it.productClass }

        fun toTransportModeType(productClass: Int): TransportMode? = productClassMap[productClass]

        fun values(): Set<TransportMode> = productClassMap.values.toSet()

        fun sortedValues(sortOrder: TransportModeSortOrder = TransportModeSortOrder.NAME): List<TransportMode> =
            when (sortOrder) {
                TransportModeSortOrder.NAME -> values().sortedBy { it.name }
                TransportModeSortOrder.PRIORITY -> values().sortedBy { it.priority }
                TransportModeSortOrder.PRODUCT_CLASS -> values().sortedBy { it.productClass }
            }
    }
}

enum class TransportModeSortOrder {
    NAME,
    PRIORITY,
    PRODUCT_CLASS,
}
