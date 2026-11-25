package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import xyz.ksharma.krail.taj.theme.KrailTheme

private const val NAME_AND_FIGURES = 3

/**
 * One car park's text with its [action] at the end edge, centred on the height of the text.
 *
 * The text is four whole items: [name], [spots], [percentFull] and [updatedAt], read in that order. The two figures share a
 * line when both fit on it and take a line each when they do not. The action stands beside the text only while every item
 * fits next to it whole. Otherwise it goes under the text, so no item is ever broken to make room for it.
 *
 * [name], [spots], [percentFull] and [action] each draw exactly one thing. [updatedAt] may draw nothing.
 */
@Composable
internal fun CarParkWithAction(
    name: @Composable () -> Unit,
    spots: @Composable () -> Unit,
    percentFull: @Composable () -> Unit,
    updatedAt: @Composable () -> Unit,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Layout(
        contents = listOf(
            {
                name()
                spots()
                percentFull()
                updatedAt()
            },
            action,
        ),
        modifier = modifier,
    ) { (textContent, actionContent), constraints ->
        val (nameItem, spotsMeasurable, percentMeasurable) = textContent
        val updatedContent = textContent.drop(NAME_AND_FIGURES)
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val button = actionContent.first().measure(loose)
        val gap = dim.spacingL.roundToPx()
        val lineGap = dim.spacingXS.roundToPx()
        val figureGap = dim.spacingML.roundToPx()

        val widestItem = textContent.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val roomBeside = constraints.maxWidth - button.width - gap
        val beside = widestItem <= roomBeside
        val textConstraints = loose.copy(maxWidth = if (beside) roomBeside else constraints.maxWidth)

        val spotsItem = spotsMeasurable.measure(textConstraints)
        val percentItem = percentMeasurable.measure(textConstraints)
        val figuresShareALine = spotsItem.width + figureGap + percentItem.width <= textConstraints.maxWidth
        val figureLines: List<List<Placeable>> =
            if (figuresShareALine) {
                listOf(
                    listOf(spotsItem, percentItem),
                )
            } else {
                listOf(listOf(spotsItem), listOf(percentItem))
            }
        val lines: List<List<Placeable>> =
            listOf(listOf(nameItem.measure(textConstraints))) +
                figureLines +
                updatedContent.map { listOf(it.measure(textConstraints)) }

        val lineHeights = lines.map { line -> line.maxOf { it.height } }
        val textHeight = lineHeights.sum() + lineGap * (lines.size - 1)
        val height = if (beside) maxOf(textHeight, button.height) else textHeight + lineGap + button.height

        layout(constraints.maxWidth, height) {
            var y = if (beside) (height - textHeight) / 2 else 0
            lines.forEachIndexed { index, line ->
                var x = 0
                line.forEach { item ->
                    item.placeRelative(x, y)
                    x += item.width + figureGap
                }
                y += lineHeights[index] + lineGap
            }
            if (beside) {
                button.placeRelative(constraints.maxWidth - button.width, (height - button.height) / 2)
            } else {
                button.placeRelative(0, textHeight + lineGap)
            }
        }
    }
}
