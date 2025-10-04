package xyz.ksharma.krail.theme.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.krail.theme.AndroidThemeManagerImpl
import xyz.ksharma.krail.theme.ThemeManager

actual val themeManagerModule = module {
    single<ThemeManager> {
        AndroidThemeManagerImpl(
            context = androidContext(),
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
}
