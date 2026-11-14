package com.itmecasey.thoroughchests.block;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.AugmentRules;
import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.Fullness;
import com.itmecasey.thoroughchests.FurnaceSim;
import com.itmecasey.thoroughchests.LockRule;
import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.RedstoneMode;
import com.itmecasey.thoroughchests.ScriptEvents;
import com.itmecasey.thoroughchests.Settings;
import com.itmecasey.thoroughchests.augment.Augments;
import com.itmecasey.thoroughchests.item.AugmentItem;
import com.itmecasey.thoroughchests.item.LockItem;
import com.itmecasey.thoroughchests.menu.TcChestMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** The chest's inventory, augment, lock, filter and settings, with the vanilla open counting and lid animation. */
public class TcChestBlockEntity extends BlockEntity implements Container, LidBlockEntity {
    private static final int EVENT_OPEN_COUNT = 1;

    private NonNullList<ItemStack> items;
    /** One-slot containers for the augment and the lock, and nine ghost slots for the filter. */
    public final SimpleContainer augmentSlot = new Slots(1);
    public final SimpleContainer lockSlot = new Slots(1);
    public final SimpleContainer filterSlots = new Slots(9);
    public final FurnaceSim sim = new FurnaceSim();
    private ItemStack cookInput = ItemStack.EMPTY;
    private ItemStack cookResult = ItemStack.EMPTY;
    private ItemStack pendingOutput = ItemStack.EMPTY;
    private boolean filterWhitelist;
    private boolean augmentEnabled = true;
    private RedstoneMode redstone = RedstoneMode.IGNORE;
    @Nullable
    private String ownerId;
    private String ownerName = "";
    private boolean keep;
    /** On a client: the item name of the installed augment, sent by the server so the renderer can draw its overlay. */
    private String clientOverlay = "";
    private int version;
    private long ticks;
    private final ChestLidController lid = new ChestLidController();
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level world, BlockPos pos, BlockState state) {
            play(world, pos, SoundEvents.CHEST_OPEN);
            ScriptEvents.fire("chest_opened", "dimension", String.valueOf(world.dimension().identifier()), "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                    "level", level().id());
        }

        @Override
        protected void onClose(Level world, BlockPos pos, BlockState state) {
            play(world, pos, SoundEvents.CHEST_CLOSE);
            ScriptEvents.fire("chest_closed", "dimension", String.valueOf(world.dimension().identifier()), "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                    "level", level().id());
        }

        @Override
        protected void openerCountChanged(Level world, BlockPos pos, BlockState state, int previous, int current) {
            world.blockEvent(pos, state.getBlock(), EVENT_OPEN_COUNT, current);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof TcChestMenu menu && menu.chest() == TcChestBlockEntity.this;
        }
    };

    public TcChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.CHEST, pos, state);
        this.items = NonNullList.withSize(level().slots(), ItemStack.EMPTY);
    }

    /** A small container that marks the chest changed. */
    private final class Slots extends SimpleContainer {
        Slots(int size) {
            super(size);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            TcChestBlockEntity.this.setChanged();
        }
    }

    private static void play(Level world, BlockPos pos, SoundEvent event) {
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, event, SoundSource.BLOCKS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    public ChestLevel level() {
        return getBlockState().getBlock() instanceof TcChestBlock block ? block.level() : ChestLevel.COPPER;
    }

    public Component displayName() {
        return getBlockState().getBlock().getName();
    }

    /** Counts up whenever anything changes, so open menus know to rebuild their search list. */
    public int version() {
        return version;
    }

    @Override
    public void setChanged() {
        version++;
        super.setChanged();
    }

    // ---- Inventory ----

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(items, slot, count);
        if (!out.isEmpty()) {
            setChanged();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
            if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
                stack.setCount(getMaxStackSize(stack));
            }
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    public NonNullList<ItemStack> rawItems() {
        return items;
    }

    /** Puts a stack into the chest (existing stacks first, then empty slots) and returns what did not fit. */
    public ItemStack insertStack(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!ScriptEvents.allowInsert(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            return stack;
        }
        ItemStack rest = stack.copy();
        for (int i = 0; i < items.size() && !rest.isEmpty(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, rest)) {
                int room = getMaxStackSize(s) - s.getCount();
                int move = Math.min(room, rest.getCount());
                if (move > 0) {
                    if (!simulate) {
                        s.grow(move);
                    }
                    rest.shrink(move);
                }
            }
        }
        for (int i = 0; i < items.size() && !rest.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                int move = Math.min(rest.getCount(), getMaxStackSize(rest));
                if (!simulate) {
                    items.set(i, rest.copyWithCount(move));
                }
                rest.shrink(move);
            }
        }
        if (!simulate && rest.getCount() != stack.getCount()) {
            setChanged();
        }
        return rest;
    }

    /** Insertion by hoppers-like automation and the magnet: the Void augment swallows or forwards what does not fit. */
    public ItemStack insertFromAutomation(ItemStack stack, boolean simulate) {
        if (Settings.getBool("lock.block_automation") && hasLock()) {
            return stack;
        }
        ItemStack rest = insertStack(stack, simulate);
        if (rest.isEmpty() || !voidActive()) {
            return rest;
        }
        return Augments.overflow(this, rest, simulate);
    }

    /** Counts matching items across the chest. */
    public int countOf(ItemStack like) {
        int n = 0;
        for (ItemStack s : items) {
            if (ItemStack.isSameItemSameComponents(s, like)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Removes up to count items matching like; returns how many were removed. */
    public int removeMatching(ItemStack like, int count) {
        int left = count;
        for (int i = 0; i < items.size() && left > 0; i++) {
            ItemStack s = items.get(i);
            if (ItemStack.isSameItemSameComponents(s, like)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
                if (s.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
            }
        }
        if (left != count) {
            setChanged();
        }
        return count - left;
    }

    /** Merges stacks and sorts them by name, then registry id, then larger stacks first. */
    public void sortItems() {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack s : items) {
            if (s.isEmpty()) {
                continue;
            }
            boolean merged = false;
            for (ItemStack t : all) {
                if (ItemStack.isSameItemSameComponents(s, t) && t.getCount() < getMaxStackSize(t)) {
                    int move = Math.min(s.getCount(), getMaxStackSize(t) - t.getCount());
                    t.grow(move);
                    s.shrink(move);
                    if (s.isEmpty()) {
                        merged = true;
                        break;
                    }
                }
            }
            if (!merged) {
                all.add(s.copy());
            }
        }
        all.sort(Comparator.comparing((ItemStack s) -> s.getHoverName().getString().toLowerCase(java.util.Locale.ROOT))
                .thenComparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                .thenComparing(s -> -s.getCount()));
        for (int i = 0; i < items.size(); i++) {
            items.set(i, i < all.size() ? all.get(i) : ItemStack.EMPTY);
        }
        setChanged();
    }

    /** Moves items the chest already holds from the player's main inventory (not the hotbar) into the chest. */
    public int quickStack(Player player) {
        int moved = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && countOf(s) > 0) {
                int before = s.getCount();
                ItemStack rest = insertStack(s, false);
                moved += before - rest.getCount();
                player.getInventory().setItem(i, rest);
            }
        }
        return moved;
    }

    public int comparatorSignal() {
        float sum = 0;
        boolean any = false;
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                any = true;
                sum += (float) s.getCount() / getMaxStackSize(s);
            }
        }
        return Fullness.signal(sum, items.size(), any);
    }

    public int usedSlots() {
        int n = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    // ---- Lock ----

    public boolean hasLock() {
        return ownerId != null && !ownerId.isEmpty();
    }

    public String ownerName() {
        return ownerName;
    }

    @Nullable
    public String ownerId() {
        return ownerId;
    }

    public boolean mayAccess(Player player) {
        return LockRule.canAccess(ownerId, player.getStringUUID(), player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER), Settings.getBool("lock.ops_bypass"));
    }

    /** Installs the lock from a stack in the player's hand. */
    public boolean installLock(ItemStack stack, Player player) {
        if (hasLock() || !(stack.getItem() instanceof LockItem) || !lockSlot.getItem(0).isEmpty()) {
            return false;
        }
        lockSlot.setItem(0, stack.copyWithCount(1));
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        lockInstalled(player);
        return true;
    }

    /** Called after a lock item entered the lock slot: its placer becomes the owner. */
    public void lockInstalled(Player player) {
        ownerId = player.getStringUUID();
        ownerName = player.getGameProfile().name();
        setChanged();
        fire("chest_locked", "owner", ownerName);
    }

    /** Called after the lock item left the lock slot. */
    public void lockRemoved() {
        String was = ownerName;
        ownerId = null;
        ownerName = "";
        setChanged();
        fire("chest_unlocked", "owner", was);
    }

    // ---- Augments ----

    @Nullable
    public AugmentRules.Installed installedAugment() {
        return augmentSlot.getItem(0).getItem() instanceof AugmentItem a ? a.installed() : null;
    }

    @Nullable
    public Augment augment() {
        AugmentRules.Installed i = installedAugment();
        return i == null ? null : i.augment();
    }

    public int augmentLevel() {
        AugmentRules.Installed i = installedAugment();
        return i == null ? 0 : i.level();
    }

    public ItemStack augmentStack() {
        return augmentSlot.getItem(0);
    }

    /** Installs or upgrades the augment from a stack the player holds; consumes one. */
    public boolean installFrom(ItemStack stack, Player player) {
        if (stack.getItem() instanceof LockItem) {
            return installLock(stack, player);
        }
        if (!(stack.getItem() instanceof AugmentItem item) || !AugmentRules.canInstall(installedAugment(), item.installed())) {
            return false;
        }
        boolean upgrade = AugmentRules.isUpgrade(installedAugment(), item.installed());
        augmentSlot.setItem(0, stack.copyWithCount(1));
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        augmentChanged(upgrade);
        return true;
    }

    /** Call after the augment slot changed: resets what belongs to the old augment and tells scripts. */
    public void augmentChanged(boolean upgrade) {
        AugmentRules.Installed now = installedAugment();
        if (now == null) {
            resetAugmentState(true);
            fire("augment_removed");
        } else {
            if (!upgrade) {
                augmentEnabled = now.augment() != Augment.VOID || Settings.getBool("void.enabled_by_default");
                resetAugmentState(false);
            }
            fire("augment_installed", "augment", now.augment().id(), "augment_level", now.level());
        }
        setChanged();
        syncToClients();
    }

    private void resetAugmentState(boolean removed) {
        if (removed) {
            returnCooking();
        }
        sim.load(0, 0, false);
        cookInput = ItemStack.EMPTY;
        cookResult = ItemStack.EMPTY;
        pendingOutput = ItemStack.EMPTY;
    }

    /** Gives a half-cooked item back to the chest. */
    private void returnCooking() {
        if (!cookInput.isEmpty()) {
            insertStack(cookInput, false);
        }
        if (!pendingOutput.isEmpty()) {
            insertStack(pendingOutput, false);
        }
    }

    /** Registry name of the installed augment's item (the overlay texture is named after it), or empty. */
    public String overlayName() {
        AugmentRules.Installed i = installedAugment();
        return i != null ? i.augment().itemName(i.level()) : clientOverlay;
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("overlay", overlayName());
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean augmentEnabled() {
        return augmentEnabled;
    }

    public void setAugmentEnabled(boolean on) {
        augmentEnabled = on;
        setChanged();
    }

    public RedstoneMode redstone() {
        return redstone;
    }

    public void setRedstone(RedstoneMode mode) {
        redstone = mode;
        setChanged();
    }

    public boolean filterWhitelist() {
        return filterWhitelist;
    }

    public void setFilterWhitelist(boolean on) {
        filterWhitelist = on;
        setChanged();
    }

    /** Whether the chest's filter lets this stack through. An empty filter lets everything through. */
    public boolean filterAllows(ItemStack stack) {
        boolean any = false;
        boolean listed = false;
        for (int i = 0; i < filterSlots.getContainerSize(); i++) {
            ItemStack f = filterSlots.getItem(i);
            if (!f.isEmpty()) {
                any = true;
                if (f.is(stack.getItem())) {
                    listed = true;
                }
            }
        }
        return !any || listed == filterWhitelist;
    }

    /** Whether the augment would act now: installed, switched on and allowed by the redstone mode. */
    public boolean augmentActive() {
        if (augment() == null || !augmentEnabled) {
            return false;
        }
        return redstone.active(level == null || level.hasNeighborSignal(worldPosition));
    }

    public boolean voidActive() {
        return augment() == Augment.VOID && augmentActive();
    }

    public ItemStack cookInput() {
        return cookInput;
    }

    public ItemStack cookResult() {
        return cookResult;
    }

    public ItemStack pendingOutput() {
        return pendingOutput;
    }

    public void setCook(ItemStack input, ItemStack result) {
        cookInput = input;
        cookResult = result;
        setChanged();
    }

    public void setPending(ItemStack stack) {
        pendingOutput = stack;
        setChanged();
    }

    public long ticks() {
        return ticks;
    }

    private void fire(String event, Object... extra) {
        if (level == null || !ScriptEvents.hasListeners(event)) {
            return;
        }
        Object[] data = new Object[extra.length + 10];
        data[0] = "dimension";
        data[1] = String.valueOf(level.dimension().identifier());
        data[2] = "x";
        data[3] = worldPosition.getX();
        data[4] = "y";
        data[5] = worldPosition.getY();
        data[6] = "z";
        data[7] = worldPosition.getZ();
        data[8] = "level";
        data[9] = level().id();
        System.arraycopy(extra, 0, data, 10, extra.length);
        ScriptEvents.fire(event, data);
    }

    // ---- Ticks, lid, openers ----

    public static void clientTick(Level world, BlockPos pos, BlockState state, TcChestBlockEntity chest) {
        chest.lid.tickLid();
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, TcChestBlockEntity chest) {
        chest.ticks++;
        com.itmecasey.thoroughchests.script.ScriptHost.touch(world);
        if (chest.augment() != null && world instanceof ServerLevel server && chest.augmentActive()) {
            Augments.tick(chest, server);
        }
    }

    @Override
    public boolean triggerEvent(int id, int value) {
        if (id == EVENT_OPEN_COUNT) {
            lid.shouldBeOpen(value > 0);
            return true;
        }
        return super.triggerEvent(id, value);
    }

    @Override
    public float getOpenNess(float partial) {
        return lid.getOpenness(partial);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
            openers.incrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
            openers.decrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState());
        }
    }

    public void recheckOpen() {
        if (!remove && level != null) {
            openers.recheckOpeners(level, worldPosition, getBlockState());
        }
    }

    public int openerCount() {
        return openers.getOpenerCount();
    }

    // ---- Moving and removing ----

    /** Keeps the contents when this block entity is removed because the block is replaced by an upgrade. */
    public void keepOnRemove() {
        keep = true;
    }

    /** Takes over everything of another chest (the upgrade path). */
    public void adopt(TcChestBlockEntity other) {
        int size = Math.max(level().slots(), other.items.size());
        items = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < other.items.size(); i++) {
            items.set(i, other.items.get(i));
        }
        copy(other.augmentSlot, augmentSlot);
        copy(other.lockSlot, lockSlot);
        copy(other.filterSlots, filterSlots);
        sim.load(other.sim.burnBank(), other.sim.cookLeft(), other.sim.cooking());
        cookInput = other.cookInput;
        cookResult = other.cookResult;
        pendingOutput = other.pendingOutput;
        filterWhitelist = other.filterWhitelist;
        augmentEnabled = other.augmentEnabled;
        redstone = other.redstone;
        ownerId = other.ownerId;
        ownerName = other.ownerName;
        other.items = NonNullList.withSize(0, ItemStack.EMPTY);
        syncToClients();
    }

    private static void copy(SimpleContainer from, SimpleContainer to) {
        for (int i = 0; i < from.getContainerSize(); i++) {
            to.setItem(i, from.getItem(i));
            from.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (keep || level == null) {
            return;
        }
        returnCooking();
        Containers.dropContents(level, pos, augmentSlot);
        Containers.dropContents(level, pos, lockSlot);
        // The base class drops the chest's own inventory because this block entity is a Container.
        super.preRemoveSideEffects(pos, state);
    }

    // ---- Save and load ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.store("augment", ItemStack.OPTIONAL_CODEC, augmentSlot.getItem(0));
        output.store("lock", ItemStack.OPTIONAL_CODEC, lockSlot.getItem(0));
        var filterOut = output.list("filter", net.minecraft.world.ItemStackWithSlot.CODEC);
        for (int i = 0; i < filterSlots.getContainerSize(); i++) {
            if (!filterSlots.getItem(i).isEmpty()) {
                filterOut.add(new net.minecraft.world.ItemStackWithSlot(i, filterSlots.getItem(i)));
            }
        }
        output.putBoolean("filter_whitelist", filterWhitelist);
        output.putBoolean("augment_enabled", augmentEnabled);
        output.putInt("redstone", redstone.ordinal());
        output.putString("owner_id", ownerId == null ? "" : ownerId);
        output.putString("owner_name", ownerName);
        output.putDouble("burn", sim.burnBank());
        output.putInt("cook_left", sim.cookLeft());
        output.putBoolean("cooking", sim.cooking());
        output.store("cook_input", ItemStack.OPTIONAL_CODEC, cookInput);
        output.store("cook_result", ItemStack.OPTIONAL_CODEC, cookResult);
        output.store("pending", ItemStack.OPTIONAL_CODEC, pendingOutput);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        clientOverlay = input.getStringOr("overlay", "");
        int highest = -1;
        for (net.minecraft.world.ItemStackWithSlot s : input.listOrEmpty("Items", net.minecraft.world.ItemStackWithSlot.CODEC)) {
            highest = Math.max(highest, s.slot());
        }
        items = NonNullList.withSize(level().sizeFor(highest), ItemStack.EMPTY);
        for (net.minecraft.world.ItemStackWithSlot s : input.listOrEmpty("Items", net.minecraft.world.ItemStackWithSlot.CODEC)) {
            if (s.slot() >= 0 && s.slot() < items.size()) {
                items.set(s.slot(), s.stack());
            }
        }
        augmentSlot.setItem(0, input.read("augment", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        lockSlot.setItem(0, input.read("lock", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        filterSlots.clearContent();
        for (net.minecraft.world.ItemStackWithSlot s : input.listOrEmpty("filter", net.minecraft.world.ItemStackWithSlot.CODEC)) {
            if (s.slot() >= 0 && s.slot() < filterSlots.getContainerSize()) {
                filterSlots.setItem(s.slot(), s.stack());
            }
        }
        filterWhitelist = input.getBooleanOr("filter_whitelist", false);
        augmentEnabled = input.getBooleanOr("augment_enabled", true);
        redstone = RedstoneMode.byOrdinal(input.getIntOr("redstone", 0));
        String owner = input.getStringOr("owner_id", "");
        ownerId = owner.isEmpty() ? null : owner;
        ownerName = input.getStringOr("owner_name", "");
        sim.load(input.getDoubleOr("burn", 0.0), input.getIntOr("cook_left", 0), input.getBooleanOr("cooking", false));
        cookInput = input.read("cook_input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        cookResult = input.read("cook_result", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        pendingOutput = input.read("pending", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    /** The block this entity belongs to (used by the loaders when they wrap the inventory). */
    public Block block() {
        return getBlockState().getBlock();
    }

    public static Direction facing(BlockState state) {
        return state.getValue(TcChestBlock.FACING);
    }
}
