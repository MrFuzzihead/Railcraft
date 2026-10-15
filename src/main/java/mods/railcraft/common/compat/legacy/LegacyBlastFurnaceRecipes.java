/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info This code is the property of CovertJaguar and may only be used
 * with explicit written permission unless otherwise specified on the license page at
 * http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.compat.legacy;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import mods.railcraft.api.crafting.RailcraftCraftingManager;
import mods.railcraft.common.blocks.aesthetics.cube.EnumCube;
import mods.railcraft.common.blocks.machine.alpha.EnumMachineAlpha;
import mods.railcraft.common.items.ItemCrowbar;
import mods.railcraft.common.items.ItemIngot;
import mods.railcraft.common.items.ItemPlate.EnumPlate;
import mods.railcraft.common.items.RailcraftItem;
import mods.railcraft.common.items.RailcraftToolItems;
import mods.railcraft.common.plugins.forge.CraftingPlugin;
import mods.railcraft.common.util.misc.Game;

/**
 * Restores the original (pre-GTNH-fork) Blast Furnace behaviour for non-GTNH packs.
 * <p>
 * The GTNH fork repurposed the Blast Furnace as an "Advanced Coke Oven" and deleted every steel recipe (see
 * GTNewHorizons/Railcraft#14). Everything in this class mirrors upstream Railcraft and is only registered when
 * {@code !Game.isGTNH}.
 */
public final class LegacyBlastFurnaceRecipes {

    private static final int BURN_TIME = 1280;

    private LegacyBlastFurnaceRecipes() {}

    /** Machine crafting recipe plus every iron -&gt; steel recipe. */
    public static void register() {
        if (Game.isGTNH) return;

        EnumMachineAlpha alpha = EnumMachineAlpha.BLAST_FURNACE;
        if (!alpha.register()) return;

        CraftingPlugin.addShapedRecipe(
                alpha.getItem(4),
                "MBM",
                "BPB",
                "MBM",
                'B',
                new ItemStack(Blocks.nether_brick),
                'M',
                new ItemStack(Blocks.soul_sand),
                'P',
                Items.magma_cream);

        addSteel(new ItemStack(Items.iron_ingot), false, false, BURN_TIME, 1);

        addSteel(new ItemStack(Items.iron_helmet), true, false, BURN_TIME * 5, 5);
        addSteel(new ItemStack(Items.iron_chestplate), true, false, BURN_TIME * 8, 8);
        addSteel(new ItemStack(Items.iron_leggings), true, false, BURN_TIME * 7, 7);
        addSteel(new ItemStack(Items.iron_boots), true, false, BURN_TIME * 4, 4);

        addSteel(new ItemStack(Items.iron_horse_armor), true, false, BURN_TIME * 4, 4);

        addSteel(new ItemStack(Items.iron_sword), true, false, BURN_TIME * 2, 2);
        addSteel(new ItemStack(Items.iron_shovel), true, false, BURN_TIME, 1);
        addSteel(new ItemStack(Items.iron_pickaxe), true, false, BURN_TIME * 3, 3);
        addSteel(new ItemStack(Items.iron_axe), true, false, BURN_TIME * 3, 3);
        addSteel(new ItemStack(Items.iron_hoe), true, false, BURN_TIME * 2, 2);
        addSteel(new ItemStack(Items.shears), true, false, BURN_TIME * 2, 2);

        addSteel(ItemCrowbar.getItem(), true, false, BURN_TIME * 3, 3);

        addSteel(new ItemStack(Items.iron_door), false, false, BURN_TIME * 6, 6);

        int recycleTime = BURN_TIME / 2;
        addSteel(RailcraftToolItems.getSteelHelm(), false, false, recycleTime * 4, 4);
        addSteel(RailcraftToolItems.getSteelArmor(), false, false, recycleTime * 6, 6);
        addSteel(RailcraftToolItems.getSteelLegs(), false, false, recycleTime * 5, 5);
        addSteel(RailcraftToolItems.getSteelBoots(), false, false, recycleTime * 3, 3);

        addSteel(RailcraftToolItems.getSteelSword(), false, false, recycleTime, 1);
        addSteel(RailcraftToolItems.getSteelPickaxe(), false, false, recycleTime * 2, 2);
        addSteel(RailcraftToolItems.getSteelHoe(), false, false, recycleTime, 1);
        addSteel(RailcraftToolItems.getSteelAxe(), false, false, recycleTime * 2, 2);
        addSteel(RailcraftToolItems.getSteelShears(), false, false, recycleTime, 1);
    }

    /** {@code iron_block} -&gt; {@code steel_block}; registered alongside the other metal block recipes. */
    public static void registerSteelBlockRecipe() {
        if (Game.isGTNH) return;
        if (EnumMachineAlpha.BLAST_FURNACE.isAvaliable()) RailcraftCraftingManager.blastFurnace
                .addRecipe(new ItemStack(Blocks.iron_block), false, false, 11520, EnumCube.STEEL_BLOCK.getItem());
    }

    /** Iron plate -&gt; steel ingot. */
    public static void registerPlateRecipe() {
        if (Game.isGTNH) return;
        RailcraftCraftingManager.blastFurnace.addRecipe(
                RailcraftItem.plate.getStack(EnumPlate.IRON),
                true,
                false,
                BURN_TIME,
                RailcraftItem.ingot.getStack(1, ItemIngot.EnumIngot.STEEL));
    }

    /** Low pressure steam engine -&gt; 12 steel ingots. */
    public static void registerSteamEngineRecipe(ItemStack engine) {
        if (Game.isGTNH) return;
        RailcraftCraftingManager.blastFurnace
                .addRecipe(engine, true, false, 15360, RailcraftItem.ingot.getStack(12, ItemIngot.EnumIngot.STEEL));
    }

    /** Low pressure boiler tank -&gt; 2 steel ingots. */
    public static void registerBoilerTankRecipe(ItemStack tank) {
        if (Game.isGTNH) return;
        RailcraftCraftingManager.blastFurnace
                .addRecipe(tank, true, false, 2560, RailcraftItem.ingot.getStack(2, ItemIngot.EnumIngot.STEEL));
    }

    private static void addSteel(ItemStack input, boolean matchDamage, boolean matchNbt, int cookTime, int amount) {
        RailcraftCraftingManager.blastFurnace.addRecipe(
                input,
                matchDamage,
                matchNbt,
                cookTime,
                RailcraftItem.ingot.getStack(amount, ItemIngot.EnumIngot.STEEL));
    }
}
