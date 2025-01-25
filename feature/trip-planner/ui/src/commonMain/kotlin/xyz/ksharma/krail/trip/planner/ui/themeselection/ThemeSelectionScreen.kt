package xyz.ksharma.krail.trip.planner.ui.themeselection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.scalingKlickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.DisabledContentAlpha
import xyz.ksharma.krail.trip.planner.ui.components.themeBackgroundColor

@Composable
fun ThemeSelectionScreen(
    selectedThemeStyle: KrailThemeStyle?,
    onThemeSelected: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface)
            .statusBarsPadding(),
    ) {
        var selectedThemeColorId: Int? by rememberSaveable(selectedThemeStyle) {
            mutableStateOf(selectedThemeStyle?.id)
        }
        val buttonBackgroundColor by animateColorAsState(
            targetValue = selectedThemeColorId?.let { themeId ->
               KrailThemeStyle.entries.find { it.id == themeId }?.hexColorCode?.hexToComposeColor()
            } ?: KrailTheme.colors.surface,
            label = "buttonBackgroundColor",
            animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        )

        Column {
            TitleBar(
                onNavActionClick = onBackClick,
                title = {},
                modifier = Modifier.fillMaxWidth(),
            )

            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 152.dp)) {
                item {
                    Text(
                        text = "Let's set the vibe!",
                        style = KrailTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp),
                    )
                }

                item {
                    Text(
                        text = "Select a color",
                        style = KrailTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp),
                    )
                }

                items(items = KrailThemeStyle.entries, key = { it.id }) { theme ->
                    ThemeSelectionRadioButton(
                        themeStyle = theme,
                        selected = selectedThemeColorId == theme.id,
                        onClick = { clickedThemeStyle ->
                            selectedThemeColorId = clickedThemeStyle.id
                        },
                    )
                }
            }
        }

        if (selectedThemeColorId != null) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                Button(
                    colors = ButtonColors(
                        containerColor = buttonBackgroundColor,
                        contentColor = getForegroundColor(buttonBackgroundColor),
                        disabledContainerColor = buttonBackgroundColor.copy(alpha = DisabledContentAlpha),
                        disabledContentColor = getForegroundColor(buttonBackgroundColor).copy(alpha = DisabledContentAlpha),
                    ),
                    onClick = {
                        selectedThemeColorId?.let { productClass ->
                            onThemeSelected(productClass)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(text = "Let's #KRAIL")
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionRadioButton(
    themeStyle: KrailThemeStyle,
    onClick: (KrailThemeStyle) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) themeBackgroundColor(themeStyle) else Color.Transparent,
        label = "backgroundColor",
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .scalingKlickable { onClick(themeStyle) }
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(vertical = 24.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color = themeStyle.hexColorCode.hexToComposeColor()),
        )

        Text(
            text = themeStyle.tagLine,
            style = KrailTheme.typography.title.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
