package xyz.ksharma.krail.platform.ops.di

import org.koin.dsl.module
import xyz.ksharma.krail.platform.ops.AndroidContentSharing
import xyz.ksharma.krail.platform.ops.ContentSharing

actual val opsModule = module {
    single<ContentSharing> { AndroidContentSharing(context = get()) }
}
