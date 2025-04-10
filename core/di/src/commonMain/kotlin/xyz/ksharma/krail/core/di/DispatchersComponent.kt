package xyz.ksharma.krail.core.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.DefaultDispatcher
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher

open class DispatchersComponent : KoinComponent {

    val defaultDispatcher: CoroutineDispatcher by inject(named(DefaultDispatcher))

    val ioDispatcher: CoroutineDispatcher by inject(named(IODispatcher))

    companion object{
        const val IODispatcher = "IODispatcher"
        const val DefaultDispatcher = "DefaultDispatcher"
    }
}

val coroutineDispatchersModule = module {

    single(named(DefaultDispatcher)) {
        Dispatchers.Default
    }

    single(named(IODispatcher)) {
        Dispatchers.IO
    }

    single {
        CoroutineScope(context = SupervisorJob() + Dispatchers.Default)
    }
}
