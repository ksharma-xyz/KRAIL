package xyz.ksharma.krail.trip.planner.network.api.service

import io.ktor.client.HttpClient
import xyz.ksharma.krail.core.appinfo.AppInfoProvider

expect fun httpClient(appInfoProvider: AppInfoProvider): HttpClient
