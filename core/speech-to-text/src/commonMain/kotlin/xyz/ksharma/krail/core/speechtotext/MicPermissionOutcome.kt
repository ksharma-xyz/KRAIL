package xyz.ksharma.krail.core.speechtotext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import xyz.ksharma.aagya.permission.AppPermission
import xyz.ksharma.aagya.permission.PermissionResult
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.aagya.permission.data.rememberPermissionController

/**
 * What asking for the microphone ended in.
 *
 * [Denied] and [NeedsSettings] are deliberately separate. The rider who just said no can be
 * asked again by tapping the mic; the rider the system will no longer prompt for cannot, and
 * showing them the same prompt-that-never-appears is the difference between a button that
 * works and one that seems broken. Only the second one is sent to Settings.
 */
sealed interface MicPermissionOutcome {
    data object Granted : MicPermissionOutcome
    data object Denied : MicPermissionOutcome
    data object NeedsSettings : MicPermissionOutcome
    data object Restricted : MicPermissionOutcome
}

/**
 * Requests the microphone permission, suspending until the rider answers, and reports how it
 * ended.
 *
 * Asking happens at the Compose layer, never inside the services:
 * [AndroidSpeechToTextService][xyz.ksharma.krail.core.speechtotext.AndroidSpeechToTextService]
 * is a plain singleton with no Activity to show the system dialog from, and
 * [IosSpeechToTextService][xyz.ksharma.krail.core.speechtotext.IosSpeechToTextService]
 * matches that contract so the microphone has exactly one asking path on both platforms.
 * Both services only read the current status.
 *
 * Backed by aagya's [PermissionController][xyz.ksharma.aagya.permission.PermissionController],
 * the same one `UserLocationManager` uses for location, so both permissions ask the same way
 * on both platforms instead of this module carrying its own Android-only launcher.
 */
@Composable
fun rememberRequestMicrophonePermission(): suspend () -> MicPermissionOutcome {
    val permissionController = rememberPermissionController()
    return remember(permissionController) {
        {
            when (permissionController.checkPermissionStatus(AppPermission.Microphone.Default)) {
                is PermissionStatus.Granted -> MicPermissionOutcome.Granted

                // Only NotDetermined can still raise the system prompt. Everything else has
                // already been answered, and asking again is a call the platform ignores.
                is PermissionStatus.NotDetermined -> {
                    val result = permissionController.requestPermission(AppPermission.Microphone.Default)
                    if (result is PermissionResult.Granted) {
                        MicPermissionOutcome.Granted
                    } else {
                        MicPermissionOutcome.Denied
                    }
                }

                is PermissionStatus.Denied -> MicPermissionOutcome.NeedsSettings
                PermissionStatus.Restricted -> MicPermissionOutcome.Restricted
            }
        }
    }
}

/**
 * Opens this app's own page in the system settings, which is the only way back from
 * [MicPermissionOutcome.NeedsSettings]. Same controller call `UserLocationManager` makes for
 * a permanently denied location permission.
 */
@Composable
fun rememberOpenAppSettings(): () -> Unit {
    val permissionController = rememberPermissionController()
    return remember(permissionController) { { permissionController.openAppSettings() } }
}
