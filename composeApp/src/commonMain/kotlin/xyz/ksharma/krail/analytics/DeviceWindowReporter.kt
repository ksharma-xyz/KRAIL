package xyz.ksharma.krail.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsUserProperty
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.setUserProperty
import xyz.ksharma.krail.core.appinfo.rememberDeviceFoldInfo
import xyz.ksharma.krail.core.log.log
import kotlin.time.Duration.Companion.milliseconds

// Once per launch, not once per composition: this survives configuration changes, which
// recreate the Activity and would otherwise re-report the same launch on every rotation.
private var launchReported = false

// Also process scoped, and for a sharper reason: unfolding recreates the Activity, so a
// baseline held in composition would be re-seeded with the *already unfolded* window and
// the COMPACT to EXPANDED transition - the whole point of the event - would be lost.
private var lastReportedSnapshot: DeviceWindowSnapshot? = null

// An unfold animation or a drag-resize emits a continuous stream of sizes. Report the
// settled state only.
private val SETTLE_DELAY = 400.milliseconds

/**
 * Reports the window the app is running in, and every settled change to it.
 *
 * Three things happen here, and they answer different questions:
 *
 * 1. [AnalyticsEvent.DeviceWindowEvent] once per launch - the denominator, plus raw dp so
 *    buckets can be re-cut against history later.
 * 2. [AnalyticsEvent.DeviceWindowChangedEvent] on settled transitions - the rider
 *    unfolding, rotating, or resizing, and whether the layout opened a second pane.
 * 3. User properties, updated with the window - these attach to every *other* event the
 *    rider fires, so "what did they do while unfolded" is a filter on the events that
 *    already exist rather than a new event per interaction.
 *
 * Placed at the navigation host so it sees the same window the layout does, and so the
 * screen name is available without a second source of truth.
 *
 * @param screenName current screen as an [xyz.ksharma.krail.core.analytics.AnalyticsScreen]
 *                   name, so transitions join to `view_screen`.
 * @param isDualPane whether the layout is currently rendering two panes.
 */
@OptIn(FlowPreview::class)
@Composable
fun TrackDeviceWindow(
    analytics: Analytics,
    screenName: String,
    isDualPane: Boolean,
) {
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    val foldInfo = rememberDeviceFoldInfo()
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val widthDp = with(density) { windowInfo.containerSize.width.toDp().value.toInt() }
    val heightDp = with(density) { windowInfo.containerSize.height.toDp().value.toInt() }
    val smallestWidthDp = minOf(widthDp, heightDp)
    val paneMode = if (isDualPane) PaneMode.DUAL else PaneMode.SINGLE
    val formFactor = DeviceWindowClassifier.formFactor(smallestWidthDp, foldInfo.state)

    val snapshot = DeviceWindowSnapshot(
        widthSizeClass = adaptiveInfo.widthSizeClass.name,
        foldState = foldInfo.state,
        paneMode = paneMode,
    )

    // Properties first, so any event fired after this change carries the new window.
    LaunchedEffect(snapshot) {
        analytics.setUserProperty(AnalyticsUserProperty.DEVICE_FORM_FACTOR, formFactor.name)
        analytics.setUserProperty(AnalyticsUserProperty.WINDOW_WIDTH_CLASS, snapshot.widthSizeClass)
        analytics.setUserProperty(AnalyticsUserProperty.PANE_MODE, snapshot.paneMode.name)
    }

    LaunchedEffect(Unit) {
        if (!launchReported) {
            launchReported = true
            lastReportedSnapshot = snapshot
            analytics.track(
                AnalyticsEvent.DeviceWindowEvent(
                    widthDp = widthDp,
                    heightDp = heightDp,
                    smallestWidthDp = smallestWidthDp,
                    widthSizeClass = adaptiveInfo.widthSizeClass.name,
                    heightSizeClass = adaptiveInfo.heightSizeClass.name,
                    orientation = DeviceWindowClassifier.orientation(widthDp, heightDp).name,
                    formFactor = formFactor.name,
                    paneMode = paneMode.name,
                    foldState = foldInfo.state.name,
                    foldOrientation = foldInfo.orientation.name,
                ),
            )
        }
    }

    // snapshotFlow only re-emits when it reads observable state, so the snapshot has to
    // go through a State holder. Reading the plain local would pin the flow to whatever
    // the window was at first composition and no unfold would ever be reported.
    val currentSnapshot = rememberUpdatedState(snapshot)

    LaunchedEffect(Unit) {
        snapshotFlow { currentSnapshot.value }
            .debounce(SETTLE_DELAY)
            .distinctUntilChanged()
            .collect { settled ->
                // Compared against the last *reported* state, not the last frame, so an
                // unfold that passes through an intermediate class does not report the
                // state it flew through.
                val previous = lastReportedSnapshot ?: settled
                if (!DeviceWindowClassifier.isReportableTransition(previous, settled)) {
                    lastReportedSnapshot = settled
                    return@collect
                }
                lastReportedSnapshot = settled
                log(
                    "[DEVICE_WINDOW] width ${previous.widthSizeClass} to ${settled.widthSizeClass}, " +
                        "fold ${previous.foldState} to ${settled.foldState}, " +
                        "pane ${previous.paneMode} to ${settled.paneMode}",
                )
                analytics.track(
                    AnalyticsEvent.DeviceWindowChangedEvent(
                        fromWidthClass = previous.widthSizeClass,
                        toWidthClass = settled.widthSizeClass,
                        fromFoldState = previous.foldState.name,
                        toFoldState = settled.foldState.name,
                        fromPaneMode = previous.paneMode.name,
                        toPaneMode = settled.paneMode.name,
                        screen = screenName,
                    ),
                )
            }
    }
}
