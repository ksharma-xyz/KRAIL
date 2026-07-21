package xyz.ksharma.krail.platform.ops.di

import org.koin.dsl.module
import xyz.ksharma.krail.platform.ops.AndroidPlatformOps
import xyz.ksharma.krail.platform.ops.CurrentActivityHolder
import xyz.ksharma.krail.platform.ops.PlatformOps

actual val opsModule = module {
    single<PlatformOps> { AndroidPlatformOps(context = get()) }

    // Must be a single: it is only useful if every reader sees the same tracked Activity.
    single { CurrentActivityHolder() }
}
