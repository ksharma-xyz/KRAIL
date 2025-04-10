package xyz.ksharma.krail.platform.ops.di

import org.koin.dsl.module
import xyz.ksharma.krail.platform.ops.ContentSharing
import xyz.ksharma.krail.platform.ops.IosContentSharing

actual val opsModule = module {
    single<ContentSharing> { IosContentSharing() }
}