package xyz.ksharma.krail.platform.ops.di

import org.koin.dsl.module
import xyz.ksharma.krail.platform.ops.AndroidPlatformOps
import xyz.ksharma.krail.platform.ops.PlatformOps

actual val opsModule = module {
    single<PlatformOps> { AndroidPlatformOps(context = get()) }
}
