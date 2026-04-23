package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.tokens.IconSizeTokens

@Composable
fun SocialConnectionIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(IconSizeTokens.XXL)
            .clip(CircleShape)
            .klickable(
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
