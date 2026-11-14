package com.itmecasey.thoroughchests.compat;

import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.CustomRecipe;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.ICategoryRegistration;
import com.itmecasey.bei.api.IRecipeRegistration;
import com.itmecasey.bei.api.RecipeCategory;
import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * BEI: two recipe categories of our own, Chest Upgrade (a chest and a card give the next chest) and Augment Upgrade (the installed level
 * and the next item give the next level). The crafting recipes show in BEI's own crafting category. Loaded only when BEI is installed.
 */
@BeiPlugin
public class ThoroughChestsBeiPlugin implements IBeiPlugin {
    private static RecipeCategory chestUpgrade;
    private static RecipeCategory augmentUpgrade;

    private static RecipeCategory chestCategory() {
        if (chestUpgrade == null) {
            chestUpgrade = RecipeCategory.builder(ModContent.id("chest_upgrade"))
                    .title(Component.translatable("viewer.thoroughchests.chest_upgrade"))
                    .station(new ItemStack(ModContent.card(ChestLevel.IRON)))
                    .size(120, 40)
                    .input(10, 12).input(36, 12)
                    .output(94, 12)
                    .build();
        }
        return chestUpgrade;
    }

    private static RecipeCategory augmentCategory() {
        if (augmentUpgrade == null) {
            augmentUpgrade = RecipeCategory.builder(ModContent.id("augment_upgrade"))
                    .title(Component.translatable("viewer.thoroughchests.augment_upgrade"))
                    .station(new ItemStack(ModContent.augmentItem(Augment.FURNACE, 2)))
                    .size(120, 40)
                    .input(10, 12).input(36, 12)
                    .output(94, 12)
                    .build();
        }
        return augmentUpgrade;
    }

    @Override
    public void registerCategories(ICategoryRegistration registration) {
        registration.register(chestCategory());
        registration.register(augmentCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ChestLevel[] levels = ChestLevel.values();
        for (int i = 1; i < levels.length; i++) {
            registration.add(chestCategory(), CustomRecipe.builder().id("chest_" + levels[i].id())
                    .input(new ItemStack(ModContent.chest(levels[i - 1])), new ItemStack(ModContent.card(levels[i])))
                    .output(new ItemStack(ModContent.chest(levels[i]))).build());
        }
        for (Augment a : Augment.values()) {
            for (int l = 2; l <= a.maxLevel(); l++) {
                registration.add(augmentCategory(), CustomRecipe.builder().id(a.itemName(l))
                        .input(new ItemStack(ModContent.augmentItem(a, l - 1)), new ItemStack(ModContent.augmentItem(a, l)))
                        .output(new ItemStack(ModContent.augmentItem(a, l))).build());
            }
        }
    }
}
