package com.itmecasey.thoroughchests.menu;

import com.itmecasey.thoroughchests.GuiFit;
import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.ScriptEvents;
import com.itmecasey.thoroughchests.Settings;
import com.itmecasey.thoroughchests.Window;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The chest menu: a window of {@code rows * 9} slots over the chest and the player inventory. The server keeps the mapping from window
 * slot to chest slot (scroll and search), so every click, shift-click and drag is resolved against the real chest. Scroll, search and the
 * window size arrive as menu button ids.
 */
public class TcChestMenu extends AbstractContainerMenu {
    public static final int X = 13;
    public static final int WIDTH = 188;
    public static final int BTN_CLEAR_QUERY = 7;
    public static final int BTN_BACKSPACE = 8;
    public static final int BTN_ROWS = 200;
    public static final int BTN_SCROLL = 1000;
    public static final int BTN_CHAR = 2000000;
    public static final int D_FILTERED = 0;
    public static final int D_USED = 1;
    public static final int D_SLOTS = 2;
    public static final int D_COUNT = 3;

    private static final Map<UUID, Integer> PREFERRED_ROWS = new HashMap<>();

    private final int rows;
    @Nullable
    private final TcChestBlockEntity chest;
    private final ContainerData data;
    private final Container window;
    private List<Integer> filtered = new ArrayList<>();
    private String query = "";
    private int scroll;
    private int lastVersion = -1;
    private boolean dirty = true;

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
        this.window = chest == null ? new SimpleContainer(rows * Window.COLS) : new WindowContainer();
        this.data = chest == null ? new SimpleContainerData(D_COUNT) : serverData(chest);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < Window.COLS; c++) {
                addSlot(new WindowSlot(window, r * Window.COLS + c, X + c * 18, GuiFit.HEAD + r * 18));
            }
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

    public int windowSlots() {
        return rows * Window.COLS;
    }

    public int playerIndex() {
        return windowSlots();
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
            return isActive() && ScriptEvents.allowInsert(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
    }

    // ---- Clicks ----

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
        if (index < playerIndex()) {
            if (!moveItemStackTo(stack, playerIndex(), slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return copy;
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
}
