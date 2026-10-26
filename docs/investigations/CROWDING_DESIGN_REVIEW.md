# Crowding in KRAIL: design review

The design record for showing how full a train, metro, bus or light rail service is. It covers what
the data allows, the chosen design, the wording rules, the rider thinking, the first field results
and the decisions. The interactive version of this page, with tappable phone mockups and every
state, is [`CROWDING_DESIGN_REVIEW.html`](CROWDING_DESIGN_REVIEW.html); open it in a browser.

What Transport for NSW publishes, and how reliable it is, lives in
[`TFNSW_OCCUPANCY_DATA.md`](TFNSW_OCCUPANCY_DATA.md). This page does not repeat it.

## What the data allows

- **Three levels, never numbers:** Seats available, Few seats, Standing only.
- **Next 30 to 50 minutes only.** Later departures carry nothing, so they show nothing.
- **Forecasts hold up:** within one level over nine times in ten (measured, below).
- **Gaps by mode:** light rail L1 to L3 and ferries report nothing. Metro has no per-stop
  forecasts. About 40% of Sydney Trains report live.

## Design

### Chart: one colour, filled boxes (decision 1A)

Each carriage is a small box in the line card's own colour. How full the box is shows the level.
One sentence above the boxes says where the seats are, and one legend row lists only the levels
present. Rejected: traffic light colours on a coloured card (cheap, clashes), labels under each
run of cars (collapse when levels alternate), "Front" and "Back" helper labels (crowding).

Cars run back to front, left to right. At 320 dp or a large font size the cars merge into three
blocks: Back, Middle, Front.

### Where it shows

| Surface | What it shows | Where the data comes from |
|---|---|---|
| Trip results, tag beside the time | "Seats at the back" (decision 2A: the tag names the zone) | The trip response the screen already loads. No loading state |
| Trip card, opened | Sentence plus carriage row | Sydney Trains trip updates feed, fetched once per results screen. Drawn only if it has arrived |
| Departure board, row tapped | Sentence, carriage row, expected level at the next stops. No Track button (decision 3A) | Same as above |
| Live trip screen | Carriage row | Feed it already downloads |
| On-device AI line | One extra line only if it says something the screen does not (decision 4A) | On the phone, behind `crowding_ai_tip_enabled`, off by default |

### States

- The tag arrives with the trip response, so it has no loading or error state: present or absent.
- The carriage row's slot is decided before the card is shown. If the feed fails afterwards, the
  row fades (about 150 ms) and folds to zero height (about 300 ms). No placeholder.
- The AI line keeps a fixed height while it writes. If it fails, repeats the screen or fails the
  fact check, it folds away. No fallback text.

### Every width

Layout follows the width the block gets and the font size, never the device. Stops stay vertical
in landscape. Foldables stack vertically with the chart capped at about 480 dp. Dual-pane lists
render as on a phone (480 dp list pane).

## Wording

Say "seats", never "room". Match the level, and match certainty with one word.

| Situation | Line |
|---|---|
| One end has seats | Seats at the back of the train. |
| One end has a few seats | A few seats left at the back. |
| Forecast, not live | Seats expected at the back. |
| Both ends have seats | Seats at both ends of the train. |
| Seats everywhere | Seats available all along the train. |
| Few seats everywhere | A few seats left along the train. |
| Full everywhere | Standing only, all along the train. |
| Next train has seats | Want a seat? The next T1, at 8:59am from Platform 1, has seats available and is due at Central at 9:26am. |
| Next bus has seats | Want a seat? The next 333, at 9:04am, has seats available. |
| Night, after about 9pm | It's late: wait in the blue Nightsafe area to board near the guard. |

Rules:

- **No car numbers.** Nobody counts carriages on a platform. Front, middle and back only.
- **Inform, never instruct.** Never "skip", "wait" or "don't board". If KRAIL says skip and the
  next one is full, KRAIL gets the blame.
- **Who gets the verb depends on the cost of being wrong.** Where to stand is cheap and checkable:
  speak to the rider. Another service costs time: the service gets the verb, the rider chooses.
- **Name another service like the station board:** line, clock time with am or pm, platform.
  Never "3 minutes behind". Arrival is "due at", never "by" (a promise).
- **Certainty in one word:** live "has", forecast "expected" or "should", proven pattern
  "usually", no data says nothing. Never "will".
- **A next service is mentioned only** when it is at least one full level quieter, leaves within
  about 10 minutes, the trip has no tight connection later, and it is not the last service of the
  night. For buses, only when the departure monitor lists that next bus at the same stop.
- **Night:** NSW Police advise the blue Nightsafe area near the guard, so KRAIL says that instead
  of pointing to the emptiest car, and never mentions a later train.
- Full sentences with a verb, two at most, about twelve words each.

## Who is looking

| Rider | Needs | Risk to design against |
|---|---|---|
| Rushing commuter | Where to stand, at a glance | Anything longer than a short line is ignored |
| Needs a seat | A seat more than a few minutes | Mentioning a later train that is full too |
| Travelling at night | To feel safe | Pointing to the emptiest car after dark |
| Visitor | Plain words | Car numbers and line codes mean nothing |
| Tight connection | To make the change | A later train that breaks the trip |

## Field results

One evening is Tier 1: a lead only, never shown to riders as "usually". A pattern needs to hold
on at least 8 of 10 weekdays (Tier 2). Forecast accuracy is a measurement and stands on its own.
Every n counts vehicle readings (one vehicle on one poll), not distinct trips.

### Monday 26 October, evening peak (5:00 to 6:30pm)

Direction 1 is the one that fills at Redfern, Town Hall and Wynyard on most suburban lines, so most
likely leaving the city.

| Service | n | Seats available | Few seats | Standing only |
|---|---|---|---|---|
| T1 Western, direction 1 | 849 | 40% | 56% | 3% |
| T1 Western, direction 2 | 693 | 99% | 1% | 0% |
| T1 North Shore, direction 1 | 576 | 83% | 17% | 0% |
| T1 North Shore, direction 2 | 701 | 93% | 7% | 0% |
| T2 Inner West, direction 1 | 1,347 | 76% | 24% | 0% |
| T2 Inner West, direction 2 | 918 | 100% | 0% | 0% |
| T3 Bankstown, direction 1 | 306 | 83% | 17% | 0% |
| T3 Bankstown, direction 2 | 129 | 100% | 0% | 0% |
| T8 Airport, direction 1 | 1,146 | 58% | 40% | 2% |
| T8 Airport, direction 2 | 895 | 99% | 1% | 0% |
| T9 Northern, direction 1 | 661 | 75% | 25% | 0% |
| T9 Northern, direction 2 | 470 | 99% | 1% | 0% |
| Blue Mountains, direction 1 | 123 | 84% | 6% | 11% |
| Blue Mountains, direction 2 | 277 | 68% | 25% | 7% |
| Central Coast, direction 2 | 37 | 27% | 0% | 73% |
| South Coast, direction 1 | 16 | 0% | 0% | 100% |
| South Coast, direction 2 | 11 | 100% | 0% | 0% |
| Metro M1, both directions | 2,746 | 34% | 26% | 40% |
| Buses, every route | 75,336 | 90% | 8% | 2% |
| Parramatta Light Rail | 255 | 100% | 0% | 0% |
| Newcastle Light Rail | 87 | 97% | 3% | 0% |

**Ends against the middle** (trains with carriage data only; "mixed" means the cars were not all
the same level):

| Line | n | Mixed readings | An end roomier | Middle roomier |
|---|---|---|---|---|
| T1 Western | 1,542 | 29% (445) | 45% | 5% |
| T1 North Shore | 1,277 | 24% (311) | 57% | 1% |
| T2 Inner West | 2,265 | 36% (817) | 61% | 0% |
| T3 Bankstown | 435 | 19% (84) | 45% | 4% |
| T8 Airport | 2,041 | 37% (760) | 40% | 11% |
| T9 Northern | 1,131 | 24% (266) | 36% | 1% |
| Blue Mountains | 400 | 60% (238) | 58% | 0% |
| Metro M1 | 2,746 | 64% (1,760) | 52% | 0% |

**Forecast accuracy** (each per-stop forecast against the train's live reading when it got there):

| Ahead | n | Exactly right | Within one level |
|---|---|---|---|
| 5-15 min | 24,580 | 68% | 96% |
| 15-30 min | 20,885 | 62% | 95% |
| 30+ min | 17,095 | 64% | 93% |

Monday morning, for comparison: 71 to 77% exact, 92 to 95% within one level.

**Buses close together** (same route and direction within 400 m; may include buses waiting at a
terminus): 12,809 pairs. One roomier: 18%. One Standing only and the other Seats
available: 3%.

### What the evening says about each line

| Verdict | Line | Why |
|---|---|---|
| Supports | Seats at both ends of the train | When cars differ, an end beat the middle on 36 to 61% of readings by line; the middle beat both ends on 1 to 11% |
| Supports, blocked | Seats at the back / at the front | Each line favours one end strongly in feed order. Which real end is unknown until the platform check |
| Weakens | How often zone advice applies | Only 19 to 37% of suburban readings had mixed cars; Metro and Blue Mountains about 60% |
| Supports | "Expected" for forecasts | Within one level 93 to 96% at every lead time |
| Kills, for now | Eases off after a named station | No station passed 6 trips in one evening |
| Weakens | Next bus has seats | Buses 90% Seats available; Standing against Seats in only 3% of close pairs |
| Weakens | A light rail tag | Parramatta all Seats available, Newcastle 97% |
| Lead | Where crowding is real | Metro M1 Standing only on 40% of readings |

## Decisions (26 October)

Code: **1A 2A 3A 4A 5C 6A**.

1. **Chart: A, one colour, filled boxes.** Calm, works for colour-blind riders. Not traffic
   light colours.
2. **Tag: A, say where the seats are** ("Seats at the back"), so the rider can act without
   opening the card. Not the overall level.
3. **Departure board tap: A, seats and carriages only.** Shortest row; the board stays scannable.
   Not a Track button or the full stop list.
4. **On-device AI: A, in version 1** behind `crowding_ai_tip_enabled`, off by default, with a
   debug override. Most phones have no model and see nothing extra.
5. **First release: C, everything,** including the carriage row in trip cards. Costs one extra
   download of the Sydney Trains trip updates feed per results screen on the shared key.
6. **Housekeeping: A,** research and rules on their own branch; the build stacks on top.

## Before building

1. **Which end is the front.** The feed's car order is not tied to direction of travel in any way
   we can see (car IDs differ on every trip), and trains reverse at termini. Check on a platform:
   note the feed's per-car levels before a train arrives, watch which end leads, compare which end
   is fuller, repeat in the other direction. Until then, "back" and "front" stay off; "both ends"
   and "middle" ship.
2. Use the level at the rider's boarding stop.
3. When carriage data exists, work the tag out from the cars, so the two never disagree.
