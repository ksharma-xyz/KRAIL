package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color? = null,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    fontFamily: FontFamily? = null,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    Text(
        text = AnnotatedString(text),
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
        fontFamily = fontFamily,
        inlineContent = inlineContent,
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color? = null,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    fontFamily: FontFamily? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    val contentAlpha = LocalContentAlpha.current
    val textStyle = style.merge(LocalTextStyle.current)
    val textColor: Color = color ?: LocalTextColor.current
        .takeIf { it != Color.Unspecified } ?: KrailTheme.colors.onSurface

    BasicText(
        text = text,
        style = textStyle.copy(
            color = textColor.copy(alpha = contentAlpha),
            textAlign = textAlign,
            fontFamily = fontFamily ?: textStyle.fontFamily,
        ),
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier,
        onTextLayout = onTextLayout,
        inlineContent = inlineContent,
    )
}

// region Previews

@ScreenshotTest
@Preview(name = "Typography")
@Composable
private fun TextPreview() {
    PreviewTheme {
        Text(text = "Typography")
        Text(text = "DisplayLarge", style = KrailTheme.typography.displayLarge)
        Text(text = "displayMedium", style = KrailTheme.typography.displayMedium)
        Text(text = "displaySmall", style = KrailTheme.typography.displaySmall)
    }
}

@Preview(showBackground = true)
@Composable
private fun TextWithColorPreview() {
    PreviewTheme {
        Column(modifier = Modifier.background(color = KrailTheme.colors.surface)) {
            Text(text = "Typography", color = KrailTheme.colors.error)
        }
    }
}

// endregion
