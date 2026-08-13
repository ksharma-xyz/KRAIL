package xyz.ksharma.krail.core.speechtotext

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred

@Composable
actual fun rememberRequestRecordAudioPermission(): suspend () -> Boolean {
    var pending by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.complete(granted)
        pending = null
    }
    return {
        val deferred = CompletableDeferred<Boolean>()
        pending = deferred
        launcher.launch(Manifest.permission.RECORD_AUDIO)
        deferred.await()
    }
}
