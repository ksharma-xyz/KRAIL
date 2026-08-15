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

## Titles

Shape: `type(scope): <verb> <the concrete thing> <where>`

The title is read in a list, next to five others, in about two seconds. Everything below
follows from that.

- **Start with a verb for the code change**: add, hide, carry, read, record, stop, remove,
  fix. Not a noun phrase, not a principle, not an aphorism.
- **Name something greppable**: a parameter, a screen, a flag, a feature. If a reviewer
  would search for the word, it belongs in the title (`resultIndex`, `named days`,
  `AI search entry point`).
- **Answer "what is different once this merges?"** The reasoning is the body's job. A title
  that states why the change is right tells you nothing about what it does.
- **Under ~70 characters**, or GitHub truncates it in the list.
- **Scan test before submitting**: read the title beside the other open PR titles. If two
  share a sentence shape, rewrite one. Parallel phrasing reads well in prose and terribly
  in a list, because the shape is the first thing seen and matching shapes look like
  duplicates.

Never: metaphors, negations, constructions repeated across a stack, or a shape reused from
an earlier title.

| Bad | Why | Better |
|---|---|---|
| `fix(trip-planner): a sentence about nothing fills nothing` | States a principle; could be any layer | `fix(trip-planner): stop filling From when the rider named no place` |
| `fix(trip-planner): with the feature off, there is no way in` | No verb, no subject, unsearchable | `fix(trip-planner): hide the AI search entry point when the flag is off` |
| `feat(trip-planner): a day the rider names is a day the search uses` | Same shape as the PR below it in the stack | `feat(trip-planner): understand named days like tomorrow and friday` |

The commit subject and the PR title should match. Rebase-merge puts the commit subject in
`main`'s history, so fixing only the PR title is cosmetic.

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
