# Releasing

A release is a merge of `development` into `main` plus a tag. One button does all of it.

## The button

**Actions → cut-release → Run workflow**, or:

```bash
gh workflow run cut-release.yml -f next_version=1.2.0
```

| input | meaning |
|---|---|
| `version` | Optional. A confirmation — it must match `AndroidManifest.xml`, which decides. |
| `next_version` | Optional. Bumps `development` to the next cycle once the release is out. |
| `allow_failing_checks` | Release even though the last `development` build failed. |

It refuses to run if the manifest is inconsistent, the tag already exists, `development`
has nothing `main` lacks, or the last development build failed. Then it merges
`development` into `main` with a **merge commit**, tags `vX.Y.Z`, builds and publishes the
release, fast-forwards `development` back onto `main`, and optionally opens the next cycle.

> The version comes from `AndroidManifest.xml`. If the cycle ended on a different number
> than planned, run `tools/bump-version.sh X.Y.Z` on `development` and merge that first.

**Still verify on a camera.** A green build says it compiles. Install the published APK
over the previous version — it must succeed *without uninstalling*, which is what proves
the signing key is unchanged.

## Doing it by hand

If the workflow is broken, or you want to drive it yourself:

### Checklist

1. **Decide the version.** `AndroidManifest.xml` already holds it — the manifest carries the *next target
   release* for the whole cycle. If the cycle turned out bigger or smaller than planned, change it now:
   ```bash
   tools/bump-version.sh 1.2.0        # only if the target changed
   ```

2. **Cut the release branch:**
   ```bash
   git switch -c release/1.1.0 development
   git push -u origin release/1.1.0
   ```

3. **Open a PR `release/1.1.0` → `main`**, title `chore(release): 1.1.0`.
   Merge it with a **merge commit** — never squash. Squashing puts a commit on `main` that is not on
   `development`, and the two branches diverge for good.

4. **Tag it:**
   ```bash
   git switch main && git pull
   git tag -a v1.1.0 -m "Recipe Lab 1.1.0"
   git push origin v1.1.0
   ```
   The tag fires `release.yml`, which rebuilds from the tag, **fails if the tag does not match the
   manifest**, signs with the project key, publishes the release with the APK and its `.sha256`, and closes
   the matching milestone.

5. **Fast-forward `development` onto `main`:**
   ```bash
   git switch development && git merge --ff-only main && git push
   ```
   Not cosmetic. The tag sits on the merge commit, which is a *descendant* of `development`'s tip — without
   this, `git describe --tags` on `development` never sees `v1.1.0` and the `-dev.N` counter never resets.

6. **Open the next cycle:**
   ```bash
   tools/bump-version.sh 1.2.0
   git commit -am "chore(release): open 1.2.0 development cycle"
   git push
   ```

7. **Verify:** the release page shows the APK, `aapt dump badging` reports the expected
   `versionName`/`versionCode`, and installing over the previous version on a camera succeeds **without
   uninstalling** (which is what proves the signing key is unchanged).

## Hotfix

```bash
git switch -c hotfix/1.1.1 main
# fix, PR → main (squash), tag v1.1.1, then:
git switch development && git merge --no-ff main && git push
```

A hotfix back-merge is `--no-ff`, not `--ff-only`: `development` has moved on by then.

## If something goes wrong

- **Tag pushed with the wrong version.** `release.yml` fails the version check before publishing anything.
  Delete the tag (`git push --delete origin vX.Y.Z`), fix the manifest, re-tag. Note that the `release-tags`
  ruleset blocks tag deletion for non-admins.
- **A release was published with a bad APK.** Fix forward with a patch release. Do not move a tag — the
  ruleset blocks it, and anyone who already downloaded has the old bytes.
