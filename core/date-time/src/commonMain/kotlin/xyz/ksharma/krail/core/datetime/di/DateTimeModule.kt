package xyz.ksharma.krail.core.datetime.di

import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The single place wall-clock time enters the object graph.
 *
 * Anything that asks "what time is it?" takes a [Clock] instead of calling
 * `Clock.System.now()` inline, so a test can hand it a clock it controls. The
 * seam has two shapes, and which one to use follows from how long the caller
 * lives:
 *
 *  - **Long-lived collaborators** (ViewModels, pollers, cache managers) take a
 *    `clock: Clock` constructor parameter, defaulting to [Clock.System]. They read
 *    the time repeatedly, at moments the test wants to control, so they need the
 *    clock itself rather than one reading of it.
 *  - **Pure helpers** (`DateTimeHelper`, `ParkRideRefreshHelper`) take
 *    `now: Instant = Clock.System.now()`. They answer one question about one
 *    instant, so passing the instant keeps them free of any dependency.
 *
 * Production resolves the same [Clock.System] either way — the default parameter
 * and this Koin binding are the same object — so nothing changes at runtime.
 */
@OptIn(ExperimentalTime::class)
val dateTimeModule = module {
    single<Clock> { Clock.System }
}
