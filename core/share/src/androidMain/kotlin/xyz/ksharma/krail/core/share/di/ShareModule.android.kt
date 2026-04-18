package xyz.ksharma.krail.core.share.di
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import xyz.ksharma.krail.core.share.AndroidShareManager
import xyz.ksharma.krail.core.share.ShareManager
actual val shareModule = module {
    single<ShareManager> {
        AndroidShareManager(context = androidContext())
    }
}
