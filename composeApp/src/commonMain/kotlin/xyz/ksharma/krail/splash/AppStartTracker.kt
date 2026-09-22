package xyz.ksharma.krail.splash

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.coroutines.ext.safeResult

/**
 * Sends `app_start`, including whether this device can run Ask KRAIL (see [AiCapability]).
 *
 * A concrete class with no interface: nothing needs a second implementation. Tests build the
 * real one over fakes of its boundaries, which exercises the code that ships.
 *
 * @param peekAiAvailability [AiTextService.peekExtractionAvailability] in production. Taken as
 * a function so tests need no fake of the whole service, and so asking can never go through
 * the variant that starts a model download.
 * @param isAiSearchEnabled Reads the Ask KRAIL flag, the same read the dialog uses.
 */
class AppStartTracker(
    private val analytics: Analytics,
    private val appInfoProvider: AppInfoProvider,
    private val peekAiAvailability: suspend () -> AiAvailability,
    private val isAiSearchEnabled: () -> Boolean,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Launched rather than awaited: the Ask KRAIL availability check is a platform call, and
     * the splash must never wait on it. The event goes out once the check answers or
     * [AiCapability.TIMEOUT_MS] passes, whichever is first. Nothing in here can fail the
     * launch: [AiCapability.of] never throws, a flag read that throws is reported as absent,
     * and anything else lands in the exception handler.
     */
    fun track(scope: CoroutineScope, krailThemeId: Int) {
        scope.launchWithExceptionHandler<AppStartTracker>(ioDispatcher) {
            val aiCapability = AiCapability.of(peekAiAvailability, ioDispatcher)
            val aiSearchEnabled = safeResult(ioDispatcher) { isAiSearchEnabled() }.getOrNull()
            send(krailThemeId, aiCapability, aiSearchEnabled)
        }
    }

    private fun send(
        krailThemeId: Int,
        aiCapability: String,
        aiSearchEnabled: Boolean?,
    ) = with(appInfoProvider.getAppInfo()) {
        log("AppInfo: $this, krailTheme: $krailThemeId, aiCapability: $aiCapability")
        analytics.track(
            AnalyticsEvent.AppStart(
                platformType = devicePlatformType.name,
                osVersion = osVersion,
                appVersion = appVersion,
                fontSize = fontSize,
                isDarkTheme = isDarkTheme,
                deviceModel = deviceModel,
                krailTheme = krailThemeId,
                locale = locale,
                timeZone = timeZone,
                aiCapability = aiCapability,
                aiSearchEnabled = aiSearchEnabled,
            ),
        )
    }
}
