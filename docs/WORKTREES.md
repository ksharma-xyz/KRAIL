# Worktrees

A worktree is a second working copy of the repo on its own branch, so one change can be built,
tested and installed without touching the checkout you are in. They live in
`~/code/.worktrees/KRAIL/<name>`, never inside the repo.

A fresh worktree has none of the gitignored local files, and cannot build until they are copied in.
This page is the whole routine: create, set up, work, clean up.

## 1. Create

```sh
~/code/batcave/bin/bc wt new KRAIL <branch> --kind pr --purpose "<what this is for>"
```

That makes the worktree from `origin/main`, registers it with a 30 day expiry, and runs step 2 for
you. Without `bc`:

```sh
git worktree add -b <branch> ~/code/.worktrees/KRAIL/<name> origin/main
```

## 2. Set up

```sh
cd ~/code/.worktrees/KRAIL/<name>
./scripts/worktree_setup.sh
```

It copies the files listed in `scripts/worktree-local-files.txt` from the primary checkout and
initialises submodules if the repo has any. It is safe to run twice: it never overwrites a file the
worktree already has unless you pass `--force`. `--check` reports what is missing and copies nothing.

| File | Why a build needs it |
|---|---|
| `local.properties` | NSW API key and SDK path. Without it Gradle stops with "Register API key and put in local.properties". |
| `androidApp/src/*/google-services.json`, `composeApp/src/{debug,release}/google-services.json` | Firebase config. Without it `processDebugGoogleServices` fails. |
| `composeApp/src/androidMain/res/`, `core/festival/src/androidDebug/` | Android resources and debug sources kept out of git. |
| `iosApp/iosApp/GoogleService-Info.plist` | iOS Firebase config. Optional, only to build the iOS app. |
| `io/bff-api/build/generated/` | Wire output. Optional, saves a full codegen run. |

The script exits with 1 and names the file when the primary checkout lacks a required one. Fix the
primary checkout first; a worktree can only copy what exists.

**Adding a new gitignored file that the build needs?** Add it to `scripts/worktree-local-files.txt`
in the same change, or the next worktree will fail on it.

`git push` from a worktree with no `local.properties` stops early and points here, instead of
failing inside Gradle.

## 3. Work

Build, test and install from the worktree as usual. The QA checklist in `CLAUDE.md` applies. Run
`./scripts/fullQualityChecks.sh` before opening the PR.

## 4. Clean up

Once the PR is merged:

```sh
./scripts/worktree_cleanup.sh ~/code/.worktrees/KRAIL/<name>
```

It removes the worktree, deletes the local branch and the remote branch, and prunes. It refuses, and
says why, when the path is the primary checkout, when there is uncommitted work, or when the branch
is not merged. A squash merge is detected from the merged PR, not from git history. `--dry-run`
shows what it would do. `--force` removes an unmerged or dirty worktree and discards that work.

With `bc`, `bc wt rm <path>` does the same and also removes the registry entry. Do not leave a
worktree behind: a forgotten one keeps a branch, a build directory and a copy of the local config on
disk.

Do not delete a branch until `gh pr view <number> --json state` says `MERGED`.

## What stays out of a worktree

Never copy secrets into git, and never commit the copied files: all of them are gitignored, so
`git status` stays clean. Do not copy `build/` output except `io/bff-api/build/generated/`; a
worktree builds its own.
