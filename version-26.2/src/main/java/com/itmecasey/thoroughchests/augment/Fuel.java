package com.itmecasey.thoroughchests.augment;

import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Burn time of a fuel item. Minecraft 26.3 moved this into an item component, so this file has a variant for it (scripts/variants). */
final class Fuel {
    private Fuel() {
    }

    /** Burn ticks one item of this stack gives in a furnace, 0 when it is no fuel. */
    static int burnTicks(ServerLevel level, TcChestBlockEntity chest, ItemStack stack) {
        return level.fuelValues().burnDuration(stack);
    }
}
