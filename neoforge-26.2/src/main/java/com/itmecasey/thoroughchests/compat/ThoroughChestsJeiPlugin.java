package com.itmecasey.thoroughchests.compat;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.ModContent;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * JEI: two recipe categories of our own, Chest Upgrade (a chest and a card give the next chest) and Augment Upgrade (the installed level
 * and the next item give the next level). The crafting recipes show in JEI's own crafting category. Loaded only when JEI is installed.
 */
@JeiPlugin
public class ThoroughChestsJeiPlugin implements IModPlugin {
    /** One upgrade shown in a category: what is there, what is added, what comes out. */
    public record Upgrade(ItemStack base, ItemStack added, ItemStack result) {
    }

    private static final IRecipeType<Upgrade> CHEST = IRecipeType.create(Identifier.fromNamespaceAndPath("thoroughchests", "chest_upgrade"), Upgrade.class);
    private static final IRecipeType<Upgrade> AUGMENT = IRecipeType.create(Identifier.fromNamespaceAndPath("thoroughchests", "augment_upgrade"), Upgrade.class);

    @Override
    public Identifier getPluginUid() {
        // JEI asks for this while mods are still loading; touching ModContent here would build the blocks too early.
        return Identifier.fromNamespaceAndPath("thoroughchests", "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new Category(CHEST, Component.translatable("viewer.thoroughchests.chest_upgrade"), gui.createDrawableItemStack(new ItemStack(ModContent.card(ChestLevel.IRON)))),
                new Category(AUGMENT, Component.translatable("viewer.thoroughchests.augment_upgrade"), gui.createDrawableItemStack(new ItemStack(ModContent.augmentItem(Augment.FURNACE, 2)))));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<Upgrade> chests = new ArrayList<>();
        ChestLevel[] levels = ChestLevel.values();
        for (int i = 1; i < levels.length; i++) {
            chests.add(new Upgrade(new ItemStack(ModContent.chest(levels[i - 1])), new ItemStack(ModContent.card(levels[i])), new ItemStack(ModContent.chest(levels[i]))));
        }
        registration.addRecipes(CHEST, chests);
        List<Upgrade> augments = new ArrayList<>();
        for (Augment a : Augment.values()) {
            for (int l = 2; l <= a.maxLevel(); l++) {
                augments.add(new Upgrade(new ItemStack(ModContent.augmentItem(a, l - 1)), new ItemStack(ModContent.augmentItem(a, l)), new ItemStack(ModContent.augmentItem(a, l))));
            }
        }
        registration.addRecipes(AUGMENT, augments);
    }

    private record Category(IRecipeType<Upgrade> type, Component title, IDrawable icon) implements IRecipeCategory<Upgrade> {
        @Override
        public IRecipeType<Upgrade> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public int getWidth() {
            return 100;
        }

        @Override
        public int getHeight() {
            return 26;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Upgrade recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).add(recipe.base());
            builder.addSlot(RecipeIngredientRole.INPUT, 30, 4).add(recipe.added());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 78, 4).add(recipe.result());
        }
    }
}
