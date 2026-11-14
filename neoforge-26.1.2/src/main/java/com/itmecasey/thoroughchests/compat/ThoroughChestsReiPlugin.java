package com.itmecasey.thoroughchests.compat;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.ModContent;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * REI: two display categories of our own, Chest Upgrade (a chest and a card give the next chest) and Augment Upgrade (the installed level
 * and the next item give the next level). The crafting recipes show in REI's own crafting category. Loaded only when REI is installed.
 */
@REIPluginClient
public class ThoroughChestsReiPlugin implements REIClientPlugin {
    private static final CategoryIdentifier<UpgradeDisplay> CHEST = CategoryIdentifier.of("thoroughchests", "chest_upgrade");
    private static final CategoryIdentifier<UpgradeDisplay> AUGMENT = CategoryIdentifier.of("thoroughchests", "augment_upgrade");

    /** What REI shows for one upgrade. */
    public static final class UpgradeDisplay extends BasicDisplay {
        private final CategoryIdentifier<UpgradeDisplay> category;

        UpgradeDisplay(CategoryIdentifier<UpgradeDisplay> category, ItemStack base, ItemStack added, ItemStack result) {
            super(List.of(EntryIngredient.of(EntryStacks.of(base)), EntryIngredient.of(EntryStacks.of(added))), List.of(EntryIngredient.of(EntryStacks.of(result))));
            this.category = category;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return category;
        }

        /** These displays are only built on the client and never saved or sent. */
        @Override
        public me.shedaniel.rei.api.common.display.DisplaySerializer<? extends me.shedaniel.rei.api.common.display.Display> getSerializer() {
            return null;
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new Category(CHEST, Component.translatable("viewer.thoroughchests.chest_upgrade"), EntryStacks.of(new ItemStack(ModContent.card(ChestLevel.IRON)))));
        registry.add(new Category(AUGMENT, Component.translatable("viewer.thoroughchests.augment_upgrade"), EntryStacks.of(new ItemStack(ModContent.augmentItem(Augment.FURNACE, 2)))));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        ChestLevel[] levels = ChestLevel.values();
        for (int i = 1; i < levels.length; i++) {
            registry.add(new UpgradeDisplay(CHEST, new ItemStack(ModContent.chest(levels[i - 1])), new ItemStack(ModContent.card(levels[i])), new ItemStack(ModContent.chest(levels[i]))));
        }
        for (Augment a : Augment.values()) {
            for (int l = 2; l <= a.maxLevel(); l++) {
                registry.add(new UpgradeDisplay(AUGMENT, new ItemStack(ModContent.augmentItem(a, l - 1)), new ItemStack(ModContent.augmentItem(a, l)),
                        new ItemStack(ModContent.augmentItem(a, l))));
            }
        }
    }

    private record Category(CategoryIdentifier<UpgradeDisplay> id, Component title, Renderer icon) implements DisplayCategory<UpgradeDisplay> {
        @Override
        public CategoryIdentifier<? extends UpgradeDisplay> getCategoryIdentifier() {
            return id;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public Renderer getIcon() {
            return icon;
        }

        @Override
        public int getDisplayHeight() {
            return 36;
        }

        @Override
        public List<Widget> setupDisplay(UpgradeDisplay display, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            Point origin = new Point(bounds.getCenterX() - 41, bounds.getCenterY() - 9);
            widgets.add(Widgets.createSlot(new Point(origin.x, origin.y)).entries(display.getInputEntries().get(0)).markInput());
            widgets.add(Widgets.createSlot(new Point(origin.x + 22, origin.y)).entries(display.getInputEntries().get(1)).markInput());
            widgets.add(Widgets.createArrow(new Point(origin.x + 46, origin.y + 1)));
            widgets.add(Widgets.createSlot(new Point(origin.x + 74, origin.y)).entries(display.getOutputEntries().get(0)).markOutput());
            return widgets;
        }
    }
}
