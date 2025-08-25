package xyz.ksharma.krail.core.appversion

import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
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

    /**
     * Checks if an optional update is available.
     * Returns true if an update is required, false otherwise.
     */
    suspend fun isOptionalUpdateAvailable(): Boolean =
        checkForUpdates() is AppVersionUpdateState.UpdateRequired

    /**
     * Checks if a forced update is required.
     */
    suspend fun isForcedUpdateRequired(): Boolean =
        checkForUpdates() is AppVersionUpdateState.ForcedUpdateRequired

    /**
     * Retrieves the copy for the app version update dialog based on the current state.
     * Returns an [AppVersionUpdateCopy] containing the title, description, and call-to-action text.
     */
    suspend fun getUpdateCopy(): AppVersionUpdateCopy?

    /**
     * Data class representing the copy text for the app version update UI elements.
     * Contains the title, description, and call-to-action text.
     */
    data class AppVersionUpdateCopy(
        val title: String,
        val description: String,
        val ctaText: String
    )
}

class RealAppVersionManager(
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
                logError("Cannot fetch latest app version for unknown platform: ")
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

        if (compareVersions(current, latestAppVersion) > 0) {
            logError("Current version ($current) is ahead of latest flag ($latestAppVersion) -> flag value is likely stale")
        }

        log("Checking app version: current=$current, minimumSupported=$minimumSupportedAppVersion, latest=$latestAppVersion")

        val state = when {
            compareVersions(current, minimumSupportedAppVersion) < 0 ->
                AppVersionUpdateState.ForcedUpdateRequired

            compareVersions(current, latestAppVersion) < 0 ->
                AppVersionUpdateState.UpdateRequired

            else -> AppVersionUpdateState.UpToDate
        }
        log("App version update state: $state")
        return state
    }

    override fun getCurrentVersion(): String = appInfoProvider.getAppInfo().appVersion

    // (TODO) pull copy from remote flags.
    override suspend fun getUpdateCopy(): AppVersionManager.AppVersionUpdateCopy? {
        return when (checkForUpdates()) {
            is AppVersionUpdateState.UpToDate -> return null

            is AppVersionUpdateState.UpdateRequired -> AppVersionManager.AppVersionUpdateCopy(
                title = "KRAIL just got better! \uD83D\uDE80",
                description = "Smoother, faster and ready for your next journey.",
                ctaText = "Get the Update",
            )

            is AppVersionUpdateState.ForcedUpdateRequired -> AppVersionManager.AppVersionUpdateCopy(
                title = "\uD83D\uDEA7 Time to Update \uD83D\uDEA7",
                description = "Important fixes and updates ahead — required to keep KRAIL running at its best.",
                //"This important update keeps KRAIL running smoothly and hope you enjoy latest\u00A0improvements!",
                ctaText = "Update Now",
            )
        }
    }

    /**
     * Compares two version strings and returns:
     * - A negative number if `currentVersion` is less than `other`
     * - Zero if they are equal
     * - A positive number if `currentVersion` is greater than `other`
     */
    private fun compareVersions(currentVersion: String, other: String): Int {
        // Parse versions into number lists: Converts version strings like "1.9.0" into lists of integers [1, 9, 0]
        val currentVersionNumbers = parseAppVersion(currentVersion)
        val otherVersionNumbers = parseAppVersion(other)

        // Handle different lengths: Uses the maximum length of both version arrays to ensure all components are compared
        val max = maxOf(currentVersionNumbers.size, otherVersionNumbers.size)

        // Component-by-component comparison: Iterates through each version component (major, minor, patch, etc.)
        for (i in 0 until max) {
            // Safe array access: Uses getOrElse(i) { 0 } to treat missing components as 0
            // Example: "1.9" vs "1.9.0" → treats "1.9" as "1.9.0"
            val ai = currentVersionNumbers.getOrElse(i) { 0 }
            val bi = otherVersionNumbers.getOrElse(i) { 0 }

            // Return comparison result as soon as we find a difference:
            // Negative: current version < other version
            // Positive: current version > other version
            if (ai != bi) return ai - bi
        }

        // Zero: versions are equal (all components matched)
        return 0
    }

    private fun parseAppVersion(strVersion: String): List<Int> =
        strVersion.split('.')
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
