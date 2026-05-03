package xyz.ksharma.krail.trip.planner.ui.settings.story

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.AppLogo
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryState

@Composable
fun OurStoryScreen(
    state: OurStoryState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TitleBar(
                modifier = Modifier.fillMaxWidth(),
                onNavActionClick = onBackClick,
                title = { Text(text = "Our story") },
            )
        }

        val dim = KrailTheme.dimensions
        Crossfade(
            targetState = state.isLoading,
            label = "OurStoryContent",
        ) { isLoading ->
            if (!isLoading) {
                LazyColumn(
                    modifier = Modifier,
                    contentPadding = PaddingValues(top = dim.spacingXXL, bottom = CONTENT_BOTTOM_PADDING),
                ) {
                    item {
                        Text(
                            state.story,
                            style = KrailTheme.typography.bodyLarge,
                            modifier = Modifier.padding(
                                horizontal = dim.pageHorizontalPadding,
                                vertical = dim.spacingXXXL,
                            ),
                        )
                    }

                    item {
                        Text(
                            text = state.disclaimer,
                            style = KrailTheme.typography.labelLarge,
                            modifier = Modifier
                                .padding(horizontal = dim.pageHorizontalPadding)
                                .padding(top = dim.spacingL),
                        )
                    }

                    item {
                        AppLogo(modifier = Modifier.padding(top = APP_LOGO_TOP_PADDING))
                    }
                }
            }
        }
    }
}

private val CONTENT_BOTTOM_PADDING = 104.dp
private val APP_LOGO_TOP_PADDING = 48.dp
