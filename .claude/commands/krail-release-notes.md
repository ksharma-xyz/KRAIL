# KRAIL Release Notes Writer

Write Play Store + App Store release notes for a new KRAIL version.

When the user invokes this skill:

1. **Ask first — before running any git commands:**
   > "Which version are you writing notes for? (e.g. v1.20.0)
   > Previous tag to compare from? (e.g. v1.19.0-RC1 — leave blank to auto-detect)"
2. Wait for the user's answer.
3. Run `git tag --sort=-version:refname | head -10` to confirm available tags. If the user left the previous tag blank, pick the closest tag before their target version.
4. Run `git log <previous-tag>..<target-version-or-HEAD> --oneline --no-merges` to get the commit list.
5. Draft **two** versions of the release notes (Android first, then iOS), following the style guide and examples below.
6. Present both versions and ask the user if they want tweaks before finalising.

---

## Two outputs required every time

| Output | Store | Limit | Tone |
|---|---|---|---|
| **Android** | Google Play Store | 500 chars | Warmer, minimal emoji as bullet points only if they're cool, casual Sydney voice |
| **iOS** | App Store | 4000 chars (but keep it short — match the examples) | More formal, numbered bullets, **no emoji whatsoever** |

Both must start with the version number on its own line: `v1.20.0` (or whatever the version is).
Both must end with **"Let's KRAIL."** (Android) or **"Let's KRAIL."** (iOS). Never "Let's KRAIL Sydney." on iOS unless it's a big feature launch.

---

## Voice & style rules

- Write like a Sydney local who built the app for themselves — warm, direct, zero corporate fluff.
- Features first, bug fixes second. Lead with what users gain, not what broke.
- Never use the word "experience". Never say "we are pleased to announce". Never say "improvements".
- Bug fixes get one line max: "Fixed: [what users were seeing]."
- Don't list every commit — synthesise into user-facing changes.
- Short sentences. Present tense. Active voice.
- Em dashes are OK in release notes (this is not user-facing UI copy).
- Numbers (1, 2, 3) for iOS bullet lists. **No emoji on iOS — ever.**
- Android: emoji are allowed as bullet points only if they genuinely add character (e.g. 🏷️ 🕐 🛠️). Use sparingly — 0–3 per release max. Never force them.
- If it's a small patch/maintenance release, keep it to 2–3 lines. Don't pad.

---

## KRAIL product context (needed to write accurate notes)

- KRAIL is a Sydney public transport app — Train, Bus, Metro, Ferry, Light Rail.
- No ads, no subscription, free. Built by one person (Karan).
- Key features: trip planner, saved trips, live departure boards, Park & Ride, journey map, Discover Sydney, service alerts, dark/light mode, themes.
- Transport data comes from Transport for NSW (TfNSW).
- "Home Screen" means the main saved trips screen. "Journey card" is a trip result row.
- Labelled stops = user-named favourite stops (e.g. "Home", "Work").

---

## Historical examples — learn the pattern from these

Use these as the sole style reference. Match the voice, length, and structure.

---

### v1.19.0 — Android (Play Store)

```
Live Departure Boards: Tap any stop on the map to see upcoming services. Filter by line (T1) or bus number. Saved stops now show live boards on your Home Screen! 🕒

"2 Mins Away": Use the new Share button to send your live ETA to friends as an image or text. 📲

Fixed: Trips no longer vanish the moment they depart—perfect for checking stops on the go. 🛠️

Let's KRAIL Sydney.
```

### v1.19.0 — iOS (App Store)

```
Live Departure Boards: Tap any stop on the map to see upcoming services. Filter by line or bus number instantly. Saved stops now show live boards on your Home Screen.

Share your ETA: New Share button in the journey card lets you send your arrival time to friends via image or text.

Trip Memory: Fixed a bug where services disappeared immediately after departing. Now you can still view your stops.

Let's KRAIL.
```

---

### v1.18.0 — Android & iOS (same text used for both)

```
Reliability, refined.

1. Latest NSW Timetables: Complete update to all NSW transport data, stops and routes are now more accurate than ever.
2. Performance Boost: Under-the-hood upgrades to our core systems for a faster, more fluid experience.
3. Deployment Tweaks: Optimised our update process to get fixes and new data to you even faster.

Lets KRAIL Sydney.
```

---

### v1.17.0 — Android & iOS (same text)

```
Explore Sydney like never before.

We've put your entire trip on a map. No more guessing which way to walk or which side of the street your bus stop is on; we'll show you exactly where to go.

See what's nearby. Every stop and station near you is now on the map. Whether you're looking for a Train, Bus, or Metro, just toggle your view to see what's close by.

Journey Cards now expand more naturally. Whether you're looking at the quick view or diving into the details, you'll never lose sight of your "On Time" or "Delay" status.

Tap the new map button inside any trip to see every interchange and stop along your route before you even leave the house.

Let's KRAIL.
```

---

### v1.16.0 — Android (Play Store)

```
A smoother ride, sorted.

🔍 Crash-Free Search: Squashed the bug causing crashes when searching for stops. Search away!

✅ Buttons Fixed: No more dead ends. All buttons—including the upgrade screen, are now fully functional.

🚀 Engine Boost: A major technical overhaul under the hood to keep KRAIL fast and future ready.

Fewer bugs, better commutes.

Let's KRAIL.
```

### v1.16.0 — iOS (App Store)

```
Reliability, refined.

Search Stability: Resolved issues causing interruptions during stop searches for a smoother experience.

Links Fixed: All navigation and upgrade buttons are now fully responsive and working as intended.

Core Update: Significant technical improvements under the hood for better performance and stability.

Fewer obstacles, better trips. Let's KRAIL.
```

---

### v1.15.0 — Android (Play Store)

```
Sydney's calling, and we've got the pulse.

✨ Discover Sydney: Tap the new SYD button at the top to see what's happening in and around the city. Your commute just got a lot more interesting.

🔍 Chill Search: No more worrying about the shift key. Whether you type 'x' or 'X', we find your stops every time.

🚀 Smooth Start: We've squashed those pesky launch crashes. KRAIL is now faster and steadier from the moment you tap the icon.

Discover more, search less. Let's KRAIL.
```

---

### v1.14.0 — Android & iOS (same text)

```
Sydney BUS riders, this one's for you.

- Search by Bus Number: You can now type a bus number (e.g., 423) directly into the search fields. KRAIL will pull up the routes and stops instantly, so you can get moving faster.
- Layout Fix (iOS): We've cleared out that extra space at the top of the screen from last version (1.13.0). It's a cleaner, tighter look for your commute.

Let's KRAIL Sydney.
```

---

### v1.10.0 — Android (Play Store)

```
✨ New: You can now choose your vibe — Light Mode or Dark Mode — no matter what your phone's on. Your KRAIL, your style.

📣 Added: "Invite Friends" button — help more Sydneysiders ride ad-free.

🐞 Fixed: Some pesky alert notification bugs that messed with timing. All smooth now.

More control, more style, more KRAIL. Update and ride easy.
```

---

### v1.9.0 — Android & iOS (same text)

```
1. Recent searches are remembered. Your last stops now stick around in search, so you can tap instead of typing.

2. Bug fix: The From / To fields now hold onto your chosen stops as you hop around the app. No more resets.

Less re-typing, more traveling. Update and ride on.
```

---

### v1.8.0 — Android (Play Store)

```
🚌 Bus numbers now show up-front on the card. No more tapping to see which bus you need.

⏱ Live status labels (On time, Early, Delayed) are finally here! One of the most requested features.

🎨 Refreshed design: Now, only transport mode colours are used to reflect Sydney's real network. Subtle borders are used to reduce visual clutter and clean space.
```

### v1.8.0 — iOS (App Store)

```
This update makes your trip easier at a glance:

1. Bus numbers are now shown upfront on the journey card, so you know your bus without expanding.

2. Live status labels (On time, Early, Delayed) are added — a top request to help you plan better.

3. The design is refreshed: no more heavy borders. We've gone with subtle dividers and kept colors only for transport modes, matching Sydney's look.

Cleaner. Simpler. More useful.
```

---

### v1.7.1 — Android (Play Store)

```
🚗 From Car to Train, without the pain. 🅿️ 🚂

Driving to the station? We've got you.

Introducing Park & Ride (Beta) — our newest feature shows you where to leave your car and hop on a train / metro, with near real-time spot availability at select stations. We're still tuning it, so if something's off, hit us up at hey@krail.app or LinkedIn.

🅿️ No more endless laps around the car park.
🚆 Just park, ride, and relax.
➕ LinkedIn added in Settings — come say hi (or stalk us 👀)
```

---

### v1.6.1 — Android (Play Store)

```
🚨 Stop Fixes Ahead! 🛠️📍

We've fixed a bug that was mixing up station names and stop locations. Now KRAIL picks the right stop every time, no more platform or bus stand confusion!
```

---

### v1.6.0 — Android (Play Store)

```
🚀 Smarter & Smoother!

🧭 Platform and bus stand info is now more clear.

💬 A new refreshed movie themed vibes, when referring your friends.

📱 Better support for iPad and enhanced user experience.

♿️ Accessibility updates for large text size.

⚙️ Lots of under-the-hood upgrades & bug fixes

Update now and ride easy! 🚌🚆✨
```

---

## Output format

Present the two drafts clearly separated:

```
═══════════════════════════════
ANDROID — Play Store
═══════════════════════════════
v1.20.0

[draft here]
(character count: N / 500)

═══════════════════════════════
iOS — App Store
═══════════════════════════════
v1.20.0

[draft here]
(character count: N / 4000)
```

Then ask: "Want any changes before I finalise these?"
