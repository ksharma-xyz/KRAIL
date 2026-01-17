package xyz.ksharma.krail

import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import xyz.ksharma.krail.di.initKoin

class KrailApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@KrailApplication)
            androidLogger()
        }
        Firebase.initialize(context = this)
    }
}

