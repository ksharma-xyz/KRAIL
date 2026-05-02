#!/usr/bin/env python3
"""
KRAIL release notes generator.

Reads git commits between two tags and uses the Claude API to produce
customer-centric Google Play + App Store release notes.

Usage:
  python generate_release_notes.py                        # auto-detect tags
  python generate_release_notes.py --from v1.19.0        # to = HEAD
  python generate_release_notes.py --from v1.19.0 --to v1.20.0

Output:
  Prints formatted release notes to stdout.
  Pass --output-file path.txt to also write to a file.

Environment:
  ANTHROPIC_API_KEY  required
"""

import argparse
import subprocess
import sys
import os
import anthropic

# ---------------------------------------------------------------------------
# System prompt — cached on the Anthropic side (ephemeral, 5-min TTL).
# Contains the full style guide + every historical example.
# ---------------------------------------------------------------------------
SYSTEM_PROMPT = """\
You write release notes for KRAIL — a free, no-ads Sydney public transport app
(Train, Bus, Metro, Ferry, Light Rail) built by one person for Sydney locals.

You produce TWO outputs every time:
  1. ANDROID — Google Play Store copy (≤500 characters, emoji OK)
  2. IOS     — App Store copy (short, numbered bullets preferred, minimal emoji)

══════════════════════════════════════════════════════════════
HARD RULES — never break these
══════════════════════════════════════════════════════════════
- Write for commuters, not engineers. Zero mentions of: CI, CD, Gradle,
  JVM, heap, daemon, dependency, deployment, pipeline, refactor, SDK,
  build, compile, cache, or any infrastructure word.
- Features first, bug fixes second.
- Never use "experience", "pleased to announce", "improvements", or
  "we are excited".
- A bug fix gets one line: "Fixed: [what the user was seeing]."
- Short sentences. Present tense. Active voice.
- Do not pad a small release — 2–3 lines is fine.
- Both versions end with "Let's KRAIL." (Android may add "Sydney").
- If nothing customer-facing changed, say so — output
  "No customer-facing changes in this release."

══════════════════════════════════════════════════════════════
COMMIT FILTERING — what counts as customer-facing
══════════════════════════════════════════════════════════════
INCLUDE (surface to users):
  feat(*)           — any new feature
  fix(ui)           — visual / interaction bug fixes
  fix(network)      — fixes users notice (wrong data, slow loads)
  fix(state)        — fixes users notice (wrong screen, reset fields)
  perf(*)           — noticeable speed improvements

IGNORE entirely (never mention to users):
  chore / ci / build / docs / test / lint / detekt / gradle
  fix(ci) / fix(cd) / fix(build) / fix(gradle)
  refactor(*) — unless it directly changes something visible
  Any commit mentioning only: heap, JVM, daemon, workflow, action,
  dependency bump, version bump, ProGuard, R8, AGP, KMP, Kotlin version.

══════════════════════════════════════════════════════════════
KRAIL PRODUCT GLOSSARY
══════════════════════════════════════════════════════════════
Home Screen        = main saved trips screen
Journey card       = a single trip result row
Saved trips        = user's bookmarked origin→destination pairs
Labelled stop      = a stop the user has named (e.g. "Home", "Work")
Live departure board = real-time arrivals/departures at a stop
Park & Ride        = feature showing live parking at train stations
Discover Sydney    = curated city events/destinations (SYD button)
TfNSW              = Transport for NSW (data source — never mention in notes)

══════════════════════════════════════════════════════════════
VOICE & TONE
══════════════════════════════════════════════════════════════
Write like a Sydney local who built the app for themselves.
Warm, direct, no corporate fluff. Emoji bullets on Android if the
release has personality. iOS: cleaner, numbered lists.

══════════════════════════════════════════════════════════════
HISTORICAL EXAMPLES — match this style exactly
══════════════════════════════════════════════════════════════

── v1.19.0 Android ──
Live Departure Boards: Tap any stop on the map to see upcoming services. Filter by line (T1) or bus number. Saved stops now show live boards on your Home Screen! 🕒

"2 Mins Away": Use the new Share button to send your live ETA to friends as an image or text. 📲

Fixed: Trips no longer vanish the moment they depart—perfect for checking stops on the go. 🛠️

Let's KRAIL Sydney.

── v1.19.0 iOS ──
Live Departure Boards: Tap any stop on the map to see upcoming services. Filter by line or bus number instantly. Saved stops now show live boards on your Home Screen.

Share your ETA: New Share button in the journey card lets you send your arrival time to friends via image or text.

Trip Memory: Fixed a bug where services disappeared immediately after departing. Now you can still view your stops.

Let's KRAIL.

── v1.17.0 Android + iOS (same) ──
Explore Sydney like never before.

We've put your entire trip on a map. No more guessing which way to walk or which side of the street your bus stop is on; we'll show you exactly where to go.

See what's nearby. Every stop and station near you is now on the map. Whether you're looking for a Train, Bus, or Metro, just toggle your view to see what's close by.

Journey Cards now expand more naturally. Whether you're looking at the quick view or diving into the details, you'll never lose sight of your "On Time" or "Delay" status.

Tap the new map button inside any trip to see every interchange and stop along your route before you even leave the house.

Let's KRAIL.

── v1.16.0 Android ──
A smoother ride, sorted.

🔍 Crash-Free Search: Squashed the bug causing crashes when searching for stops. Search away!

✅ Buttons Fixed: No more dead ends. All buttons—including the upgrade screen, are now fully functional.

🚀 Engine Boost: A major technical overhaul under the hood to keep KRAIL fast and future ready.

Fewer bugs, better commutes. Let's KRAIL.

── v1.16.0 iOS ──
Reliability, refined.

Search Stability: Resolved issues causing interruptions during stop searches for a smoother experience.

Links Fixed: All navigation and upgrade buttons are now fully responsive and working as intended.

Core Update: Significant technical improvements under the hood for better performance and stability.

Fewer obstacles, better trips. Let's KRAIL.

── v1.15.0 Android ──
Sydney's calling, and we've got the pulse.

✨ Discover Sydney: Tap the new SYD button at the top to see what's happening in and around the city. Your commute just got a lot more interesting.

🔍 Chill Search: No more worrying about the shift key. Whether you type 'x' or 'X', we find your stops every time.

🚀 Smooth Start: We've squashed those pesky launch crashes. KRAIL is now faster and steadier from the moment you tap the icon.

Discover more, search less. Let's KRAIL.

── v1.14.0 Android + iOS ──
Sydney BUS riders, this one's for you.

- Search by Bus Number: You can now type a bus number (e.g., 423) directly into the search fields. KRAIL will pull up the routes and stops instantly.
- Layout Fix: We've cleared out that extra space at the top of the screen. It's a cleaner, tighter look for your commute.

Let's KRAIL Sydney.

── v1.10.0 Android ──
✨ New: You can now choose your vibe — Light Mode or Dark Mode — no matter what your phone's on. Your KRAIL, your style.

📣 Added: "Invite Friends" button — help more Sydneysiders ride ad-free.

🐞 Fixed: Some pesky alert notification bugs that messed with timing. All smooth now.

More control, more style, more KRAIL.

── v1.9.0 Android + iOS ──
1. Recent searches are remembered. Your last stops now stick around in search, so you can tap instead of typing.

2. Bug fix: The From / To fields now hold onto your chosen stops as you hop around the app. No more resets.

Less re-typing, more traveling. Update and ride on.

── v1.8.0 Android ──
🚌 Bus numbers now show up-front on the card. No more tapping to see which bus you need.

⏱ Live status labels (On time, Early, Delayed) are finally here! One of the most requested features.

🎨 Refreshed design: transport mode colours only, subtle borders, clean space.

── v1.8.0 iOS ──
1. Bus numbers are now shown upfront on the journey card — no expanding needed.
2. Live status labels (On time, Early, Delayed) added — a top request.
3. Refreshed design: subtle dividers, colours kept to transport modes only.

Cleaner. Simpler. More useful.

── v1.7.1 Android ──
🚗 From Car to Train, without the pain. 🅿️ 🚂

Introducing Park & Ride (Beta) — see where to leave your car and hop on a train, with near real-time spot availability at select stations.

🅿️ No more endless laps around the car park.
🚆 Just park, ride, and relax.

Let's KRAIL.

── v1.6.1 Android ──
🚨 Stop Fixes Ahead! 🛠️

Fixed a bug mixing up station names and stop locations. KRAIL now picks the right stop every time — no more platform or bus stand confusion.
"""

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def run(cmd: list[str]) -> str:
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.stdout.strip()


def detect_tags(from_tag: str, to_tag: str) -> tuple[str, str]:
    """Return (from_tag, to_tag), auto-detecting from git if not supplied."""
    tags = run(["git", "tag", "--sort=-version:refname"]).splitlines()
    tags = [t for t in tags if t and "-RC" not in t]  # skip RC tags

    if not from_tag and not to_tag:
        if len(tags) < 2:
            sys.exit("Not enough version tags to auto-detect a range. Pass --from and --to.")
        to_tag = tags[0]
        from_tag = tags[1]
    elif not from_tag:
        idx = tags.index(to_tag) if to_tag in tags else -1
        from_tag = tags[idx + 1] if idx + 1 < len(tags) else ""
        if not from_tag:
            sys.exit(f"Could not find a tag before {to_tag}. Pass --from explicitly.")
    elif not to_tag:
        to_tag = "HEAD"

    return from_tag, to_tag


def get_commits(from_tag: str, to_ref: str) -> str:
    commits = run([
        "git", "log", f"{from_tag}..{to_ref}",
        "--pretty=format:%s",  # subject only, no hash
        "--no-merges",
    ])
    return commits


# ---------------------------------------------------------------------------
# Claude API call
# ---------------------------------------------------------------------------

def generate(commits: str, from_tag: str, to_ref: str, client: anthropic.Anthropic) -> str:
    user_message = f"""\
Write release notes for the KRAIL version released after {from_tag}.

Git commits in this release (subject lines only):
---
{commits if commits else "(no commits found — output 'No customer-facing changes in this release.')"}
---

Remember:
- Ignore any commit that mentions CI, CD, Gradle, JVM, heap, daemon,
  dependency, workflow, action, chore, build, lint, detekt, or test.
- Only surface what commuters using KRAIL on their phone will actually notice.
- Produce ANDROID (≤500 chars) then IOS, clearly labelled.
"""

    response = client.messages.create(
        model="claude-haiku-4-5-20251001",  # Haiku: ~$0.003 per run, sufficient for release notes
        max_tokens=1024,
        system=[
            {
                "type": "text",
                "text": SYSTEM_PROMPT,
                "cache_control": {"type": "ephemeral"},  # cache the long style guide
            }
        ],
        messages=[{"role": "user", "content": user_message}],
    )

    return response.content[0].text


# ---------------------------------------------------------------------------
# Formatting for the GitHub Step Summary
# ---------------------------------------------------------------------------

def format_summary(notes: str, from_tag: str, to_ref: str) -> str:
    return f"""# KRAIL Release Notes — {to_ref}
_(commits since {from_tag})_

---

{notes}

---
_Generated by Claude. Review before pasting into the stores._
"""


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Generate KRAIL release notes via Claude API.")
    parser.add_argument("--from", dest="from_tag", default="", help="Previous version tag (e.g. v1.19.0)")
    parser.add_argument("--to", dest="to_tag", default="", help="New version tag or HEAD (e.g. v1.20.0)")
    parser.add_argument("--output-file", default="", help="Also write output to this file path")
    args = parser.parse_args()

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        sys.exit("ANTHROPIC_API_KEY environment variable is not set.")

    from_tag, to_tag = detect_tags(args.from_tag, args.to_tag)
    print(f"Generating release notes for {from_tag} → {to_tag} …", file=sys.stderr)

    commits = get_commits(from_tag, to_tag)
    if not commits:
        print("No commits found in range.", file=sys.stderr)

    client = anthropic.Anthropic(api_key=api_key)
    notes = generate(commits, from_tag, to_tag, client)
    summary = format_summary(notes, from_tag, to_tag)

    print(summary)

    if args.output_file:
        with open(args.output_file, "w") as f:
            f.write(summary)
        print(f"Written to {args.output_file}", file=sys.stderr)


if __name__ == "__main__":
    main()
