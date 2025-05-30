package xyz.ksharma.krail.park.ride.network

import io.ktor.client.HttpClient

expect fun parkRideHttpClient(baseClient: HttpClient): HttpClient
