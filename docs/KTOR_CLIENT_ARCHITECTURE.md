# The Ktor client

How KRAIL's one HTTP client is put together, which parts of Ktor it deliberately
does not use, and what to check when the version moves.

`docs/NETWORK_RELIABILITY.md` is the companion: it covers what a failure *means* and
what the app may say about it. This file covers the client that produces the failure.

---

## One client, one configuration, one seam

There is exactly one `HttpClient` in the app. It is built by `baseHttpClient()` in
`:core:network` and handed out by Koin as a singleton. Every service takes it as a
constructor parameter.

Since the 3.6 upgrade the client has one platform-specific line:

```
commonMain/HttpClient.kt                 baseHttpClient()  — builds the client
commonMain/KrailHttpClientDefaults.kt    installKrailDefaults()  — every plugin
commonMain/KrailHttpClientEngine.kt      expect fun krailHttpClientEngine()
androidMain/KrailHttpClientEngine.android.kt   = OkHttp.create()
iosMain/KrailHttpClientEngine.ios.kt           = Darwin.create()
```

Before, `baseHttpClient` was itself an `expect`/`actual` pair, and the entire
configuration block existed twice as a byte-for-byte copy. Two copies of a request
pipeline is two pipelines. An edit to one is invisible to the other, nothing compares
them, and the first symptom is a behaviour that reproduces on one platform only. The
engine is the one thing that genuinely differs, so the engine is the only thing a
platform source set now declares.

This is the same rule the repo already applied to credentials: a new upstream that
needs a header adds an `ApiCredential` case, not an `expect`/`actual` pair.

### Plugin order is a decision, not an accident

`installKrailDefaults` installs, in this order:

| # | Plugin | Why it sits there |
|---|---|---|
| 1 | `defaultRequest` | Stamps `X-Krail-Version` on every call, retries included |
| 2 | `ContentNegotiation` | JSON decoding. Never overrides an `Accept` a caller set |
| 3 | `Logging` | Debug builds only. Method, URL, status. Never bodies |
| 4 | `HttpRequestRetry` (`installKrailRetry`) | Before the validator, so a retried response is validated too |
| 5 | Captive-portal validator | Runs while the response headers still exist |
| 6 | `HttpTimeout` | Last, so its budget covers everything above it |

Reordering 4 and 5 would let a captive portal's login page be retried as if it were a
transient server failure. Reordering 6 would time the request without timing the
retries.

### The engine is load-bearing for the error taxonomy

`error/NetworkErrorClassifier.android.kt` is written against OkHttp's exceptions:
`UnknownHostException` for both airplane mode and a genuine DNS failure,
`java.net.SocketTimeoutException` for a stalled read. `NetworkErrorClassifier.ios.kt`
reads `DarwinHttpRequestException.origin.code` against Apple's `NSURLErrorDomain`
values, which no other engine raises.

Changing either engine silently invalidates a classifier and every test written
against it. That is why the engine is named in source rather than resolved from the
dependency graph — see the `ktor-client-engine-defaults` entry below.

---

## Version: 3.6.0

Upgraded from 3.4.1 (3.5.0, 3.5.1, 3.5.2 and 3.6.0 were all skipped over at once).
No API KRAIL uses changed. Nothing in the diff was forced by the upgrade.

### What the upgrade is actually for

KRAIL cancels HTTP requests constantly — polling is gated on `WhileSubscribed`, so
every navigation away and every backgrounding cancels an in-flight call. Five fixes in
this range are in exactly that path:

| Issue | Fix | Why it matters here |
|---|---|---|
| KTOR-9773 | OkHttp cancellation could close the response body on the Android main thread | A cancelled poll doing disk/socket teardown on the main thread |
| KTOR-9762 | Android could hang when cancelling a streaming response | GTFS realtime and the static feed both stream bytes |
| KTOR-9627 | Blocking bridges are cancelled with the coroutine | Same path, one layer down |
| KTOR-9870 | OkHttp: fewer coroutine dispatches and allocations | Every request, on the platform most riders use |
| KTOR-9834 | `HttpClient` no longer eagerly initialises SLF4J during Android startup | The client is a Koin singleton built during startup |

And one on the iOS side: KTOR-9817, where Darwin's `CertificatePinner` over-released
Core Foundation references.

None of these were observed as a KRAIL bug. They are the reason the upgrade is worth
doing rather than deferring, not a claim that something was broken.

### What was evaluated and not adopted

**`ktor-client-engine-defaults` (KTOR-9645).** A new curated artifact: add it to
`commonMain` and `HttpClient()` picks an engine per target. It resolves to the right
engines for KRAIL — the JVM variant depends on `ktor-client-okhttp`, the Apple
variants on `ktor-client-darwin` — so adopting it would compile and run.

Declined for three reasons:

1. There is no `androidJvm` variant. An Android consumer resolves the **JVM** variant,
   which also drags `org.slf4j:slf4j-api` onto Android for nothing.
2. It converts an explicit decision into a resolution order. The engine is not an
   implementation detail here; two classifier files and their tests depend on which
   one runs. A future Ktor release changing engine priority would change KRAIL's error
   classification with no diff in this repo.
3. It saves two one-line dependency declarations.

Revisit if KRAIL adds a third platform, where the arithmetic changes.

**`ContentTypeMergeStrategy.SkipIfPresent` (KTOR-5009).** New in 3.6: stops
`ContentNegotiation` from appending its registered types to an `Accept` header the
caller set explicitly.

Five services (`RealTripPlanningService`, `RealDeparturesService`,
`RealGtfsRealtimeService`, `BffGtfsRealtimeRepository`, `RealParkRideService`) ask for
`application/x-protobuf` and then read the response with `readRawBytes()`. If the JSON
converter appended `application/json`, an upstream would be free to answer JSON to a
caller about to hand the bytes to a protobuf decoder, and the failure would surface as
a decode error a long way from its cause.

It does not append — measured, not assumed, on both 3.4.1 and 3.6.0 with a `MockEngine`
recording the outgoing header. So the setting is not needed, and adding it would be a
second place where the two facts have to agree. `ContentNegotiationAcceptHeaderTest`
now pins the behaviour instead, so an upgrade that changes the default fails the build
rather than a rider's trip.

**Commonised `HttpCache` file storage (KTOR-9735).** `FileCacheStorage` now uses
`kotlinx-io` `Path` and works outside the JVM, so a shared on-disk HTTP cache is
possible for the first time. Genuinely interesting for the GTFS static feed, which
downloads large archives. Out of scope here: a cache is a correctness decision about
staleness on a live departures screen, not a dependency bump. Tracked as follow-up.

### Dependencies removed in the same change

- `ktor-client-auth` from six modules. No module installs the `Auth` plugin; KRAIL
  authenticates with a header from `ApiCredential`.
- The whole Ktor block from `:io:gtfs`, which imports no Ktor type at all.
- `ktor-client-darwin` from `:sandook`, which is a database module.

Left alone deliberately: the engine declarations in `:composeApp` and `:discover:ui`.
They look unused because no Kotlin file imports them, but both depend on
`coil-network-ktor3`, which builds its own client and needs an engine on the
classpath. Removing them compiles and then fails at runtime when an image loads.

---

## When the version moves again

1. Re-run `ContentNegotiationAcceptHeaderTest`. It is the only guard on the protobuf
   endpoints' `Accept` header.
2. Re-run `NetworkErrorClassifierTest`. It drives a real client at unresolvable hosts,
   refused connections and timeouts, so it is what catches an engine changing which
   exception it throws.
3. Check the release notes for engine changes on OkHttp and Darwin specifically. The
   rest of the client is plugins, and plugins fail loudly. The engine fails quietly, by
   changing an exception type that a `when` branch still compiles against.
4. Check whether `ktor-client-engine-defaults` has grown an `androidJvm` variant. That
   would remove the first of the three reasons above.
