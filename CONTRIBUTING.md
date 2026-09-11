# Contributing

How work moves through this repo. CI enforces most of it, so reading this saves you a red build.

- [Branches](#branches)
- [Commits](#commits)
- [Pull requests](#pull-requests)
- [Issues and milestones](#issues-and-milestones)
- [Testing on the camera](#testing-on-the-camera)
- [Releases](#releases)

## Branches

| Branch | Role |
|---|---|
| `main` | **Releases only.** Every commit on it is a release, tagged `vX.Y.Z`. Never commit here directly. |
| `development` | Default branch. Integration. Every push builds a `-dev.N` APK attached to the rolling [`dev` prerelease](https://github.com/voxivoid/recipe-lab-sony-a6000/releases/tag/dev). |
| work branches | One per issue, cut from `development`, merged back into it. |

```
main         ──●──────────────────●──────────────●──   releases only
                \                /              /
development   ───●──●──●──●──●──●──●──●──●──●──●───    integration
                    \      /   \     /
work branches    feat/12-…   fix/19-…
```

Naming — the prefix is the commit type, so the branch says what kind of change it carries:

```
feat/<issue>-<slug>        feat/123-brand-jump-top-dial
fix/<issue>-<slug>         fix/131-wb-finetune-sign
docs/ refactor/ chore/ build/ ci/ perf/ test/   same shape
hotfix/<x.y.z>             branched off main → PR to main → back-merged to development
```

Every branch except `hotfix/*` carries its issue number. There is no `release/*` branch:
**create-release** merges `development` into `main` itself.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/). Checked by the `commit-lint` job.

```
feat(browser): jump to a brand with the top dial

The picker had no fast path past 77 recipes.

Closes #123
```

**Types:** `feat` `fix` `docs` `refactor` `perf` `test` `build` `ci` `chore` `revert`.
A breaking change takes `!` after the scope and a `BREAKING CHANGE:` footer.

**Scopes** (optional — plenty of `docs:` commits carry none):

| scope | covers |
|---|---|
| `ui` | panel, chips, badges, toast, layout |
| `input` | key handling, wheel and dial, scan codes |
| `browser` | the brand/recipe picker |
| `recipes` | the recipe table itself |
| `tools` | the snapshot/diff tool, `tools/` scripts |
| `build` | `build.cmd`, `build.sh`, NDK, toolchain |
| `ci` | `.github/workflows` |
| `docs` | documentation |
| `deps` | the `jni/platform` submodule |
| `release` | `chore(release): x.y.z` only |

**Issue linkage.** Do not hand-write the issue number in the subject. Put `Closes #123` in the body — it
closes the issue on merge — and let the branch name carry it too. GitHub appends the **PR** number to the
subject automatically when the PR is squash-merged, so the commit on `development` ends up reading
`feat(browser): jump to a brand with the top dial (#45)`. Two bare `#N` in one subject would be ambiguous,
since issues and PRs share a number space.

## Pull requests

**The PR title becomes the commit message — and the release.** A squash merge leaves only the title, so it
is the string semantic-release reads to decide the next version. A PR titled `chore:` releases nothing
however large its diff; `fix:` makes a patch, `feat:` a minor, `!` a major. The `pr-title` check exists for
exactly this reason.

- work branch → `development`: **squash merge**. One commit per issue; your WIP never surfaces.
- `development` → `main`: **merge commit**, never squash. Squashing would put a commit on `main` that is not
  on `development` and the branches would diverge permanently.

Required checks: `build`, `version-consistency`, `commit-lint`.

## Issues and milestones

- Every unit of work gets an issue before a branch.
- **Milestones are versions** (`v1.1.0`, `v1.2.0`) plus a permanent `Backlog`. The release workflow closes a
  milestone when its tag ships.
- Labels: `type: …` mirrors the commit type, `scope: …` mirrors the commit scope, plus `priority: p1|p2|p3`
  and `status: blocked|needs-triage`.
- **`needs-on-camera-verification`** is the important one. Nothing about recipes, settings-store IDs or live
  preview can be validated by CI. A green build is not evidence the change works.

## Testing on the camera

A green build only proves it compiles. Before asking for a merge:

1. Grab the APK — your PR's workflow artifact, or `RecipeLab-dev.apk` from the `dev` prerelease.
2. Install it: `pmca-console.py install -d native -f RecipeLab.apk` (or PMCA-GUI → Install App).
3. Exercise the change, then **power-cycle the camera** and confirm the setting survived. The store is
   written through the backup driver; a look that vanishes after a power cycle was never really stored.
4. Say in the PR what you tested and on which body/firmware.

Never commit an APK or a keystore. Both are gitignored; releases carry the binaries.

## Releases

**Actions → create-release → Run workflow** (`-f dry_run=true` to just see what would ship). semantic-release
reads the commits, decides the version, builds, tags and publishes; the workflow merges `development` into
`main` around it and fast-forwards back. Nobody picks a version number.
Details: **[docs/RELEASING.md](docs/RELEASING.md)**.

Handy aliases:

```bash
git config alias.releases 'log --first-parent --oneline main'
git config alias.since-release '!git log $(git describe --tags --abbrev=0)..HEAD --no-merges --oneline'
```
