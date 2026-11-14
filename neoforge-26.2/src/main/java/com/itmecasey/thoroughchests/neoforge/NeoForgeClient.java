package com.itmecasey.thoroughchests.neoforge;

import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.ThoroughChests;
import com.itmecasey.thoroughchests.client.TcChestRenderer;
import com.itmecasey.thoroughchests.client.TcChestScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only setup: the screens and the chest renderer. Never loaded on a dedicated server. */
@EventBusSubscriber(modid = ThoroughChests.MODID, value = Dist.CLIENT)
public final class NeoForgeClient {
    private NeoForgeClient() {
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        for (var type : ModContent.menus()) {
            event.register(type, TcChestScreen::new);
        }
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModContent.CHEST, TcChestRenderer::new);
    }
}
