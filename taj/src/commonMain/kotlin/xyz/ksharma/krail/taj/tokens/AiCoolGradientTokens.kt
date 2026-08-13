package xyz.ksharma.krail.taj.tokens

import androidx.compose.ui.graphics.Color

/**
 * A second AI gradient, distinct from [AiGradientTokens]. [AiGradientTokens] signals
 * "this text was generated from these transport modes" (the alert-summary card); this one
 * signals "you are actively talking to KRAIL's AI right now" — the search-input entry
 * point, listening/thinking states, and their confirm step. Fixed regardless of the
 * rider's selected [xyz.ksharma.krail.taj.theme.KrailThemeStyle], the same way Gemini's and
 * Copilot's gradients stay fixed across every host surface they appear in rather than
 * re-tinting to match. Whether these two AI gradients should eventually unify into one is
 * still open — see `docs/investigations/AI_SEARCH_INPUT_MODE.md`.
 */
object AiCoolGradientTokens {
    val Blue = Color(0xFF2D7FFF)
    val Violet = Color(0xFF8B5CF6)
    val Pink = Color(0xFFFF2D9E)

    /** Ordered stops for the wheel mark's stroke and any matching rotating border. */
    val stops: List<Color> = listOf(Blue, Violet, Pink)
}
