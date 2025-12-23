package xyz.ksharma.core.test.appversion

import kotlinx.coroutines.test.runTest
import xyz.ksharma.core.test.fakes.FakeAppInfo
import xyz.ksharma.core.test.fakes.FakeAppInfoProvider
import xyz.ksharma.core.test.fakes.FakeFlag
import xyz.ksharma.krail.core.appversion.AppVersionUpdateState
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.appversion.RealAppVersionManager
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import kotlin.test.Test
import kotlin.test.assertEquals

class AppVersionManagerTest {

    private val fakeFlag = FakeFlag()
    private val fakeAppInfoProvider = FakeAppInfoProvider()
    private val appVersionManager = RealAppVersionManager(fakeAppInfoProvider, fakeFlag)

    @Test
    fun `getCurrentVersion returns app version from AppInfoProvider`() {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "2.1.0")

        // When
        val result = appVersionManager.getCurrentVersion()

        // Then
        assertEquals("2.1.0", result)
    }

    @Test
    fun `checkForUpdates returns UpToDate when current version equals latest`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0"),
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `checkForUpdates returns UpdateRequired when current version is less than latest`() =
        runTest {
            // Given
            fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.4.0",)
            fakeFlag.setFlagValue(
                FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
                FlagValue.StringValue("1.0.0")
            )
            fakeFlag.setFlagValue(
                FlagKeys.LATEST_APP_VERSION_ANDROID.key,
                FlagValue.StringValue("1.5.0")
            )

            // When
            val result = appVersionManager.checkForUpdates()

            // Then
            assertEquals(AppVersionUpdateState.UpdateRequired, result)
        }

    @Test
    fun `checkForUpdates returns ForcedUpdateRequired when current version is less than minimum supported`() =
        runTest {
            // Given
            fakeAppInfoProvider.mockAppInfo = FakeAppInfo(
                appVersion = "0.9.0",
                
            )
            fakeFlag.setFlagValue(
                FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
                FlagValue.StringValue("1.0.0")
            )
            fakeFlag.setFlagValue(
                FlagKeys.LATEST_APP_VERSION_ANDROID.key,
                FlagValue.StringValue("1.5.0")
            )

            // When
            val result = appVersionManager.checkForUpdates()

            // Then
            assertEquals(AppVersionUpdateState.ForcedUpdateRequired, result)
        }

    @Test
    fun `checkForUpdates uses iOS version flag for iOS platform`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(
            appVersion = "1.4.0",
            devicePlatformType = DevicePlatformType.IOS,
        )
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(FlagKeys.LATEST_APP_VERSION_IOS.key, FlagValue.StringValue("1.5.0"))

        // When
        val result = appVersionManager.checkForUpdates()

        // Then
        assertEquals(AppVersionUpdateState.UpdateRequired, result)
    }

    @Test
    fun `checkForUpdates returns UpToDate when current version is blank`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "")

        // When
        val result = appVersionManager.checkForUpdates()

        // Then
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `checkForUpdates returns UpToDate for unknown platform`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(
            appVersion = "1.0.0",
            devicePlatformType = DevicePlatformType.UNKNOWN
        )
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("0.9.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `version comparison works correctly for complex versions`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(
            appVersion = "1.2.3",
            
        )
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.2.2")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.2.4")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then
        assertEquals(AppVersionUpdateState.UpdateRequired, result)
    }

    @Test
    fun `checkForUpdates returns UpdateRequired when current version 1_9_0 is less than latest 2_0_0`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.9.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("2.0.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then
        assertEquals(AppVersionUpdateState.UpdateRequired, result)
    }

    // Tests for new fallback logic and edge cases

    @Test
    fun `checkForUpdates returns UpToDate when latestAppVersion Remote Config is empty string`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("") // Empty string - should use fallback
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Should return UpToDate because latest falls back to current version (1.5.0)
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `checkForUpdates returns UpToDate when minimumSupportedAppVersion Remote Config is empty string`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("") // Empty string - should use "1.0.0" fallback
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("2.0.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Should work with fallback minimum version "1.0.0"
        assertEquals(AppVersionUpdateState.UpdateRequired, result)
    }

    @Test
    fun `checkForUpdates returns UpToDate when both Remote Config values are empty`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Guard clause should catch this and return UpToDate
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `checkForUpdates returns UpToDate when latestAppVersion is blank with whitespace`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("   ") // Whitespace only
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Should handle blank strings (including whitespace)
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `checkForUpdates uses current version as fallback when iOS latest version is empty`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(
            appVersion = "1.8.0",
            devicePlatformType = DevicePlatformType.IOS
        )
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_IOS.key,
            FlagValue.StringValue("") // Empty - should fallback to 1.8.0
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Current (1.8.0) equals latest (1.8.0 fallback) -> UpToDate
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `checkForUpdates still works when minimum version has fallback and latest is valid`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "0.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("") // Empty - fallback to "1.0.0"
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("2.0.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Current (0.5.0) < minimum (1.0.0 fallback) -> ForcedUpdateRequired
        assertEquals(AppVersionUpdateState.ForcedUpdateRequired, result)
    }

    @Test
    fun `checkForUpdates returns UpToDate when current version is ahead of latest due to empty Remote Config`() = runTest {
        // Given - Testing beta/staging scenario
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "2.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("") // Empty - falls back to current 2.5.0
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Current equals latest (both 2.5.0) -> UpToDate
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `checkForUpdates returns UpToDate when current version is legitimately ahead of latest`() = runTest {
        // Given - Beta tester scenario
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "3.0.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("2.0.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - Current (3.0.0) > latest (2.0.0) -> UpToDate (logs warning)
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `version comparison handles versions with different component counts`() = runTest {
        // Given - Testing "1.9" vs "1.9.0" scenario
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.9")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.9.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - 1.9 should be treated as 1.9.0, so UpToDate
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `version comparison handles versions with non-numeric suffixes`() = runTest {
        // Given - Testing "1.9.0-beta" scenario
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.9.0-beta")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.9.0")
        )

        // When
        val result = appVersionManager.checkForUpdates()

        // Then - parseAppVersion filters out non-digits, so "1.9.0-beta" becomes "1.9.0"
        assertEquals(AppVersionUpdateState.UpToDate, result)
    }

    @Test
    fun `getUpdateCopy returns null when app is up to date`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.getUpdateCopy()

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `getUpdateCopy returns optional update copy when update is required`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.4.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.getUpdateCopy()

        // Then
        assertEquals("KRAIL just got better! 🚀", result?.title)
        assertEquals("Smoother, faster and ready for your next journey.", result?.description)
        assertEquals("Get the Update", result?.ctaText)
    }

    @Test
    fun `getUpdateCopy returns forced update copy when forced update is required`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "0.9.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.getUpdateCopy()

        // Then
        assertEquals("🚧 Time to Update 🚧", result?.title)
        assertEquals(
            "Important fixes and updates ahead — required to keep KRAIL running at its best.",
            result?.description
        )
        assertEquals("Update Now", result?.ctaText)
    }

    @Test
    fun `isOptionalUpdateAvailable returns true when update is required`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.4.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.isOptionalUpdateAvailable()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `isOptionalUpdateAvailable returns false when app is up to date`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.isOptionalUpdateAvailable()

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `isForcedUpdateRequired returns true when forced update is required`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "0.9.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.isForcedUpdateRequired()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `isForcedUpdateRequired returns false when app is up to date`() = runTest {
        // Given
        fakeAppInfoProvider.mockAppInfo = FakeAppInfo(appVersion = "1.5.0")
        fakeFlag.setFlagValue(
            FlagKeys.MIN_SUPPORTED_APP_VERSION.key,
            FlagValue.StringValue("1.0.0")
        )
        fakeFlag.setFlagValue(
            FlagKeys.LATEST_APP_VERSION_ANDROID.key,
            FlagValue.StringValue("1.5.0")
        )

        // When
        val result = appVersionManager.isForcedUpdateRequired()

        // Then
        assertEquals(false, result)
    }
}
