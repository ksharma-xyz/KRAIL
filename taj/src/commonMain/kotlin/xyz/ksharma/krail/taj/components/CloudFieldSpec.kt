package xyz.ksharma.krail.taj.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens

/**
 * How much the field moves, how fast, and what colours it is made of.
 *
 * [Ambient] is the wallpaper tuning: one hue, barely-there drift, meant to be noticed only if
 * you stare. A surface where the field IS the subject wants more travel and more than one hue,
 * and says so rather than the component guessing from context.
 */
@Immutable
data class CloudFieldSpec(
    /** Three colours, nearest blob first. `null` uses the theme colour and two tints of it. */
    val colors: List<Color>? = null,
    /** Multiplies how far each blob travels from its home position. */
    val motionScale: Float = 1f,
    /** Multiplies how fast it travels. Periods stay pairwise coprime as they scale. */
    val speedScale: Float = 1f,
    /** Multiplies peak alpha. Several saturated hues at once need less than one hue does. */
    val alphaScale: Float = 1f,
) {
    companion object {
        val Ambient = CloudFieldSpec()

        /**
         * The AI pair for the rider's theme, drifting.
         *
         * One factory rather than a spec built at each call site, because the three surfaces
         * that wear this field (the AI input, and both of SearchStop's panes) have to look
         * like the same weather. Only [motionScale] differs: a field that IS the screen can
         * travel further than one sitting behind a list of stops being read.
         */
        fun aiPair(
            themeColorHex: String,
            motionScale: Float = AI_PAIR_MOTION_SCALE,
        ): CloudFieldSpec = CloudFieldSpec(
            colors = AiThemeGradientTokens.stopsFor(themeColorHex),
            motionScale = motionScale,
            speedScale = AI_PAIR_SPEED_SCALE,
            alphaScale = AI_PAIR_ALPHA_SCALE,
        )
    }
}

// Alpha comes down as the hue count goes up: three saturated brand colours at the ambient peak
// fight each other and whatever text sits on top of them.
private const val AI_PAIR_MOTION_SCALE = 2.6f
private const val AI_PAIR_SPEED_SCALE = 1.5f
private const val AI_PAIR_ALPHA_SCALE = 0.85f
