package xyz.ksharma.krail.trip.planner.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_heart
import krail.feature.trip_planner.ui.generated.resources.ic_info
import krail.feature.trip_planner.ui.generated.resources.ic_paint
import krail.feature.trip_planner.ui.generated.resources.ic_pen
import krail.feature.trip_planner.ui.generated.resources.ic_wifi
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.social.SocialConnectionRow
import xyz.ksharma.krail.core.social.model.SocialType
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.AppLogo

@Composable
fun SettingsScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onChangeThemeClick: () -> Unit = {},
    onReferFriendClick: () -> Unit = {},
    onAboutUsClick: () -> Unit = {},
    onIntroClick: () -> Unit = {},
    onSocialLinkClick: (SocialType) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { Text(text = "Settings") },
            )

            LazyColumn(
                modifier = Modifier,
                contentPadding = PaddingValues(bottom = 104.dp),
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
                    Text(
                        text = "About",
                        style = KrailTheme.typography.title,
                        modifier = Modifier
                            .padding(top = 24.dp, bottom = 16.dp)
                            .padding(horizontal = 26.dp),
                    )
                }

                item {
                    SettingsItem(
                        icon = painterResource(Res.drawable.ic_info),
                        text = "How to KRAIL?",
                        onClick = onIntroClick,
                    )
                }

                item {
                    SettingsItem(
                        icon = painterResource(Res.drawable.ic_pen),
                        text = "Our story",
                        onClick = { onAboutUsClick() }
                    )
                }

                item {
                    SettingsItem(
                        icon = painterResource(Res.drawable.ic_wifi),
                        text = "Stay connected",
                        detailContent = {
                            SocialConnectionRow(
                                modifier = Modifier
                                    .padding(start = (24 + 16 + 6).dp, end = 16.dp),
                                onClick = { socialType -> onSocialLinkClick(socialType) }
                            )
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.fillMaxWidth().height(108.dp))
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .background(KrailTheme.colors.surface)
                .navigationBarsPadding()
                .padding(bottom = 10.dp, top = 16.dp),
        ) {
            AppLogo()
            Text(
                text = "v$appVersion",
                style = KrailTheme.typography.bodyLarge,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/*
@Composable
private fun SocialConnectionBox(
    type: SocialType,
    onSocialLinkClick: (SocialType) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .klickable(
                indication = null,
                onClick = {
                    onSocialLinkClick(type)
                },
            )
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}*/

@Composable
private fun SettingsItem(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    detailContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val themeColor by LocalThemeColor.current
    Column(
        modifier = modifier.fillMaxWidth()
            .klickable(enabled = onClick != null, onClick = onClick ?: {})
            .semantics(mergeDescendants = true) {},
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
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

        if (detailContent != null) {
            detailContent.invoke()
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }
        if (showDivider) {
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}
