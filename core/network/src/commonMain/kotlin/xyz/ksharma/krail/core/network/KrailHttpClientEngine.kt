package xyz.ksharma.krail.core.network

import io.ktor.client.engine.HttpClientEngine

/**
 * The engine, and nothing else.
 *
 * Declared explicitly rather than through Ktor 3.6's `ktor-client-engine-defaults`
 * artifact: the engine each platform runs is the thing the failure classifiers in
 * `error/NetworkErrorClassifier.*.kt` are written against, so it is a decision worth
 * stating in source rather than one worth resolving from the dependency graph.
 * `docs/KTOR_CLIENT_ARCHITECTURE.md` records that comparison in full.
 */
internal expect fun krailHttpClientEngine(): HttpClientEngine
