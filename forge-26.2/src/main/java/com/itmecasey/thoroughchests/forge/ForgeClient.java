package com.itmecasey.thoroughchests.forge;

import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.client.TcChestRenderer;
import com.itmecasey.thoroughchests.client.TcChestScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup: the screens and the chest renderer. The events only fire on a client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void install(FMLJavaModLoadingContext context) {
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(ForgeClient::setup);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClient::renderers);
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(ForgeClient::layers);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var type : ModContent.menus()) {
                MenuScreens.register(type, TcChestScreen::new);
            }
        });
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.itmecasey.thoroughchests.client.AugmentPlateModel.LAYER, com.itmecasey.thoroughchests.client.AugmentPlateModel::createLayer);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.CHEST, TcChestRenderer::new);
    }
}
