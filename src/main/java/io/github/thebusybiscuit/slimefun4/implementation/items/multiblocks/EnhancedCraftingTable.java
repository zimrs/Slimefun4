package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.papermc.lib.PaperLib;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class EnhancedCraftingTable extends AbstractCraftingTable {

    @ParametersAreNonnullByDefault
    public EnhancedCraftingTable(ItemGroup itemGroup, SlimefunItemStack item) {
        super(itemGroup, item, new ItemStack[] { null, null, null, null, new ItemStack(Material.CRAFTING_TABLE), null, null, new ItemStack(Material.DISPENSER), null }, BlockFace.SELF);
    }

    @Override
    public void onInteract(Player p, Block b) {
        Block possibleDispenser = b.getRelative(BlockFace.DOWN);
        BlockState state = PaperLib.getBlockState(possibleDispenser, false).getState();

        if (state instanceof Dispenser dispenser) {
            Inventory inv = dispenser.getInventory();
            List<ItemStack[]> inputs = RecipeType.getRecipeInputList(this);

            for (int i = 0; i < inputs.size(); i++) {
                if (isCraftable(inv, inputs.get(i))) {
                    ItemStack output = RecipeType.getRecipeOutputList(this, inputs.get(i)).clone();

                    if (SlimefunUtils.canPlayerUseItem(p, output, true)) {
                        craft(inv, possibleDispenser, p, b, output);
                    }

                    return;
                }
            }

            if (inv.isEmpty()) {
                Slimefun.getLocalization().sendMessage(p, "machines.inventory-empty", true);
            } else {
                Slimefun.getLocalization().sendMessage(p, "machines.pattern-not-found", true);
            }
        }
    }

    private void craft(Inventory inv, Block dispenser, Player p, Block b, ItemStack output) {
        Inventory fakeInv = createVirtualInventory(inv);
        Inventory outputInv = findOutputInventory(output, dispenser, inv, fakeInv);

        if (outputInv != null) {
            SlimefunItem sfItem = SlimefunItem.getByItem(output);

            var waitCallback = false;
            if (sfItem instanceof SlimefunBackpack backpack) {
                waitCallback = upgradeBackpack(p, inv, backpack, output, () -> {
                    p.getWorld().playSound(b.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1, 1);
                    outputInv.addItem(output);
                });
            }

            for (int j = 0; j < 9; j++) {
                ItemStack item = inv.getContents()[j];

                if (item != null && item.getType() != Material.AIR) {
                    ItemUtils.consumeItem(item, true);
                }
            }

            if (!waitCallback) {
                p.getWorld().playSound(b.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1, 1);
                outputInv.addItem(output);
            }
        } else {
            Slimefun.getLocalization().sendMessage(p, "machines.full-inventory", true);
        }
    }

    private boolean isCraftable(Inventory inv, ItemStack[] recipe) {
        for (int j = 0; j < inv.getContents().length; j++) {
            if (!SlimefunUtils.isItemSimilar(inv.getContents()[j], recipe[j], true, true, false)) {
                if (SlimefunItem.getByItem(recipe[j]) instanceof SlimefunBackpack) {
                    if (!SlimefunUtils.isItemSimilar(inv.getContents()[j], recipe[j], false, true, false)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        return true;
    }

}
