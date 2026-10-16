#!/usr/bin/env bash
# Rebase the non-GTNH restoration branch onto the latest GTNH master and build the mod.
# Run from the repository root:  bash scripts/rebase-gtnh.sh
set -euo pipefail

FEATURE="${FEATURE_BRANCH:-gtnh-compat}"
UPSTREAM="${UPSTREAM_REMOTE:-upstream}"
FORK="${FORK_REMOTE:-origin}"

git config rerere.enabled true
git config rerere.autoupdate true

echo "==> Fetching ${UPSTREAM} and ${FORK}"
git fetch "$UPSTREAM" --prune
git fetch "$FORK" --prune

echo "==> Rebasing ${FEATURE} onto ${UPSTREAM}/master"
git switch "$FEATURE"
git rebase "${UPSTREAM}/master"

echo "==> Building"
./gradlew build

echo
echo "==> Release jar(s) in build/libs:"
ls -1 build/libs/*.jar | grep -vE -- '(-dev|-sources|-api)\.jar$|dirty' || true
echo
echo "Copy the Railcraft-<version>.jar above into your modpack's mods/ folder."
