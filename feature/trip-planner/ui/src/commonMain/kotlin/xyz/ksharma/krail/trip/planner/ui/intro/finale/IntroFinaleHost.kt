package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme

// Nothing behind the layer is read out while it is up. Applied only while a run is being played.
private val HiddenFromAccessibility: Modifier = Modifier.clearAndSetSemantics { }

/**
 * The finale's host, at the app root. It draws [content], the app, and above it as a sibling, only while a run is
 * being played, the finale layer. It never wraps the app in a layer of its own, so a map under it is never
 * composited offscreen, which iOS cannot do.
 *
 * Idle, which is nearly always, it is one box and one state read, and it draws nothing and runs nothing. A run
 * [IntroFinaleSignal.start]ed by the Intro is played here with the frame clock, finite and tied to the
 * lifecycle: it is cancelled when the host leaves the composition or the app is stopped, and a host that finds
 * a run it did not start ends it at once instead of playing it again (see [IntroFinaleSignal]).
 *
 * While it is up the layer swallows every touch, a tap anywhere skips it, and what is behind it is hidden from a
 * screen reader, which hears `Let's KRAIL` once.
 *
 * [reduceMotion] is the rider's setting: no wordmark, no glow and no flood, only a short cross-fade.
 */
@Composable
fun IntroFinaleHost(
    signal: IntroFinaleSignal,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = isReduceMotionEnabled(),
    content: @Composable () -> Unit,
) {
    val request by signal.request.collectAsStateWithLifecycle()
    DisposableEffect(signal) {
        signal.attachHost()
        onDispose { signal.detachHost() }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().then(if (request != null) HiddenFromAccessibility else Modifier)) {
            content()
        }
        request?.let { FinaleLayer(signal = signal, request = it, reduceMotion = reduceMotion) }
    }
}

@Composable
private fun FinaleLayer(signal: IntroFinaleSignal, request: FinaleRequest, reduceMotion: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val frame = remember(request.id) { mutableStateOf(finaleFrameAt(0L, reduceMotion)) }
    val tapped = remember(request.id) { mutableStateOf(false) }
    LaunchedEffect(request.id, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (signal.claim(request.id)) {
                playFinale(signal, frame, tapped, reduceMotion)
            } else {
                recoverFinale(signal)
            }
            signal.end(request.id)
        }
    }
    FinaleOverlay(
        frame = frame,
        themeColour = remember(request.themeHexColorCode) { request.themeHexColorCode.hexToComposeColor() },
        groundColour = KrailTheme.colors.surface,
        onTap = { tapped.value = true },
    )
}
