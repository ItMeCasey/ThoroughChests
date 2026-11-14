package com.itmecasey.thoroughchests.forge;

import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.ThoroughChests;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Forge entry point for Minecraft 26.x. Registers the content and exposes every chest slot to pipes. */
@Mod(ThoroughChests.MODID)
public final class ForgeThoroughChests {
    public ForgeThoroughChests(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, ChestConfig.SPEC);
        ModConfigEvent.Loading.getBus(context.getModBusGroup()).addListener(e -> onConfig(e.getConfig()));
        ModConfigEvent.Reloading.getBus(context.getModBusGroup()).addListener(e -> onConfig(e.getConfig()));
        com.itmecasey.thoroughchests.script.Bridges.install(FMLPaths.GAMEDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgeThoroughChests::register);
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeThoroughChests::attach);
        ForgeClient.install(context);
        com.itmecasey.thoroughchests.Dev.call("com.itmecasey.thoroughchests.forge.dev.ForgeDev", "install", context);
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

    private static void attach(AttachCapabilitiesEvent.BlockEntities event) {
        if (!(event.getObject() instanceof TcChestBlockEntity chest)) {
            return;
        }
        LazyOptional<IItemHandler> lazy = LazyOptional.of(() -> new ChestHandler(chest));
        event.addCapability(ModContent.id("items"), new ICapabilityProvider() {
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ITEM_HANDLER ? lazy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(lazy::invalidate);
    }
}
