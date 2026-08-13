package xyz.ksharma.krail.core.speechtotext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import xyz.ksharma.aagya.permission.AppPermission
import xyz.ksharma.aagya.permission.PermissionResult
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.aagya.permission.data.rememberPermissionController

/**
 * Requests the microphone permission, suspending until the rider answers, and reports whether
 * it ended up granted.
 *
 * Asking has to happen at the Compose layer rather than inside the service:
 * [AndroidSpeechToTextService][xyz.ksharma.krail.core.speechtotext.AndroidSpeechToTextService]
 * is a plain singleton with no Activity to show the system dialog from, so on its own it can
 * only read the current status.
 *
 * Backed by aagya's [PermissionController][xyz.ksharma.aagya.permission.PermissionController],
 * the same one `UserLocationManager` uses for location, so both permissions ask the same way
 * on both platforms instead of this module carrying its own Android-only launcher.
 *
 * A status that is already denied returns false without a second prompt, since the platform
 * will not show one again. Callers surface that rather than starting a recogniser that cannot
 * hear anything.
 */
@Composable
fun rememberRequestRecordAudioPermission(): suspend () -> Boolean {
    val permissionController = rememberPermissionController()
    return remember(permissionController) {
        {
            when (permissionController.checkPermissionStatus(AppPermission.Microphone.Default)) {
                is PermissionStatus.Granted -> true

                is PermissionStatus.NotDetermined ->
                    permissionController.requestPermission(AppPermission.Microphone.Default) is
                    PermissionResult.Granted

                is PermissionStatus.Denied,
                PermissionStatus.Restricted,
                -> false
            }
        }
    }
}
