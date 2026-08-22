package xyz.ksharma.krail.taj.tokens

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `KrailThemeStyle.aiGradientPartner` being a constructor parameter means a new theme cannot
 * compile without one. It cannot make the choice a *good* one, which is what this measures.
 *
 * The rule the partner exists to satisfy was written in prose in `AiThemeGradientTokens`' KDoc and
 * checked by nobody: under roughly 40 degrees of hue apart the two ends blend into one muddy band,
 * and too far apart they stop reading as related.
 *
 * That KDoc also claimed the shipped pairs sit between 97 and 134 degrees. They do not, and writing
 * this test is how that surfaced. The real spread is 96.8 (Purple Drip into Bus) to 147.5 (Bus into
 * Magic Yellow, the deliberate cool-to-warm crossing). The window below is set from the design rule
 * rather than from that prose: a floor comfortably clear of the muddy zone, and a ceiling just past
 * the widest pair anyone chose on purpose.
 *
 * Self-healing over the enum, so a seventh theme is measured the day it is added and fails with the
 * angle it actually chose rather than with "you forgot something".
 */
class ThemeGradientPairTest {

    @Test
    fun `every theme's gradient partner sits far enough around the wheel`() {
        KrailThemeStyle.entries.forEach { style ->
            val gap = hueDistance(style.hexColorCode.hexToComposeColor(), style.aiGradientPartner)

            assertTrue(
                gap in MIN_PARTNER_HUE..MAX_PARTNER_HUE,
                "${style.name}: its gradient partner is ${gap.toInt()} degrees away. Under " +
                    "${MIN_PARTNER_HUE.toInt()} the two ends blend into one muddy band; over " +
                    "${MAX_PARTNER_HUE.toInt()} they stop reading as related. Pick a different " +
                    "KRAIL colour rather than widening this window.",
            )
        }
    }

    @Test
    fun `a theme is never its own partner`() {
        KrailThemeStyle.entries.forEach { style ->
            assertTrue(
                style.aiGradientPartner != style.hexColorCode.hexToComposeColor(),
                "${style.name} partners itself, so its gradient has nowhere to travel.",
            )
        }
    }

    /**
     * The gradient's middle stop exists to bend the path around the dull zone a straight blend
     * passes through. If it ever came back as one of the ends, the bend is gone.
     */
    @Test
    fun `the middle stop is distinct from both ends`() {
        KrailThemeStyle.entries.forEach { style ->
            val stops = AiThemeGradientTokens.stopsFor(style)

            assertEquals(
                STOP_COUNT,
                stops.size,
                "${style.name}: expected $STOP_COUNT gradient stops, got ${stops.size}.",
            )
            assertTrue(
                stops[1] != stops[0] && stops[1] != stops[2],
                "${style.name}: the middle stop equals one of the ends, so the gradient is a " +
                    "straight blend again.",
            )
        }
    }

    /**
     * A hex the enum does not know can only come from persisted preferences written by an older
     * build, so falling back is right. Drawing nothing, or crashing, would not be.
     */
    @Test
    fun `an unknown theme hex falls back to a usable gradient`() {
        val stops = AiThemeGradientTokens.stopsFor("#FF123456")

        assertEquals(STOP_COUNT, stops.size)
        assertTrue(
            stops.toSet().size == STOP_COUNT,
            "The fallback gradient collapsed to fewer than $STOP_COUNT distinct colours.",
        )
    }

    private fun hueDistance(first: Color, second: Color): Float {
        val raw = abs(hueOf(first) - hueOf(second))
        return if (raw > HALF_TURN) FULL_TURN - raw else raw
    }

    private fun hueOf(color: Color): Float {
        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        val chroma = max - min
        if (chroma == 0f) return 0f

        val hue = when (max) {
            color.red -> SECTOR * (((color.green - color.blue) / chroma) % SECTORS)
            color.green -> SECTOR * (((color.blue - color.red) / chroma) + GREEN_OFFSET)
            else -> SECTOR * (((color.red - color.green) / chroma) + BLUE_OFFSET)
        }
        return (hue + FULL_TURN) % FULL_TURN
    }

    private companion object {
        /** Well clear of the ~40 degree muddy zone, and under the tightest shipped pair at 96.8. */
        const val MIN_PARTNER_HUE = 90f

        /** Just past Bus into Magic Yellow at 147.5, the widest pair chosen deliberately. */
        const val MAX_PARTNER_HUE = 150f
        const val STOP_COUNT = 3

        const val FULL_TURN = 360f
        const val HALF_TURN = 180f
        const val SECTOR = 60f
        const val SECTORS = 6f
        const val GREEN_OFFSET = 2f
        const val BLUE_OFFSET = 4f
    }
}
