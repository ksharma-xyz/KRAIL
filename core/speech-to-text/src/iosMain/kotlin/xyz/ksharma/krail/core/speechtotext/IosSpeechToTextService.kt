package xyz.ksharma.krail.core.speechtotext

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTaskHintDictation
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import xyz.ksharma.krail.core.log.log
import kotlin.coroutines.resume

/**
 * `SFSpeechRecognizer` + `AVAudioEngine`, both plain Objective-C-compatible frameworks (no
 * Swift-only surface like Foundation Models), so this is a direct Kotlin/Native cinterop
 * call with no Swift shim needed — see `IosAiTextService` for the contrasting case.
 *
 * Microphone permission is owned by aagya at the Compose layer
 * ([rememberRequestMicrophonePermission]), so this service only reads the current status,
 * mirroring [AndroidSpeechToTextService] which likewise never shows the system dialog
 * itself. `AVAudioApplication.recordPermission` is an iOS 17+ API, which the deployment
 * target satisfies (see `iosApp` build settings).
 *
 * Speech-recognizer authorization has no aagya family, so it is still requested here. Per
 * this project's `ios_permission_must_request.md` lesson: [checkAvailability] actively
 * calls `SFSpeechRecognizer.requestAuthorization` when status is not yet determined, it
 * does not just inspect `authorizationStatus()` — otherwise the app never appears in
 * Settings for the rider to grant the permission from.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosSpeechToTextService : SpeechToTextService {

    // Nullable, and built lazily rather than in a field initialiser. SFSpeechRecognizer()
    // returns nil when the device's current locale has no speech model. As an eagerly
    // initialised field on a Koin single, that turned a rider's locale into a crash at the
    // moment the home screen built its ViewModel: the whole app, not the feature. Lazy and
    // nullable means an unusable recogniser reports NOT_AVAILABLE like any other dead end.
    //
    // runCatching, not an elvis: cinterop declares this initialiser non-null, so a nil from
    // Objective-C arrives as a thrown error rather than as a null to test for.
    private val recognizer: SFSpeechRecognizer? by lazy {
        runCatching { SFSpeechRecognizer(locale = NSLocale.currentLocale) }
            .recoverCatching { SFSpeechRecognizer() }
            .onFailure { log("SpeechToTextService: no recogniser for this locale: ${it.message}") }
            .getOrNull()
    }

    private var audioEngine: AVAudioEngine? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null

    override suspend fun checkAvailability(): SpeechToTextAvailability {
        val speechAuthorized = requestSpeechAuthorizationIfNeeded()
        val micAuthorized = microphoneAuthorized()
        val result = when {
            !speechAuthorized || !micAuthorized ->
                SpeechToTextAvailability.Unavailable(reason = SpeechUnavailableReasons.PERMISSION_REQUIRED)

            recognizer?.available != true ->
                SpeechToTextAvailability.Unavailable(reason = SpeechUnavailableReasons.NOT_AVAILABLE)

            else -> SpeechToTextAvailability.Available
        }
        log("SpeechToTextService: checkAvailability -> $result")
        return result
    }

    override fun startListening(): Flow<SpeechToTextResult> = callbackFlow {
        val speechRecognizer = recognizer
        if (speechRecognizer?.available != true) {
            trySend(SpeechToTextResult.Error(reason = SpeechUnavailableReasons.NOT_AVAILABLE))
            close()
            return@callbackFlow
        }

        val request = SFSpeechAudioBufferRecognitionRequest().also { recognitionRequest = it }
        configureRequest(request = request, recognizer = speechRecognizer)
        val engine = startAudioEngineFeeding(request)
        val inputNode = engine.inputNode

        // What the silence watcher below reads. Written from the recognition callback, read
        // from a coroutine: both run on the main dispatcher, so no synchronisation is needed
        // beyond them not being able to interleave mid-statement.
        val session = TranscriptWatch()

        speechRecognizer.recognitionTaskWithRequest(request) { result, error ->
            when {
                error != null -> {
                    trySend(SpeechToTextResult.Error(reason = error.localizedDescription))
                    close()
                }

                result != null -> {
                    val isFinal = result.isFinal()
                    session.record(text = result.bestTranscription.formattedString, isFinal = isFinal)
                        ?.let { trySend(it) }
                    if (isFinal) close()
                }
            }
        }

        // The end-of-speech detection iOS does not have. See watchForSilence.
        launch { watchForSilence(session = session) }

        awaitClose {
            engine.stop()
            inputNode.removeTapOnBus(0u)
            request.endAudio()
            audioEngine = null
            recognitionRequest = null
            releaseAudioSession()
        }
    }

    override fun stopListening() = stopFeedingAudio()

    /**
     * Takes the microphone away and tells the request that was all the audio there is, in that
     * order. Both are needed and the order is the point: `endAudio` on a request still being
     * appended to leaves the recogniser waiting on audio nobody meant to send.
     *
     * The recogniser still owes a final transcript afterwards, so this ends the *listening*, not
     * the flow — see [watchForSilence], which is the other caller.
     */
    private fun stopFeedingAudio() {
        audioEngine?.stop()
        audioEngine?.inputNode?.removeTapOnBus(0u)
        recognitionRequest?.endAudio()
    }

    /**
     * The request settings that mirror Android's recognizer intent extras.
     *
     * Two of them had no counterpart here at all, which is why the same sentence behaved
     * differently on the two platforms even with identical silence windows.
     *
     * `EXTRA_PREFER_OFFLINE` maps to [requiresOnDeviceRecognition]. Left unset, iOS defaults
     * it to false and streams the rider's audio to Apple's servers: a network round trip in
     * the middle of a spoken sentence, partial results arriving in a different rhythm from
     * the on-device path, and audio leaving the phone for a feature whose Android half never
     * sends it anywhere. Android's extra is a preference and falls back silently; this flag
     * is a hard requirement that fails the session outright when no on-device model is
     * installed, so it is set only when the recogniser reports one. Same "prefer, do not
     * require" intent, written the way this platform expresses it.
     *
     * `LANGUAGE_MODEL_FREE_FORM` maps to the dictation task hint. The default, unspecified,
     * leaves the recogniser guessing at the shape of what it is hearing; a rider saying where
     * they are going is dictation, not a search term and not a yes/no confirmation.
     */
    private fun configureRequest(
        request: SFSpeechAudioBufferRecognitionRequest,
        recognizer: SFSpeechRecognizer,
    ) {
        request.shouldReportPartialResults = true
        if (recognizer.supportsOnDeviceRecognition()) {
            request.requiresOnDeviceRecognition = true
        }
        request.taskHint = SFSpeechRecognitionTaskHintDictation
    }

    /**
     * Opens the microphone and points it at [request].
     *
     * `setActive` is called explicitly rather than relied on as a side effect of starting the
     * engine, because the matching deactivation in [releaseAudioSession] is what lets another
     * app's audio resume, and an activation nobody made is a deactivation nobody thinks to
     * pair. An earlier comment here claimed cinterop did not expose `setActive` at all; it
     * does, on this Kotlin version, for both the two and three argument forms.
     */
    private fun startAudioEngineFeeding(request: SFSpeechAudioBufferRecognitionRequest): AVAudioEngine {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryRecord, error = null)
        AVAudioSession.sharedInstance().setActive(true, error = null)

        val engine = AVAudioEngine().also { audioEngine = it }

        val inputNode = engine.inputNode
        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = AUDIO_TAP_BUFFER_SIZE,
            format = inputNode.outputFormatForBus(0u),
        ) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }

        engine.prepare()
        engine.startAndReturnError(null)
        return engine
    }

    /**
     * The end-of-speech detection iOS does not have.
     *
     * `SFSpeechAudioBufferRecognitionRequest` never decides for itself that the rider has
     * finished: `isFinal` arrives only after `endAudio()`, and nothing here used to call it.
     * Android's recogniser ends a session on its own after a few seconds of quiet, so the same
     * sentence finished in about four seconds there and ran to the ViewModel's ten to fifteen
     * second ceiling here. That ceiling is a backstop for a recogniser that never reports an
     * end, and on iOS it had quietly become the only thing ending any session at all.
     *
     * Watches for words rather than for quiet audio: the audio tap sees room noise on every
     * buffer, while the transcript only gains a word when one is recognised, which is the thing
     * actually being waited on. Words, not changes — see [TranscriptWatch.record].
     *
     * One window, where Android has two: four seconds of quiet after a phrase that sounds
     * unfinished, three after one that sounds complete. A trip said out loud almost always ends
     * on a complete-sounding phrase, so three is the window that fires there. iOS cannot tell
     * those two apart, so it gets the one that matches the common case, and it used to copy the
     * other one - which is the second of extra waiting this used to have.
     *
     * The two platforms are still not measuring the same event, so the numbers being equal is a
     * coincidence worth not relying on: Android measures silence, this measures the recogniser's
     * last word ARRIVING, which lags the rider's last word by however long recognition takes.
     */
    private suspend fun watchForSilence(session: TranscriptWatch) {
        while (currentCoroutineContext().isActive) {
            delay(SILENCE_POLL_MILLIS)
            val quietFor = session.quietForMillis()
            // Nothing recognised yet earns a longer grace period: that window covers a rider
            // who tapped the mic and is still drawing breath, not one who has trailed off mid
            // sentence.
            val longEnough = if (session.heardAnything) {
                SILENCE_THAT_ENDS_THE_SESSION_MILLIS
            } else {
                SILENCE_BEFORE_ANY_SPEECH_MILLIS
            }
            if (quietFor >= longEnough) {
                log("SpeechToTextService: ${quietFor}ms without new words, ending audio")
                // The exact finish stopListening asks for, and for the same reason it takes the
                // microphone away first. `endAudio` means "that was all the audio there is", and
                // the tap was still running underneath it, appending room noise to a request
                // that had been told it was closed. The recogniser is under no obligation to
                // finish a request still being written to, which is how a session that had
                // decided the rider was done still ran to the ViewModel's ceiling.
                //
                // Not close(): the recogniser still owes a final transcript, and the flow stays
                // open to receive it.
                stopFeedingAudio()
                return
            }
        }
    }

    /**
     * Hands the audio session back, and says so.
     *
     * The session was put into `AVAudioSessionCategoryRecord` to listen and then left there.
     * Recording interrupts whatever the rider had playing, and without a deactivation carrying
     * `NotifyOthersOnDeactivation` the other app is never told it may resume — so a rider who
     * used the mic once lost their music until they killed KRAIL. Deactivating restores the
     * default category as well, which is what lets any later playback behave normally.
     */
    private fun releaseAudioSession() {
        AVAudioSession.sharedInstance().setActive(
            active = false,
            withOptions = NOTIFY_OTHERS_ON_DEACTIVATION,
            error = null,
        )
    }

    private suspend fun requestSpeechAuthorizationIfNeeded(): Boolean {
        val current = SFSpeechRecognizer.authorizationStatus()
        if (current != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined) {
            return current == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
        }
        return suspendCancellableCoroutine { continuation ->
            SFSpeechRecognizer.requestAuthorization { status ->
                continuation.resume(
                    status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized,
                )
            }
        }
    }

    // Status read only. By the time this runs, aagya's controller has already raised the
    // system dialog from the mic button (see rememberRequestMicrophonePermission), and only
    // a granted answer starts listening. Anything but granted here means a caller skipped
    // that flow, and the answer is the same one AndroidSpeechToTextService gives: report
    // PERMISSION_REQUIRED rather than ask a second time from a layer that should not own it.
    private fun microphoneAuthorized(): Boolean =
        AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionGranted
}

// The same number as Android's SILENCE_AFTER_A_COMPLETE_SOUNDING_PHRASE_MILLIS, which is the
// window that actually fires over there: a trip said out loud almost always ends on a
// complete-sounding phrase. NOT Android's other window, the four-second one this used to copy,
// which is why iOS took about a second longer to finish the same sentence.
//
// What this measures is not the same event Android measures, and that is worth knowing before
// tuning it: iOS waits on the recogniser's last word ARRIVING, which already lags the rider's
// last word being spoken, so three seconds here is felt as a little more than Android's three.
// Two was tried and is the floor - below it, "from Central to..." <thinking> starts being cut
// off, which is a rider-reported bug on the Android side already and the reason those windows
// were widened rather than narrowed.
private const val SILENCE_THAT_ENDS_THE_SESSION_MILLIS = 3_000L

// Before the first word. Longer than the mid-sentence window: the rider has just tapped the
// mic and may still be deciding what to ask for. Still well inside the ViewModel's ten second
// ceiling, so a rider who taps the mic and says nothing is not left with a live microphone.
private const val SILENCE_BEFORE_ANY_SPEECH_MILLIS = 6_000L

// How often the watcher looks. Fine enough that the end of a sentence is not noticeably late,
// coarse enough to be free.
private const val SILENCE_POLL_MILLIS = 250L

// One tap buffer. The recogniser is fed whatever arrives; this only sets how often.
private const val AUDIO_TAP_BUFFER_SIZE: UInt = 1024u

// `NOTIFY_OTHERS_ON_DEACTIVATION` is not exposed by the Kotlin/Native
// AVFAudio bindings for this deployment target (the option constants come through as a plain
// bitmask parameter with no named values), so the documented value is written out here rather
// than left off. Without the flag the deactivation is silent and other apps never resume.
private const val NOTIFY_OTHERS_ON_DEACTIVATION: ULong = 1uL
