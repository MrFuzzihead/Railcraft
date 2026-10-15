# GTNH-fork Railcraft — non-GTNH regression audit & restoration plan

Audited against:
- **Fork HEAD:** `fd0d8423` (`MrFuzzihead/Railcraft` master)
- **Last upstream CovertJaguar commit before the GTNH fork:** `d342c512` (ancestor of the GTNH tree)

Everything below was verified by comparing `d342c512` with `HEAD` (whitespace/format-insensitive
token diff + recipe-call diff), not by guesswork. 209 commits exist between the fork point and
HEAD; most are format/build/GT-only. This document lists only the changes that **remove or alter
behavior for players who do not run the GTNH pack** (`dreamcraft` absent).

---

## 0. TL;DR

The GTNH team introduced two classes of change:

1. **Correctly gated** stuff (`if (Game.isGTNH)` / `Loader.isModLoaded("dreamcraft")` /
   `@Optional` / `Module.GREGTECH`). These are safe and can be kept as-is.
2. **Ungated, global changes** that silently removed Railcraft functionality for everyone.
   The Blast Furnace is simply the most visible one. The complete list is §2.

`Game.isGTNH` is set in `Railcraft.preInit` (`Railcraft.java:211`):
```java
Game.isGTNH = Loader.isModLoaded("dreamcraft");
```
So it is already the perfect, cheap switch for "GTNH pack vs. everything else". The restoration
work should be: **keep the GTNH code path byte-for-byte, add the upstream code path in an
`else`/`!Game.isGTNH` branch, and isolate the restored code in helper classes** so upstream
merges stay trivial.

---

## 1. What is *properly* gated (leave alone)

These use `Game.isGTNH`/`dreamcraft`/modules/`@Optional` and behave correctly for non-GTNH packs:

| Area | File(s) | Gate |
|---|---|---|
| Rock crusher recipe registration | `ModuleFactory.java` (l.165+), `EnumBrick.java`, `BlockFactoryStairs.java`, `ModuleMagic.java` | `!Game.isGTNH` |
| Rock crusher logic, GUI, RF, BuildCraft triggers | `TileRockCrusher.java` | `Game.isGTNH` (see bug in §2.13) |
| Rock crusher CraftGuide integration | `RockCrusherPlugin.java` | `Game.isGTNH` |
| Inter-mod `rock-crusher` messages | `Railcraft.java:173` | `!Game.isGTNH` |
| IC2 macerator recipe add/remove | `ModuleIC2.java:144`, `IC2Plugin.java` | `dreamcraft` check |
| GT electric feeder / energy loader GT energy net | `TileElectricFeeder.java`, `TileEnergyLoader.java` | `@Optional` + `gregtech` |
| GT hazmat vs. Railcraft overalls | `BlockTrack.java:242`, `TileSteamTrap.java:86`, `ItemOveralls.java` | `Module.GREGTECH` + `@Optional` |
| Packed-ice stairs recipe | `BlockFactoryStairs.java:62` | `!Game.isGTNH` |
| Firestone ignite config, anvil GUI style | `FirestoneTickHandler`, `GuiAnvil` | config options (default = upstream) |

No action needed for these.

---

## 2. Ungated regressions (affect every non-GTNH pack)

### 2.1 CRITICAL — Blast Furnace steel recipes deleted

- **Commit:** `ab5e8df6` *"Bf recipe remove (#14)"*
- **File:** `ModuleFactory.java` l.104–149 (giant `/* ... */` block), `ItemPlate.java:108`,
  `ModuleSteam.java:75` & `:159`, `ModuleTransport.java:247–259`
- **What was removed (complete list, verified by call-diff):**

  | Input | Output | Cook time |
  |---|---|---|
  | `iron_ingot` | 1× Steel ingot | 1280 |
  | iron helmet / chest / legs / boots | 5 / 8 / 7 / 4× Steel | ×5/×8/×7/×4 |
  | iron horse armor | 4× Steel | ×4 |
  | iron sword / shovel / pickaxe / axe / hoe / shears | 2 / 1 / 3 / 3 / 2 / 2× Steel | ×2/×1/×3/×3/×2/×2 |
  | crowbar | 3× Steel | ×3 |
  | iron door | 6× Steel | ×6 |
  | all Steel tools/armor (recycling) | 1–6× Steel | recycleTime (burnTime/2) |
  | `iron_block` | `EnumCube.STEEL_BLOCK` | 11520 |
  | Iron Plate (`ItemPlate`) | Steel ingot | 1280 |
  | Low-pressure Steam Engine (`ModuleSteam`) | 12× Steel | 15360 |
  | Low-pressure Boiler Tank (`ModuleSteam`) | 2× Steel | 2560 |
  | Iron/Steel tank item (`ModuleTransport`) | 4× Steel nugget | 640 |

- **Effect:** the Blast Furnace exists and is craftable but produces **no steel at all**.
  Steel is effectively unobtainable in a vanilla-ish pack, which breaks steel tools/armor/plates,
  steel blocks, the Steel Anvil, minecarts, the Rolling Machine's steel plates, Steam Oven,
  Turbine, high-pressure boilers, etc. The fallback path
  `registerAltSteelFurnaceRecipe()` (`ModuleFactory.java:515`) only runs when the Blast Furnace is
  *unavailable*, so it never fires here.

- **Restore:** yes, entire block, for `!Game.isGTNH`.

### 2.2 CRITICAL — "Blast Furnace" renamed to "Advanced Coke Oven"

- **Commit:** `ab5e8df6` (same commit)
- **Files/lang keys:**
  - `railcraft.gui.blast.furnace` → `Advanced Coke Oven` (`en_US.lang:355`), `Kokerei II` (`de_DE.lang:348`), `Продвинутая коксовая печь` (`ru_RU.lang:355`)
  - `tile.railcraft.machine.alpha.blast.furnace.name` → `Advanced Coke Oven Brick` (`en_US.lang:759`)
- **Effect:** in non-GTNH packs the machine is presented as a GTNH "Advanced Coke Oven";
  the tooltip still says "Multi-Block: 3x4x3 (Hollow)" and other locales still say "Blast Furnace",
  so the item is inconsistent.
- **Restore:** runtime name selection (see §4.3). Do **not** just edit the lang value back —
  that fights upstream merges. Add new `.classic` keys and select at runtime.

### 2.3 Blast Furnace repurposed as ACO — GTNH-only recipes leak to everyone

- **Commits:** `ab5e8df6`, `b508e783`
- **File:** `ModuleFactory.java:99–102` and `:528`
- **What was added unconditionally:**
  ```java
  blastFurnace.addRecipe(new ItemStack(Items.coal, 1, 0), true, false, 20, coalCoke);
  blastFurnace.addRecipe(new ItemStack(Blocks.coal_block), false, false, 180, COKE_BLOCK);
  // ...in postInit:
  blastFurnace.addRecipe(log, false, false, 20, new ItemStack(Items.coal, 1, 1)); // logs -> charcoal
  ```
  The last one did not exist upstream at all.
- **Effect:** in non-GTNH the Blast Furnace becomes a second Coke Oven instead of a steel maker.
- **Restore:** gate all three to `Game.isGTNH`.

### 2.4 Blast Furnace automatic fuel input removed

- **Commit:** `f4657a22` *"Remove Automatic Fuel Input in TileBlastFurnace / ACO (#100)"*
- **File:** `TileBlastFurnace.java` — the `AdjacentInventoryCache invCache` field and
  `InvTools.moveOneItem(invCache.getAdjacentInventories(), mBlock.invFuel, FUEL_FILTER)` call
  were deleted from `updateEntity()`.
- **Effect:** the Blast Furnace no longer pulls matching fuel from adjacent inventories (hoppers,
  chests, pipes). Only manual insertion / item pipes into the fuel slot remain.
- **Restore (optional):** gate the `moveOneItem` call to `!Game.isGTNH` if you want upstream
  behavior. This is a QoL feature; decide per taste.

### 2.5 Iron & Steel multiblock tanks are uncraftable

- **Commit:** `6de38fea` *"remove default Tank recipes (Iron, Steel)"*
- **File:** `ModuleTransport.java:62–63` (calls commented) and `:247–259` (methods commented)
- **Effect:** `initIronTank()`/`initSteelTank()` are never called, but those methods were the only
  thing calling `EnumMachineBeta.TANK_IRON_*`/`TANK_STEEL_*.register()`. The blocks still exist in
  the creative tab (the metadata is on `blockMachineBeta`, which other machines register), but
  **there is no survival recipe**, and the Iron/Steel tank items used by Tank Carts
  (`ModuleLocomotives.java:57`, `ModuleSteam.java:238`) are effectively unobtainable.
- **Restore:** yes, for `!Game.isGTNH`.

### 2.6 Vanilla-rail → Standard-rail conversion removed

- **Commit:** `53df5da3` *"Remove 8 tracks -> 1 standard rail recipe (#40)"*
- **File:** `ModuleCore.java` (removed `CraftingPlugin.addShapelessRecipe(RailcraftItem.rail.getStack(1, STANDARD), 8× Blocks.rail)` from `!useOldRecipes()` block)
- **Effect:** in GTNH, standard rails are (nearly) free, so the conversion was removed. In a normal
  pack it is the only way to recycle vanilla rails into Railcraft rails. The other direction
  (8 standard rails + railbed → 32 vanilla rails) still exists.
- **Restore:** yes, for `!Game.isGTNH`.

### 2.7 High-pressure Steam Boiler produces 2× steam

- **Commit:** `a0dc844e` *"Improve efficiency of the High Pressure Boiler Tank by two (#17)"*
- **File:** `SteamBoiler.java:90–92` and `:153`
  ```java
  public double getHeatLevel() { return heat / Steam.MAX_HEAT_LOW; }   // was heat / getMaxHeat()
  ...
  fuel += Steam.FUEL_HEAT_INEFFICIENCY * Math.min(getHeatLevel(), 1);   // was getHeatLevel()
  ```
  `getHeatLevel()` is used by `convertSteam()` (`partialConversions += numTanks * getHeatLevel()`),
  so a high-pressure boiler (max heat = `MAX_HEAT_HIGH`) now reaches level 2 instead of 1 →
  up to 40 mB/t instead of 20 mB/t (the `Math.min` only compensates the *fuel* side).
  Lang tooltips were changed from "20 mB" to "up to 40L".
- **Effect:** GTNH balance change applied to all packs.
- **Restore:** make `getHeatLevel()` conditional:
  ```java
  return Game.isGTNH ? heat / Steam.MAX_HEAT_LOW : heat / getMaxHeat();
  ```
  (`Math.min(...,1)` in `getFuelPerCycle` then becomes a no-op for non-GTNH — harmless.)

### 2.8 Creosote boiler fuel value inflated

- **Commit:** `ade6d59d` *"Update creosote burn value as fluid to match burn value when in a bucket"*
- **File:** `ModuleCore.java:127` — `FuelManager.addBoilerFuel(creosote, 4800)` → `6400`
- **Restore:** conditional (or leave; small balance change).

### 2.9 Coke Oven marked "DEPRECATED"

- **Commit:** `6b9127de` *"Add Deprecated Tooltip to Coke Oven (#127)"*
- **File:** `en_US.lang:762` —
  `tile.railcraft.machine.alpha.coke.oven.tip=§cDEPRECATED - Will be removed in next major update§r\nMulti-Block: 3x3x3 (Hollow)`
- **Effect:** non-GTNH players are told Railcraft's core Coke Oven is deprecated.
- **Restore:** runtime tooltip-key selection per §4.3, or a `.classic` lang key.

### 2.10 Cart Filter recipe rejects every item with a container

- **Commit:** `01b4b239` *"Fixed issue by disabling crafting in certain conditions."*
- **File:** `CartFilterRecipe.java` — after finding the filter item:
  ```java
  if (filterItem.getItem().hasContainerItem(filterItem)) return null;
  ```
- **Effect:** this was aimed at GTNH's IC2 cells, but it also blocks **buckets** and anything else
  with a container. Tank Cart filtering with a filled bucket no longer crafts.
- **Restore:** gate to `Game.isGTNH` (or narrow the check to `ic2` cell items).

### 2.11 Silk-touch no longer gives the "silk-touch drop" variant

- **Commits:** `7d010512` (removed overrides), then `903dd68f` *"Rework block breaking to fix
  anchors not storing their time (#93)"* (re-added a simplified `removedByPlayer`)
- **File:** `BlockMachine.java:169–178`
  ```java
  if (willHarvest && !player.capabilities.isCreativeMode) dropBlockAsItem(world, x, y, z, 0, 0);
  super.removedByPlayer(...); return false;
  ```
  Upstream used `getBlockDroppedSilkTouch()` when the player had Silk Touch.
- **Effect:** for machines whose silk-touch drop differs (most importantly **colored Iron/Steel
  tanks** — `TileTankBase.getBlockDroppedSilkTouch` adds the color NBT), the color is lost even with
  Silk Touch. Anchors were the reason for the change: `TileAnchorWorld.getDrops` preserves the
  stored fuel/time, while the silk-touch variant did not.
- **Restore:** restore upstream's silk-touch branch for `!Game.isGTNH`, and (optionally) override
  `getBlockDroppedSilkTouch` in the anchor tiles to return `getDrops(fortune)` so GTNH keeps its
  anchor fix.

### 2.12 GT-metal "Advanced Tanks" module runs in non-GTNH packs

- **Commits:** `0a7ca8e9`, `cb0ebdeb`, `13186254`, … (GTNH-added feature)
- **File:** `ModuleAdvancedTanks.java`, new `EnumMachineZeta`/`EnumMachineEta`
- **Effect:** Aluminium/Stainless/Titanium/… tanks are registered for every pack (visible in
  creative), but their base crafting recipes were removed (`13186254`) and the material oredicts
  (`plateAluminium`, …) don't exist outside GT. Only the "1 tank → 8 dyed tanks" recipe survives.
- **Restore:** gate `ModuleAdvancedTanks` (or its `initFirst`) on GT being loaded, so non-GTNH
  packs don't get ghost machines.

### 2.13 BUG — Rock Crusher BuildCraft pause gate has an inverted check

- **Commit:** `7130a379` *"Fix Rock Crusher (#38)"*
- **File:** `TileRockCrusher.java:343`
  ```java
  public void actionActivated(IActionExternal action) {
      if (Game.isGTNH) {            // <-- inverted
          ... mBlock.actions.add(action);
      }
  }
  ```
  while `processActions()` (l.330) only *consumes* actions when `!Game.isGTNH`.
  Result: **the pause gate never works in either mode** — non-GTNH never records the action,
  GTNH records it but ignores and clears it.
- **Fix:** make `actionActivated` use `if (!Game.isGTNH)` (or drop the gate on both methods and
  restore upstream behavior; GTNH wanted the feature off, so gating both with `!Game.isGTNH`
  preserves their intent).

---

### 2.14 Intent & priority classification (secondary regressions)

The GTNH PR descriptions are unusually explicit about intent; where available they are quoted below.
This determines whether a change is a *break*, *GTNH balance*, *GTNH content*, or *QoL*.

| Doc ref | Item | Intent evidence | Category | Action |
|---|---|---|---|---|
| 2.7 | HP Steam Boiler 2× output | PR #17: *"Double it from 20L/t to 40L/t to make it more worthwhile to spend the additional material on it… at least now it provides a bonus over just using two LP tanks."* | **GTNH balance** (steel cost) | Gate to GTNH |
| 2.6 | 8 rails → standard rail removed | PR #40: *"Addressing this issue: GT-New-Horizons-Modpack#9730"* | **GTNH balance / pack economy** | Gate to GTNH |
| 2.8 | Creosote 4800→6400 | PR #52: cites the GTNH wiki page *`Railcraft_Boiler#GTNH_Changes`* | **GTNH balance (documented)** | Gate to GTNH |
| 2.9 | Coke Oven "DEPRECATED" tooltip | PR #127 (label: Chore); GTNH plans to delete the Coke Oven | **GTNH messaging leak** | Gate / revert |
| 2.10 | Cart Filter rejects container items | PR #2: *"Fixes Dupes-Exploits-GTNH#13"* (IC2/GT cell dupe) | **Break** — `CartFilterRecipe` is the only filter path and every bucket fails `hasContainerItem` | Fix / gate; narrow to the dupe items |
| 2.11 | Silk-touch drop variant removed | PR #93: anchor fuel-loss fix (*"Closes GT-New-Horizons-Modpack#19659"*) | **QoL collateral** — `TileTankBase` is the only `getBlockDroppedSilkTouch` override, so only colored tanks lose color | Restore silk-touch for `!isGTNH` + anchor `getBlockDroppedSilkTouch` override |
| 2.4 | Blast Furnace auto-fuel-input removed | PR #100: *"obscure… withdraws 1 item/tick… inventory size of 27 or higher… removes this feature which will improve performance"* | **QoL / optimization** | Keep, or restore *and fix* the two bugs |
| 2.12 | GT "Advanced Tanks" registered | GTNH-added feature; base recipes live in GT's assembler | **GTNH content leak** (uncraftable ghosts, no crash) | Gate module on GT loaded |
| 2.13 | Rock Crusher pause gate inverted | PR #38: rock crusher re-added *"Disabled when GTNH is detected"* (i.e. restored **for non-GTNH**) | **Bug** — gate dead in both modes | Fix the inverted condition |

Summary of items 5–13 from the chat review: **2 breaks** (2.10, 2.13), **3 GTNH balance** (2.6, 2.7, 2.8),
**2 GTNH-specific leaks** (2.9, 2.12), **2 QoL trade-offs** (2.4, 2.11).
None of these crash or prevent loading; the only genuinely "broken" features are cart filtering and the
BuildCraft rock-crusher gate, with colored-tank Silk Touch as a minor cosmetic loss.

---

## 3. Smaller / lower-priority deltas

| Change | Commit | Impact |
|---|---|---|
| Machine block material/harvest levels changed (`Material.rock` + explicit `axe`/`pickaxe` levels for water tank & coke oven) | `d3f16e69`, `d6a3663e`, `7d010512` | Mostly a fix; no restore needed |
| `TileTankBase.getDrops` override removed (no white-color NBT on normal break) | `88c0f54f` | Same net effect as upstream for *normal* breaks; see §2.11 for silk touch |
| World Anchor disconnect/link fixes, anchor time storage | `af64f6d8`, `903dd68f` | Bug fixes, keep |
| Update-checker removed | `007f191f`, `ba60e857` | Fine |
| Boiler/firebox "voiding" fixes, IC2-cell fixes, packet fixes, many crash fixes | `c24af073`, `7f42205e`, `6a468204`, … | Keep |
| Firestone ignite config, GT hazmat integration, GT energy net, anvil GUI config | recent commits | New optional features, keep |
| `TileBlastFurnace` GUI texture moved to `railcraft:textures/gui/gui_blast_furnace.png` | `9672e6b8` | Resource-pack-friendly; keep |

---

## 4. Recommended architecture for this fork (merge-friendly)

### 4.1 Ground rule

> **Never delete, comment out, or rewrite GTNH's lines. Wrap them and add an `else` path.**

That way a `git merge upstream/master` only has to reconcile the few lines you *added*, and
upstream edits inside `if (Game.isGTNH) { ... }` apply cleanly.

### 4.2 Put restored content in dedicated helper classes

Create (suggested):

```
mods/railcraft/common/compat/legacy/
    LegacyBlastFurnaceRecipes.java   // machine recipe + all iron->steel recipes + steel block
    LegacyTankRecipes.java           // initIronTank/initSteelTank/defineIronTank + steel nugget recycling
    LegacyTrackRecipes.java          // 8 rails -> standard rail
    LegacyBoilerTuning.java          // creosote value, heat level (static helpers)
```

Each helper exposes one `register()`/`apply()` method and is called from the module:

```java
// ModuleFactory.initFirst()
if (Game.isGTNH) {
    RailcraftCraftingManager.blastFurnace.addRecipe(new ItemStack(Items.coal, 1, 0), true, false, 20, ...);
    RailcraftCraftingManager.blastFurnace.addRecipe(new ItemStack(Blocks.coal_block), false, false, 180, ...);
} else {
    LegacyBlastFurnaceRecipes.register();
}
```

```java
// ModuleFactory.initSecond()
if (!Game.isGTNH) {
    LegacyBlastFurnaceRecipes.registerCraftingRecipe();   // nether brick + soul sand + magma cream
} else if (EnumMachineAlpha.BLAST_FURNACE.isAvaliable() && EnumBrick.INFERNAL.getBlock() != null) {
    /* leave GTNH's infernal-brick recipe untouched */
}

// ModuleFactory.postInit()
for (ItemStack log : logs) {
    RailcraftCraftingManager.cokeOven.addRecipe(...);
    if (Game.isGTNH) RailcraftCraftingManager.blastFurnace.addRecipe(log, false, false, 20, new ItemStack(Items.coal, 1, 1));
}
```

```java
// ModuleTransport.initFirst()
if (Game.isGTNH) {
    // initIronTank(); initSteelTank();   <-- GTNH keeps them disabled
} else {
    LegacyTankRecipes.register();
}
```

```java
// ModuleCore.initFirst(), inside `if (!RailcraftConfig.useOldRecipes())`, after the 4 shaped recipes:
if (!Game.isGTNH) LegacyTrackRecipes.register();
```

### 4.3 Runtime name & tooltip selection

Both the item/tile name and the GUI title resolve from lang keys:
- item/tile name → `ItemMachine.getUnlocalizedName` → `IEnumMachine.getTag()` (+ `.name`)
- GUI title → the `name` string passed to `TileMultiBlockOven` (`"railcraft.gui.blast.furnace"`)
- tooltip → `IEnumMachine.getTag() + ".tip"`

Add an overridable localization tag to `IEnumMachine` (default keeps upstream behavior):

```java
public interface IEnumMachine {
    String getTag();                       // unchanged, also used for config/NBT/save data

    default String getLocalizationTag() {  // new
        return getTag();
    }
    ...
}
```

Override in `EnumMachineAlpha` for the two affected machines, and switch
`ItemMachine.getUnlocalizedName` + `TileMachineBase.getLocalizationTag` to use
`getLocalizationTag()`:

```java
// EnumMachineAlpha
@Override
public String getLocalizationTag() {
    if (!Game.isGTNH) {
        if (this == BLAST_FURNACE) return "tile.railcraft.machine.alpha.blast.furnace.classic";
        if (this == COKE_OVEN)    return "tile.railcraft.machine.alpha.coke.oven.classic";
    }
    return getTag();
}
```

Then **add** new lang keys instead of editing GTNH's (so upstream lang edits never conflict):

```
tile.railcraft.machine.alpha.blast.furnace.classic.name=Blast Furnace Brick
tile.railcraft.machine.alpha.blast.furnace.classic.tip=Multi-Block: 3x4x3 (Hollow)
tile.railcraft.machine.alpha.coke.oven.classic.name=Coke Oven Brick
tile.railcraft.machine.alpha.coke.oven.classic.tip=Multi-Block: 3x3x3 (Hollow)
railcraft.gui.blast.furnace.classic=§7 Blast Furnace
```

(`getTag() + ".name"` becomes `getLocalizationTag() + ".name"` in `TileMachineBase`, and
`ItemMachine.getUnlocalizedName` uses `getLocalizationTag()`.)

For the GUI title, make `TileBlastFurnace` choose the string in its constructor:

```java
super(Game.isGTNH ? "railcraft.gui.blast.furnace" : "railcraft.gui.blast.furnace.classic", 3, patterns);
```

For the Coke Oven deprecation line, `EnumMachineAlpha.getToolTip` can swap the tip key the same
way (`getLocalizationTag() + ".tip"` + `LocalizationPlugin.hasTag(...)` fallback), so non-GTNH sees
the classic tip while GTNH keeps their deprecation warning.

### 4.4 Balance switches

`SteamBoiler`:
```java
public double getHeatLevel() {
    return Game.isGTNH ? heat / Steam.MAX_HEAT_LOW : heat / getMaxHeat();
}
```
`ModuleCore` creosote:
```java
FuelManager.addBoilerFuel(creosote, Game.isGTNH ? 6400 : 4800);
```

### 4.5 Optional: config override for testing / edge packs

Add to `RailcraftConfig` (default = auto):
```java
// -1 = auto (dreamcraft), 0 = force legacy, 1 = force GTNH
private static int gtnhMode = -1;
public static boolean gtnhMode() {
    return gtnhMode == 1 || (gtnhMode != 0 && Game.isGTNH);
}
```
Then use `RailcraftConfig.gtnhMode()` in every new gate. This lets a pack dev test both code
paths without installing/removing dreamcraft, and lets a non-GTNH pack that happens to ship
dreamcraft still get legacy behavior. If you do not want that complexity, `Game.isGTNH` alone is
fine and matches upstream style.

### 4.6 Language files are a merge trap

GTNH inlined the `lang/` submodule into `src/main/resources/assets/railcraft/lang/` and edits
`en_US.lang` heavily. Follow the `.classic`-key approach above and **do not** revert their lines;
that keeps every upstream lang change a clean fast-forward.

---

## 5. Concrete checklist

### Pass 1 — Blast Furnace (DONE, commit-ready)

- [x] `ModuleFactory.initFirst` — ACO coal recipes wrapped in `if (Game.isGTNH)`; legacy recipes registered via
      `LegacyBlastFurnaceRecipes.register()` in the `else`.
- [x] New `mods/railcraft/common/compat/legacy/LegacyBlastFurnaceRecipes` — machine recipe (nether brick) + all
      iron→steel recipes + steel block + iron plate→steel + steam engine→12 steel + LP boiler tank→2 steel. All methods
      self-gate on `Game.isGTNH`.
- [x] `ModuleFactory.initSecond` — GTNH infernal-brick recipe gated to `Game.isGTNH`.
- [x] `ModuleFactory.postInit` — log→charcoal gated to `Game.isGTNH`.
- [x] `ItemPlate.defineRecipes` — restored Iron Plate → Steel for `!Game.isGTNH`.
- [x] `ModuleSteam` — restored Steam Engine / Low Boiler Tank recycling for `!Game.isGTNH`.
- [x] `BlastFurnaceCraftingManager.getFuels` — GTNH-only `coal` / `coal_block` fuel entries gated (upstream used
      charcoal + coal coke/firestone only).
- [x] Names/lang: `IEnumMachine.getLocalizationTag()` default method + `EnumMachineAlpha` override + `ItemMachine` /
      `TileMachineBase` use it + `TileBlastFurnace` classic GUI title; `.classic` name/gui keys added to all locales
      (GTNH keys left untouched for clean upstream merges).

### Pass 2 — Iron/Steel tanks (DONE, commit-ready)

- [x] `ModuleTransport.initFirst` — `initIronTank()` / `initSteelTank()` restored and gated to `!Game.isGTNH`.
- [x] `ModuleTransport` — `defineIronTank` / `initIronTank` / `initSteelTank` un-commented (reusing the existing
      `defineTank` / `addColorRecipes` / `getColorTank` helpers).
- [x] `LegacyBlastFurnaceRecipes.registerTankRecipe` — iron tank → 4 steel nuggets added to the BF helper.

### Remaining passes

- [ ] **Pass 3 — must-fix breaks**: `TileRockCrusher.actionActivated` inverted gate (`2.13`); `CartFilterRecipe`
      container-item rejection gated/narrowed (`2.10`).
- [ ] **Pass 4 — GTNH balance gates**: `SteamBoiler.getHeatLevel` (`2.7`); 8 rails → standard rail (`2.6`); creosote
      value (`2.8`); Coke Oven deprecation tooltip (`2.9`, `.classic` tip key).
- [ ] **Pass 5 — GTNH content/QoL**: gate `ModuleAdvancedTanks` on GT (`2.12`); silk-touch drop fix (`2.11`); optional
      Blast Furnace auto-fuel restore (`2.4`).
- [ ] Optional config override for GTNH mode.

---

## Appendix — commit map

| Commit | Summary | Non-GTNH impact |
|---|---|---|
| `6f349d4b` | Neuter the rock crusher | Later restored behind `!isGTNH` (`7130a379`); bug remaining |
| `6de38fea` | Remove default (Iron, Steel) tank recipes | **Regression** |
| `13186254` | Remove GTNH advanced tank recipes | GT-only feature |
| `01b4b239` | Cart filter container-item block | **Regression** |
| `ab5e8df6` | Remove Blast Furnace recipes + rename | **Critical regression** |
| `a0dc844e` | HP boiler 2× efficiency | **Balance regression** |
| `c24af073` | Boiler voiding fix | Fix |
| `b508e783` | ACO coal/charcoal fix | **Leaks to non-GTNH** |
| `53df5da3` | Remove 8 rails → rail recipe | **Regression** |
| `7130a379` | Re-add rock crusher, `!isGTNH` gated | Bug (inverted gate) |
| `2f1d91b0` | Nuke IC2 recipes (dreamcraft-gated) | Gated, safe |
| `ade6d59d` | Creosote 4800→6400 | **Balance regression** |
| `01af27d8`… | Enum `.values()` caching, leak fixes, crash fixes | Fixes |
| `903dd68f` / `7d010512` | Block-breaking rework | Silk-touch **regression** |
| `88c0f54f` | No default color NBT on tanks | Minor |
| `f4657a22` | Remove Blast Furnace auto fuel input | **Feature regression** |
| `6b9127de` | Coke Oven "DEPRECATED" tooltip | **Cosmetic regression** |
| `8ca08e3b`, `34a6f2d5`, `8cf207d0`, `b10ba1a1`, `7ef3d8de`, `0c1e7498`, `eeb0dcdf` | GT hazard/energy/firestone integration | Gated/optional, safe |
