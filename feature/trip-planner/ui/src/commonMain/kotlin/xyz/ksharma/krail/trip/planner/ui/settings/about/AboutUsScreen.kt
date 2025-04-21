package xyz.ksharma.krail.trip.planner.ui.settings.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.AppLogo
import xyz.ksharma.krail.trip.planner.ui.state.settings.about.AboutUsState

@Composable
fun AboutUsScreen(
    state: AboutUsState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
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
                title = { Text(text = "Our story") },
            )
        }

        AnimatedVisibility(
            visible = state.isLoading,
        ) {
            LazyColumn(
                modifier = Modifier,
                contentPadding = PaddingValues(top = 20.dp, bottom = 104.dp),
            ) {

                item {
                    Text(
                        state.story,
                        style = KrailTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }

                item {
                    Text(
                        text = state.disclaimer,
                        style = KrailTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)
                    )
                }

                item {
                    AppLogo(modifier = Modifier.padding(top = 48.dp))
                }
            }
        }
    }
}
