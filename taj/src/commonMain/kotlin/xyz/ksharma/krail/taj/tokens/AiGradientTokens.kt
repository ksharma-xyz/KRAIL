package xyz.ksharma.krail.taj.tokens

import androidx.compose.ui.graphics.Color

/**
 * The one gradient used everywhere on-device AI output appears in the app — a single
 * consistent "AI touched this" signal, not a color invented per feature.
 *
 * Stops are real KRAIL brand values, not invented hex: three
 * `xyz.ksharma.krail.core.transport.TransportMode.colorCode` values plus
 * [xyz.ksharma.krail.taj.theme.KrailColors.magicYellow] (duplicated here rather than taking
 * a dependency on `:core:transport` from `:taj`; keep these in sync if either source changes).
 * An AI summary spans however many transport modes a trip actually uses, so a multi-modal
 * gradient fits the content, not just the branding.
 */
object AiGradientTokens {
    /** == TransportMode.Train.colorCode */
    val Train = Color(0xFFF6891F)

    /** == KrailColors.magicYellow */
    val MagicYellow = Color(0xFFFFC800)

    /** == TransportMode.Metro.colorCode */
    val Metro = Color(0xFF009B77)

    /** == TransportMode.Bus.colorCode */
    val Bus = Color(0xFF00B5EF)

    /** Ordered stops for both the wheel mark's stroke and the card's rotating border. */
    val stops: List<Color> = listOf(Train, MagicYellow, Metro, Bus)
}
