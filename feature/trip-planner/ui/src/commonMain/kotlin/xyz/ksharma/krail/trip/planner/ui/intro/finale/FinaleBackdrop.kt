package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** How the theme colour is drawn: not at all, as a circle that is still growing, or as the whole screen. */
internal enum class FloodShape { NONE, CIRCLE, FULL }

/**
 * What the backdrop draws for a frame, as plain numbers so a table can hold it. The ground has its own opacity
 * ([groundAlpha], already counting the layer's). The flood is the theme colour as [flood], at [floodAlpha], and when it
 * is a circle it is [circleShare] of the way grown. A flood that has covered everything is a rectangle and not a circle
 * that only just reaches the corners, so no pixel is left to the edge of one.
 */
internal data class BackdropPlan(
    val groundAlpha: Float,
    val flood: FloodShape,
    val floodAlpha: Float,
    val circleShare: Float,
)

/** The plan for [frame]: the ground and the flood both take the layer's opacity, so the fade lifts them together. */
internal fun backdropPlan(frame: FinaleFrame): BackdropPlan = BackdropPlan(
    groundAlpha = frame.groundAlpha * frame.overlayAlpha,
    flood = when {
        frame.floodRadius >= 1f -> FloodShape.FULL
        frame.floodRadius > 0f -> FloodShape.CIRCLE
        else -> FloodShape.NONE
    },
    floodAlpha = frame.overlayAlpha,
    circleShare = frame.floodRadius,
)

// The ground, then the flood, from the plan.
internal fun DrawScope.drawBackdrop(frame: FinaleFrame, themeColour: Color, groundColour: Color) {
    val plan = backdropPlan(frame)
    if (plan.groundAlpha > 0f) drawRect(color = groundColour, alpha = plan.groundAlpha)
    when (plan.flood) {
        FloodShape.FULL -> drawRect(color = themeColour, alpha = plan.floodAlpha)
        FloodShape.CIRCLE -> drawCircle(
            color = themeColour,
            radius = floodRadiusPx(plan.circleShare, size.width, size.height, center.x, center.y),
            center = center,
            alpha = plan.floodAlpha,
        )
        FloodShape.NONE -> Unit
    }
}
