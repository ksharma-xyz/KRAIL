#!/usr/bin/env python3
"""
Compute the next iOS build number to use for a TestFlight upload.

Strategy: next = max(asc_latest, github_floor) + 1

  - ASC is the ground truth for what Apple has already accepted
    (covers manual uploads that CI doesn't know about).
  - The GitHub variable acts as a monotonic floor so we never go
    backwards even if the ASC API returns a stale/lower value.

Prints a single integer to stdout.

Required environment variables:
    APP_STORE_CONNECT_API_KEY_KEY_ID    – Key ID  (e.g. "ABCDEF1234")
    APP_STORE_CONNECT_API_KEY_ISSUER_ID – Issuer UUID
    APP_STORE_CONNECT_API_KEY_KEY       – PEM private key (the full .p8 content)
    ASC_APP_ID                          – Numeric App Store Connect app ID
    IOS_BUILD_NUMBER_FLOOR              – Current GitHub variable value (integer)

Dependencies (install via pip in a venv before running):
    PyJWT>=2.8  cryptography>=42
"""

import os
import sys
import time
import json
import urllib.request
import urllib.error

import jwt  # PyJWT


# ---------------------------------------------------------------------------
# ASC JWT
# ---------------------------------------------------------------------------

def _make_token() -> str:
    key_id = os.environ["APP_STORE_CONNECT_API_KEY_KEY_ID"]
    issuer_id = os.environ["APP_STORE_CONNECT_API_KEY_ISSUER_ID"]
    private_key = os.environ["APP_STORE_CONNECT_API_KEY_KEY"]

    return jwt.encode(
        payload={
            "iss": issuer_id,
            "iat": int(time.time()),
            "exp": int(time.time()) + 1200,
            "aud": "appstoreconnect-v1",
        },
        key=private_key,
        algorithm="ES256",
        headers={"kid": key_id},
    )


# ---------------------------------------------------------------------------
# ASC API
# ---------------------------------------------------------------------------

def _fetch_asc_latest(app_id: str, token: str) -> int:
    """Return the highest build version number Apple has received for this app."""
    url = (
        f"https://api.appstoreconnect.apple.com/v1/builds"
        f"?filter[app]={app_id}"
        f"&fields[builds]=version"
        f"&limit=200"
        f"&sort=-version"
    )
    numbers: list[int] = []
    while url:
        req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                data = json.loads(resp.read())
        except urllib.error.HTTPError as e:
            body = e.read().decode(errors="replace")
            print(f"::warning::ASC API error {e.code}: {body}", file=sys.stderr)
            break
        for build in data.get("data", []):
            try:
                numbers.append(int(build["attributes"]["version"]))
            except (KeyError, ValueError):
                pass
        url = data.get("links", {}).get("next")
    return max(numbers) if numbers else 0


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    floor_str = os.environ.get("IOS_BUILD_NUMBER_FLOOR", "0")
    try:
        floor = int(floor_str)
    except ValueError:
        print(f"::warning::IOS_BUILD_NUMBER_FLOOR='{floor_str}' is not an integer — using 0", file=sys.stderr)
        floor = 0

    app_id = os.environ.get("ASC_APP_ID", "")
    if not app_id:
        print("::warning::ASC_APP_ID not set — skipping ASC lookup, using floor only", file=sys.stderr)
        print(floor + 1)
        return

    required = [
        "APP_STORE_CONNECT_API_KEY_KEY_ID",
        "APP_STORE_CONNECT_API_KEY_ISSUER_ID",
        "APP_STORE_CONNECT_API_KEY_KEY",
    ]
    if not all(os.environ.get(k) for k in required):
        print("::warning::ASC credentials not fully set — using floor only", file=sys.stderr)
        print(floor + 1)
        return

    try:
        token = _make_token()
        asc_latest = _fetch_asc_latest(app_id, token)
        print(f"ASC latest: {asc_latest}  |  GitHub floor: {floor}", file=sys.stderr)
        next_build = max(asc_latest, floor) + 1
        print(next_build)
    except Exception as exc:  # pylint: disable=broad-except
        print(f"::warning::Failed to fetch ASC build numbers: {exc}", file=sys.stderr)
        # Fall back to floor + 1 so the build isn't blocked
        print(floor + 1)


if __name__ == "__main__":
    main()

