# Request to KRAIL-BFF · Extract `krail-api-proto` to a shared repo

> Audience: maintainers of `KRAIL-BFF`. From: KRAIL app side. Drop this
> doc anywhere in `KRAIL-BFF/docs/handover/`; it's self-contained.
>
> Purpose: ask the BFF team to extract the `.proto` files into a
> separate, tag-versioned repo so the KRAIL app can share the wire
> contract without copying or vendoring. This unblocks Phase C of the
> KRAIL app's BFF integration plan (the proto / screen-shaped path).

---

## §1 · TL;DR

1. Create a new public repo `<your-handle>/krail-api-proto` (MIT or
   equivalent).
2. Move the proto sources from `KRAIL-BFF/server/src/main/proto/` into
   that new repo. Lay them out per `API_SCHEMA_DESIGN.md §4`.
3. **Every field declared `optional` (nullable on the wire).** Backward and
   forward compat is built on this. The runtime contract is enforced by
   server-side unit tests + client-side mappers — see §3.
4. **Server must never emit `null` for contract-required fields**, even
   though the wire format permits it. Substitute sensible defaults (empty
   string, 0, `_UNSPECIFIED` enum, empty list, default-populated nested
   message) when upstream data is missing. See §3 default table.
5. **Everything is automated via GitHub Actions** — CI lint + breaking-change
   check, release workflow, daily auto-bump PR in both BFF and KRAIL. No
   "I ran it on my laptop" steps. See §5.
6. Tag `v0.1.0` via the release workflow.
7. Replace the local protos in `KRAIL-BFF` with a git submodule pinned to
   that tag. Update `wire { sourcePath { srcDir(...) } }` to read from the
   submodule path. Add the contract-enforcement unit tests. Confirm the
   BFF builds + tests pass.
8. Tell the KRAIL app side: the repo URL, the tag, the workflow run
   links, and anything we should know about the cadence.

After step 8, KRAIL will add the same submodule and stand up an `:io:bff-api`
module that runs Wire codegen against the shared source.

---

## §2 · Why this is the right shape

This is not a new proposal — it's the recommendation you already wrote into
your own docs:

- **`API_SCHEMA_DESIGN.md §4`** ("KMP sharing strategy") ranks three options
  and recommends **Option 1: separate public repo + git submodule in both
  consumers**. Reproduced for convenience:

  > 1. Separate public repo + git submodule in both consumers (recommended).
  >    Cleanest ownership; no Maven publishing infra; submodule update is
  >    one command; the contract being public is a feature.
  > 2. Separate public repo published to GitHub Packages / JitPack as a
  >    Maven artifact. Cleaner for cross-team work; overkill for a one-person
  >    project.
  > 3. Embed the proto files inside KRAIL-BFF, KRAIL points at a path.
  >    Simplest now, biggest pain later when the contract has independent
  >    users — don't.

- **`STATUS.md §2`** lists this as a known one-time chore:

  > The `.proto` files currently live inside this repo
  > (`server/src/main/proto/`). For the KRAIL app to share generated Kotlin
  > classes you need to extract them to a standalone repo:
  > - New repo `ksharma-xyz/krail-api-proto` (public, MIT or whatever).
  > - Move `trip.proto`, `stops_dataset.proto`, `routes_dataset.proto` into it.
  > - Tag `v0.1.0`.
  > - BFF: replace local protos with a git submodule pointing at that tag.
  > - KRAIL app: same submodule, with Wire codegen in `commonMain`.
  >
  > This is a one-time chore. Do it after deploy, before app integration.

So this request is just asking you to action `STATUS.md §2`.

### Why share `.proto`, not generated Kotlin

The KRAIL app needs Kotlin classes that match what BFF emits on the wire.
We could publish the BFF's Wire-generated Kotlin as a Maven artifact, but:

- BFF runs Wire on JVM. KRAIL runs Wire on **KMP common** (Android + iOS).
  Targets emit slightly different Kotlin (e.g. iOS-specific actuals,
  KMP `expect` shims). One codegen does not fit both.
- Publishing Kotlin couples the two repos to the same Wire / kotlinx-protobuf
  version. With shared `.proto` source, each side bumps its own codegen at
  its own pace.
- The `.proto` is the contract. Kotlin is implementation. Versioning the
  contract — not the codegen output — is what `API_SCHEMA_DESIGN.md §4`
  Versioning describes.

---

## §3 · What we'd like the new repo to look like

Mirror `API_SCHEMA_DESIGN.md §4 Proposed layout`:

```
krail-api-proto/                  separate public repo
├── README.md                     (one paragraph + Wire snippets for both consumers)
├── LICENSE                       (MIT or your usual choice)
├── version.txt                   ("0.1.0")
└── proto/
    ├── core/
    │   ├── lat_lng.proto         (when shared types land — Phase 1)
    │   ├── transit_line.proto
    │   └── …
    └── api/
        ├── trip.proto            (current — at least this exists today)
        ├── stops_dataset.proto   (current)
        ├── routes_dataset.proto  (current)
        └── …                     (screen-shaped messages as they ship)
```

For `v0.1.0` you only need to move the three protos that exist today
(`trip.proto`, `stops_dataset.proto`, `routes_dataset.proto`). The
core/ shared-type split happens when those types are introduced in a
later version per the design doc.

### Submodule wiring on the BFF side

Replace whatever currently reads `server/src/main/proto/` with a submodule
read:

```kotlin
// settings.gradle.kts (or the module that owns Wire codegen)
wire {
    kotlin {
        sourcePath { srcDir("$rootDir/krail-api-proto/proto") }
    }
}
```

KRAIL will mirror this exactly, except `targets { commonMain }` for KMP.

### Versioning

SemVer at the package level, mostly per `API_SCHEMA_DESIGN.md §4 Versioning`,
amended by the nullability rule below:

- **Adding fields = minor bump.** Old clients ignore the field; new clients
  see `null` when talking to old servers (see §3 Nullability).
- **Removing fields = minor bump too**, since every field is optional.
  Document the removal in the release notes; clients eventually drop the
  reference. Optionally hold the field in `reserved` for a release.
- **Renaming = major bump.** Same field number with a new name = wire-compat
  but source-breaking on consumers. Coordinate with KRAIL release cadence.
- Each release tagged in git. Both consumers point at a **tag**, not a
  branch. `git submodule update --remote` is reserved for explicit version
  bumps (and is wired into CI per §5 below).

### Nullability — every field is optional, by default

**Rule: every proto field is declared `optional`.** No `required`, no
default-without-presence. In proto3 + Wire this means generated Kotlin sees
each field as `T?` and the parser distinguishes "field absent" from "field
present with default value."

Why:

- **Forward / backward compat is free.** Server can add a field without
  breaking older clients (they ignore it). Server can stop populating a
  field without crashing newer clients (they get `null`). No major bump
  needed for either.
- **Schema evolution doesn't gate releases.** BFF and KRAIL ship on
  independent cadences. Strict required-fields would force lock-step
  deploys.

Enforcement is split across the two sides; the proto is the permissive
common ground:

| Layer | Enforcement | What "missing" means |
|---|---|---|
| Proto schema | Nullable; "absent" is legal on the wire. | n/a — both sides must accept it. |
| **Server (BFF)** | **Unit tests assert** that response builders populate every contractually-required field. **`null` must never be emitted for those fields, even though the proto allows it** — when upstream data is missing, the BFF substitutes a sensible default (see below). CI fails if a builder leaves a promised field unset. | A bug in BFF — fix before merge. |
| **Client (KRAIL)** | Maps the nullable proto into a non-null domain model at the network-layer boundary. Missing required fields → mapper returns a typed parse error / falls back to the kill-switch path, never crashes UI. | Treated as a degraded response; logged with `correlationId` for follow-up. |

The proto says "could be null"; the BFF's CI tests prove the promise holds.
Adding a new field is a one-side change with no client work needed until
KRAIL wants to use it.

### Two field flavours — "contract-required" vs "genuinely optional"

The nullability default is a wire-format concession. The **contract** still
distinguishes two kinds of field:

| Flavour | Wire | Server emits | Examples |
|---|---|---|---|
| **Contract-required** | `optional` (nullable) | **Always populated. Never `null`.** Use a default when upstream data is missing. | `journey_id`, `origin`, `display_time`, `transit_line.color_hex` |
| **Genuinely optional** | `optional` (nullable) | `null` when not applicable. | `total_walk_duration` (null when no walking leg), `interchange`, `realtime_trip_id` (null when journey isn't trackable) |

Server defaults for contract-required fields when upstream is missing data:

| Type | Default |
|---|---|
| `string` | `""` (empty string) — never null. Display layer can render an em-space or skip the badge if empty, but receives a defined value. |
| `int32` / `int64` | `0`. |
| `float` / `double` | `0.0`. |
| `bool` | `false`. |
| Enum (e.g. `TransportMode`) | `*_UNSPECIFIED` variant (always field number `0` per §3 Backward compatibility rule 4). |
| Nested message (e.g. `StopRef`) | An instance with all of *its* contract-required fields populated to defaults. Never `null`. |
| Repeated | Empty list. Never `null`. |

How each side knows which is which:

- **Documented in proto comments** above each field (`// contract: required`
  vs `// contract: optional`). One-word convention; lives next to the field
  so it's obvious in code review.
- **BFF unit tests** assert no contract-required field is null in any
  response builder, with the substitutions above. One test per response
  type, parameterised over all required field paths.
- **KRAIL mappers** treat any contract-required field that arrives null as
  a bug and surface it (log + parse error / kill-switch fallback). Treat
  genuinely-optional null as a normal absent value.

This keeps the wire format permissive (so backward-compat is easy) while
keeping the runtime contract strict (so neither side is surprised by null
in fields it relies on).

### Backward compatibility — built on nullability

Practical rules, downstream of the nullability default:

1. **Never reuse a field number.** Mark removed fields with `reserved 7;`
   (or `reserved "old_name";`) and add a comment.
2. **Never change a field's type.** Add a new field with a new number;
   deprecate the old one in a release note; remove later.
3. **Never change a field's `optional`-ness.** All fields stay nullable
   for the life of the schema.
4. **Enums:** always include a `*_UNSPECIFIED = 0` variant. New variants
   are additive; old clients see `_UNSPECIFIED` for unknown values.
5. **Oneofs:** adding new variants is fine; removing them is a major bump
   (clients exhaust-match).

---

## §4 · What we need from you to action

A short list, in order. Every step that touches a repo must be reproducible
via GitHub Actions (see §5) — no "I ran it on my laptop" steps.

1. [ ] Create the repo. Name `krail-api-proto`, public, MIT (or whatever
       you've used elsewhere). Branch protection on `main` (require PR,
       require status checks, no force-push).
2. [ ] Copy the three proto files from `KRAIL-BFF/server/src/main/proto/`
       into `proto/api/` of the new repo. No content changes — but **add
       `// contract: required` / `// contract: optional` comments** above
       each field per §3. If a field's status isn't obvious, default to
       `required` and note any uncertain cases in the PR description.
3. [ ] Add a brief `README.md`: purpose, layout, "how to consume from
       BFF" (Wire snippet), "how to consume from KRAIL" (Wire snippet,
       `targets { commonMain }`), versioning rule, the
       contract-required-vs-optional convention.
4. [ ] Set up the four GitHub Actions workflows in §5 below
       (CI, release, BFF auto-bump, KRAIL auto-bump notification).
5. [ ] Cut `v0.1.0` via the release workflow. The tag should appear in
       `Releases` with auto-generated notes.
6. [ ] In `KRAIL-BFF`: add `krail-api-proto` as a submodule at repo root,
       pinned to `v0.1.0`. Update Wire's `sourcePath` to read from there.
       Delete the originals from `server/src/main/proto/`. Add the
       contract-enforcement unit tests (§3 Two field flavours).
7. [ ] Confirm BFF builds, tests pass, and your existing handover docs
       (`KRAIL_APP_INTEGRATION_HANDOVER.md`, `KRAIL_API_REFERENCE.md`,
       `BFF_ADOPTION_GUIDE.md`) still reference correct paths — search
       for `server/src/main/proto/` in `docs/` and update if needed.
8. [ ] Reply with:
       - Repo URL.
       - Tag (likely `v0.1.0`).
       - Workflow run links (proves the automation works).
       - Anything KRAIL needs to know — submodule SHA bump cadence,
         breaking-change policy, who maintains versioning.

---

## §5 · CI / GitHub Actions automation

Manual chores rot. Every step below is a workflow file in `.github/workflows/`.
The list is short on purpose — easy to maintain, hard to bypass.

### §5.1 · `krail-api-proto` repo workflows

**`ci.yml` — runs on every PR and push to `main`.**

- Lint protos with `buf lint` (or equivalent — `protoc --error_format` if
  you prefer to stay tooling-light). Fails on anything malformed.
- Run `buf breaking --against` against the previous tag. Blocks PRs that
  introduce wire-breaking changes (rename, type change, field-number reuse)
  unless the PR is explicitly tagged with a `major-bump` label. This is the
  enforcement mechanism for §3 Backward compatibility.
- Generate Kotlin via Wire JVM **and** KMP common configurations as a smoke
  test. Both have to compile clean before merge — catches issues that show
  up on one consumer but not the other.

**`release.yml` — runs on tag push (`v*.*.*`) or via manual dispatch.**

- Validates `version.txt` matches the tag.
- Runs the same lint + breaking-change check as `ci.yml` for paranoia.
- Creates a GitHub Release with auto-generated notes from PR titles since
  the previous tag (`gh release create … --generate-notes`).
- Optional: publishes a tarball of the `proto/` tree as a release asset
  (so consumers who don't want submodules can curl it — KRAIL still uses
  the submodule).

Manual-dispatch input: `bump_type` ∈ `{minor, patch, major}`. Workflow
computes the next version from `version.txt`, commits the bump on a
release branch, opens a PR, and tags after merge. One-button releases.

### §5.2 · `KRAIL-BFF` workflows (changes to existing CI)

**`proto-bump.yml` — scheduled daily + manual dispatch.**

- Runs `git submodule update --remote krail-api-proto` to fetch the latest
  tag (configurable: `latest-tag` vs `latest-commit-on-main`; default
  `latest-tag`).
- If the SHA changed: opens a PR titled `chore(proto): bump to vX.Y.Z`
  with the diff, auto-assigned to the BFF maintainer. Existing CI runs
  on the PR — if anything breaks (e.g. a contract test fails because BFF
  builders no longer match the new schema), the PR can't merge until
  fixed. Manual review required regardless.
- This is the deliberate, gate-able way to consume schema changes.

**Existing PR / push workflows extended:**

- Submodule fetch in `actions/checkout` with `submodules: true`.
- The contract-enforcement tests (§3 Two field flavours) run as part of the
  normal test suite — no separate workflow, just additional tests.

### §5.3 · `KRAIL` workflows (mirror of §5.2)

Same `proto-bump.yml` shape on the KRAIL side. Bumps the submodule, opens
a PR, lets `./scripts/fullQualityChecks.sh` (Android compile + iOS compile
+ detekt) gate the merge. KRAIL maintainer reviews and merges manually.

KRAIL **never auto-merges** proto bumps — schema changes can shift UI in
subtle ways (a nullable becomes non-null at runtime, a new enum variant
needs UI handling). Human review is the safety net.

### §5.4 · Versioning automation

The workflows above keep version bumps mechanical and traceable:

- **Adding a contract-optional field**: PR to `krail-api-proto` → `ci.yml`
  passes (no breaking change) → manual `release.yml` dispatch with
  `bump_type: minor` → tag `v0.x+1.0` → BFF `proto-bump.yml` opens a PR
  → KRAIL `proto-bump.yml` opens a PR.
- **Adding a contract-required field**: same as above, but the BFF PR
  also has to add the response-builder population + unit test before it
  can merge. Detected by the contract-enforcement tests failing on the
  bump PR.
- **Removing a field**: PR adds `reserved` for the field number, removes
  the field; minor bump per §3 Versioning. Same flow.
- **Renaming a field** (major bump): PR adds the new field, deprecates
  the old in proto comments; major bump. BFF + KRAIL release notes call
  out the rename and the deprecation timeline.

### §5.5 · What stays manual

- Deciding **whether** to bump (and to what level). The workflows automate
  the mechanics, not the judgment.
- Reviewing breaking-change PRs and approving the major-bump label.
- Releasing a "compat shim" version when a major bump is in flight (BFF
  serves both shapes during the deprecation window).

---

## §6 · What KRAIL will do once §4 is done

Mirror image, on the KRAIL side:

1. Add `krail-api-proto` as a submodule at `KRAIL/krail-api-proto/`,
   pinned to the same tag the BFF cut.
2. Create a new `:io:bff-api` module in KRAIL. Apply the Wire plugin
   (already in use at version `6.2.0` per `gradle/libs.versions.toml`).
   Configure `wire { kotlin { sourcePath { srcDir("$rootDir/krail-api-proto/proto") }; targets { commonMain } } }`.
3. Add the KRAIL-side `proto-bump.yml` workflow (mirror of §5.3). Daily
   schedule + manual dispatch; opens a PR, never auto-merges.
4. Validate iOS codegen builds before any consumer takes a dependency on
   the generated classes. Fallback if Wire's iOS target misbehaves:
   `kotlinx-serialization-protobuf` (last resort, same wire format,
   hand-mapped messages).
5. Add the network-layer mappers that translate nullable proto into a
   non-null domain model, with explicit handling for contract-required
   fields arriving null (log + parse error / kill-switch fallback per §3).
6. Stand up the first proto-shaped service (`/api/v1/trip/plan-proto` is
   the only proto endpoint that exists today — good first proof).
7. Track each subsequent screen-shaped endpoint as it lands per
   `API_SCHEMA_DESIGN.md §2` — no reshape on KRAIL's side, just a new
   proto file on the next minor version of `krail-api-proto`, picked up
   by the auto-bump workflow.

---

## §7 · Things we are explicitly **not** asking for

- Renaming or restructuring the proto messages themselves. v0.1.0 is just
  "extract what exists, in place." Restructuring is a separate decision.
- The screen-shaped messages from `API_SCHEMA_DESIGN.md §2`
  (`TripResultsResponse`, `DepartureBoardResponse`, etc.) — those are
  designed-not-built per `STATUS.md`, and don't need to ship for this
  request. They can land in `v0.2.0+` whenever the BFF is ready.
- Maven publishing. Submodule pattern is the recommendation; a Maven
  artifact is option #2 in `API_SCHEMA_DESIGN.md §4` and explicitly marked
  "overkill for a one-person project."
- BFF deployment. Independent track per `STATUS.md §1`.

---

## §8 · Open questions for the BFF side

Decide before kicking off §4. Ping back to KRAIL with the answers so the
KRAIL plan can be finalised:

1. **Repo name** — `krail-api-proto` (matches `STATUS.md §2`)? Or different?
2. **Repo visibility** — public (recommended in `API_SCHEMA_DESIGN.md §4
   Public-repo implications`) or private? Public is the doc's
   recommendation; the contract is recoverable from the APK anyway.
3. **License** — MIT, Apache-2.0, or whatever pattern your other repos use.
4. **Versioning** — confirm package-level SemVer per
   `API_SCHEMA_DESIGN.md §4`, not per-message. Tag-based; consumers pin a
   tag.
5. **Cut `v0.1.0` now, or wait for screen-shaped messages?** I lean cut
   now with just the three current protos — gets KRAIL unblocked on Phase
   C plumbing. Screen-shaped messages slot into `v0.2.0+` when BFF builds
   them. Pre-cutting `v0.1.0` lets KRAIL stand up `:io:bff-api` in parallel
   with BFF's screen-shaped work.
6. **Lint tooling** — `buf` (industry standard, requires installing one
   binary in CI) or stick to `protoc`? `buf` makes breaking-change
   detection one line; `protoc` is leaner. I lean `buf` for the
   breaking-change check alone.
7. **Auto-bump cadence** — daily schedule plus manual dispatch is the
   default in §5; does that match your release rhythm? Some teams prefer
   weekly to reduce PR noise.
8. **Contract annotation convention** — `// contract: required` /
   `// contract: optional` in proto comments per §3, or do you want a
   stronger machine-checkable form (e.g. a custom proto option /
   `[(contract.required) = true]`)? Comments are simpler; custom options
   need a `buf` plugin or extra tooling but make the BFF unit-test
   generation mechanical rather than manual.

---

## §9 · References

- `KRAIL-BFF/docs/reference/API_SCHEMA_DESIGN.md` §4 — the original
  recommendation. Authoritative.
- `KRAIL-BFF/STATUS.md` §2 — the pre-existing TODO this request is
  asking you to action.
- `KRAIL-BFF/docs/reference/BFF_ADOPTION_GUIDE.md` §"Prerequisites
  checklist" — first item: "`krail-api-proto` repo exists and is pinned."
  KRAIL can't tick that box on a real migration until §4 of this doc is
  done.
- KRAIL-side plan that depends on this work:
  `KRAIL/docs/bff-integration-plan.md` Phase C.
