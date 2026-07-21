package xyz.ksharma.krail

import android.app.Application
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module
import xyz.ksharma.krail.deeplink.AppDeepLinkHandler
import xyz.ksharma.krail.deeplink.RealAppDeepLinkHandler
import xyz.ksharma.krail.di.initKoin
import xyz.ksharma.krail.platform.ops.CurrentActivityHolder

class KrailApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase (GitLive Firebase API in composeApp/commonMain)
        initializeFirebase(context = this)

        // Initialize Koin DI
        initKoin {
            androidContext(this@KrailApplication)
            androidLogger()
            modules(androidAppModule)
        }

        // Play's in-app review flow needs a real Activity, which the application Context
        // cannot stand in for. Tracking starts here so the holder is current before any
        // screen can trigger a request.
        get<CurrentActivityHolder>().startTracking(application = this)
    }
}

/** Android-platform bindings that can't live in the shared composeApp module. */
private val androidAppModule = module {
    single<AppDeepLinkHandler> {
        RealAppDeepLinkHandler(
            pendingDeepLinkManager = get(),
            flag = get(),
            appInfoProvider = get(),
            debugNetworkConfigStore = get(),
        )
    }
}
