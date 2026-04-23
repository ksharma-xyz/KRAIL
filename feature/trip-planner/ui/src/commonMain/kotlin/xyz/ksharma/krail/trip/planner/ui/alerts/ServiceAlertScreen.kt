package xyz.ksharma.krail.trip.planner.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.ui.tooling.PreviewScreen
import xyz.ksharma.krail.taj.components.SheetTitleBar
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

private val CONTENT_PADDING_BOTTOM = 48.dp
private val BOTTOM_SPACER_HEIGHT = 64.dp

@Composable
fun ServiceAlertScreen(
    serviceAlerts: ImmutableSet<ServiceAlert>,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    var expandedAlertId by rememberSaveable { mutableStateOf<Int?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(top = dim.spacingXL, bottom = CONTENT_PADDING_BOTTOM),
        modifier = modifier.background(color = KrailTheme.colors.bottomSheetBackground),
    ) {
        item("title_bar") {
            SheetTitleBar(
                title = {
                    Text(text = "Service Alerts")
                },
            )
        }

        itemsIndexed(
            items = serviceAlerts.toImmutableList(),
            key = { _, item -> item.heading.lowercase() },
        ) { index, alert ->
            CollapsibleAlert(
                serviceAlert = alert,
                index = index + 1,
                modifier = Modifier.padding(horizontal = dim.spacingXL, vertical = dim.spacingM),
                collapsed = expandedAlertId != alert.hashCode(),
                onClick = {
                    expandedAlertId = if (expandedAlertId == alert.hashCode()) {
                        null
                    } else {
                        alert.hashCode()
                    }
                },
            )
        }

        item("bottom_spacing") {
            Spacer(modifier = Modifier.height(BOTTOM_SPACER_HEIGHT))
        }
    }
}

@Preview
@Composable
private fun PreviewServiceAlertScreen() {
    PreviewScreen {
        ServiceAlertScreen(
            serviceAlerts = persistentSetOf(
                ServiceAlert(
                    heading = "Service Alert 1",
                    message = "This is a service alert 1",
                ),
                ServiceAlert(
                    heading = "Service Alert 2",
                    message = "This is a service alert 2",
                ),
                ServiceAlert(
                    heading = "Service Alert 3",
                    message = "This is a service alert 3",
                ),
            ),
        )
    }
}
