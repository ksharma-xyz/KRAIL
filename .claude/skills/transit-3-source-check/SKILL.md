---
name: transit-3-source-check
description: Reproduce a "KRAIL is missing a service" report by querying the same trip from three independent sources - the NSW trip planner API, the NSW departure monitor, and the transportnsw.info website - and diffing them. Use whenever a rider reports a departure the app does not show, a wrong duration, an unexpected interchange, or a suspect "load previous / load more" result. Answers "is this our bug or the upstream API's behaviour" before any code is read.
---

# Three-source transit check

A rider reports a missing or wrong service. Before reading any KRAIL code, establish
**which layer is actually wrong**. There are three independent sources, and they
routinely disagree. Query all three for the same origin, destination and minute.

| # | Source | What it tells you |
|---|---|---|
| 1 | `/v1/tp/trip` with KRAIL's exact params | What KRAIL is given. An **optimised itinerary set**, not a departure list |
| 2 | `/v1/tp/departure_mon` at each end | **Ground truth**: every service that actually calls at the stop |
| 3 | transportnsw.info trip planner | What the official app/site shows the same rider |

The verdict falls out of the pattern:

| 1 vs 3 | 1 vs 2 | Verdict |
|---|---|---|
| same | same | Nothing is missing. Re-read the report |
| same | source 2 has services source 1 omits | **Upstream filtering**, not a KRAIL bug. See "Why the API drops services" |
| differs | - | KRAIL or the site is sending different params. Diff the query strings |

If KRAIL on device differs from source 1, only then is it our bug.

## Setup

The API key lives in `local.properties`. Never paste it into an issue, PR, artifact
or anything leaving the machine.

```sh
KEY=$(grep '^ANDROID_NSW_TRANSPORT_API_KEY' local.properties | cut -d= -f2-)
```

Resolve stop IDs first. Take the first result whose `type` is `stop`:

```sh
curl -s -H "Authorization: apikey $KEY" \
 "https://api.transport.nsw.gov.au/v1/tp/stop_finder?outputFormat=rapidJSON&type_sf=any&name_sf=Seven+Hills+Station&coordOutputFormat=EPSG:4326&TfNSWSF=true&version=10.2.1.42" \
 | python3 -c "import json,sys; [print(l['id'],'|',l['name']) for l in json.load(sys.stdin)['locations'][:5]]"
```

IDs seen often here: Central `200060`, Town Hall `200070`, Leppington `217933`,
Glenfield `216710`, Mascot `202010`, Seven Hills `214710`, Toongabbie `214610`,
Richmond `275310`.

## Source 1: the trip API, with KRAIL's exact params

Copy the params from `RealTripPlanningService.appendTripQueryParams`. Sending a
different set proves nothing. As of this writing:

```sh
BASE="https://api.transport.nsw.gov.au/v1/tp/trip"
Q="name_origin=$ORIGIN&name_destination=$DEST&type_origin=any&type_destination=any\
&depArrMacro=dep&itdDate=$YYYYMMDD&itdTime=$HHMM&calcNumberOfTrips=6\
&TfNSWTR=true&version=10.2.1.42&coordOutputFormat=EPSG:4326&itOptionsActive=1\
&computeMonomodalTripBicycle=false&cycleSpeed=16&useElevationData=1&outputFormat=rapidJSON"

curl -s -H "Authorization: apikey $KEY" "$BASE?$Q" | python3 -c "
import json,sys
for j in json.load(sys.stdin).get('journeys') or []:
    l=j['legs']
    dep=(l[0]['origin'].get('departureTimeEstimated') or l[0]['origin']['departureTimePlanned'])
    arr=(l[-1]['destination'].get('arrivalTimeEstimated') or l[-1]['destination']['arrivalTimePlanned'])
    print(dep[11:16],'->',arr[11:16],'|',
          ' + '.join(str(x['transportation'].get('disassembledName')) for x in l),'|',
          ' + '.join(str(x['transportation'].get('destination',{}).get('name')) for x in l))
"
```

Times come back in UTC. Sydney is UTC+10 (AEST) or UTC+11 (AEDT, October to April).
`itdDate`/`itdTime` are local, the response is not. Getting this backwards has cost a
wrong diagnosis before.

To see the stopping pattern of a leg, print `stopSequence`. Names are
`"Toongabbie Station, Platform 4"`, so match on a substring, never on equality.

### Two traps in this endpoint

- **`calcNumberOfTrips` caps at 10.** Values of 12 or more silently return **four**
  journeys, not more. Never assume a larger number widens the window.
- **`depArrMacro=arr` walks backwards** from the anchor, returning the services
  immediately *before* it. `dep` only ever walks forwards. This is why "show previous"
  must anchor on arrival.

## Source 2: the departure monitor, at both ends

This is the only source that lists **every** service calling at a stop, unfiltered.

```sh
curl -s -H "Authorization: apikey $KEY" \
 "https://api.transport.nsw.gov.au/v1/tp/departure_mon?outputFormat=rapidJSON\
&coordOutputFormat=EPSG:4326&mode=direct&type_dm=stop&name_dm=$STOP\
&itdDate=$YYYYMMDD&itdTime=$HHMM&departureMonitorMacro=true&TfNSWDM=true&version=10.2.1.42" \
 | python3 -c "
import json,sys
for e in (json.load(sys.stdin).get('stopEvents') or []):
    t=(e.get('departureTimeEstimated') or e.get('departureTimePlanned'))
    tr=e.get('transportation',{})
    print(t[11:16],'|',tr.get('disassembledName'),'|',tr.get('destination',{}).get('name'),
          '| plat',e.get('location',{}).get('properties',{}).get('platform'))
"
```

**It returns about 40 events and ignores `limit`.** At a busy stop that is ten
minutes of coverage. Call it repeatedly at 15-minute anchors and union the results,
keyed by timestamp, or you will conclude a service does not exist when you simply
did not fetch far enough.

### Onward stops: the param that is not in the docs

By default each stop event carries no route. Append **`&depType=stopEvents&includeCompleteStopSeq=1`**
and every event gains `onwardLocations` and `previousLocations`: the full remaining
stop list through to the terminus, each with `arrivalTimePlanned` and a platform.

That turns the board into a direct answer to "which of these actually goes where I am
going", without the trip planner and therefore without either of its filters:

```sh
curl -s -H "Authorization: apikey $KEY" \
 "https://api.transport.nsw.gov.au/v1/tp/departure_mon?outputFormat=rapidJSON\
&coordOutputFormat=EPSG:4326&mode=direct&type_dm=stop&name_dm=$ORIGIN\
&itdDate=$YYYYMMDD&itdTime=$HHMM&departureMonitorMacro=true&TfNSWDM=true&version=10.2.1.42\
&depType=stopEvents&includeCompleteStopSeq=1" \
 | python3 -c "
import json,sys
TARGET='Seven Hills'
for e in (json.load(sys.stdin).get('stopEvents') or []):
    ow=[(o.get('disassembledName') or o.get('name','')) for o in (e.get('onwardLocations') or [])]
    hit=[x for x in ow if TARGET in x]
    if not hit: continue
    tr=e.get('transportation',{})
    t=(e.get('departureTimeEstimated') or e.get('departureTimePlanned'))[11:16]
    print(t,'|',tr.get('disassembledName'),'to',tr.get('destination',{}).get('name'),'|',hit[0])
"
```

The three other spellings tried (`includeCompleteStopSeq` alone, `useAllStops`,
`itdLPxx_showOnward`) are silently ignored. Only the pair works.

Caveat before building on it: `onwardLocations` carries `arrivalTimePlanned` but
`arrivalTimeEstimated` is null. The departure is real-time, the arrival is not.

Run it at the **destination** too. A service the trip API never offers still appears
on the destination board, which is how you prove the service is real.

To prove a specific train calls at an intermediate stop, ask for a trip to its
**terminus** and read the `stopSequence` of the leg. That is what confirmed the
17:12 Town Hall to Richmond calls at Seven Hills.

## Source 3: transportnsw.info

```
https://transportnsw.info/trip-planner/plan?excludedModes=11&from=<ORIGIN>&to=<DEST>
```

The `from`/`to` values are the same numeric stop IDs. Date and time params in the URL
are **ignored**; the page always opens at "Leaving now". To check another time, use
the "Leaving now" control or the "Later times" / "Earlier times" buttons on the page.

If the site and source 1 agree, KRAIL is not diverging from the official planner and
the issue is upstream policy, not a KRAIL defect. Say so plainly in the report.

## Source 4 (optional): KRAIL itself

Only needed when sources 1 to 3 already agree, to prove KRAIL renders what it is given.

```sh
./gradlew :androidApp:assembleDebug
adb install -r -t androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n xyz.ksharma.krail.debug/xyz.ksharma.krail.MainActivity
```

Force-stop when finished. The timetable screen polls every 30 seconds for as long as
it is open:

```sh
adb shell am force-stop xyz.ksharma.krail.debug
```

## Why the API drops services

Two mechanisms account for every "missing departure" report seen so far. Both are
upstream, both are invisible in the response, and telling them apart decides the fix.

**Minimum interchange time.** A connection tighter than the default allowance is
rejected, so the API pairs the service with a later connection instead. That slower
itinerary is then usually dominated by the next departure, and the original service
vanishes. Diagnose by re-running the query with `changeSpeed=fast` appended: if the
service appears, this is the cause. Values are `fast`, `normal` (the default) and
`slow`. Cross-platform interchanges are the common case, since a one-minute step
across an island platform is real but below the allowance.

**Pareto dominance.** A journey that departs earlier and arrives later than another
is dropped, however catchable it is. `changeSpeed` has no effect on this, and on a
single-leg journey there is nothing else to relax. Diagnose by finding the service on
the destination board (source 2), confirming it calls at both ends, and checking
whether a later departure beats it on arrival.

Neither is a KRAIL bug. Both are rider-visible as "the app is missing trains", which
is why the report always arrives sounding like one.

## Report format

State the verdict first, then the evidence, in this order:

1. **Verdict**: KRAIL bug, upstream API behaviour, or report not reproducible.
2. **The service in question**, with its real times from source 2 and the platforms.
3. **Side-by-side** of source 1 and source 3 for the same minute.
4. **Mechanism**, if upstream: interchange time or dominance, with the query that
   demonstrates it.
5. **What would change if we acted**, including what the change costs.

Do not open with code. If the first thing you want to read is
`TripResponseMapper.kt`, you have skipped the step that decides whether the mapper is
even involved.

## Related

- `docs/investigations/NSW_715_WALK_LEG_INVESTIGATION.md` - an earlier case closed
  the same way, as upstream data quality rather than a KRAIL merge bug.
- `docs/MAESTRO_TRIAGE.md` - the same "read the evidence in this order" discipline
  for a red end-to-end lane.
