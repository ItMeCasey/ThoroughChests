package com.itmecasey.thoroughchests.forge.dev;

import com.itmecasey.thoroughchests.dev.SelfTest;
import com.itmecasey.thoroughchests.dev.ShotTour;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Development hooks for Forge: the server self test and the client screenshot tour. Only built with -PdevTools. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install(Object context) {
        if (SelfTest.requested()) {
            ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        if (FMLEnvironment.dist.isClient() && System.getenv("THOROUGHCHESTS_SHOTS") != null) {
            TickEvent.ClientTickEvent.Post.BUS.addListener((TickEvent.ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }
}
