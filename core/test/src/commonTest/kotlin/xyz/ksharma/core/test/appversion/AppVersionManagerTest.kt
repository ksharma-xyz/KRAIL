package xyz.ksharma.core.test.appversion

import kotlinx.coroutines.test.runTest
import xyz.ksharma.core.test.fakes.FakeAppInfo
import xyz.ksharma.core.test.fakes.FakeAppInfoProvider
import xyz.ksharma.core.test.fakes.FakeFlag
import xyz.ksharma.krail.core.appinfo.AppVersionUpdateState
import xyz.ksharma.krail.core.appinfo.DevicePlatformType
import xyz.ksharma.krail.core.appinfo.RealAppVersionManager
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
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
}
