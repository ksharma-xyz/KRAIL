# Ideas from the Sydney Trains trip updates feed

Not planned. A parking place for features that could reuse the download the crowding feature
already makes (`/v2/gtfs/realtime/sydneytrains`, about 285 KB, once per results screen). Field
details are in [`TFNSW_OCCUPANCY_DATA.md`](../investigations/TFNSW_OCCUPANCY_DATA.md), "What else
the Sydney Trains trip updates feed carries". Checked on one evening, 26 October 2026, 619 trips.

| Idea | Feed field | Seen that evening | What a rider would get | Open questions |
|---|---|---|---|---|
| Cancelled trains | `TripDescriptor.schedule_relationship = CANCELED` | 97 trips | A cancelled service marked on trip results and the departure board instead of shown as running | Does the trip planner already drop or flag these? Compare the same trip in both sources first |
| Replacement trips | `schedule_relationship = REPLACEMENT` | 139 trips | Know the timetable changed (trackwork) | What a replacement trip looks like in the trip planner response |
| Skipped stops | `StopTimeUpdate.schedule_relationship = SKIPPED` | 30 stops | "This train won't stop at Redfern" before the rider boards | Whether the trip planner already reflects it |
| Short trains | Carriage count per stop (4 or 8) | 55 stops with 4 cars | "4-car train: it stops in the middle of the platform" | Which platforms have markers; wording |
| Train type and facilities | Vehicle extension 1007 field 3, one letter per type | Every trip | "This train has toilets" on long trips | Letter meanings are not published; verify each against the fleet before use |

Delays are already in the trip planner response, so they are not an idea here.

Before picking any of these up, run the `transit-3-source-check` skill on a few affected trips to
see what the trip planner and transportnsw.info already show. An idea is only worth building if
KRAIL would show something those sources do not.

## Smarter crowding, parked for later

Ideas from testing crowding on a phone (27 October). Each one states facts from the feed and
leaves the decision to the rider: never "take this one", "wait" or "skip". All are written by
code, not an on-device model (see the design review for why).

| Idea | What the rider would see | Check before building |
|---|---|---|
| A tag per train on a trip with a change | `T5 Seats   T8 Standing only` on the closed card, so a packed second train is not hidden behind an empty first one | Fits at 320 dp and 200% font |
| Trains just ahead, when yours has no forecast yet | "T8: no forecast yet. The 7:42 and 7:47 from Glenfield are standing only." | Replay collected peaks: how often back-to-back trains on a line share a level. Ship only if they usually do |
| Seats opening up or the train filling later in the trip | "More seats expected from Strathfield." / "Expected to fill up from Parramatta." | Rules and replay in `TFNSW_OCCUPANCY_DATA.md`, "Forecasts along the trip" |
| The second train of a trip with a change | "Your next train, the T8 from Central, is expected to be standing only." | Second train's forecast must exist at the change station |
