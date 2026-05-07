#!/usr/bin/env python3
"""
Decodes NSW_STOPS.pb (Wire/protobuf format) into a CSV used by FuzzyStopEvalTest.

Pure Python — no pip dependencies required.

Usage (run from project root):
    python3 scripts/extract_nsw_stops.py

Output:
    feature/trip-planner/ui/src/commonTest/resources/nsw_stops_eval.csv
"""

import os
import sys

PB_PATH = "io/gtfs/src/commonMain/composeResources/files/NSW_STOPS.pb"
OUT_PATH = "feature/trip-planner/ui/src/androidHostTest/resources/nsw_stops_eval.csv"


# ── Minimal protobuf binary decoder ──────────────────────────────────────────
# Handles wire types used by NswStop: varint (0), 64-bit (1), length-delimited (2), 32-bit (5).

def _read_varint(buf: bytes, pos: int) -> tuple[int, int]:
    result, shift = 0, 0
    while pos < len(buf):
        b = buf[pos]; pos += 1
        result |= (b & 0x7F) << shift
        shift += 7
        if not (b & 0x80):
            break
    return result, pos


def _read_ld(buf: bytes, pos: int) -> tuple[bytes, int]:
    """Read a length-delimited field (string, bytes, or nested message)."""
    length, pos = _read_varint(buf, pos)
    return buf[pos: pos + length], pos + length


def _skip_field(buf: bytes, pos: int, wire_type: int) -> int:
    if wire_type == 0:
        _, pos = _read_varint(buf, pos)
    elif wire_type == 1:
        pos += 8
    elif wire_type == 2:
        _, pos = _read_ld(buf, pos)
    elif wire_type == 5:
        pos += 4
    return pos


# ── NswStop message parser ─────────────────────────────────────────────────

def _parse_nsw_stop(buf: bytes) -> tuple[str, str, list[int]]:
    """
    Returns (stopId, stopName, productClasses).

    NswStop proto3 field numbers:
      1 = stopId      (string)
      2 = stopName    (string)
      3 = lat         (double)
      4 = lon         (double)
      5 = productClass (repeated int32, packed in proto3)
      6 = isParent    (bool)
    """
    stop_id = ""
    stop_name = ""
    product_classes: list[int] = []
    pos = 0
    while pos < len(buf):
        tag, pos = _read_varint(buf, pos)
        field = tag >> 3
        wire  = tag & 0x07

        if field == 1 and wire == 2:
            raw, pos = _read_ld(buf, pos)
            stop_id = raw.decode("utf-8", errors="replace")
        elif field == 2 and wire == 2:
            raw, pos = _read_ld(buf, pos)
            stop_name = raw.decode("utf-8", errors="replace")
        elif field == 5 and wire == 2:
            # packed repeated int32
            raw, pos = _read_ld(buf, pos)
            inner = 0
            while inner < len(raw):
                v, inner = _read_varint(raw, inner)
                product_classes.append(v)
        elif field == 5 and wire == 0:
            # non-packed repeated int32 (older encoding)
            v, pos = _read_varint(buf, pos)
            product_classes.append(v)
        else:
            pos = _skip_field(buf, pos, wire)

    return stop_id, stop_name, product_classes


# ── NswStopList message parser ────────────────────────────────────────────

def _parse_nsw_stop_list(buf: bytes) -> list[tuple[str, str, list[int]]]:
    """
    NswStopList proto3:
      1 = nswStops (repeated NswStop)
    """
    stops: list[tuple[str, str, list[int]]] = []
    pos = 0
    while pos < len(buf):
        tag, pos = _read_varint(buf, pos)
        field = tag >> 3
        wire  = tag & 0x07

        if field == 1 and wire == 2:
            raw, pos = _read_ld(buf, pos)
            stop = _parse_nsw_stop(raw)
            if stop[0] and stop[1]:
                stops.append(stop)
        else:
            pos = _skip_field(buf, pos, wire)

    return stops


# ── Main ──────────────────────────────────────────────────────────────────

def main() -> None:
    if not os.path.exists(PB_PATH):
        sys.exit(
            f"ERROR: '{PB_PATH}' not found.\n"
            "Make sure you run this script from the project root directory."
        )

    print(f"Reading {PB_PATH} …", file=sys.stderr)
    with open(PB_PATH, "rb") as f:
        data = f.read()

    stops = _parse_nsw_stop_list(data)
    print(f"Decoded {len(stops)} stops.", file=sys.stderr)

    out_dir = os.path.dirname(OUT_PATH)
    os.makedirs(out_dir, exist_ok=True)

    with open(OUT_PATH, "w", encoding="utf-8") as out:
        out.write("stopId|stopName|productClasses\n")
        for stop_id, stop_name, product_classes in stops:
            pcs = ",".join(str(p) for p in product_classes)
            # Escape any pipe chars in names (extremely rare but safe)
            out.write(f"{stop_id}|{stop_name.replace('|', '_')}|{pcs}\n")

    print(f"Wrote {OUT_PATH}", file=sys.stderr)
    print(f"Done — {len(stops)} stops exported.", file=sys.stderr)


if __name__ == "__main__":
    main()
