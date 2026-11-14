package com.itmecasey.thoroughchests;

import com.itmecasey.thoroughchests.block.TcChestBlock;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import com.itmecasey.thoroughchests.item.AugmentItem;
import com.itmecasey.thoroughchests.item.UpgradeCardItem;
import com.itmecasey.thoroughchests.menu.TcChestMenu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Every block, item, block entity type and menu type the mod adds, built once and handed to the loader's registry event. */
public final class ModContent {
    public static final String MODID = ThoroughChests.MODID;

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<ChestLevel, TcChestBlock> CHESTS = new java.util.EnumMap<>(ChestLevel.class);
    private static final Map<String, AugmentItem> AUGMENT_ITEMS = new LinkedHashMap<>();
    private static final Map<ChestLevel, UpgradeCardItem> CARDS = new java.util.EnumMap<>(ChestLevel.class);
    private static final List<MenuType<TcChestMenu>> MENUS = new ArrayList<>();
    public static final BlockEntityType<TcChestBlockEntity> CHEST;

    static {
        MapColor[] colors = {MapColor.COLOR_ORANGE, MapColor.METAL, MapColor.GOLD, MapColor.DIAMOND, MapColor.COLOR_BLACK};
        for (ChestLevel level : ChestLevel.values()) {
            String name = level.id() + "_chest";
            TcChestBlock block = new TcChestBlock(level, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)))
                    .mapColor(colors[level.ordinal()]).strength(2.5F + level.ordinal(), level.blastResistance()).sound(SoundType.WOOD)
                    .noOcclusion());
            CHESTS.put(level, block);
            BLOCKS.put(name, block);
            ITEMS.put(name, new BlockItem(block, itemProps(name).useBlockDescriptionPrefix()));
        }
        CHEST = new BlockEntityType<>(TcChestBlockEntity::new, Set.copyOf(CHESTS.values()));
        for (ChestLevel level : ChestLevel.values()) {
            if (level != ChestLevel.COPPER) {
                String name = level.id() + "_upgrade_card";
                UpgradeCardItem card = new UpgradeCardItem(level, itemProps(name));
                CARDS.put(level, card);
                ITEMS.put(name, card);
            }
        }
        for (Augment augment : Augment.values()) {
            for (int l = 1; l <= augment.maxLevel(); l++) {
                String name = augment.itemName(l);
                AugmentItem item = new AugmentItem(augment, l, itemProps(name));
                AUGMENT_ITEMS.put(name, item);
                ITEMS.put(name, item);
            }
        }
        for (int rows = GuiFit.MIN_ROWS; rows <= GuiFit.MAX_ROWS; rows++) {
            int r = rows;
            MENUS.add(new MenuType<>((id, inv) -> new TcChestMenu(menu(r), r, id, inv), FeatureFlags.VANILLA_SET));
        }
    }

    private ModContent() {
    }

    public static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static TcChestBlock chest(ChestLevel level) {
        return CHESTS.get(level);
    }

    public static UpgradeCardItem card(ChestLevel level) {
        return CARDS.get(level);
    }

    public static AugmentItem augmentItem(Augment augment, int level) {
        return AUGMENT_ITEMS.get(augment.itemName(level));
    }

    public static Item item(String name) {
        return ITEMS.get(name);
    }

    public static Map<String, Block> blocks() {
        return BLOCKS;
    }

    public static Map<String, Item> items() {
        return ITEMS;
    }

    /** The menu type with the given number of chest rows. */
    public static MenuType<TcChestMenu> menu(int rows) {
        return MENUS.get(Math.max(GuiFit.MIN_ROWS, Math.min(GuiFit.MAX_ROWS, rows)) - GuiFit.MIN_ROWS);
    }

    public static List<MenuType<TcChestMenu>> menus() {
        return MENUS;
    }

    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thoroughchests"))
                .icon(() -> new ItemStack(ITEMS.get("diamond_chest")))
                .displayItems((params, output) -> ITEMS.values().forEach(output::accept))
                .build();
    }

    public static void registerBlocks(BiConsumer<String, Block> out) {
        BLOCKS.forEach(out);
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        ITEMS.forEach(out);
    }

    public static void registerBlockEntities(BiConsumer<String, BlockEntityType<?>> out) {
        out.accept("chest", CHEST);
    }

    public static void registerMenus(BiConsumer<String, MenuType<?>> out) {
        for (int rows = GuiFit.MIN_ROWS; rows <= GuiFit.MAX_ROWS; rows++) {
            out.accept("chest_" + rows, menu(rows));
        }
    }
}
