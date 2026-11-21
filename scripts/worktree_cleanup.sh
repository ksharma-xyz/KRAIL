#!/usr/bin/env bash
#
# Removes a finished worktree, its local branch and its remote branch, once the PR is merged.
#
#   ./scripts/worktree_cleanup.sh <path-to-worktree>
#   ./scripts/worktree_cleanup.sh --force <path>   # also remove one that is not merged or not clean
#   ./scripts/worktree_cleanup.sh --dry-run <path> # say what would happen, change nothing
#
# It refuses, and says why, when:
#   - the path is the primary checkout,
#   - the worktree has uncommitted changes or untracked files (ignored files do not count, so the
#     copied local config and build output never block it),
#   - the branch is not merged. Squash merges are detected from the merged PR, not from git ancestry.
#
# Run ./scripts/worktree_setup.sh when you create a worktree; see docs/WORKTREES.md.
#
# Exit status: 0 removed (or dry run), 1 refused, 2 bad usage.

set -euo pipefail

force=0
dry=0
target=""

while [ $# -gt 0 ]; do
  case "$1" in
    --force) force=1 ;;
    --dry-run) dry=1 ;;
    -h|--help) sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    -*) echo "worktree_cleanup: unknown option $1" >&2; exit 2 ;;
    *) target="$1" ;;
  esac
  shift
done

[ -n "$target" ] || { echo "usage: worktree_cleanup.sh [--force] [--dry-run] <path-to-worktree>" >&2; exit 2; }
[ -d "$target" ] || { echo "worktree_cleanup: $target is not a directory" >&2; exit 2; }

wt="$(cd "$target" && pwd -P)"
common="$(git -C "$wt" rev-parse --path-format=absolute --git-common-dir)"
primary="$(cd "$(dirname "$common")" && pwd -P)"

# -ef compares the files themselves, so a path typed in another case on a case-insensitive disk still matches.
if [ "$wt" -ef "$primary" ]; then
  echo "worktree_cleanup: refusing, $wt is the primary checkout." >&2
  exit 1
fi

branch="$(git -C "$wt" branch --show-current)"
[ -n "$branch" ] || { echo "worktree_cleanup: $wt has a detached HEAD, remove it by hand." >&2; exit 1; }

run() {
  if [ "$dry" = "1" ]; then echo "  would run: $*"; else "$@"; fi
}

# 1. Nothing uncommitted. Ignored files are not listed by --porcelain, so they never block this.
dirty="$(git -C "$wt" status --porcelain)"
if [ -n "$dirty" ] && [ "$force" = "0" ]; then
  echo "worktree_cleanup: refusing, $wt has uncommitted work:" >&2
  printf '%s\n' "$dirty" | head -10 >&2
  echo "Commit or push it, or pass --force to throw it away." >&2
  exit 1
fi

# 2. Merged. A squash merge leaves the branch out of main's history, so ask GitHub as well.
git -C "$primary" fetch --quiet origin 2>/dev/null || true
merged=0
if git -C "$primary" merge-base --is-ancestor "$branch" origin/main 2>/dev/null; then
  merged=1
elif command -v gh >/dev/null 2>&1; then
  count="$(cd "$primary" && gh pr list --head "$branch" --state merged --json number --jq 'length' 2>/dev/null || echo 0)"
  [ "${count:-0}" -gt 0 ] && merged=1
fi
if [ "$merged" = "0" ] && [ "$force" = "0" ]; then
  echo "worktree_cleanup: refusing, branch $branch is not merged (no merged PR, not in origin/main)." >&2
  echo "Merge the PR first, or pass --force to remove it anyway." >&2
  exit 1
fi

echo "worktree_cleanup: removing $wt and branch $branch (merged: $([ "$merged" = 1 ] && echo yes || echo no))"
run git -C "$primary" worktree remove ${force:+--force} "$wt"
run git -C "$primary" branch -D "$branch"
if git -C "$primary" ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
  run git -C "$primary" push --quiet origin --delete "$branch"
fi
run git -C "$primary" worktree prune
echo "worktree_cleanup: done. If you track worktrees in your own list, remove this one from it."
