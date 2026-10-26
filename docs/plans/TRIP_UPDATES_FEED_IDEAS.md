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
