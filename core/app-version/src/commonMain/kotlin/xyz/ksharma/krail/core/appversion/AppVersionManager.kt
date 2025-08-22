package xyz.ksharma.krail.core.appinfo

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.asString

interface AppVersionManager {

    /**
     * Checks the current app version against the latest available version.
     * Returns an [AppVersionUpdateState] indicating whether the app is up to date,
     * if an update is required, or if a forced update is necessary.
     */
    suspend fun checkForUpdates(): AppVersionUpdateState

    /**
     * Retrieves the current version of the app.
     */
    fun getCurrentVersion(): String
}

internal class RealAppVersionManager(
    private val appInfoProvider: AppInfoProvider,
    private val flag: Flag,
) : AppVersionManager {

    private val minimumSupportedAppVersion: String by lazy {
        flag.getFlagValue(FlagKeys.MIN_SUPPORTED_APP_VERSION.key).asString()
    }

    private val latestAppVersion: String by lazy {

        when (appInfoProvider.getAppInfo().devicePlatformType) {
            DevicePlatformType.ANDROID -> {
                log("Fetching latest app version for Android: ")
                flag.getFlagValue(FlagKeys.LATEST_APP_VERSION_ANDROID.key)
                    .asString()
            }

            DevicePlatformType.IOS -> {
                log("Fetching latest app version for iOS: ")
                flag.getFlagValue(FlagKeys.LATEST_APP_VERSION_IOS.key)
                    .asString()
            }

            DevicePlatformType.UNKNOWN -> {
                log("Fetching latest app version for unknown platform: ")
                // If the platform is unknown, we can't determine the latest version
                ""
            }
        }
    }

    override suspend fun checkForUpdates(): AppVersionUpdateState {
        log("Checking app version updates...")
        val current = getCurrentVersion()
        log("Current app version: $current")
        if (current.isBlank()) return AppVersionUpdateState.UpToDate

        log("Checking app version: current=$current, minimumSupported=$minimumSupportedAppVersion, latest=$latestAppVersion")

        val x = when {
            compareVersions(current, minimumSupportedAppVersion) < 0 ->
                AppVersionUpdateState.ForcedUpdateRequired

            compareVersions(current, latestAppVersion) < 0 ->
                AppVersionUpdateState.UpdateRequired

            else -> AppVersionUpdateState.UpToDate
        }
        log("App version update state: $x")
        return x
    }

    override fun getCurrentVersion(): String = appInfoProvider.getAppInfo().appVersion

    private fun compareVersions(currentVersion: String, other: String): Int {
        val av = parse(currentVersion)
        val bv = parse(other)
        val max = maxOf(av.size, bv.size)
        for (i in 0 until max) {
            val ai = av.getOrElse(i) { 0 }
            val bi = bv.getOrElse(i) { 0 }
            if (ai != bi) return ai - bi
        }
        return 0
    }

    private fun parse(v: String): List<Int> =
        v.split('.')
            .map { seg ->
                seg.filter { it.isDigit() }
                    .takeIf { it.isNotEmpty() }
                    ?.toIntOrNull() ?: 0
            }
}


sealed interface AppVersionUpdateState {

    /**
     * Indicates that the app is up to date and no updates are available.
     */
    data object UpToDate : AppVersionUpdateState

    /**
     * Indicates that an update is available, but it is optional for the user to update the app.
     */
    data object UpdateRequired : AppVersionUpdateState

    /**
     * Indicates that an update is available and it is mandatory for the user to update the app.
     * The user must update the app to continue using it.
     */
    data object ForcedUpdateRequired : AppVersionUpdateState
}
