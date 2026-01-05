package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContainerColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun SheetTitleBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(end = 16.dp, start = 8.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            CompositionLocalProvider(
                LocalTextColor provides KrailTheme.colors.onSurface,
                LocalTextStyle provides KrailTheme.typography.headlineMedium,
            ) {
                title()
            }
        }
        actions?.let {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompositionLocalProvider(
                    LocalContainerColor provides KrailTheme.colors.onSurface,
                ) {
                    actions()
                }
            }
        }
    }
}
