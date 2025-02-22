package xyz.ksharma.krail.io.gtfs.di

import org.koin.dsl.module
import xyz.ksharma.krail.io.gtfs.nswstops.IosResourceFactory
import xyz.ksharma.krail.io.gtfs.nswstops.ResourceFactory

actual val resourceLoaderModule = module {
    single<ResourceFactory> { IosResourceFactory() }
}
