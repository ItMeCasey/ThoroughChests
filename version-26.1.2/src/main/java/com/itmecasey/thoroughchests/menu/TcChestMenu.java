package com.itmecasey.thoroughchests.menu;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.AugmentRules;
import com.itmecasey.thoroughchests.GuiFit;
import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.RedstoneMode;
import com.itmecasey.thoroughchests.Settings;
import com.itmecasey.thoroughchests.Window;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import com.itmecasey.thoroughchests.item.AugmentItem;
import com.itmecasey.thoroughchests.item.LockItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The chest menu: a window of {@code rows * 9} slots over the chest, the augment slot, the lock slot, nine filter ghost slots and the
 * player inventory. The server keeps the mapping from window slot to chest slot (scroll and search), so every click, shift-click and
 * drag is resolved against the real chest. Scroll, search, sort and the toggles arrive as menu button ids.
 */
public class TcChestMenu extends AbstractContainerMenu {
    public static final int X = 13;
    public static final int WIDTH = 188;
    public static final int BTN_SORT = 1;
    public static final int BTN_STACK = 2;
    public static final int BTN_TOGGLE = 3;
    public static final int BTN_REDSTONE = 4;
    public static final int BTN_FILTER_MODE = 5;
    public static final int BTN_CLEAR_LINK = 6;
    public static final int BTN_CLEAR_QUERY = 7;
    public static final int BTN_BACKSPACE = 8;
    public static final int BTN_ROWS = 200;
    public static final int BTN_SCROLL = 1000;
    public static final int BTN_CHAR = 2000000;
    public static final int D_FILTERED = 0;
    public static final int D_AUGMENT = 1;
    public static final int D_LEVEL = 2;
    public static final int D_ENABLED = 3;
    public static final int D_REDSTONE = 4;
    public static final int D_WHITELIST = 5;
    public static final int D_LOCKED = 6;
    public static final int D_USED = 7;
    public static final int D_SLOTS = 8;
    public static final int D_COUNT = 9;

    private static final Map<UUID, Integer> PREFERRED_ROWS = new HashMap<>();

    private final int rows;
    @Nullable
    private final TcChestBlockEntity chest;
    private final ContainerData data;
    private final Container window;
    private final Container augment;
    private final Container lock;
    private final Container filter;
    private List<Integer> filtered = new ArrayList<>();
    private String query = "";
    private int scroll;
    private int lastVersion = -1;
    private boolean dirty = true;
    private final Player viewer;

    /** The rows a player gets when opening a chest: their remembered size, never more than the chest needs. */
    public static int rowsFor(Player player, TcChestBlockEntity chest) {
        int wanted = PREFERRED_ROWS.getOrDefault(player.getUUID(), Settings.getInt("gui.default_rows"));
        int needed = Math.max(1, (chest.getContainerSize() + Window.COLS - 1) / Window.COLS);
        return Math.max(GuiFit.MIN_ROWS, Math.min(Math.min(wanted, needed), GuiFit.MAX_ROWS));
    }

    public static void rememberRows(Player player, int rows) {
        PREFERRED_ROWS.put(player.getUUID(), Math.max(GuiFit.MIN_ROWS, Math.min(GuiFit.MAX_ROWS, rows)));
    }

    /** Client side. */
    public TcChestMenu(MenuType<TcChestMenu> type, int rows, int id, Inventory inventory) {
        this(type, rows, id, inventory, null);
    }

    /** Server side. */
    public TcChestMenu(MenuType<TcChestMenu> type, int rows, int id, Inventory inventory, @Nullable TcChestBlockEntity chest) {
        super(type, id);
        this.rows = rows;
        this.chest = chest;
        this.viewer = inventory.player;
        this.window = chest == null ? new SimpleContainer(rows * Window.COLS) : new WindowContainer();
        this.augment = chest == null ? new SimpleContainer(1) : chest.augmentSlot;
        this.lock = chest == null ? new SimpleContainer(1) : chest.lockSlot;
        this.filter = chest == null ? new SimpleContainer(9) : chest.filterSlots;
        this.data = chest == null ? new SimpleContainerData(D_COUNT) : serverData(chest);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < Window.COLS; c++) {
                addSlot(new WindowSlot(window, r * Window.COLS + c, X + c * 18, GuiFit.HEAD + r * 18));
            }
        }
        int tools = GuiFit.HEAD + rows * GuiFit.ROW + 2;
        addSlot(new Slot(augment, 0, X + 22, tools) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AugmentItem a && AugmentRules.canInstall(installed(), a.installed());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(lock, 0, X, tools) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof LockItem && (chest == null || !chest.hasLock());
            }

            @Override
            public boolean mayPickup(Player player) {
                return chest == null || chest.mayAccess(player);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(filter, i, X + i * 18, tools + 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        int py = GuiFit.HEAD + rows * GuiFit.ROW + GuiFit.TOOLS + 14;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inventory, c + r * 9 + 9, X + c * 18, py + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inventory, c, X + c * 18, py + 58));
        }
        addDataSlots(data);
        if (chest != null) {
            chest.startOpen(inventory.player);
            refresh();
        }
    }

    private ContainerData serverData(TcChestBlockEntity c) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case D_FILTERED -> filtered.size();
                    case D_AUGMENT -> c.augment() == null ? 0 : c.augment().ordinal() + 1;
                    case D_LEVEL -> c.augmentLevel();
                    case D_ENABLED -> c.augmentEnabled() ? 1 : 0;
                    case D_REDSTONE -> c.redstone().ordinal();
                    case D_WHITELIST -> c.filterWhitelist() ? 1 : 0;
                    case D_LOCKED -> c.hasLock() ? 1 : 0;
                    case D_USED -> c.usedSlots();
                    case D_SLOTS -> c.getContainerSize();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return D_COUNT;
            }
        };
    }

    @Nullable
    private AugmentRules.Installed installed() {
        return augment.getItem(0).getItem() instanceof AugmentItem a ? a.installed() : null;
    }

    public int rows() {
        return rows;
    }

    @Nullable
    public TcChestBlockEntity chest() {
        return chest;
    }

    public int value(int index) {
        return data.get(index);
    }

    public int scroll() {
        return scroll;
    }

    public int filteredCount() {
        return data.get(D_FILTERED);
    }

    public int maxScroll() {
        return Window.maxScroll(filteredCount(), rows);
    }

    /** Client side: remember the scroll the screen shows (the server decides what the slots hold). */
    public void setScroll(int value) {
        scroll = Window.clampScroll(value, filteredCount(), rows);
    }

    @Nullable
    public Augment augmentKind() {
        int a = data.get(D_AUGMENT);
        return a <= 0 ? null : Augment.values()[a - 1];
    }

    public int windowSlots() {
        return rows * Window.COLS;
    }

    public int augmentIndex() {
        return windowSlots();
    }

    public int lockIndex() {
        return windowSlots() + 1;
    }

    public int filterIndex() {
        return windowSlots() + 2;
    }

    public int playerIndex() {
        return windowSlots() + 11;
    }

    // ---- Server side: window mapping ----

    private String searchText(int i) {
        ItemStack s = chest.getItem(i);
        if (s.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(s.getItem());
        StringBuilder sb = new StringBuilder(s.getHoverName().getString().toLowerCase(Locale.ROOT));
        sb.append(' ').append(key.getPath()).append(" @").append(key.getNamespace());
        s.typeHolder().tags().forEach(t -> sb.append(" #").append(t.location().getPath()).append(" #").append(t.location()));
        return sb.toString();
    }

    private void refresh() {
        if (chest == null) {
            return;
        }
        if (dirty || lastVersion != chest.version()) {
            filtered = Window.filtered(chest.getContainerSize(), query, this::searchText);
            if (query.isEmpty()) {
                filtered = new ArrayList<>(filtered);
            }
            lastVersion = chest.version();
            dirty = false;
            scroll = Window.clampScroll(scroll, filtered.size(), rows);
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public void sendAllDataToRemote() {
        refresh();
        super.sendAllDataToRemote();
    }

    private final class WindowContainer extends SimpleContainer {
        WindowContainer() {
            super(rows * Window.COLS);
        }

        private int map(int slot) {
            return Window.map(filtered, scroll, slot);
        }

        @Override
        public ItemStack getItem(int slot) {
            int i = map(slot);
            return i < 0 ? ItemStack.EMPTY : chest.getItem(i);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            int i = map(slot);
            return i < 0 ? ItemStack.EMPTY : chest.removeItem(i, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            int i = map(slot);
            return i < 0 ? ItemStack.EMPTY : chest.removeItemNoUpdate(i);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            int i = map(slot);
            if (i >= 0) {
                chest.setItem(i, stack);
            }
        }

        @Override
        public boolean isEmpty() {
            return chest.isEmpty();
        }

        @Override
        public void setChanged() {
            chest.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return chest.stillValid(player);
        }
    }

    private final class WindowSlot extends Slot {
        WindowSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return scroll * Window.COLS + getContainerSlot() < data.get(D_FILTERED);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && com.itmecasey.thoroughchests.ScriptEvents.allowInsert(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
    }

    // ---- Clicks ----

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= filterIndex() && slotId < filterIndex() + 9 && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            ItemStack carried = getCarried();
            filter.setItem(slotId - filterIndex(), carried.isEmpty() || button == 1 ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        if (slotId == augmentIndex() && input == ContainerInput.PICKUP && chest != null) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                if (carried.getItem() instanceof AugmentItem a && AugmentRules.canInstall(installed(), a.installed())) {
                    boolean upgrade = AugmentRules.isUpgrade(installed(), a.installed());
                    augment.setItem(0, carried.copyWithCount(1));
                    carried.shrink(1);
                    chest.augmentChanged(upgrade);
                }
                return;
            }
            super.clicked(slotId, button, input, player);
            chest.augmentChanged(false);
            return;
        }
        if (slotId == lockIndex() && chest != null && input != ContainerInput.QUICK_MOVE) {
            boolean had = chest.hasLock();
            super.clicked(slotId, button, input, player);
            syncLock(player, had);
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    private void syncLock(Player player, boolean had) {
        boolean has = !lock.getItem(0).isEmpty();
        if (has && !had) {
            chest.lockInstalled(player);
        } else if (!has && had) {
            chest.lockRemoved();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (chest == null || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index >= filterIndex() && index < playerIndex()) {
            return ItemStack.EMPTY;
        }
        if (index < playerIndex()) {
            boolean wasLock = index == lockIndex();
            if (wasLock && !slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, playerIndex(), slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (index == augmentIndex()) {
                chest.augmentChanged(false);
            } else if (wasLock) {
                chest.lockRemoved();
            }
            return copy;
        }
        if (stack.getItem() instanceof AugmentItem || stack.getItem() instanceof LockItem) {
            int before = stack.getCount();
            boolean ok = chest.installFrom(stack, player);
            if (ok) {
                slot.setChanged();
                return copy.copyWithCount(before - stack.getCount() == 0 ? before : 1);
            }
            return ItemStack.EMPTY;
        }
        ItemStack rest = chest.insertStack(stack, false);
        if (rest.getCount() == stack.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.setByPlayer(rest);
        return copy;
    }

    // ---- Buttons ----

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (chest == null) {
            return false;
        }
        if (id >= BTN_CHAR) {
            query = query + new String(Character.toChars(id - BTN_CHAR));
            dirty = true;
            scroll = 0;
            return true;
        }
        if (id >= BTN_SCROLL) {
            scroll = Window.clampScroll(id - BTN_SCROLL, filtered.size(), rows);
            return true;
        }
        if (id >= BTN_ROWS && id < BTN_ROWS + 100) {
            rememberRows(player, id - BTN_ROWS);
            if (player instanceof ServerPlayer sp) {
                int newRows = rowsFor(player, chest);
                if (newRows != rows) {
                    sp.openMenu(new SimpleMenuProvider((cid, inv, p) -> new TcChestMenu(ModContent.menu(newRows), newRows, cid, inv, chest), chest.displayName()));
                }
            }
            return true;
        }
        switch (id) {
            case BTN_SORT -> chest.sortItems();
            case BTN_STACK -> chest.quickStack(player);
            case BTN_TOGGLE -> chest.setAugmentEnabled(!chest.augmentEnabled());
            case BTN_REDSTONE -> chest.setRedstone(chest.redstone().next());
            case BTN_FILTER_MODE -> chest.setFilterWhitelist(!chest.filterWhitelist());
            case BTN_CLEAR_LINK -> clearLink();
            case BTN_CLEAR_QUERY -> {
                query = "";
                dirty = true;
                scroll = 0;
            }
            case BTN_BACKSPACE -> {
                if (!query.isEmpty()) {
                    query = query.substring(0, query.offsetByCodePoints(query.length(), -1));
                    dirty = true;
                    scroll = 0;
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void clearLink() {
        ItemStack stack = chest.augmentStack();
        if (stack.getItem() instanceof AugmentItem a && a.augment() == Augment.VOID) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            chest.setChanged();
        }
    }

    public String query() {
        return query;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (chest != null) {
            chest.stopOpen(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return chest == null || (!chest.isRemoved() && chest.stillValid(player) && chest.mayAccess(player));
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byOrdinal(data.get(D_REDSTONE));
    }
}
