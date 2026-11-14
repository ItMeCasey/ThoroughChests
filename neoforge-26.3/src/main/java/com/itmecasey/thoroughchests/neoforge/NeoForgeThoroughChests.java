package com.itmecasey.thoroughchests.neoforge;

import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.ThoroughChests;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
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
    public NeoForgeThoroughChests(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.LOCAL, ChestConfig.SPEC, "thoroughchests-common.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> onConfig(e.getConfig()));
        modBus.addListener((ModConfigEvent.Reloading e) -> onConfig(e.getConfig()));
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ClientConfigScreen.register(container);
        }
        com.itmecasey.thoroughchests.script.Bridges.install(FMLPaths.GAMEDIR.get());
        modBus.addListener(NeoForgeThoroughChests::register);
        modBus.addListener(NeoForgeThoroughChests::capabilities);
        com.itmecasey.thoroughchests.Dev.call("com.itmecasey.thoroughchests.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void onConfig(ModConfig config) {
        if (config.getSpec() == ChestConfig.SPEC) {
            ChestConfig.push();
        }
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
