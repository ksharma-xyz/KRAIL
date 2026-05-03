# Release Notes

Per-version Play Store + App Store copy for KRAIL releases. One markdown file
per shipped version (`v1.20.0.md`, `v1.21.0.md`, …) containing the final
text that gets pasted into the Google Play / App Store consoles.

These files are the source of truth for what users see in the stores. Keep
them around for history — release notes are a useful project changelog and
the AI workflow uses past examples to learn the voice for new ones.

## How to write notes for a new release

You have two ways to draft them. Both produce the same output (a markdown
file in this folder), and you should review the result either way.

### Option 1 — Claude Code skill (interactive, local)

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

Use this when you want a conversational drafting flow with the ability to
push back on the wording before anything gets written to disk.

### Option 2 — GitHub Actions workflow (one-shot, headless)

Run `.github/workflows/generate-release-notes.yml` from the Actions tab:

- **From tag** — previous release tag (e.g. `v1.19.0`)
- **To tag** — new release tag (e.g. `v1.20.0`)

The workflow calls the Anthropic API with `claude-haiku-4-5-20251001`
(~$0.003 per run) and writes the result to:

- The job summary of the run (copy from there into the stores)
- The body of the matching GitHub Release draft

Use this when you've already cut + tagged the release and just want the
copy generated from CI without spinning up a local chat. Note: this
workflow does **not** commit the result to `docs/release-notes/` — that's
a manual step. Paste the generated text into a new `v{version}.md` and
open a PR (or run the skill afterward to save it).

**Prerequisite:** `ANTHROPIC_API_KEY` must be set in repo secrets.

## Voice & style guide

Don't reinvent the voice each release — there's a single source of truth at
`.claude/commands/krail-release-notes.md`. Both options above pull from it.
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
release-3-tag.yml  →  v{v} tag       (publish for production rollout)
   ↓
generate-release-notes.yml  (run this OR /krail-release-notes locally)
   ↓
docs/release-notes/v{v}.md  (commit the draft, paste into stores)
   ↓
create-github-release.yml  →  GitHub Release published
   ↓
bump-after-release.yml  →  PR to bump main if it isn't already ahead
```
