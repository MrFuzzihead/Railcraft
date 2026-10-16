# Testing checklist — non-GTNH restoration branch

Branch: `gtnh-compat` @ `3b273a26` (6 commits on top of GTNH `fd0d8423`)
Goal: non-GTNH packs get upstream Railcraft behaviour; GTNH (`dreamcraft`) behaviour is **unchanged**.

Everything here is manual in-game testing. `./gradlew build` (compile + checkstyle + spotless) is
already green in CI/locally.

---

## 0. Environment

- **Legacy instance** — Railcraft jar, **no `dreamcraft`** (ideally no GregTech). `Game.isGTNH == false`.
- **GTNH instance** — Railcraft jar **with `dreamcraft`** present. `Game.isGTNH == true`.
  - `Game.isGTNH = Loader.isModLoaded("dreamcraft")`, so `dreamcraft` alone flips the switch.
- Recommended helper mods:
  - **NEI** — to inspect crafting recipes (required for several checks).
  - **BuildCraft** — only for the rock-crusher gate test.
  - **IC2 / Forestry / Thaumcraft** — only for the cart-filter container variants and BF fuels.
- Use a **fresh world** in creative for most checks; a survival world for recipe/balance checks.
- After each run, scan `logs/latest.log` for `ERROR`, `Exception`, and "Missing translation" lines.

Legend: **[NEI]** needs NotEnoughItems, **[BC]** needs BuildCraft, **[IC2]**/**[Forestry]** as noted.

---

## 1. Automated

- [ ] `./gradlew build` succeeds (done — re-run to confirm on your JDK).
- [ ] No new ERROR/exception in the log on startup (both instances).

---

## 2. Non-GTNH instance (legacy behaviour)

### 2.1 Blast Furnace — recipes & identity
- [ ] Item is named **"Blast Furnace Brick"** (not "Advanced Coke Oven Brick").
- [ ] Craft it with **nether brick + soul sand + magma cream** (`MBM / BPB / MBM`). [NEI]
- [ ] Build the 3x4x3 hollow multiblock; GUI title reads **"Blast Furnace"**.
- [ ] Iron ingot → **1 steel ingot** (~1280 ticks / 64 s).
- [ ] Iron helmet/chest/legs/boots → 5/8/7/4 steel.
- [ ] Iron horse armour → 4 steel; sword/shovel/pickaxe/axe/hoe/shears → 2/1/3/3/2/2 steel.
- [ ] Crowbar → 3 steel; iron door → 6 steel; **iron block → steel block**.
- [ ] Recycling: steel helm/armour/legs/boots/sword/pickaxe/hoe/axe/shears → steel (recycleTime = 640 × multiplier).
- [ ] Rolling-machine **iron plate → steel ingot**.
- [ ] Low-pressure **Steam Engine → 12 steel**; low-pressure **Boiler Tank → 2 steel**.
- [ ] Iron Tank → **4 steel nuggets** (see 2.2).
- [ ] Logs are **not** accepted by the BF (only the Coke Oven turns logs into charcoal).

### 2.2 Iron/Steel multiblock tanks
- [ ] Iron Tank Wall/Gauge/Valve craftable from **iron plates** (+ glass/lever/iron bars). [NEI]
- [ ] Steel Tank Wall/Gauge/Valve craftable from **steel plates**. [NEI]
- [ ] Colour recipe: 1 tank + dye → 8 coloured tanks; colour is visible and persists.
- [ ] Build an Iron Tank multiblock and a Steel Tank multiblock; fill/drain fluid; no crash.
- [ ] Iron tank recycles to **4 steel nuggets** in the Blast Furnace.
- [ ] Tank Cart can be crafted with an iron tank (module fallback); HP Boiler can use a steel tank.

### 2.3 Cart filtering
- [ ] **Tank Cart + filled bucket** (water/lava) crafts a filtered Tank Cart, returning the empty bucket. [NEI]
- [ ] Repeat with a Railcraft creosote bottle/can/cell (if enabled). [NEI]
- [ ] Repeat with a Forestry can and an IC2 cell (if installed). [NEI]
- [ ] Cargo Cart + an arbitrary item still filters.
- [ ] The filtered cart's filter NBT/behaviour is correct in-world.

### 2.4 Rock Crusher
- [ ] Build the 3x2x2 (or 3x3x3) Rock Crusher; GUI opens.
- [ ] With RF power (or `requirePower=false`) it crushes cobblestone/gravel/ores and outputs by chance.
- [ ] **BuildCraft gate pause**: put a BC gate on a structure pipe next to the crusher, select the
      `Pause` action, trigger it → the crusher stops; clear it → it resumes. [BC]

### 2.5 Steam Boiler balance & tooltips
- [ ] **High Pressure Boiler** at full heat produces **20 mB/t (400 mB/s) per tank** — measure with a
      steam engine or by timing a tank drain. It must **not** be 40 mB/t. (Easiest: attach a Commercial
      Steam Engine and confirm the fuel/steam spend matches upstream, or compare against GTNH instance.)
- [ ] **Low Pressure Boiler** still produces 10 mB/t (200 mB/s) per tank.
- [ ] HP boiler tank item tooltip reads **"Produces 20 mB of Steam per tick"** (not "800L per second").
- [ ] Steam engine tooltips read **"Uses 40/10/20 mB of Steam per tick"** (not per-second L).
- [ ] Creosote Oil as liquid boiler fuel uses the upstream value (a bucket lasts noticeably longer than
      in GTNH mode — comparative check).

### 2.6 Rails
- [ ] **8 vanilla rails (shapeless) → 1 standard rail** exists and works. [NEI]
- [ ] The existing standard-rails → vanilla-rails recipes are unchanged. [NEI]

### 2.7 Coke Oven tooltip
- [ ] Hovering the Coke Oven Brick shows **no "DEPRECATED"** line.

### 2.8 Advanced (GT-material) tanks
- [ ] Aluminium/Stainless/Titanium/Tungstensteel/Palladium/Iridium/Osmium/Neutronium tank items are
      **not** present in the creative tab or NEI. [NEI]
- [ ] No startup crash / "missing block" errors from the disabled module.

### 2.9 Silk touch
- [ ] Break a **coloured tank with Silk Touch** → item keeps its colour.
- [ ] Break a coloured tank **without** Silk Touch → plain (white) tank (upstream behaviour).
- [ ] **Anchors**: Silk-Touch and normal-break a fuelled World Anchor → the dropped item retains its
      stored fuel/time (this was the GTNH fix — confirm it still holds).
- [ ] No **double drops** when breaking any machine (compare item count to before).

### 2.10 Blast Furnace auto fuel input
- [ ] Place a chest with coal coke adjacent to the BF; the BF pulls **a full stack within a few ticks**
      (not 1 item/tick).
- [ ] Repeat with a **small inventory** (hopper / single-slot) → still pulls (the 27-slot restriction is gone).
- [ ] It does **not** pull non-fuel items out of the adjacent inventory.
- [ ] It does **not** pull fuel from an adjacent Blast Furnace.
- [ ] It stops pulling once the fuel slot is full (64).

---

## 3. GTNH instance (regression — must be unchanged)

### 3.1 Blast Furnace = "Advanced Coke Oven"
- [ ] Named **"Advanced Coke Oven Brick"**; GUI title **"Advanced Coke Oven"**.
- [ ] Crafted with **Infernal Brick + magma cream** (` B / BPB / B `).
- [ ] coal → coal coke (20 t); coal block → coke block (180 t); logs → charcoal.
- [ ] **No** iron→steel recipes exist (BF cannot make steel).
- [ ] Coal **and** coal block are accepted as BF fuel; charcoal is **not** turned into coal coke.
- [ ] **No** auto fuel input (chest adjacent to BF does nothing).

### 3.2 Other regressions
- [ ] Iron/Steel tanks are **not** craftable; advanced GT tanks still register/behave as before.
- [ ] Tank Cart + bucket filter is **rejected** (dupe guard) — expected.
- [ ] Rock Crusher is disabled (no GUI, does nothing) — expected.
- [ ] HP boiler produces 40 mB/t; tooltips say "800L per second"; creosote is 6400.
- [ ] **8 rails → standard rail** recipe is absent.
- [ ] Coke Oven tooltip **does** show the DEPRECATED line.
- [ ] Silk touch uses the GTNH drop path; anchors retain fuel; no double drops.

---

## 4. Cross-cutting

- [ ] **Dedicated server**: start with the mod, join, place/break machines, no server-side exceptions.
- [ ] **Existing save** (made before this branch): loads with no crash; machine blocks and inventories intact.
- [ ] Switch a world between instances (with/without `dreamcraft`): no crash (block/TE IDs unchanged).
- [ ] Non-English locale (e.g. `ru_RU`, `de_DE`, `zh_CN`): BF name, Coke Oven tooltip and boiler/engine
      tooltips show the **classic** text, not the raw key or the GTNH text.
- [ ] Creative tab contents differ correctly between the two instances (no advanced tanks outside GTNH).
- [ ] NEI/CraftGuide: recipes for BF, tanks and rails display correctly. [NEI]

---

## 5. Sign-off

- [ ] Non-GTNH instance: all of §2 passes.
- [ ] GTNH instance: all of §3 passes.
- [ ] Cross-cutting §4 passes.
- [ ] Reviewer/owner sign-off: ______________________  Date: __________

After sign-off: commit this checklist and open the PR.
