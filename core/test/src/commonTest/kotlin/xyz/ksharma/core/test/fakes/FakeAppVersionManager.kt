package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.core.appversion.AppVersionUpdateState

class FakeAppVersionManager: AppVersionManager {

    var versionUpdateState: AppVersionUpdateState = AppVersionUpdateState.UpToDate
    var mockCurrentVersion: String = "1.0.0"

    override suspend fun checkForUpdates(): AppVersionUpdateState {
        return versionUpdateState
    }

    override fun getCurrentVersion(): String {
        return mockCurrentVersion
    }
}
