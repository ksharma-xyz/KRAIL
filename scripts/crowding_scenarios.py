#!/usr/bin/env python3
"""Find live trains that show each crowding scenario right now.

Downloads the Sydney Trains trip updates feed once, works out every upcoming stop's carriage
pattern with the same zone maths as the app (`CarriageLevels.zoneLevels`), and prints one
From and To pair per scenario that a tester can type into KRAIL in the next half hour. The
scenarios and what the rider should see are in docs/testing/CROWDING_SCENARIOS.md.

Forecasts change every minute, so the pairs are only good for a short while. Run it again
rather than reusing an old list.

Reads the NSW API key from local.properties (never printed). Stop names come from
/v1/tp/stop_finder, one call per stop that is printed.

Usage:
    python3 scripts/crowding_scenarios.py
    python3 scripts/crowding_scenarios.py --window 5 40   # trains leaving 5 to 40 minutes out
"""

from __future__ import annotations

import argparse
import json
import time
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = "https://api.transport.nsw.gov.au/"
USER_AGENT = "KRAIL-crowding-scenarios/1.0 (https://github.com/ksharma-xyz/KRAIL)"

# GTFS occupancy to the app's three levels (SeatLevel.fromGtfs): 0 seats, 1 few, 2 standing.
LEVEL = {0: 0, 1: 0, 2: 1, 3: 2, 4: 2, 5: 2}
LETTER = "SFX"
CARS_PER_END_CAR = 6  # CarriageLevels: one end car per end up to 11 cars


def api_key() -> str:
    for line in (ROOT / "local.properties").read_text().splitlines():
        if line.startswith("ANDROID_NSW_TRANSPORT_API_KEY"):
            return line.split("=", 1)[1].strip().strip('"')
    raise SystemExit("ANDROID_NSW_TRANSPORT_API_KEY not found in local.properties")


def fetch(path: str, key: str) -> bytes:
    request = urllib.request.Request(
        BASE + path, headers={"Authorization": f"apikey {key}", "User-Agent": USER_AGENT}
    )
    with urllib.request.urlopen(request, timeout=40) as response:
        return response.read()


# Minimal protobuf reader: the feed's TfNSW extension 1007 is not in any published .proto.
def varint(buf: bytes, i: int) -> tuple[int, int]:
    result = shift = 0
    while True:
        byte = buf[i]
        i += 1
        result |= (byte & 0x7F) << shift
        shift += 7
        if byte < 0x80:
            return result, i


def fields(buf: bytes) -> dict[int, list]:
    out: dict[int, list] = {}
    i = 0
    while i < len(buf):
        key, i = varint(buf, i)
        number, wire = key >> 3, key & 7
        if wire == 0:
            value, i = varint(buf, i)
        elif wire == 1:
            value, i = buf[i:i + 8], i + 8
        elif wire == 5:
            value, i = buf[i:i + 4], i + 4
        elif wire == 2:
            length, i = varint(buf, i)
            value, i = buf[i:i + length], i + length
        else:
            raise ValueError(f"wire type {wire}")
        out.setdefault(number, []).append(value)
    return out


def first(message: dict, number: int, default=None):
    return message[number][0] if number in message else default


def cars_at(stop: dict) -> list[int] | None:
    """StopTimeUpdate extension 1007: one message per car, 2 = position, 7 = occupancy."""
    cars = []
    for raw in stop.get(1007, []):
        car = fields(raw)
        if 7 in car:
            cars.append((first(car, 2, 0), LEVEL.get(first(car, 7), 2)))
    return [level for _, level in sorted(cars)] or None


def scenario(levels: list[int]) -> str:
    if len(set(levels)) == 1:
        return ["Seats all along", "Few seats all along", "Standing all along"][levels[0]]
    end = max(1, len(levels) // CARS_PER_END_CAR)
    zones = {"back": max(levels[:end]), "middle": max(levels[end:-end]), "front": max(levels[-end:])}
    best = min(zones.values())
    quietest = {zone for zone, level in zones.items() if level == best}
    if quietest == {"back", "front"}:
        return "Seats at both ends"
    if quietest == {"middle"}:
        return "Seats in the middle"
    if len(quietest) == 1:
        return "Seats at one end only"
    return "Mixed"


def upcoming_trips(feed: bytes, now: float, window: tuple[int, int]):
    for entity in fields(feed).get(2, []):
        update = first(fields(entity), 3)
        if update is None:
            continue
        update = fields(update)
        trip_id = first(fields(first(update, 1, b"")), 1, b"").decode()
        stops = []
        for raw in update.get(2, []):
            stop = fields(raw)
            times = fields(first(stop, 3, b"")) or fields(first(stop, 2, b""))
            stops.append((first(times, 2), first(stop, 4, b"").decode(), cars_at(stop)))
        yield trip_id, [s for s in stops if s[0]], window


def stop_name(stop_id: str, key: str, cache: dict) -> str:
    if stop_id not in cache:
        query = urllib.parse.urlencode({
            "outputFormat": "rapidJSON", "type_sf": "stop", "name_sf": stop_id,
            "coordOutputFormat": "EPSG:4326", "TfNSWSF": "true", "version": "10.2.1.42",
        })
        locations = json.loads(fetch(f"v1/tp/stop_finder?{query}", key)).get("locations", [])
        cache[stop_id] = locations[0].get("disassembledName", stop_id) if locations else stop_id
    return cache[stop_id]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--window", nargs=2, type=int, default=[8, 35], metavar=("MIN", "MAX"),
                        help="minutes from now a train must leave within (default 8 35)")
    args = parser.parse_args()
    key = api_key()
    now = time.time()
    lo, hi = args.window[0] * 60, args.window[1] * 60
    found: dict[str, tuple] = {}
    for trip_id, stops, _ in upcoming_trips(fetch("v2/gtfs/realtime/sydneytrains", key), now, args.window):
        for index, (leaves, stop_id, levels) in enumerate(stops):
            if not levels or not lo <= leaves - now <= hi:
                continue
            later = [s for s in stops[index + 1:] if s[2]]
            if len(later) < 2:
                continue
            found.setdefault(scenario(levels), (leaves, trip_id, stop_id, later[2 if len(later) > 2 else -1][1], levels))
            # Seats changing further along the trip (parked idea, see TRIP_UPDATES_FEED_IDEAS.md).
            for leaves_later, later_stop, later_levels in later:
                if min(later_levels) < min(levels):
                    found.setdefault("Seats open up later", (leaves, trip_id, stop_id, later_stop, levels))
                    break
                if min(later_levels) > min(levels):
                    found.setdefault("Fills up later", (leaves, trip_id, stop_id, later_stop, levels))
                    break
    names: dict[str, str] = {}
    print(f"Live at {datetime.now():%H:%M}. S seats, F few seats, X standing only, one letter per car.\n")
    for name in sorted(found):
        leaves, trip_id, origin, destination, levels = found[name]
        print(f"{name:24} {datetime.fromtimestamp(leaves):%H:%M}  "
              f"{stop_name(origin, key, names)} to {stop_name(destination, key, names)}  "
              f"{''.join(LETTER[level] for level in levels)}  (trip {trip_id.split('.')[0]})")


if __name__ == "__main__":
    main()
