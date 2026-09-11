<!--
  The PR TITLE becomes the commit message on `development`, so it must be a valid
  Conventional Commit subject:  type(scope): subject
  types   feat fix docs refactor perf test build ci chore revert
  scopes  ui input browser recipes tools build ci docs deps release   (optional)
  Do not put the issue number in the title — put it in the Closes line below.
-->

Closes #

## What changed

<!-- One or two sentences. The why matters more than the what. -->

## Tested on the camera

<!-- A green build only proves it compiles. Delete this section only for docs/CI-only changes. -->

- Body / firmware:
- What I exercised:

## Checklist

- [ ] Built locally (`build.cmd` or `./build.sh`)
- [ ] Tested on an A6000 **and power-cycled** — the look survives a restart
- [ ] README recipe count updated, if recipes changed
- [ ] Version strings untouched (the release PR owns those)
- [ ] No APK or keystore added
