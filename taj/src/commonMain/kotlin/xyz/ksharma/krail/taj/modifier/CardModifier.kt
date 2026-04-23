package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.taj.tokens.ComponentTokens
import xyz.ksharma.krail.taj.tokens.StrokeTokens

val CardShape = RoundedCornerShape(ComponentTokens.CardCornerRadius)

@Composable
fun Modifier.cardBorder(): Modifier = this.border(
    shape = CardShape,
    width = StrokeTokens.Thin,
    color = themeColor(),
)

@Composable
fun Modifier.cardBackground(): Modifier = this.background(
    shape = CardShape,
    color = themeBackgroundColor(),
)
