# Release Notes

Per-version Play Store + App Store copy for KRAIL releases. One markdown file
per shipped version (`v1.20.0.md`, `v1.21.0.md`, …) containing the final
text that gets pasted into the Google Play / App Store consoles.

These files are the source of truth for what users see in the stores. Keep
them around for history — release notes are a useful project changelog and
the AI workflow uses past examples to learn the voice for new ones.

## How to write notes for a new release

Run the project skill from a Claude Code chat:

```
/krail-release-notes
```

The skill (`.claude/commands/krail-release-notes.md`) will:

1. Ask which version you're writing for and the previous tag to compare against.
2. Run `git log <previous>..<target>` to gather the commit list.
3. Draft both Android (Play Store, ≤500 chars, casual Sydney voice) and iOS
   (App Store, formal, no emoji) variants in your terminal.
4. Iterate with you on tweaks.
5. Write the agreed copy to `docs/release-notes/v{version}.md`.

Drafting happens locally — there is no CI workflow for this. Keeping it
local means you get to push back on wording before anything is written to
disk, and we don't pay for API calls every time a release is cut.

## Voice & style guide

Don't reinvent the voice each release — there's a single source of truth at
`.claude/commands/krail-release-notes.md`. The skill pulls from it.
Highlights:

- **Features first, bug fixes second.** Lead with what users gain.
- **Android** is warmer; emoji as bullet points are OK if they add character (≤3 per release). Casual Sydney voice. ≤500 chars.
- **iOS** is more formal; numbered bullets, **no emoji whatsoever**.
- Both start with the bare version on its own line (`v1.20.0`) and end with **"Let's KRAIL."** (or `"Let's KRAIL Sydney."` for big launches).
- Never use the word "experience". Never say "improvements". Bug fixes get one line max.
- Em dashes are fine in release notes copy (the project-wide ban applies to in-app UI strings, not store copy).

## File format

Every committed file should look like this:

```markdown
# KRAIL v{version} — Release Notes

## Android — Google Play Store

v{version}

{android notes}

---

## iOS — App Store

v{version}

{ios notes}
```

This makes diffs across versions readable and lets the skill find prior
notes to learn from.

## Workflow position

```
release-1-cut.yml  →  prod/{v}      (cut release branch, bump main)
release-2-deploy-rc.yml  →  RC build to GP Internal + TestFlight
                         →  draft GitHub Release for v{v} (refreshed each RC)
   ↓
/krail-release-notes  (run locally in Claude Code)
   ↓
docs/release-notes/v{v}.md  (commit the draft, paste into stores)
   ↓
Publish the draft in the GitHub UI  →  creates v{v} tag + release goes public
   ↓
bump-after-release.yml  →  PR to bump main if it isn't already ahead
```
