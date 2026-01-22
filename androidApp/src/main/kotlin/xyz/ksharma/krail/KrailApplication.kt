package xyz.ksharma.krail

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import xyz.ksharma.krail.di.initKoin

class KrailApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase (GitLive Firebase API in composeApp/commonMain)
        initializeFirebase(context = this)

        // Initialize Koin DI
        initKoin {
            androidContext(this@KrailApplication)
            androidLogger()
        }
    }
}
