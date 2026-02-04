package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContainerColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun Divider(
    modifier: Modifier = Modifier,
    type: DividerType = DividerType.HORIZONTAL,
    color: Color? = null,
) {
    CompositionLocalProvider(LocalContainerColor provides KrailTheme.colors.onSurface.copy(alpha = 0.2f)) {
        Box(
            modifier = modifier
                .then(
                    when (type) {
                        DividerType.HORIZONTAL ->
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)

                        DividerType.VERTICAL ->
                            Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                    },
                )
                .background(color = color ?: LocalContainerColor.current),
        )
    }
}

enum class DividerType {
    HORIZONTAL,
    VERTICAL,
}

@Preview
@Composable
private fun DividerHorizontalPreview() {
    PreviewTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(KrailTheme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Divider()
        }
    }
}

@Preview
@Composable
private fun DividerVerticalPreview() {
    PreviewTheme {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(KrailTheme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Divider(type = DividerType.VERTICAL)
        }
    }
}
