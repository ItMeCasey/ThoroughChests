package com.itmecasey.thoroughchests.neoforge;

import com.itmecasey.thoroughchests.Settings;
import com.itmecasey.thoroughchests.augment.Augments;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Every slot of a chest as a NeoForge item handler. With the Void augment on, an insertion that finds the whole chest full is
 * swallowed (no link) so pipes never block on a full chest; a link cannot be forwarded inside a transaction, so then the pipe keeps the items.
 */
final class ChestHandler implements ResourceHandler<ItemResource> {
    private final TcChestBlockEntity chest;
    private final ResourceHandler<ItemResource> inner;

    ChestHandler(TcChestBlockEntity chest) {
        this.chest = chest;
        this.inner = VanillaContainerWrapper.of(chest);
    }

    private boolean locked() {
        return Settings.getBool("lock.block_automation") && chest.hasLock();
    }

    @Override
    public int size() {
        return locked() ? 0 : inner.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return inner.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return inner.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return inner.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return inner.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int in = inner.insert(index, resource, amount, transaction);
        int rest = amount - in;
        if (rest > 0 && chest.voidActive() && Augments.linked(chest) == null) {
            ItemStack left = resource.toStack(rest);
            if (chest.filterAllows(left) && Augments.isOverflowing(chest, left)) {
                return amount;
            }
        }
        return in;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return inner.extract(index, resource, amount, transaction);
    }
}
