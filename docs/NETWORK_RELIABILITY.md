# Network reliability

How KRAIL decides why a request failed, and what it is allowed to tell a rider about
it.

Read this before changing anything under `core/network/`, before adding a new
`Real*Service`, and before adding a new upstream API.

---

## The one rule

**Transport state is an input to classifying a failure. The request outcome is the
authority.**

Everything else in this document follows from that sentence, so it is worth being
precise about why the obvious alternative is wrong.

The obvious design is a boolean called `isOnline`, read before each request. It
produces a system that lies to riders confidently, for two independent reasons.

**The OS answer is a hint, not a fact.** `ConnectivityManager` and `NWPathMonitor`
report the state of the local transport, not whether anything can be reached. A
phone joined to a cafe Wi-Fi behind a captive portal reports connected. So does one
on a VPN whose tunnel has dropped, one on a carrier blackholing packets, and one
with full bars and a dead NSW upstream. Android's `NET_CAPABILITY_VALIDATED` is a
real improvement and is what `AndroidConnectivityObserver` gates on, but it is still
a probe result from a moment ago, not a guarantee about the next request.

**The exception alone cannot classify either.** On Android, airplane mode and a
genuine DNS failure on a working network both surface as `UnknownHostException`.
Nothing in the exception distinguishes "you have no network" from "the network you
have cannot find this host". Those are different sentences to a rider and one of
them is not their fault.

Hence the signature:

```kotlin
expect fun Throwable.toNetworkError(isTransportDown: Boolean): NetworkError
```

Same throwable, different answer depending on what the OS was reporting at the
moment it was thrown.

### The corollary the UI has to honour

The app never renders a claim sourced only from the OS flag. "You are offline" is
shown when a request failed **and** transport was down. An idle app with the radio
off shows nothing new until it tries something and fails.

Note the parameter's polarity. It is `isTransportDown`, which maps to
`TransportState.mayClaimOffline`, and that is **false** while the state is still
`Unknown`. An app that has not yet heard from the OS must not tell a rider they are
offline.

---

## The three transport states

`TransportState` has three values, not two, and the third is load-bearing.

| State | Meaning | `shouldAttemptRequest` | `mayClaimOffline` |
|---|---|---|---|
| `Up` | OS reports a validated route | yes | no |
| `Down` | OS reports no usable network | no | yes |
| `Unknown` | no callback has arrived yet | **yes** | **no** |

`Unknown` is the initial value. Reporting `Down` before the first callback would
make a perfectly good first request fail fast, which is exactly the bug a naive
`isOnline = false` default produces. Read it as "do not act on this yet": it is
permission to try, and it is not evidence of anything.

---

## Failure taxonomy

This table is the specification for `toNetworkError` and the fixture its tests are
written from. Three things read it: the classifier implementations, the classifier
tests, and the copy any screen shows. Keep one copy, here.

| Condition | Android (OkHttp) | iOS (Darwin) | Transport | Classify | Retry |
|---|---|---|---|---|---|
| Airplane mode, radio off | `UnknownHostException` | `NSURLErrorNotConnectedToInternet` (-1009) | down | `Offline` | no, wait for reconnect |
| DNS fails, radio up | `UnknownHostException` | `NSURLErrorCannotFindHost` (-1003), `NSURLErrorDNSLookupFailed` (-1006) | up | `Unreachable` | yes, backoff |
| Connect refused or reset | `ConnectException`, `SocketException`, `NoRouteToHostException` | `NSURLErrorCannotConnectToHost` (-1004), `NSURLErrorNetworkConnectionLost` (-1005) | up | `Unreachable` | yes, backoff |
| Connect timeout | `ConnectTimeoutException` (Ktor) | same | either | `Timeout` | yes, once |
| Read stalls mid-body | `SocketTimeoutException` | same | either | `Timeout` | yes, once |
| Whole request over 30s | `HttpRequestTimeoutException` | same | either | `Timeout` | yes, once |
| Upstream 5xx | `ServerResponseException` | same | up | `Upstream(code)` | yes, backoff |
| 4xx: bad key, bad stop id | `ClientRequestException` | same | up | `Request(code)` | never |
| Captive portal login page | `CaptivePortalException` from the response validator | same | up | `CaptivePortal` | no, needs the rider |
| Response shape changed | `JsonConvertException` | same | up | `Malformed` | never |
| TLS failure | `SSLException` | `NSURLErrorSecureConnectionFailed` (-1200) | up | `Unknown` | never |
| Screen left, job cancelled | `CancellationException` | same | either | **not a failure, rethrown** | n/a |

### Verification status

| Platform | Verified | How |
|---|---|---|
| Android | **yes** | `NetworkErrorClassifierTest` drives a real Ktor OkHttp client at an unresolvable `.invalid` host (RFC 2606), an unrouted TEST-NET-3 address (RFC 5737) and a closed loopback port, and classifies whatever the engine genuinely throws. |
| iOS | **no** | Written from Apple's documented `NSURLErrorDomain` codes and never exercised against a real network change. |

**The iOS column is a hypothesis, not an observation.** An iOS simulator shares the
host machine's network stack and has no airplane mode, so the no-transport case
cannot be produced on one at all. The first person to run this on a real iPhone
should log the actual `DarwinHttpRequestException.origin.code` for each row, correct
whatever is wrong, and flip this table's iOS row to yes.

### Why the tests use a real client

A test that does `UnknownHostException().toNetworkError(...)` proves the `when`
branch is wired and proves nothing about whether OkHttp actually throws that type
for the condition in question. Every Android case drives a real request at a real
address, so an engine upgrade that changes the exception surface fails the build
instead of silently reclassifying live failures.

### Captive portal versus malformed

Both arrive as a failure to read the body, and they need different handling, so the
distinction cannot come from the exception. It comes from the response
`Content-Type`: an endpoint that promised JSON or protobuf and returned `text/html`
is a portal, not a schema change. That check lives in a response validator, because
by the time the deserialisation exception exists the headers are gone.

---

## Decisions

Each row is a choice that was made deliberately, with the observation that should
reopen it. A decision record without a revisit trigger is a fossil.

| Decision | Why | Would revisit if |
|---|---|---|
| Classify from request outcomes only. No reachability probe. | A probe costs a request on every interval, drains battery, and is stale the moment it returns. The content-type validator catches the portal case at the moment it matters. | `CaptivePortal` turns out to be a meaningful share of failures, meaning riders hit it repeatedly before a request catches it. |
| `kotlin.Result` plus `NetworkException`, not a sealed `ApiResult`. | A sealed type makes failure impossible to ignore, which is better in the abstract, but rewrites every service signature, call site and fake. `suspendSafeResult` already threads `Result` and already handles cancellation correctly. | A failure gets silently ignored at a call site because `Result` allowed it. One real instance settles it. |
| Hand-rolled iOS observer over `nw_path_monitor`, no KMP dependency. | Roughly sixty lines over an API Kotlin/Native already exposes. A dependency that has to be read anyway the first time it misbehaves in a tunnel is a bad trade. | The bridge needs more than about a hundred lines, or misreports state in a way that takes more than a day to chase. |
| `Unreachable` stays distinct from `Upstream`. | Merging cases later is free; splitting one after the fact means revisiting every call site. | The UI renders both identically for two releases running and nobody proposes a difference. Then merge them and delete a case. |
| Three transport states rather than a boolean. | `Unknown` prevents both a pointless fast-fail on the first request and an offline claim made on no evidence. | Never, unless the OS starts reporting synchronously at process start. |

---

## Adding a new upstream API

1. **Do not add a per-feature `HttpClient`.** There is one factory in
   `:core:network`. A new API that needs a credential adds an `ApiCredential` case,
   not an `expect`/`actual` pair. The guard test enforces this.
2. Return `Result<T>` from every service method, with `NetworkException` as the only
   failure type. Wrap the call in `suspendSafeResult`, which already refuses to
   swallow `CancellationException`.
3. Add the service to the register below.
4. If the endpoint can fail in a way this taxonomy does not cover, add the row here
   first, then the classifier branch, then the test.

---

## The service register

<!-- REGISTER: every Real*Service method belongs in this table. -->

Every network-calling service and the contract it returns. This table exists because
the app previously had two incompatible error contracts in the same layer, added one
service at a time by people who each made a reasonable local choice.

`NetworkServiceRegisterTest` reads this table, scans for `Real*Service` classes, and fails
when the two disagree. Adding a service is therefore a deliberate choice about its contract
rather than a copy of whatever the module next door did.

| Service | Contract | Notes |
|---|---|---|
| `RealTripPlanningService` | `Result<T>` via `NetworkCaller` | `trip()` and `stopFinder()`. |
| `RealDeparturesService` | `Result<T>` via `NetworkCaller` | `departures()`. |
| `RealParkRideService` | `Result<T>` via `NetworkCaller` | `fetchCarParkFacilities()` both overloads. `fetchAvailabilityForStops()` throws instead: its `null` already means "BFF off, use the per-facility path", and a `Result<T?>` would give callers two ways to say nothing. It still routes through `NetworkCaller`, so what escapes is a `NetworkException`. |
| `RealNswGtfsService` | throws | **Not migrated.** Downloads static GTFS schedule archives at app start, not rider-facing request/response traffic: nothing renders a message when it fails and a retry is the next app start. Surfaced by the register guard rather than by anybody noticing, which is the register working. Migrate it if its failures ever reach a screen. |
| `RealGtfsRealtimeService` | `GtfsRealtimeResult` sealed class | **Deliberate exception.** It already returns a typed result with a `Unchanged` case that `Result<T>` cannot express, and its failures are consumed by a poller that falls back to direct polling rather than surfacing them. Folding it into `Result<T>` would lose the third case for no gain. If it ever needs to tell a rider why it failed, `Error.cause` becomes a `NetworkError`. |

### The `endpoint` label is not the routed path

Every service passes `NetworkCaller` a stable label, e.g. `/v1/tp/trip`, rather than the URL
it actually hit. A trip can go to NSW direct, to the BFF's JSON pass-through, or to the BFF's
proto endpoint, and labelling by the real path would split one metric three ways. The
`upstream` parameter already carries that distinction, so the label stays constant.

### Why `NetworkCaller` and not each service

Classification needs the transport state observed **at the moment the call failed**. Four
services each reaching for the observer would be four separate decisions about how to read it,
which is exactly how the app came to have two error contracts. One class does it, every service
goes through it.

`suspendSafeResult` in `:core:coroutines-ext` is not deprecated by this. It stays the right
tool for non-network work; `NetworkCaller` exists only where a throwable needs classifying
against connectivity.

---

## What the guards hold, and what they cannot see

| Guard | Holds |
|---|---|
| Exhaustive `when` in `isRetryable`, `recoversOnReconnect`, `isConnectionProblem` | A new `NetworkError` case cannot compile until someone decides what it means for all three. |
| `NetworkErrorTest` | The decisions themselves, which the compiler cannot check: a 4xx is never retried, only `Offline` waits for reconnection, an upstream outage never reads as the rider's connection problem. |
| `NetworkErrorClassifierTest` | That real engine exceptions map as the table says, on Android. |

Not held by anything:

- **The iOS classifier.** See the verification table above.
- **Whether the transport observers are correct.** They are thin wrappers over OS
  callbacks no unit test can reach. `logTransitions()` exists so they can be checked
  by hand: toggle airplane mode and read the log.
- **Whether a screen shows the right message for a case.** That is the UI work, and
  it is tracked separately.
