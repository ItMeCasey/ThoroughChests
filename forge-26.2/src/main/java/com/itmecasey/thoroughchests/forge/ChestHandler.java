package com.itmecasey.thoroughchests.forge;

import com.itmecasey.thoroughchests.ScriptEvents;
import com.itmecasey.thoroughchests.augment.Augments;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Every slot of a chest as a Forge item handler. With the Void augment on, an insertion that finds the whole chest full is destroyed,
 * so pipes never block on a full chest.
 */
final class ChestHandler implements IItemHandler {
    private final TcChestBlockEntity chest;

    ChestHandler(TcChestBlockEntity chest) {
        this.chest = chest;
    }

    @Override
    public int getSlots() {
        return chest.getContainerSize();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return chest.getItem(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= chest.getContainerSize()) {
            return stack;
        }
        ItemStack here = chest.getItem(slot);
        ItemStack rest = stack;
        if (here.isEmpty() || ItemStack.isSameItemSameComponents(here, stack)) {
            int room = chest.getMaxStackSize(stack) - here.getCount();
            int move = Math.min(room, stack.getCount());
            if (move > 0 && ScriptEvents.allowInsert(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                if (!simulate) {
                    if (here.isEmpty()) {
                        chest.setItem(slot, stack.copyWithCount(move));
                    } else {
                        here.grow(move);
                        chest.setChanged();
                    }
                }
                rest = move >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - move);
            }
        }
        if (!rest.isEmpty() && chest.voidActive() && Augments.isOverflowing(chest, rest)) {
            return Augments.overflow(chest, rest, simulate);
        }
        return rest;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= chest.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemStack here = chest.getItem(slot);
        if (here.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, here.getCount());
        ItemStack out = here.copyWithCount(take);
        if (!simulate) {
            chest.removeItem(slot, take);
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return chest.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot >= 0 && slot < chest.getContainerSize();
    }
}
