package xyz.ksharma.krail.discover.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun DiscoverCardLabel(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(text, style) {
        textMeasurer.measure(
            AnnotatedString(text),
            style = style,
            maxLines = 2
        )
    }

    Layout(
        content = {
            Text(
                text = text,
                style = style,
                maxLines = 2,
                color = KrailTheme.colors.secondaryLabel,
            )
        },
        modifier = modifier
    ) { measurables, constraints ->
        // Use measured width, but allow height to wrap up to 2 lines
        val placeable = measurables.first().measure(
            Constraints(
                minWidth = 0,
                maxWidth = textLayoutResult.size.width,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
        )
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}