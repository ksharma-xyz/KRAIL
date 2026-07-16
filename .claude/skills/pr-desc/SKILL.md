---
name: pr-desc
description: Generate PR titles, descriptions, and commit messages for this repo. Use whenever raising a PR (gt submit), amending a PR body (gh pr edit), or writing a commit message that will be pushed. Enforces the public-repo content policy and the standard PR template.
---

# PR Description Generator

KRAIL is a public open-source repository. Every commit message and PR description is
permanently public, and GitHub keeps PR-description **edit history** — scrubbing after
the fact does not fully remove content. Get it right on first submit.

## Content policy (hard rules)

A PR description or commit message may only describe **what changed in the code** and
**how it was verified**. Never include:

- Business or strategic motives (store compliance, age-rating considerations,
  monetization, growth plans, marketing timing)
- Internal analytics or metrics (click counts, user counts, usage percentages,
  funnel numbers, crash rates)
- Anything from private repos, dashboards, or internal docs (KRAIL-Analytics,
  KRAIL-Marketing, Firebase console data)
- User data of any kind, even aggregated
- Names of unreleased features or unannounced plans beyond what the diff itself reveals

If context is needed to review the change, describe the *technical* rationale
("row is unused", "simplifies state handling"), not the business one.

## Style

- No em dashes in PR/commit prose
- No arrow characters
- Plain "what changed" statements; no hype, no filler
- Title: conventional commit format, e.g. `refactor(settings): remove social links row`

## Template

```markdown
## What

One or two sentences: what this PR changes, in code terms.

## Changes

- Bullet list of concrete changes (file/class level)
- Note anything intentionally left untouched and why (technical reason only)

## Testing

- Test tasks run and their result
- detekt status
- Compile targets verified

## Snapshots

| Screen | Before | After |
|---|---|---|
| ScreenName | <img src="" width="220"> | <img src="" width="220"> |

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## Snapshots section rules

- **Required whenever the PR touches UI** (Composables, theme, resources); delete the
  section for non-UI PRs.
- Build the snapshots (Roborazzi screenshot tests / preview captures) for the touched
  screens and embed them as a **table**, one row per screen, Before/After columns.
- Always use width-limited thumbnails: `<img src="..." width="220">`. Never paste
  full-size images; large images bloat the PR description and slow review.
- New screens with no "before" state: use a single After column or note "new screen".

## Checklist before submitting

1. Re-read the draft: would any line be fine on the repo's public front page? If not, cut it.
2. Motives mention a store, rating, metric, or internal repo? Cut or rephrase technically.
3. Commit message follows the same rules (it is just as public as the PR body).
