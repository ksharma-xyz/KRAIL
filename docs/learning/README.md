# Learning log

A dated record of bugs that were expensive to find, and of how the finding went wrong.

This is not a changelog and not a postmortem template. A changelog records what changed; a
postmortem asks who was paged. This asks one question: **what would have made this cheaper,
and can it be turned into a rule, a script, or a check?**

An entry earns its place by being non-obvious. "Fixed a null check" does not go here. A bug
that survived green detekt, green tests and a code read does.

---

## When to add an entry

Write one when any of these is true:

- The first fix was wrong, and so was the second
- Detekt, unit tests and compile were all green while the bug was live
- The cause was in a layer nobody was looking at (manifest, Gradle, the window, the platform)
- Diagnosis needed instrumentation rather than reading code
- The same class of bug could plausibly hit another screen or module

## Format

One file per incident: `YYYY-MM-DD-short-slug.md`.

```markdown
# Title: what broke, in one line

**Date** · **Area** · **Cost** (rough: builds, install cycles, elapsed)

## Symptom
What was actually seen on the device. Screenshot description, not theory.

## Root cause
What was really wrong. If there were several, number them.

## Why it took so long
The honest part. Wrong theories in order, and what made each one look plausible.

## What would have caught it sooner
The technique, in a form reusable next time.

## Actions taken
Rules, scripts, tests or docs added. Link them. Unchecked boxes if not done yet.
```

## Rules for writing one

- **Name the wrong turns.** An entry that only records the right answer teaches nothing.
  The wrong theories are the content — they are what will look plausible again next time.
- **Prefer a check over a paragraph.** If it can be a script or a test, write that and link
  it. Prose is the fallback for what cannot be automated, not the default.
- **No blame, no metrics, no user data.** These files are in a public repo. Describe code
  and process only. See the content policy in `.claude/skills/pr-desc/SKILL.md`.
- **Link outward.** If a rule came out of the entry, the rule lives in the relevant doc
  (`docs/LAYOUT_AND_INSETS.md`, `docs/POLLING_LIFECYCLE.md`, …) and the entry points at it.
  The entry is the story; the doc is the instruction.

## Index

| Date | Entry | Class of bug |
|---|---|---|
| 2026-08-16 | [IME pan and the unbounded Column child](2026-08-16-ime-pan-and-unbounded-column.md) | Layout / window insets |
| 2026-08-16 | [Clipped inside its own parent](2026-08-16-clipped-inside-its-own-parent.md) | Layout / test that could not fail |
| 2026-08-21 | [Sheet closed by a text callback](2026-08-21-sheet-closed-by-a-text-callback.md) | State / unintended callback |
| 2026-08-22 | [Three contrast guards, and the theme colour that fell between them](2026-08-22-three-contrast-guards-and-the-gap-between-them.md) | Design system / a guard that measured the wrong pairing |
| 2026-08-22 | [A lane that never ran](2026-08-22-a-lane-that-never-ran.md) | CI / a non-zero exit that named nothing |
| 2026-08-22 | [Two platforms, two vocabularies](2026-08-22-two-platforms-two-vocabularies.md) | KMP / expect-actual behavioural drift |
| 2026-08-23 | [The screen that opened on the wrong surface](2026-08-23-the-screen-that-opened-on-the-wrong-surface.md) | UI / a default no test could see |
| 2026-08-23 | [The blank transcript that cleared the field](2026-08-23-the-blank-transcript-that-cleared-the-field.md) | KMP / a value one platform never produces |
| 2026-09-05 | [A bucket that was mostly typing](2026-09-05-a-bucket-that-was-mostly-typing.md) | Analytics / an event that counted keystrokes as intents |
