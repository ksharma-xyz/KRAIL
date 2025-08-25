package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor

val CardShape = RoundedCornerShape(16.dp)

@Composable
fun Modifier.cardBorder(): Modifier = this.border(
    shape = CardShape,
    width = 1.dp,
    color = themeColor(),
)

@Composable
fun Modifier.cardBackground(): Modifier = this.background(
    shape = CardShape,
    color = themeBackgroundColor(),
)
