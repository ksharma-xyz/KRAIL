package xyz.ksharma.krail.taj.theme

import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Structural guards on [KrailColors] that only show up at runtime, on a real theme switch.
 *
 * JVM-only because both rely on `kotlin.reflect`, which common code does not have.
 */
class KrailColorsParityTest {

    /**
     * No constructor parameter may have a default value.
     *
     * A defaulted parameter is the failure this test exists for: `KrailDarkColors` stops passing
     * it, the compiler is happy, and the field silently takes the light value in dark mode. Worse,
     * `animateKrailColors` builds the interpolated `KrailColors` by naming every field explicitly,
     * so a field that nobody remembers to add there freezes at its default and never animates —
     * and there is no compile error to notice, because the default fills the gap.
     */
    @Test
    fun `no KrailColors constructor parameter is optional`() {
        val optional = KrailColors::class.primaryConstructor
            ?.parameters
            .orEmpty()
            .filter { it.isOptional }
            .mapNotNull { it.name }

        assertTrue(
            optional.isEmpty(),
            "KrailColors parameters ${optional.joinToString()} have default values. Remove the " +
                "defaults: a defaulted colour can be omitted from KrailDarkColors and from " +
                "animateKrailColors without a compile error, so it silently keeps its light-mode " +
                "value and never animates across a theme switch.",
        )
    }

    /**
     * Light and dark must actually differ, field by field.
     *
     * A token that reads identically in both schemes is either intentional (and belongs in
     * [INTENTIONALLY_MODE_INVARIANT] with the reason) or is a copy-paste in `Color.kt` that leaves
     * dark mode wearing a light-mode colour.
     */
    @Test
    fun `light and dark differ on every field that is not deliberately shared`() {
        val identical = KrailColors::class.memberProperties
            .filter { it.get(KrailLightColors) == it.get(KrailDarkColors) }
            .map { it.name }
            .toSet()

        val unexplained = identical - INTENTIONALLY_MODE_INVARIANT.keys
        assertTrue(
            unexplained.isEmpty(),
            "${unexplained.joinToString()} is the same colour in light and dark mode. If that " +
                "is deliberate, add it to INTENTIONALLY_MODE_INVARIANT with the reason; " +
                "otherwise give it a dark-mode value in Color.kt.",
        )

        val nowDiffering = INTENTIONALLY_MODE_INVARIANT.keys - identical
        assertTrue(
            nowDiffering.isEmpty(),
            "${nowDiffering.joinToString()} now differs between light and dark, so its " +
                "INTENTIONALLY_MODE_INVARIANT entry is stale — delete it.",
        )
    }

    private companion object {

        /** Tokens that are the same in both schemes on purpose, and why. */
        val INTENTIONALLY_MODE_INVARIANT = mapOf(
            "badge" to "A pure-red unread dot. It reads as an alert in both schemes and is " +
                "never tinted by the theme.",
            "magicYellow" to "A fixed brand accent. Shifting it per scheme would make the same " +
                "gradient look like two different brands.",
            "scrim" to "Black in both schemes: a scrim dims whatever is behind it, and the " +
                "surface it dims is already scheme-appropriate.",
            "stopLabelSurface" to "Stop-label pills are deliberately dark in both schemes so " +
                "they read as one visual treatment regardless of theme — see Color.kt.",
            "onStopLabelSurface" to "The content colour for the above; light in both schemes " +
                "for the same reason.",
            "userLocationDot" to "A map marker drawn over map tiles, not over a theme surface, " +
                "so the scheme does not apply to it.",
        )
    }
}
