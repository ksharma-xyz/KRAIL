package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.themeColor

@Composable
fun Modifier.themeBorder(): Modifier = this.border(
    shape = RoundedCornerShape(16.dp),
    width = 1.dp,
    color = themeColor()
)
