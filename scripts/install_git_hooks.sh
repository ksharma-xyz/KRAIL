#!/usr/bin/env bash
#
# Installs the repo's versioned git hooks into this clone.
#
# .git/hooks is not tracked, so a hook that lives only there exists on one machine and nowhere
# else. The hooks themselves are versioned under scripts/git-hooks/; this copies them into place.
# Idempotent: it rewrites a hook only when the installed copy differs, and says so.
#
# Installs into the COMMON git dir, so one run covers every linked worktree of this clone.
# Deliberately does not set core.hooksPath: a global hooks path may already be set on this
# machine, and a per-repo one would replace it rather than add to it.
#
# Run by ./scripts/fullQualityChecks.sh, so a clone that runs the pre-PR gate once is covered.

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
hooks_dir="$(cd "$repo_root" && cd "$(git rev-parse --git-common-dir)" && pwd)/hooks"
mkdir -p "$hooks_dir"

for src in "$repo_root"/scripts/git-hooks/*; do
  [ -f "$src" ] || continue
  name="$(basename "$src")"
  dest="$hooks_dir/$name"

  if [ -f "$dest" ] && cmp -s "$src" "$dest"; then
    continue
  fi

  # Keep whatever was there before. A hand-edited or tool-installed hook is someone's work.
  if [ -f "$dest" ]; then
    cp "$dest" "$dest.bak"
    echo "install_git_hooks: replaced $name (previous copy kept as $name.bak)"
  else
    echo "install_git_hooks: installed $name"
  fi

  cp "$src" "$dest"
  chmod +x "$dest"
done
