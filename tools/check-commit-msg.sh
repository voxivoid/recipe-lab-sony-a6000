#!/usr/bin/env bash
# Conventional Commits gate. Zero dependencies — there is no Node in this repo.
#
#   tools/check-commit-msg.sh "feat(ui): add a thing"      check one subject
#   tools/check-commit-msg.sh --range BASE..HEAD           check every subject in a range
#
# Merge and Revert subjects, and GitHub's "(#123)" squash suffix, are accepted.
set -uo pipefail

TYPES='feat|fix|docs|refactor|perf|test|build|ci|chore|revert'
SCOPES='ui|input|browser|tools|recipes|build|ci|docs|deps|release'
RE="^($TYPES)(\(($SCOPES)\))?!?: .{1,72}$"

fail=0

check() {
  local subject="$1" source="$2"
  case "$subject" in
    Merge\ *|Revert\ *) return 0 ;;
  esac
  # strip GitHub's squash-merge PR suffix before matching
  local bare
  bare="$(printf '%s' "$subject" | sed -E 's/ \(#[0-9]+\)$//')"
  if ! printf '%s' "$bare" | grep -qE "$RE"; then
    echo "✗ $source: $subject" >&2
    fail=1
  else
    echo "✓ $source: $subject"
  fi
}

if [ "${1:-}" = "--range" ]; then
  range="${2:?usage: $0 --range BASE..HEAD}"
  while IFS= read -r s; do [ -n "$s" ] && check "$s" "commit"; done < <(git log --format='%s' "$range")
else
  check "${1:?usage: $0 \"<subject>\" | --range BASE..HEAD}" "subject"
fi

if [ "$fail" -ne 0 ]; then
  cat >&2 <<MSG

Expected:  type(scope): subject
  type   $TYPES
  scope  $SCOPES  (optional)
  subject  <= 72 chars, imperative, no trailing period

Link the issue with a "Closes #123" footer, not in the subject. See docs/CONTRIBUTING.md.
MSG
fi
exit "$fail"
