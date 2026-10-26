# Crowding scenarios

Every crowding case a rider can meet, what KRAIL must show for it, how to find one on the live
feed, and which test holds it. Read with
[`CROWDING_DESIGN_REVIEW.md`](../investigations/CROWDING_DESIGN_REVIEW.md) (the wording table is
the source of truth) and [`TFNSW_OCCUPANCY_DATA.md`](../investigations/TFNSW_OCCUPANCY_DATA.md)
(what the feeds carry).

Cars are written one letter per car in feed order: **S** seats available, **F** few seats,
**X** standing only. An end is the end car (one per end up to 11 cars), the middle is the rest,
and each zone counts as its busiest car.

## Scenarios

The tag is the words beside the time on the closed card and the departure row, led by one small
bar per car when the cars are known. The sentence sits above the boxes on the opened leg card
and the opened departure row.

| Scenario | Example cars | Tag | Sentence (forecast) |
|---|---|---|---|
| Seats all along | `SSSSSSSS` | Seats available | Seats expected all along the train. |
| Few seats all along | `FFFFFFFF` | Few seats | A few seats expected along the train. |
| Standing all along | `XXXXXXXX` | Standing only | Standing only expected all along the train. |
| Seats at both ends | `SSFFFSSS` | Seats at both ends | Seats expected at both ends. |
| Few seats at both ends | `FXXXXXXF` | Few seats at both ends | A few seats expected at both ends. |
| Seats in the middle | `XSSSSSSX` | Seats in the middle | Seats expected in the middle. |
| Seats at one end only | `SSFFFFFF` | Few seats (the level most cars share) | None until "front" and "back" are switched on |
| No car data | none | Whole-train words from the trip planner, or nothing | None, and no boxes |
| Bus | none | Seats available, Few seats or Standing only, once the bus has started its trip | None |
| Night, after 9pm | any | As above | It's late. Wait in the blue Nightsafe area to board near the guard. |
| Trip with a change of trains | per train | First train only (one tag per train is parked) | One per leg card |

Behaviour that holds in every scenario:

- The forecast refreshes with the results screen every 30 seconds. Fills and bars ease to a new
  level, changed words cross-fade, and a tag that appears or goes away opens or folds top-down.
- One failed refresh keeps the last forecast. Three in a row clear it, and the boxes fold away.
- A departure row opens on tap only when it has cars to show.

## Testing on the live feed

Forecasts change every minute, so live checks are manual, never CI. When a live check finds a
case the tests do not hold, capture that stop into `CrowdingScenarioFeedFixture` the same way
(bytes copied from the response) and add a row here. To find a train for each
scenario right now:

```sh
python3 scripts/crowding_scenarios.py
```

It prints one From and To pair per scenario for trains leaving in the next 8 to 35 minutes. Type
the pair into KRAIL, open the train's card, and compare with the table. Trains with no car data
are common (only some train types report), so a scenario missing from the output just means no
train shows it at that moment; run it again in a few minutes.

For a trip with a change of trains, search Leppington to Wynyard (T5 then T8 at Glenfield) or
Schofields to Leppington (T1 then T5).

Maestro note: on the stop search, wait for the typed name to appear in the results before
tapping a result. The list briefly shows the previous search's rows, and tapping too early picks
the wrong stop.

## Automated coverage

| Layer | What it holds | Where |
|---|---|---|
| Wording | Every row of the wording table | `CrowdingWordingTest` |
| Zone maths | Ends, middle, typical level, 4 and 8 cars, 0- and 1-based positions | `CarriageLevelsTest` |
| Feed decode | Extension 1007 read from captured feed bytes | `CarriageIndexMapperTest` |
| Real scenarios end to end | One real captured stop per scenario: decode, index, cars, tag, sentence, night line | `CrowdingScenarioTest` with `CrowdingScenarioFeedFixture` |
| Scenarios on screen | Each scenario's tag on the closed card and departure row, sentence and cars on the opened leg card and opened row | `CrowdingScenarioUiTest` (Robolectric) |
| Joining the feed to trips | Cars, sentence and tag attached at the boarding stop; refresh and failure rules | `JourneyCarriagesTest`, `DepartureCarriagesTest` |
| Screens | Tag, carriage row and opened departure row in light, dark and large font | Screenshot tests in `:feature:trip-planner:ui` |
| Departure row state | Opens, survives rotation and polling, closes | `DepartureRowExpandTest` |
