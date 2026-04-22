package xyz.ksharma.krail.feature.track.network

import io.ktor.client.HttpClient

expect fun gtfsRealtimeHttpClient(baseClient: HttpClient): HttpClient
