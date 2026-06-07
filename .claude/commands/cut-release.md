# Cut Release

Trigger a KRAIL release branch cut via local CLI → GitHub Actions. No manual GH UI steps needed.

**Trigger phrases:** "cut a release", "start next version rollout", "ship 1.x", "release time", `/cut-release`

## Steps — run these without asking unless something is ambiguous

1. Read current version:
   ```bash
   grep -oP '(?<=versionName = ")[^"]+' androidApp/build.gradle.kts
   ```

2. Check the prod branch doesn't already exist:
   ```bash
   git ls-remote --heads origin "prod/{version}"
   ```
   If it exists, stop and tell the user.

3. Run the release script — this triggers the GH Actions workflow from local CLI:
   ```bash
   ./scripts/cut-release.sh
   ```
   The script reads the current version, prints what it will do, and asks for a single `y/N` confirmation before firing `gh workflow run`. Answer `y`.

   To specify the next version explicitly (e.g. skip to 1.25.0):
   ```bash
   ./scripts/cut-release.sh 1.25.0
   ```

4. After the script exits successfully, tell the user:
   - `prod/{version}` is being created — GH Actions fires automatically
   - "2. Deploy RC" auto-triggers on that push → builds signed AAB, tags `v{version}-RC1`, distributes to GP Internal + TestFlight + Firebase Friends, creates a draft GitHub Release
   - Watch: `https://github.com/ksharma-xyz/KRAIL/actions`

## What happens in CI (no action needed from user)

| Stage | What |
|---|---|
| `release-1-cut.yml` | Creates `prod/{version}` from main; opens bump-main PR |
| `release-2-deploy-rc.yml` | Fires on push to `prod/*`; quality gate → build AAB → RC tag → distribute |
| Draft release | Auto-created on GitHub, stays draft until user publishes |

## Keeping this skill in sync with CI

The source of truth is `.github/workflows/release-1-cut.yml`. If inputs, branch naming, or flow change there, update this skill and `scripts/cut-release.sh` in the same PR.
