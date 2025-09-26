package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
internal fun TagLineWithEmoji(
    tagline: String,
    emoji: String,
    emojiColor: Color? = null,
    tagColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 20.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = emoji,
            style = KrailTheme.typography.introTagline,
            color = emojiColor,
        )

        Text(
            text = tagline,
            style = KrailTheme.typography.introTagline,
            color = tagColor,
        )
    }
}
