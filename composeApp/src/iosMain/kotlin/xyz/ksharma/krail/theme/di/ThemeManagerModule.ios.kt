package xyz.ksharma.krail.theme.di

import org.koin.dsl.module
import xyz.ksharma.krail.theme.IOSThemeManagerImpl
import xyz.ksharma.krail.theme.ThemeManager

actual val themeManagerModule = module {
    single<ThemeManager> {
        IOSThemeManagerImpl()
    }
}
