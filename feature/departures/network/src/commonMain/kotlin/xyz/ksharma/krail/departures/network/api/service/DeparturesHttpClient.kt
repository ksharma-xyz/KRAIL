package xyz.ksharma.krail.departures.network.api.service

import io.ktor.client.HttpClient

expect fun departuresHttpClient(baseClient: HttpClient): HttpClient
