package xyz.ksharma.krail.trip.planner.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_heart
import krail.feature.trip_planner.ui.generated.resources.ic_info
import krail.feature.trip_planner.ui.generated.resources.ic_paint
import krail.feature.trip_planner.ui.generated.resources.ic_pen
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.AppLogo

@Composable
fun SettingsScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
    showDebugConfig: Boolean = false,
    onBackClick: () -> Unit = {},
    onChangeThemeClick: () -> Unit = {},
    onReferFriendClick: () -> Unit = {},
    onAboutUsClick: () -> Unit = {},
    onIntroClick: () -> Unit = {},
    onDebugConfigClick: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            .testTag(TripPlannerTestTags.SETTINGS_SCREEN)
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        ) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { Text(text = "Settings") },
            )

            LazyColumn(
                modifier = Modifier,
                contentPadding = PaddingValues(bottom = CONTENT_BOTTOM_PADDING),
            ) {
                item(key = "settings-change-theme") {
                    SettingsItem(
                        icon = painterResource(Res.drawable.ic_paint),
                        text = "Change Theme",
                        modifier = Modifier.testTag(TripPlannerTestTags.SETTINGS_ITEM_THEME),
                        onClick = {
                            onChangeThemeClick()
                        },
                    )
                }

                item(key = "settings-invite") {
                    SettingsItem(
                        icon = painterResource(Res.drawable.ic_heart),
                        text = "Invite your friends \uD83D\uDC95",
                        modifier = Modifier.testTag(TripPlannerTestTags.SETTINGS_ITEM_INVITE),
                        onClick = {
                            onReferFriendClick()
                        },
                    )
                }

                item(key = "settings-about-title") {
                    Text(
                        text = "About",
                        style = KrailTheme.typography.title,
                        modifier = Modifier
                            .padding(top = dim.spacingXXXL, bottom = dim.spacingXL)
                            .padding(horizontal = SECTION_TITLE_HORIZONTAL_PADDING),
                    )
                }

                item(key = "settings-how-to") {
                    SettingsItem(
                        icon = painterResource(Res.drawable.ic_info),
                        text = "How to KRAIL?",
                        modifier = Modifier.testTag(TripPlannerTestTags.SETTINGS_ITEM_HOW_TO),
                        onClick = onIntroClick,
                    )
                }

                item(key = "settings-our-story") {
                    SettingsItem(
                        icon = painterResource(Res.drawable.ic_pen),
                        text = "Our story",
                        modifier = Modifier.testTag(TripPlannerTestTags.SETTINGS_ITEM_OUR_STORY),
                        onClick = { onAboutUsClick() },
                    )
                }

                if (showDebugConfig) {
                    item(key = "settings-debug-config") {
                        SettingsItem(
                            icon = painterResource(Res.drawable.ic_paint),
                            text = "Debug Config",
                            modifier = Modifier
                                .testTag(TripPlannerTestTags.SETTINGS_ITEM_DEBUG_CONFIG),
                            onClick = onDebugConfigClick,
                        )
                    }
                }

                item(key = "settings-footer-spacer") {
                    Spacer(modifier = Modifier.fillMaxWidth().height(FOOTER_SPACER_HEIGHT))
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .background(KrailTheme.colors.surface)
                .navigationBarsPadding()
                .padding(bottom = dim.spacingML, top = dim.spacingXL),
        ) {
            AppLogo()
            Text(
                text = "v$appVersion",
                style = KrailTheme.typography.bodyLarge,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.fillMaxWidth().padding(top = dim.spacingXL),
                textAlign = TextAlign.Center,
            )
        }
    }
}

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
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier.fillMaxWidth()
            .klickable(enabled = onClick != null, onClick = onClick ?: {})
            .semantics(mergeDescendants = true) {},
    ) {
        Spacer(modifier = Modifier.height(dim.spacingXXXL))
        Row(
            modifier = Modifier
                .padding(horizontal = dim.pageHorizontalPadding)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                contentDescription = null,
                painter = icon,
                colorFilter = ColorFilter.tint(color = themeColor.hexToComposeColor()),
                modifier = Modifier.size(dim.iconM),
            )
            Text(
                text = text,
                style = KrailTheme.typography.bodyLarge,
            )
        }

        if (detailContent != null) {
            detailContent.invoke()
        } else {
            Spacer(modifier = Modifier.height(dim.spacingXXXL))
        }
        if (showDivider) {
            Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
        }
    }
}

private val CONTENT_BOTTOM_PADDING = 104.dp
private val SECTION_TITLE_HORIZONTAL_PADDING = 26.dp
private val FOOTER_SPACER_HEIGHT = 108.dp
