@file:Suppress("MagicNumber")

package xyz.ksharma.krail.core.transport

import kotlinx.serialization.Serializable

private const val BUS_BLUE = "#00B5EF"

/**
 * Transport mode with all display and lookup data baked in.
 *
 * The current set covers TfNSW. When a new city is added (e.g. Melbourne), create a
 * separate `VicTransportMode` sealed class and extract a common interface at that point.
 * Don't over-engineer now — YAGNI.
 *
 * Note on naming: [productClass] is TfNSW terminology for the API product-type code.
 */
@Serializable
sealed class TransportMode(
    val colorCode: String,
    /** Single-word name used for the icon letter (e.g. "Bus" → "B"). */
    val name: String,
    val productClass: Int,
    val priority: Int,
    val searchPriority: Int,
    /** Human-readable label shown in filter chips and UI strings. Defaults to [name]. */
    val displayName: String = name,
) {
    open val iconBorderColorCode: String get() = "#FFFFFF"

    open val iconLetterColorCode: String get() = "#FFFFFF"

    @Serializable object Train : TransportMode("#F6891F", "Train", 1, 1, 1)

    @Serializable object Metro : TransportMode("#009B77", "Metro", 2, 3, 2)

    @Serializable object Bus : TransportMode(BUS_BLUE, "Bus", 5, 2, 6)

    @Serializable object LightRail : TransportMode("#E4022D", "Light Rail", 4, 4, 3)

    @Serializable object Ferry : TransportMode("#5AB031", "Ferry", 9, 5, 4)

    @Serializable object Coach : TransportMode("#742282", "Coach", 7, 6, 5)

    // Same icon as Bus — differentiated by displayName and the "School service" leg label.
    @Serializable object SchoolBus : TransportMode(
        colorCode = BUS_BLUE,
        name = "Bus",
        productClass = 11,
        priority = 7,
        searchPriority = 7,
        displayName = "School Bus",
    )

    companion object {
        const val SCHOOL_BUS_PRODUCT_CLASS = 11

        val all: List<TransportMode> by lazy { listOf(Train, Metro, Bus, LightRail, Ferry, Coach, SchoolBus) }

        private val byProductClass: Map<Int, TransportMode> by lazy { all.associateBy { it.productClass } }

        fun fromProductClass(productClass: Int): TransportMode? = byProductClass[productClass]
    }
}
