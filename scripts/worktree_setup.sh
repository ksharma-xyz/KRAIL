#!/usr/bin/env bash
#
# Makes a fresh git worktree buildable: copies the gitignored local files it needs from the primary
# checkout and initialises the proto submodule. Run it once, straight after creating a worktree.
#
#   ./scripts/worktree_setup.sh                    # set up the worktree you are standing in
#   ./scripts/worktree_setup.sh <path-to-worktree>
#   ./scripts/worktree_setup.sh --check [<path>]   # report what is missing, copy nothing
#   ./scripts/worktree_setup.sh --force [<path>]   # overwrite files the worktree already has
#   ./scripts/worktree_setup.sh --from <primary>   # copy from this checkout, not the detected one
#
# The list of files lives in scripts/worktree-local-files.txt. See docs/WORKTREES.md for the whole
# create, set up and clean up routine.
#
# Exit status: 0 when every required file is in place, 1 when a required file is missing, 2 on bad
# usage.

set -euo pipefail

mode="copy"
force=0
primary=""
target=""

while [ $# -gt 0 ]; do
  case "$1" in
    --check) mode="check" ;;
    --force) force=1 ;;
    --from)
      shift
      [ $# -gt 0 ] || { echo "worktree_setup: --from needs a path" >&2; exit 2; }
      primary="$1"
      ;;
    -h|--help) sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    -*) echo "worktree_setup: unknown option $1" >&2; exit 2 ;;
    *) target="$1" ;;
  esac
  shift
done

target="$(cd "${target:-.}" && git rev-parse --show-toplevel)"

# The primary checkout is the work tree that owns the common git dir.
if [ -z "$primary" ]; then
  common="$(git -C "$target" rev-parse --path-format=absolute --git-common-dir)"
  primary="$(dirname "$common")"
fi
primary="$(cd "$primary" && pwd -P)"
target="$(cd "$target" && pwd -P)"

if [ "$primary" -ef "$target" ]; then
  echo "worktree_setup: $target is the primary checkout, nothing to copy."
  exit 0
fi

list="$target/scripts/worktree-local-files.txt"
[ -f "$list" ] || { echo "worktree_setup: $list not found" >&2; exit 2; }

echo "worktree_setup: from $primary"
echo "worktree_setup:   to $target"

missing=0
copied=0
present=0
skipped=0

while IFS= read -r line || [ -n "$line" ]; do
  entry="${line%%#*}"
  entry="$(printf '%s' "$entry" | sed 's/[[:space:]]*$//; s/^[[:space:]]*//')"
  [ -n "$entry" ] || continue

  optional=0
  case "$entry" in \?*) optional=1; entry="${entry#\?}" ;; esac

  src="$primary/${entry%/}"
  dst="$target/${entry%/}"

  if [ ! -e "$src" ]; then
    if [ "$optional" = "1" ]; then
      echo "  skip     $entry (optional, not in the primary checkout)"
      skipped=$((skipped + 1))
    else
      echo "  MISSING  $entry (required, and not in the primary checkout)"
      missing=$((missing + 1))
    fi
    continue
  fi

  if [ -e "$dst" ] && [ "$force" = "0" ]; then
    echo "  ok       $entry (already there)"
    present=$((present + 1))
    continue
  fi

  if [ "$mode" = "check" ]; then
    if [ "$optional" = "1" ]; then
      echo "  absent   $entry (optional, the primary checkout has it)"
    else
      echo "  MISSING  $entry (the worktree does not have it; run without --check to copy)"
      missing=$((missing + 1))
    fi
    continue
  fi

  mkdir -p "$(dirname "$dst")"
  if [ -d "$src" ]; then
    mkdir -p "$dst"
    cp -R "$src/." "$dst/"
  else
    cp "$src" "$dst"
  fi
  echo "  copied   $entry"
  copied=$((copied + 1))
done < "$list"

# Wire codegen reads the .proto files from the submodule; an empty directory fails the build.
if [ -f "$target/.gitmodules" ]; then
  if [ "$mode" = "check" ]; then
    if [ -z "$(git -C "$target" submodule status 2>/dev/null | grep -v '^-' || true)" ]; then
      echo "  MISSING  submodules (run without --check to initialise them)"
      missing=$((missing + 1))
    fi
  else
    git -C "$target" submodule update --init --recursive >/dev/null 2>&1 \
      && echo "  ok       submodules initialised" \
      || { echo "  MISSING  submodules (git submodule update failed)"; missing=$((missing + 1)); }
  fi
fi

echo "worktree_setup: copied $copied, already there $present, skipped $skipped, missing $missing"
if [ "$missing" -gt 0 ]; then
  echo "worktree_setup: the worktree will not build until the MISSING entries are fixed." >&2
  exit 1
fi
echo "worktree_setup: ready. Clean up with ./scripts/worktree_cleanup.sh when the PR is merged."
