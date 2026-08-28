package xyz.ksharma.krail.core.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Guards the one invariant three separate features assume: every [TransportMode] owns a
 * distinct [TransportMode.productClass].
 *
 * Nothing enforces it today. `TransportMode.byProductClass` is built with `associateBy`, so a
 * duplicate class does not fail, it **silently overwrites** - eight modes become a seven-entry
 * map and [TransportMode.fromProductClass] starts returning the wrong mode for the loser.
 * That function is how every TfNSW response is turned into a mode, so the damage is quiet and
 * app-wide: wrong icon, wrong colour and wrong name on journey cards, mode filters selecting
 * the wrong chip, product-class analytics attributed to the wrong mode, and the mode-filter
 * `LazyRow` in `TimeTableScreen` crashing on a duplicate key.
 *
 * This is a live footgun rather than a theoretical one: [TransportMode.SchoolBus] already
 * shares both its colour and its `name` with [TransportMode.Bus] and differs only by product
 * class, so adding a mode by copying that block is one forgotten edit away from a collision.
 */
class TransportModeProductClassTest {

    @Test
    fun `every transport mode has a distinct product class`() {
        val productClasses = TransportMode.all.map { it.productClass }

        assertEquals(
            productClasses.size,
            productClasses.distinct().size,
            "Duplicate productClass in TransportMode.all: " +
                "${productClasses.groupBy { it }.filterValues { it.size > 1 }.keys}. " +
                "byProductClass would silently drop a mode.",
        )
    }

    @Test
    fun `every mode is reachable from its own product class`() {
        // The `associateBy` round trip. Fails for the overwritten mode if two ever collide.
        TransportMode.all.forEach { mode ->
            val resolved = TransportMode.fromProductClass(mode.productClass)

            assertNotNull(resolved, "No mode resolves for productClass ${mode.productClass}")
            assertSame(mode, resolved, "productClass ${mode.productClass} resolves to $resolved")
        }
    }

    @Test
    fun `an unknown product class resolves to nothing rather than a wrong mode`() {
        assertEquals(null, TransportMode.fromProductClass(productClass = -1))
    }

    @Test
    fun `school bus stays distinct from bus despite sharing a colour and name`() {
        // The pair most likely to be collapsed by a copy-paste edit.
        assertEquals(TransportMode.Bus.name, TransportMode.SchoolBus.name)
        assertEquals(TransportMode.Bus.colorCode, TransportMode.SchoolBus.colorCode)

        assertSame(TransportMode.Bus, TransportMode.fromProductClass(TransportMode.Bus.productClass))
        assertSame(
            TransportMode.SchoolBus,
            TransportMode.fromProductClass(TransportMode.SCHOOL_BUS_PRODUCT_CLASS),
        )
    }
}
