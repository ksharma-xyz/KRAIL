package xyz.ksharma.krail.core.aitext

import android.content.Context
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons.CHECK_FAILED
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons.DEVICE_UNSUPPORTED
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons.MODEL_DOWNLOADING
import xyz.ksharma.krail.core.log.log

/**
 * ML Kit GenAI (Gemini Nano) is beta and the surface has moved between releases — the API
 * used here was confirmed against the actual `com.google.mlkit:genai-summarization:1.0.0-beta1`
 * classes (all `checkFeatureStatus`/`downloadFeature`/`runInference` calls return Guava
 * `ListenableFuture`, not a Play Services `Task`), not assumed from documentation. Re-verify
 * against https://developers.google.com/ml-kit/genai/summarization/android on any version bump.
 *
 * [extractTripIntent] uses a *different* ML Kit GenAI module — `genai-prompt` — confirmed by
 * decompiling the actual jar to have a genuinely free-form custom-prompt surface
 * (`GenerativeModel.generateContent`, plain Kotlin `suspend fun`, no Guava future needed)
 * unlike `genai-summarization`, whose `SummarizationRequest.Builder` only accepts raw text
 * with no prompt/instruction parameter at all. The two modules are separate features with
 * separate availability/download state — [checkAvailability] only governs [summarize];
 * [extractTripIntent] checks its own model's status independently.
 *
 * Gemini Nano is hardware-gated to a subset of Android devices and, even where supported,
 * the model is downloaded on demand rather than bundled — [checkAvailability] can return
 * [AiAvailability.Unavailable] for "not this device" and "not downloaded yet" alike. Callers
 * don't need to distinguish the two: both mean "render nothing."
 */
internal class AndroidAiTextService(private val context: Context) : AiTextService {

    // Lazily built, reused across calls — construction itself does not touch the model.
    private val summarizer: Summarizer by lazy {
        Summarization.getClient(SummarizerOptions.builder(context).build())
    }

    private val promptModel: GenerativeModel by lazy { Generation.getClient() }

    // Singleton service (one instance for the app's lifetime via Koin `single<AiTextService>`),
    // so a scope tied to this instance's own lifetime is safe here — needed because
    // GenerativeModel.download() returns a *cold* Flow<DownloadStatus>: calling it alone
    // does nothing until collected, and collecting to completion inside extractTripIntent's
    // own suspend call would block the caller for as long as the download takes (potentially
    // minutes). Fire-and-forget collection on this scope instead, same "kick off the
    // download, don't block, it'll be available on a later call" shape as
    // Summarizer.downloadFeature's callback-based fire-and-forget above.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun checkAvailability(): AiAvailability = runCatching {
        when (summarizer.checkFeatureStatus().await()) {
            FeatureStatus.AVAILABLE -> AiAvailability.Available

            FeatureStatus.DOWNLOADABLE -> {
                // Fire-and-forget: start the on-demand model download for next time, but
                // never block this call on it — a slow/failed download must not delay or
                // break alert rendering.
                runCatching { summarizer.downloadFeature(noOpDownloadCallback) }
                AiAvailability.Unavailable(reason = MODEL_DOWNLOADING)
            }

            FeatureStatus.DOWNLOADING -> AiAvailability.Unavailable(reason = MODEL_DOWNLOADING)

            else -> AiAvailability.Unavailable(reason = DEVICE_UNSUPPORTED)
        }
    }.getOrElse { throwable ->
        AiAvailability.Unavailable(reason = "$CHECK_FAILED: ${throwable.message}")
    }.also { result ->
        // Deliberately unconditional, not debug-gated: `adb logcat | grep AiTextService`
        // is the one line that answers "why is nothing showing" (flag off vs. device
        // unavailable vs. a failed call), without needing ad-hoc logging re-added by hand.
        log("AiTextService: checkAvailability -> $result")
    }

    override suspend fun summarize(text: String): String? = runCatching {
        val request = SummarizationRequest.builder(text).build()
        summarizer.runInference(request).await().summary
    }.getOrElse { throwable ->
        log("AiTextService: summarize failed: ${throwable.message}")
        null
    }.also { result ->
        log("AiTextService: summarize -> ${if (result == null) "null" else "${result.length} chars"}")
    }

    override suspend fun checkExtractionAvailability(): AiAvailability = runCatching {
        when (val status = promptModel.checkStatus()) {
            FeatureStatus.AVAILABLE -> AiAvailability.Available
            FeatureStatus.DOWNLOADABLE -> {
                triggerPromptModelDownload()
                AiAvailability.Unavailable(reason = MODEL_DOWNLOADING)
            }
            FeatureStatus.DOWNLOADING -> AiAvailability.Unavailable(reason = MODEL_DOWNLOADING)
            // The raw status stays in the log line below, not in the reason: the reason is a
            // contract the UI branches on, and gluing a number onto it broke every equality
            // check that ever tried to read it.
            else -> {
                log("AiTextService: extraction unsupported, status=$status")
                AiAvailability.Unavailable(reason = DEVICE_UNSUPPORTED)
            }
        }
    }.getOrElse { throwable ->
        AiAvailability.Unavailable(reason = "$CHECK_FAILED: ${throwable.message}")
    }.also { result ->
        log("AiTextService: checkExtractionAvailability -> $result")
    }

    override suspend fun extractTripIntent(text: String): TripIntentExtraction? = runCatching {
        if (checkExtractionAvailability() !is AiAvailability.Available) {
            return@runCatching null
        }
        val response = promptModel.generateContent(buildExtractionPrompt(text))
        val rawCandidate = response.candidates.firstOrNull()?.text
        rawCandidate?.let(::parseTripIntentJson)
    }.getOrElse { throwable ->
        log("AiTextService: extractTripIntent failed: ${throwable.message}")
        null
    }.also { result ->
        log("AiTextService: extractTripIntent -> ${if (result == null) "null" else "parsed"}")
    }

    private fun triggerPromptModelDownload() {
        serviceScope.launch {
            log("AiTextService: prompt model download starting")
            promptModel.download()
                .catch { throwable -> log("AiTextService: prompt model download failed: ${throwable.message}") }
                .collect { downloadStatus -> log("AiTextService: prompt model download status -> $downloadStatus") }
        }
    }
}

private val extractionJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/**
 * No structural guarantee from `genai-prompt` (unlike iOS's eventual `@Generable` bridge,
 * which gets constrained decoding for free) — the model can still wrap the JSON in prose or
 * a markdown code fence despite instructions. Grabs the substring between the first `{` and
 * last `}` before decoding rather than decoding the raw response verbatim, and any decode
 * failure collapses to `null` same as every other failure mode on this interface.
 */
internal fun parseTripIntentJson(raw: String): TripIntentExtraction? {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    if (start == -1 || end == -1 || end < start) return null
    return runCatching {
        extractionJson.decodeFromString<TripIntentExtraction>(raw.substring(start, end + 1))
    }.getOrNull()
}

private fun buildExtractionPrompt(riderText: String): String = """
    You extract structured trip-planning fields from a rider's message for a public
    transport app. Respond with ONLY a single JSON object and nothing else — no
    explanation, no markdown code fence — matching exactly this shape:
    {"originText": string or null, "destinationText": string or null, "timeIntent": {"isArrival": true or false, "timeText": string} or null, "modeHints": array of strings}

    Rules:
    - originText / destinationText are short place names as the rider said them (e.g.
      "home", "Central Station"), never a full sentence. null if not mentioned.
    - timeIntent.isArrival is true for "arrive by" / "need to be there by" / "by <time>",
      false for "leave at" / "leaving around" / "departing at". timeIntent is null if the
      rider gave no time at all.
    - timeIntent.timeText is the time phrase verbatim as the rider said it (e.g. "9am",
      "6:30pm") — do not convert or resolve it. Include any day word said with it
      ("tomorrow at 9am", "friday 6pm", "tonight at 7"), because the day is part of when
      they are travelling and is resolved later from this same string.
    - modeHints lists any transport mode words mentioned (e.g. "train", "bus"), verbatim,
      lowercase. Empty array if none mentioned.
    - If the rider names only ONE place, decide which field it belongs in from the
      phrasing. "go home", "let's go to X", "take me to X", "heading to X", "I need to
      get to X" all mean X is the DESTINATION and originText is null. Only put a lone
      place in originText when the wording is clearly about leaving, such as "from X" or
      "leaving X". A rider saying where they are going is far more common than a rider
      saying only where they are.
    - Never guess a field the rider didn't actually say. Use null / empty array instead.

    Examples:
    "let's go home" -> {"originText": null, "destinationText": "home", "timeIntent": null, "modeHints": []}
    "leaving home around 9" -> {"originText": "home", "destinationText": null, "timeIntent": {"isArrival": false, "timeText": "around 9"}, "modeHints": []}
    "town hall to bondi junction by 6pm friday" -> {"originText": "town hall", "destinationText": "bondi junction", "timeIntent": {"isArrival": true, "timeText": "6pm friday"}, "modeHints": []}

    Rider's message: "$riderText"

    JSON:
""".trimIndent()

private val noOpDownloadCallback = object : DownloadCallback {
    override fun onDownloadStarted(bytesToDownload: Long) = Unit
    override fun onDownloadProgress(totalBytesDownloaded: Long) = Unit
    override fun onDownloadCompleted() = Unit
    override fun onDownloadFailed(exception: GenAiException) = Unit
}
