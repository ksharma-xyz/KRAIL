# Ask KRAIL — what it can actually do

A record of what shipped, in rider language, so nobody has to re-derive it from the code when
writing release notes, a store listing, or a screenshot script.

Written because capability gets forgotten faster than it gets built. Several things on this list
took a full day of work and are invisible unless you know to try them.

**Content rule:** this file is in a public repository. No user counts, no percentages, no
revenue. Describe what the app does, never how many people do it.

---

## The one-line version

**Say or type a whole journey, including when, and KRAIL fills in the trip.**

---

## What a rider can say

Everything here works typed or spoken.

| They say | It understands |
|---|---|
| "Home to work" | Their own labels, resolved to their own stops |
| "Office by 9am" | *Office* means their Work label |
| "Central to Parramatta after 6pm" | Two stops and a departure window |
| "Get me home by 9pm" | One end only — it works out where they are |
| "Town Hall to Bondi Junction in 20 minutes" | A relative time |
| "Home to work by 9am Monday" | A named day, not just a clock time |
| "Get me to uni" | *Uni*, *university*, *campus*, *college* — all the same place |

### Their words, not ours

`work` / `office` / `job` / `workplace`, `home` / `house`, `uni` / `university` / `campus` /
`college` all resolve to the label they set. They never have to learn the app's vocabulary.

### Times it understands

Clock times ("9am", "6:30pm"), relative offsets ("in 20 minutes", "in 2 hours"), named days
("today", "tonight", "tomorrow", "friday", "tues"), and arrive-by versus leave-after.

A named day beats the next-occurrence guess: someone who says Friday means Friday.

---

## Things that are quietly clever

Worth demonstrating, because none of them announce themselves.

**It knows where you're starting from — and prefers a place you named.** Say "get me home" while
you're at work and the trip starts from *your* Work stop, not from whichever bus shelter happens
to be nearest. A stop you pinned yourself is better evidence than proximity.

**It suggests something you'd actually ask for.** The line above the box changes with the time
of day and the day of the week, built from your own labels and saved trips. Weekday morning
offers the commute with an arrive-by time. Weekday evening offers to get you home. **Sunday
afternoon offers tomorrow's commute** — which is when people check it.

**It never suggests work on a Saturday.** And if the commute is all it knows about you, it
offers to get you home rather than showing two stations you have nothing to do with.

**Speaking never submits for you.** Words appear as you speak them, and the send button waits.
A mis-heard word is yours to fix before anything happens.

**It runs on the phone.** The model is on-device. What a rider types stays on their phone.

**It says when it doesn't know.** An unrecognised place is quoted back — "no stop called
*Hogwarts*" — rather than a confident wrong guess. If only one end resolves, that field fills and
the other is left to them.

---

## What it looks like

The surface is painted from the rider's chosen theme: two colours from that theme drifting
behind the input, the same pair on the mic ring that opens it, the border, and the search
screen's background. Change the theme and all of it follows.

---

## Good screenshot / demo moments

1. **Speaking, mid-sentence** — words appearing live in the box
2. **"Office by Monday morning"** resolved into a full trip with a time chip
3. **Sunday afternoon** showing `Try "Home to Work by 9am tomorrow"`
4. **The drifting background**, ideally as a short capture rather than a still
5. **Same screen in two themes**, showing the colours follow

---

## Not yet true

Do not promise these.

- No prompt to set a label when a rider says a word they have not pinned yet
- The model can still mis-split a sentence like "10am Monday work"
- Location improves the origin only when permission is already granted — it never asks

---

## Where the detail lives

- `feature/trip-planner/ui/ASK_KRAIL_UX.md` — every rule and why it exists
- `feature/trip-planner/ui/ASK_KRAIL_MANUAL_TESTS.md` — how to verify each claim here
- `feature/trip-planner/ui/AI_SEARCH_UX.md` — the pipeline and its failure modes
