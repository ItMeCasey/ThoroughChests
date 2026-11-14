package com.itmecasey.thoroughchests.augment;

import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

/** Burn time of a fuel item. Minecraft 26.3 keeps it in the cooking fuel component, resolved with a loot context. */
final class Fuel {
    private Fuel() {
    }

    /** Burn ticks one item of this stack gives in a furnace, 0 when it is no fuel. */
    static int burnTicks(ServerLevel level, TcChestBlockEntity chest, ItemStack stack) {
        if (!stack.has(DataComponents.COOKING_FUEL)) {
            return 0;
        }
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, chest.getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, chest)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chest.getBlockPos()))
                .withParameter(LootContextParams.CONTAINER, chest)
                .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }
}
