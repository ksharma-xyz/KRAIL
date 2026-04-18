package xyz.ksharma.krail.core.share.di
import org.koin.dsl.module
import xyz.ksharma.krail.core.share.IosShareManager
import xyz.ksharma.krail.core.share.ShareManager
actual val shareModule = module {
    single<ShareManager> {
        IosShareManager()
    }
}
