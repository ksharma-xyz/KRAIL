package xyz.ksharma.krail.trip.planner.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import xyz.ksharma.krail.trip.planner.ui.alerts.summary.AiAlertSummaryCard
import xyz.ksharma.krail.trip.planner.ui.alerts.summary.AlertSummaryEvent
import xyz.ksharma.krail.trip.planner.ui.alerts.summary.AlertSummaryUiState
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert

private val CONTENT_PADDING_BOTTOM = 48.dp
private val BOTTOM_SPACER_HEIGHT = 64.dp

@Composable
fun ServiceAlertScreen(
    serviceAlerts: ImmutableSet<ServiceAlert>,
    modifier: Modifier = Modifier,
    alertSummaryState: AlertSummaryUiState? = null,
    onSummaryEvent: (AlertSummaryEvent) -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    var expandedAlertId by rememberSaveable { mutableStateOf<String?>(null) }

    // Fires an event up to the owning ViewModel rather than calling AiTextService here —
    // this composable never touches DI directly. The ViewModel dedupes per alert-set
    // content hash, so recomposition/relaunch (e.g. rotation) is safe to call again.
    LaunchedEffect(serviceAlerts) {
        onSummaryEvent(AlertSummaryEvent.SummaryRequested(serviceAlerts))
    }

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

        // Renders nothing unless a summary actually arrived or is in flight — covers the
        // flag being off, the device failing the availability check, and any mid-call
        // failure identically. No entry here in every one of those cases.
        alertSummaryState?.let { state ->
            item("ai_alert_summary") {
                AiAlertSummaryCard(
                    state = state,
                    onVoteClick = { vote -> onSummaryEvent(AlertSummaryEvent.VoteClicked(vote)) },
                    // Extra bottom padding vs. the alert cards' own spacingM — the AI card
                    // needs a visibly bigger gap before the first alert card so it reads as
                    // its own distinct section, not just another item in the same list.
                    modifier = Modifier.padding(
                        start = dim.spacingXL,
                        top = dim.spacingM,
                        end = dim.spacingXL,
                        bottom = dim.spacingXL,
                    ),
                )
            }
        }

        itemsIndexed(
            items = serviceAlerts.toImmutableList(),
            // ServiceAlert.alertId, not the index. Prefixing the index would also be unique,
            // but it makes identity positional: reordering or inserting an alert renumbers
            // every key after it, so Compose treats untouched rows as new ones and drops
            // their animation and state. The alerts arrive as a Set, so (heading, message)
            // is distinct by construction and survives reordering. Prefixed to keep it clear
            // of this list's static item keys.
            key = { _, item -> "alert_${item.alertId}" },
        ) { index, alert ->
            CollapsibleAlert(
                serviceAlert = alert,
                index = index + 1,
                modifier = Modifier.padding(horizontal = dim.spacingXL, vertical = dim.spacingM),
                // Same identity as the list key. hashCode() was an Int over unbounded
                // text, so two alerts could collide and expand together.
                collapsed = expandedAlertId != alert.alertId,
                onClick = {
                    expandedAlertId = if (expandedAlertId == alert.alertId) null else alert.alertId
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
