package xyz.ksharma.krail.core.appinfo.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.krail.core.appinfo.AndroidAppInfoProvider
import xyz.ksharma.krail.core.appinfo.AppInfoProvider

actual val appInfoModule = module {
    single<AppInfoProvider> {
        AndroidAppInfoProvider(
            context = androidContext(),
        )
    }
}
