package com.itmecasey.thoroughchests.neoforge;

import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.ThoroughChests;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point for Minecraft 26.x. Registers the content and exposes every chest slot to pipes. */
@Mod(ThoroughChests.MODID)
public final class NeoForgeThoroughChests {
    public NeoForgeThoroughChests(IEventBus modBus) {
        ThoroughChests.init(FMLPaths.CONFIGDIR::get);
        com.itmecasey.thoroughchests.script.ScriptBridge.install();
        modBus.addListener(NeoForgeThoroughChests::register);
        modBus.addListener(NeoForgeThoroughChests::capabilities);
        com.itmecasey.thoroughchests.Dev.call("com.itmecasey.thoroughchests.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.MENU, helper -> ModContent.registerMenus((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModContent.CHEST, (be, side) -> new ChestHandler(be));
    }
}
