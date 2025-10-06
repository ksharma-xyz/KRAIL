package xyz.ksharma.krail.theme.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.krail.theme.AndroidThemeManagerImpl
import xyz.ksharma.krail.theme.ThemeManager

actual val themeManagerModule = module {
    single<ThemeManager> {
        AndroidThemeManagerImpl(
            context = androidContext(),
        )
    }
}
