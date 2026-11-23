package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * The style as it is when [weight] is null, and the same style at [weight] otherwise. It is how a card takes a heavier weight for a
 * surface that asks for one (`textWeight`) and stays exactly as it was for every surface that does not.
 */
internal fun TextStyle.withWeight(
    weight: FontWeight?,
): TextStyle = if (weight == null) this else copy(fontWeight = weight)
