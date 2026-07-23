package xyz.ksharma.krail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.appreview.AppReviewDebugSignal
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Debug-only stand-in for the platform review sheet.
 *
 * A **sideloaded** debug build can never show the real Play card, and both platforms throttle
 * and report nothing, so on a device there is otherwise no way to confirm the review trigger
 * fired. This observes [AppReviewDebugSignal] and shows a sheet naming the [source] moment
 * whenever a request fires. It is mounted only in debug builds and is **not** the review; the
 * real platform call still happens alongside it.
 */
@Composable
internal fun AppReviewDebugProof() {
    val signal: AppReviewDebugSignal = koinInject()
    var firedSource by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(signal) {
        signal.requests.collect { firedSource = it }
    }

    val source = firedSource ?: return
    val dim = KrailTheme.dimensions
    ModalBottomSheet(
        containerColor = KrailTheme.colors.bottomSheetBackground,
        onDismissRequest = { firedSource = null },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = dim.spacingXL, vertical = dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) {
            Text(
                text = "Review would fire now",
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
            )
            Text(
                text = "Debug proof only. The real Play or StoreKit sheet is throttled and " +
                    "never shows on a sideloaded build, so this stands in to confirm the " +
                    "trigger fired and every gate passed.",
                style = KrailTheme.typography.bodyLarge,
                color = KrailTheme.colors.onSurface,
            )
            Text(
                text = "Moment: $source",
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
            )
            TextButton(
                dimensions = ButtonDefaults.largeButtonSize(),
                onClick = { firedSource = null },
            ) {
                Text(text = "Got it")
            }
        }
    }
}
