# Fuzzy Stop Search

## Why it exists

Analytics over 60 days showed ~7-8% of stop searches returned zero results (~321 events, ~130 users). Most of these came from:
- Typos and fat-finger swaps (`blackyown`, `cebtral`)
- Missing spaces (`townhall`, `bellav`)
- Wrong first syllable (`earraw` for `Warrawong`)
- Abbreviated street types (`york rd` for `York Road`)
- Progressive typing and missing letters (`schofil`, `schofld`, `townh`)

The fuzzy layer silently bridges these gaps without changing the exact-match path.

---

## How search works end-to-end

```
User types query
       │
       ▼
1. Exact DB search  (LIKE '%query%')
       │
       ├─ ≥ 5 results ──► sort by priority → return
       │
       └─ < 5 results ──► FUZZY PATH
              │
              ▼
       2. Trigram prefilter  (pull ≤200 candidate stops from DB)
              │
              ▼
       3. Score each candidate  (tokenOverlap + concatSplit + prefix bonus)
              │
              ▼
       4. Keep candidates above score threshold
              │
              ▼
       5. Merge exact + fuzzy, deduplicate, re-prioritise
              │
              ▼
       6. Return top 50
```

The fuzzy path only runs when exact search returns fewer than 5 stops — it's a **fallback, not a replacement**.

---

## Abbreviation expansion

Before any comparison, both the query and each candidate stop name are **normalised**:

1. Lowercase and trim
2. Strip punctuation (keep letters, digits, spaces)
3. Collapse multiple spaces
4. Expand common abbreviations **per token** (whole token only, not substring)

| Typed | Expanded |
|---|---|
| `rd` | `road` |
| `st` | `street` |
| `stn` | `station` |
| `ave` / `av` | `avenue` |
| `pde` | `parade` |
| `hwy` | `highway` |
| `fwy` | `freeway` |
| `dr` | `drive` |
| `ct` | `court` |
| `pl` | `place` |
| `blvd` | `boulevard` |
| `sq` | `square` |
| `ln` | `lane` |
| `crn` | `corner` |

**Query expansion is bidirectional**: the user can type `rd` and match `Road` (query `rd` → `road`), or type `road` and match a stop named `York Rd` (candidate `rd` kept unexpanded → `rd`; query `road` matches via prefix since `road`.startsWith(`rd`) is false but `rd` is a prefix of `road`). The word "station" in a stop name is **not** expanded to "stn"; expansion only goes from abbreviation to full form.

**Candidate stops are NOT abbreviation-expanded.** Only the user query is expanded. This prevents `St James Station` → `street james station` from matching `cowper street` perfectly, and prevents `Alison Park Blackwall Point Rd` → `…road` from matching `blacktown road` exactly at the `road` token.

**"St" at position 0 is never expanded** (in either query or candidate), because leading `St` is a saint prefix (`St James`, `St Leonards`), not a street abbreviation. `St` elsewhere (e.g. `Pitt St`) still expands to `street` in the query.

---

## Trigram prefilter

Scoring 37,000+ stops per keystroke is too slow. Instead:

1. Normalise the query and split into tokens (minimum 2 characters each)
2. For each token, extract 3-character substrings at positions 0, 1, 2, and 3:
   - `"schofld"` → `"sch"`, `"cho"`, `"hof"`, `"ofl"`
   - `"cebtral"` → `"ceb"`, `"ebt"`, `"btr"`, `"tra"` (position 3 `"tra"` hits `"central"`)
   - `"earraw"` → `"ear"`, `"arr"`, `"rra"`, `"raw"` (position 1 `"arr"` hits `"warrawong"`)
3. Deduplicate, then run up to 4 DB `LIKE '%trigram%'` queries
4. Cap the candidate pool at 200 stops

3-character substrings are specific enough that each DB query returns tens of rows rather than hundreds. Each trigram is capped at 50 stops so a high-volume trigram (e.g. `"hal"` matching hundreds of "Hall" stops) cannot consume all 200 slots and starve the other trigrams.

For a single-character typo, at least one of the four positional trigrams will share an exact 3-char run with the target name — so the correct stop reliably ends up in the candidate pool.

### High-priority stop seeding

Before the trigram queries run, the ~15 high-priority stops (Central, Town Hall, Wynyard, Tallawong, etc.) are fetched directly by stop ID and pre-loaded into the candidate pool. This guarantees major train stations are always scored, regardless of what the trigram prefilter finds. Stops that don't score above the threshold for the current query are still filtered out by the scorer.

The fetch uses `Sandook.selectStopsByIds(...)` — a single batch SQL query (`WHERE stopId IN :stopIds`) instead of N round-trips, so seeding adds one DB call regardless of how many high-priority IDs are configured.

---

## Scoring

Each candidate is scored with two complementary functions and the higher one wins, with bonuses:

```
score = max(tokenOverlapScore, concatSplitScore × 0.9) + prefixBonus + specificityBonus
```

### Token overlap score

Splits both query and candidate into tokens. For each query token, finds the best-matching candidate token using three signals:

| Signal | How | Example |
|---|---|---|
| **Prefix** | Does the candidate token start with the query token? | `schofil` → `schofields` = 1.0 |
| **LCS** | Longest common substring ÷ query token length | `earraw` in `warrawong` = 5/6 = 0.83 |
| **Edit distance** | `1 − levenshtein(q, c) / max(len(q), len(c))` | `blackyown` vs `blacktown` = 1 − 1/9 = 0.89 |

After the multi-token gate passes, an **abbreviation reverse-match** boost is applied: if a candidate token at position > 0 is an abbreviation that expands to the query token (e.g. candidate `st` → query `street`), that query token scores 1.0. Position 0 is excluded because leading `St` is a saint prefix (`St James`), not a street abbreviation. The boost is applied after gate evaluation so it does not weaken false-positive protection (the gate still uses raw pre-boost scores).

Token scores are **length-weighted** so a 9-char token like `blacktwon` dominates a 2-char fragment like `ro`. Per-token scores are averaged across all query tokens.

### Concat split score

Handles missing spaces. Searches each candidate token as a substring of the raw query (without splitting it), then divides by query length:

- `"townhall"` → finds `"town"` at pos 0, `"hall"` at pos 4 → matched 8/8 = 1.0
- `"bellav"` → finds `"bella"` at pos 0, `"v"` partial → matched 5/6 = 0.83

Dividing by query length (rather than `max(totalCandidateLen, queryLen)`) means `"townhall"` matching `"town"` + `"hall"` in `"town hall station"` scores 8/8 = 1.0 rather than 8/15 = 0.53. Candidates with extra tokens are handled by the specificity bonus instead.

### Prefix bonus

If the normalised candidate name **starts with** the normalised query, adds `+0.1`. This rewards stops whose full name begins with exactly what the user typed.

### Specificity bonus

Adds `(queryTokenCount / max(queryTokenCount, candidateTokenCount)) × 0.1`. This breaks ties in favour of shorter, more-specific stop names:

- `"wollongong"` (1 token) vs `"Wollongong Station"` (2 tokens): bonus = 1/2 × 0.1 = 0.05
- `"wollongong"` (1 token) vs `"Wollongong Rd opp Earle St"` (5 tokens): bonus = 1/5 × 0.1 = 0.02

Both stops score 1.1 on the base signals, so without this bonus the road stop (listed earlier in the DB) would win. With it, `Wollongong Station` ranks first.

### Multi-token quality gate

For queries with ≥ 2 tokens, a candidate is **rejected outright** (score forced to 0) if 2 or more of its per-token scores (using raw, pre-boost signals) fall below **0.6**. This prevents weak partial overlaps from accumulating into a passing average:

- `"blacktown road"` vs `"Alison Park Blackwall Point Rd"`: `"blacktown"` best-matches `"blackwall"` at 0.56 and `"road"` best-matches `"rd"` at 0.50 — both below 0.6, so the stop is rejected.
- `"bella vista m"` vs `"Bella Vista Station"`: `"bella"` = 1.0, `"vista"` = 1.0, `"m"` ≈ 0.2 — only one token below 0.6, so the stop is kept.

The gate uses **pre-boost scores** (before abbreviation reverse-matching). This is critical: `"road"` vs `"rd"` scores 0.50 without the abbrev boost, keeping the gate effective. If the boosted score were used, `"road"` would score 1.0 against `"rd"` and the gate would not fire.

Single-token queries are not affected by this gate.

---

## Score thresholds

Short queries are noisier (a 2-character query matches almost anything), so they need a higher score to pass:

| Query length | Minimum score |
|---|---|
| 1-3 characters | 0.85 |
| 4-6 characters | 0.55 |
| 7+ characters | 0.50 |

---

## High-priority stops

Certain stops (major interchanges: Central, Town Hall, Wynyard, Tallawong, etc.) are declared high-priority via remote config (`high_priority_stop_ids`). These always sort to the **top of the result list**, regardless of transport mode rank or whether they came from exact or fuzzy search.

Sorting is three-level:
1. High-priority stop? → 0 (top), else 1
2. Transport mode priority (Train > Metro > Bus > ...)
3. Stop name alphabetically

Both the exact-only path and the merged exact+fuzzy path pass through `prioritiseStops`, so this guarantee holds in all cases.

---

## Feature flag

The fuzzy fallback is gated by `enable_fuzzy_stop_search` (Firebase Remote Config, default `false`). Flip it to `true` in the console to enable for production traffic.

When the flag is `false`, the search returns exact DB results only (same behaviour as before this feature was added).

---

## Input boundary handling

Before any DB query, regex normalisation, or scoring runs, the user query passes through three boundary checks in `RealStopResultsManager.fetchStopResults`:

1. **Truncate to `MAX_QUERY_LENGTH` (64 chars).** The longest legitimate query observed in 60-day analytics is ~30 characters; 64 gives ~2× headroom while rejecting pasted megabyte-sized strings before they can blow up DB `LIKE`, regex, or Levenshtein matrices.
2. **Trim leading/trailing whitespace.** `LIKE 'X%'` won't tolerate a leading space, so `"  central"` was previously missing matches.
3. **Reject queries shorter than `MIN_QUERY_LENGTH` (2 chars).** Single-character substring `LIKE` returns a flood of accidental matches and the ranker is too noisy to be useful below this length. Returns empty immediately, with zero DB cost.

These are covered by `StopResultsManagerQueryBoundaryTest` in `commonTest`.

---

## Production hardening

Beyond the algorithm, three runtime guards live in `RealStopResultsManager`:

- **Off-Main dispatch.** `fetchStopResults` runs inside `withContext(Dispatchers.Default)`. The ViewModel launches it on `viewModelScope` (Main by default), so without this wrapper the DB queries plus 200-candidate fuzzy scoring would block the typing path on slower Androids.
- **Partial-success on fuzzy failure.** The fuzzy fetch is wrapped in `runCatching`. If the ranker or its DB queries throw (corrupt index, unexpected scoring input), the call falls back to exact-only results and logs the failure — the user still sees their exact matches rather than an error screen.
- **Cancellation via the ViewModel.** `SearchStopViewModel` cancels the previous `searchJob` on every keystroke and debounces 100ms. Combined with the 200-candidate production cap (≈ 20 ms total ranking work), stale rankings drop within one cache window. The ranker itself is intentionally not `suspend` — making it so just to call `ensureActive()` between candidates would force every test into `runTest {}` for negligible real-world benefit.

---

## Eval framework and discovery

`FuzzyStopSearchEvalTest` (in `androidHostTest`) loads all 37,208 real NSW stops from `nsw_stops_eval.csv` and grades the ranker against assertion cases derived from 60-day zero-result analytics. The CSV is the **benchmark snapshot**; regenerate via `python3 scripts/extract_nsw_stops.py` only when stop data changes.

Two test methods:

- `eval fuzzy stop quality against real NSW stops` — runs `cases` with include/exclude assertions; fails below 80% pass rate.
- `discover new queries against real NSW stops` — runs `discoveryQueries` and prints top-10 results to stdout without asserting. Use this to ask **"what would the user actually see if they typed X?"** before promoting to a hard assertion.

To explore a new query: add the string to `discoveryQueries`, run

```
./gradlew :feature:trip-planner:ui:testAndroidHostTest \
  --tests "*.FuzzyStopSearchEvalTest.discover*"
```

inspect the printed results, then promote to `cases` with an `EvalCase(...)` once you're happy.

---

## What it cannot solve

- **Civic addresses** (`29 Bathurst St`, `219 Kent St`) — these don't exist as stops in GTFS. Needs a geocoding layer (separate ticket).
- **Route number searches** (`702`, `M50`) — handled by the separate route-search path, not fuzzy matching. Future work: short-circuit when the route or stop ID matches exactly so the noisy fuzzy results don't drown out the obviously-intended one result.
