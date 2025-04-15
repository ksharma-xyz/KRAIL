package xyz.ksharma.krail.trip.planner.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_dev
import krail.feature.trip_planner.ui.generated.resources.ic_smile
import krail.feature.trip_planner.ui.generated.resources.ic_heart
import krail.feature.trip_planner.ui.generated.resources.ic_paint
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun SettingsScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onChangeThemeClick: () -> Unit = {},
    onReferFriendClick: () -> Unit = {},
    onAboutUsClick: () -> Unit = {},
    onIntroClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { Text(text = "Settings") },
            )
        }

        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(top = 20.dp, bottom = 104.dp),
        ) {
            item {
                SettingsItem(
                    icon = painterResource(Res.drawable.ic_paint),
                    text = "Change Theme",
                    onClick = {
                        onChangeThemeClick()
                    }
                )
            }

            item {
                SettingsItem(
                    icon = painterResource(Res.drawable.ic_heart),
                    text = "Invite your friends \uD83D\uDC95",
                    onClick = {
                        onReferFriendClick()
                    }
                )
            }

            item {
                SettingsItem(
                    icon = painterResource(Res.drawable.ic_heart),
                    text = "Intro to KRAIL",
                    onClick = onIntroClick,
                )
            }

            item {
                SettingsItem(
                    icon = painterResource(Res.drawable.ic_dev),
                    text = "KRAIL App Version: $appVersion",
                )
            }

            item {
                SettingsItem(
                    icon = painterResource(Res.drawable.ic_smile),
                    text = "About KRAIL",
                    onClick = {
                        onAboutUsClick()
                    }
                )
            }

        }
    }
}

@Composable
private fun SettingsItem(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val themeColor by LocalThemeColor.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .klickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            contentDescription = null,
            painter = icon,
            colorFilter = ColorFilter.tint(color = themeColor.hexToComposeColor()),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = text,
            style = KrailTheme.typography.bodyLarge,
        )
    }
    if (showDivider) {
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}
