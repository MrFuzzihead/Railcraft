# Rebase workflow — keeping the non-GTNH fixes on top of GTNH

This repository has two kinds of branches:

| Branch | Meaning |
|---|---|
| `master` | **Mirror of GTNH** (`upstream/master`, i.e. `GTNewHorizons/Railcraft`). Never commit your own work here. |
| `gtnh-compat` | The non-GTNH restoration work (6 commits) **on top of** `master`. This is what gets built and dropped into the modpack. |

Remotes:
- `origin` → `https://github.com/MrFuzzihead/Railcraft.git` (your fork)
- `upstream` → `https://github.com/GTNewHorizons/Railcraft.git` (GTNH)

---

## One-time setup

```bash
git config rerere.enabled true
git config rerere.autoupdate true      # auto-apply recorded conflict resolutions
```

`rerere` remembers how you resolved a conflict and replays it on the next rebase, which makes
repeated GTNH updates much less painful. The helper scripts set this for you too.

---

## Whenever GTNH pushes a new release

### Option A — script (recommended)

Windows:
```bat
scripts\rebase-gtnh.bat
```
Git Bash / Linux / macOS:
```bash
bash scripts/rebase-gtnh.sh
```

The script fetches `upstream`, rebases `gtnh-compat` onto `upstream/master`, runs
`./gradlew build`, and prints the release jar path.

### Option B — manual

```bash
git fetch upstream --prune
git switch gtnh-compat
git rebase upstream/master        # or: git rebase <release-tag>
./gradlew build
```

---

## Picking the jar for the modpack

`./gradlew build` writes to `build/libs/`. There are several jars:

| File | Use |
|---|---|
| `Railcraft-<version>.jar` | **This is the one.** Drop into `mods/`. |
| `Railcraft-<version>-dev.jar` | Dev/deobfuscated — do not ship. |
| `Railcraft-<version>-sources.jar` | Sources — do not ship. |
| `Railcraft-<version>-api.jar` | API — do not ship. |
| `Railcraft-<version>-dirty*.jar` | Built with uncommitted changes — ignore; rebuild on a clean tree. |

The version string is `<GTNH-version>-master.<n>+<sha>`; the `<n>`/`<sha>` are just this fork's commits.

> Tip: build on a clean working tree (`git status`) so you get a non-`dirty` jar.

---

## Optional — keep the fork's `master` current

`master` is a pure mirror. To update it on your fork after a GTNH release:

```bash
git fetch upstream --prune
git switch master
git merge --ff-only upstream/master
git push origin master
```

Then rebase the feature branch (Option A/B above).

And to publish the rebased feature branch:

```bash
git push --force-with-lease origin gtnh-compat
```

(`--force-with-lease` is required because rebasing rewrites the feature commits.)

---

## If a rebase hits a conflict

1. Resolve the conflicting files.
2. `git add <files>`
3. `git rebase --continue`
4. To bail out: `git rebase --abort` (returns to the pre-rebase state; the pushed
   `origin/gtnh-compat` is always a safe fallback).

Likely conflict spots (all are additive/gated, so usually trivial):
- `ModuleFactory` — the large commented-out Blast Furnace block.
- `EnumMachineAlpha` / `EnumMachineBeta` — `getToolTip` / `getLocalizationTag`.
- `TileBlastFurnace` — auto fuel input.
- `SteamBoiler.getHeatLevel`.
- `src/main/resources/assets/railcraft/lang/*.lang` — the `.classic.*` keys sit next to
  GTNH's own keys; keep both sides.
