package com.itmecasey.thoroughchests.client;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.GuiFit;
import com.itmecasey.thoroughchests.RedstoneMode;
import com.itmecasey.thoroughchests.Window;
import com.itmecasey.thoroughchests.menu.TcChestMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The chest screen. It draws the vanilla look with plain fills, fits itself to the window (the menu is reopened with fewer or more rows when
 * the screen is smaller or taller), and holds the search box, scrollbar, sort and quick-stack buttons and the augment controls.
 */
public class TcChestScreen extends AbstractContainerScreen<TcChestMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int TEXT = 0xFF404040;
    private static final int TRACK_X = TcChestMenu.X + 162 + 4;
    private static final int TRACK_W = 6;
    private static long lastRequest;

    private EditBox search;
    private Button toggle;
    private Button redstone;
    private Button mode;
    private Button clearLink;
    private String sent = "";
    private boolean fitChecked;
    private boolean draggingScroll;
    private int lastScrollSent = -1;

    public TcChestScreen(TcChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TcChestMenu.WIDTH, GuiFit.height(menu.rows()));
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = TcChestMenu.X;
        this.inventoryLabelY = GuiFit.HEAD + menu.rows() * GuiFit.ROW + GuiFit.TOOLS + 3;
    }

    @Override
    protected void init() {
        super.init();
        int rows = menu.rows();
        search = new EditBox(font, leftPos + TcChestMenu.X, topPos + 18, 96, 12, Component.translatable("gui.thoroughchests.search"));
        search.setMaxLength(40);
        search.setResponder(this::searchChanged);
        addRenderableWidget(search);
        setInitialFocus(search);
        addRenderableWidget(Button.builder(Component.translatable("gui.thoroughchests.sort"), b -> press(TcChestMenu.BTN_SORT))
                .bounds(leftPos + TcChestMenu.X + 98, topPos + 17, 30, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.thoroughchests.stack"), b -> press(TcChestMenu.BTN_STACK))
                .bounds(leftPos + TcChestMenu.X + 130, topPos + 17, 32, 14).build());
        int ty = topPos + GuiFit.HEAD + rows * GuiFit.ROW + 2;
        clearLink = addRenderableWidget(Button.builder(Component.literal("x"), b -> press(TcChestMenu.BTN_CLEAR_LINK))
                .bounds(leftPos + TcChestMenu.X + 42, ty + 2, 12, 14).build());
        toggle = addRenderableWidget(Button.builder(Component.empty(), b -> press(TcChestMenu.BTN_TOGGLE))
                .bounds(leftPos + TcChestMenu.X + 56, ty + 2, 28, 14).build());
        redstone = addRenderableWidget(Button.builder(Component.empty(), b -> press(TcChestMenu.BTN_REDSTONE))
                .bounds(leftPos + TcChestMenu.X + 86, ty + 2, 44, 14).build());
        mode = addRenderableWidget(Button.builder(Component.empty(), b -> press(TcChestMenu.BTN_FILTER_MODE))
                .bounds(leftPos + TcChestMenu.X + 132, ty + 2, 30, 14).build());
        updateButtons();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void updateButtons() {
        boolean has = menu.augmentKind() != null;
        toggle.active = has;
        redstone.active = has;
        mode.active = has && menu.augmentKind().usesFilter();
        toggle.setMessage(Component.translatable(menu.value(TcChestMenu.D_ENABLED) != 0 ? "gui.thoroughchests.on" : "gui.thoroughchests.off"));
        RedstoneMode rm = menu.redstoneMode();
        redstone.setMessage(Component.translatable("gui.thoroughchests.redstone." + rm.name().toLowerCase(java.util.Locale.ROOT)));
        mode.setMessage(Component.translatable(menu.value(TcChestMenu.D_WHITELIST) != 0 ? "gui.thoroughchests.allow" : "gui.thoroughchests.deny"));
        clearLink.visible = menu.augmentKind() == Augment.VOID;
    }

    private void searchChanged(String text) {
        if (text.equals(sent)) {
            return;
        }
        if (text.startsWith(sent)) {
            text.substring(sent.length()).codePoints().forEach(cp -> press(TcChestMenu.BTN_CHAR + cp));
        } else if (sent.startsWith(text)) {
            int back = sent.codePointCount(text.length(), sent.length());
            for (int i = 0; i < back; i++) {
                press(TcChestMenu.BTN_BACKSPACE);
            }
        } else {
            press(TcChestMenu.BTN_CLEAR_QUERY);
            text.codePoints().forEach(cp -> press(TcChestMenu.BTN_CHAR + cp));
        }
        sent = text;
        lastScrollSent = -1;
        menu.setScroll(0);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
        int slots = menu.value(TcChestMenu.D_SLOTS);
        if (!fitChecked && slots > 0) {
            fitChecked = true;
            int needed = Math.max(1, (slots + Window.COLS - 1) / Window.COLS);
            int fit = GuiFit.rows(height, GuiFit.MAX_ROWS);
            int desired = Math.min(needed, fit);
            long now = System.currentTimeMillis();
            if (desired != menu.rows() && now - lastRequest > 1000) {
                lastRequest = now;
                press(TcChestMenu.BTN_ROWS + fit);
            }
        }
    }

    // ---- Scrolling ----

    private int trackX() {
        return leftPos + TRACK_X;
    }

    private int trackY() {
        return topPos + GuiFit.HEAD;
    }

    private int trackH() {
        return menu.rows() * GuiFit.ROW;
    }

    private void scrollTo(int row) {
        row = Math.max(0, Math.min(menu.maxScroll(), row));
        if (row != menu.scroll() || row != lastScrollSent) {
            lastScrollSent = row;
            menu.setScroll(row);
            press(TcChestMenu.BTN_SCROLL + row);
        }
    }

    private void scrollFromMouse(double mouseY) {
        int total = menu.rows() + menu.maxScroll();
        int handle = Math.max(14, trackH() * menu.rows() / Math.max(1, total));
        double frac = (mouseY - trackY() - handle / 2.0) / Math.max(1, trackH() - handle);
        scrollTo((int) Math.round(Math.max(0, Math.min(1, frac)) * menu.maxScroll()));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x >= leftPos && x < leftPos + imageWidth && y >= topPos + GuiFit.HEAD && y < topPos + GuiFit.HEAD + trackH()) {
            scrollTo(menu.scroll() + (scrollY > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (menu.maxScroll() > 0 && event.x() >= trackX() && event.x() < trackX() + TRACK_W && event.y() >= trackY() && event.y() < trackY() + trackH()) {
            draggingScroll = true;
            scrollFromMouse(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScroll) {
            scrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    // ---- Drawing ----

    private static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, LIGHT);
        g.fill(x + 3, y + 3, x + w - 1, y + h - 1, DARK);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
    }

    private static void slotBox(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        g.fill(x, y, x + 18, y + 18, LIGHT);
        g.fill(x, y, x + 17, y + 17, SLOT_BG);
        g.fill(x - 1, y - 1, x + 17, y, SLOT_EDGE);
        g.fill(x - 1, y - 1, x, y + 17, SLOT_EDGE);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        bevel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) {
            if (s.isActive()) {
                slotBox(g, leftPos + s.x, topPos + s.y);
            }
        }
        // Scrollbar
        int tx = trackX();
        int ty = trackY();
        int th = trackH();
        g.fill(tx, ty, tx + TRACK_W, ty + th, SLOT_EDGE);
        g.fill(tx + 1, ty + 1, tx + TRACK_W, ty + th, LIGHT);
        g.fill(tx + 1, ty + 1, tx + TRACK_W - 1, ty + th - 1, SLOT_BG);
        int total = menu.rows() + menu.maxScroll();
        int handle = Math.max(14, th * menu.rows() / Math.max(1, total));
        int hy = menu.maxScroll() == 0 ? ty : ty + (th - handle) * menu.scroll() / menu.maxScroll();
        g.fill(tx, hy, tx + TRACK_W, hy + handle, 0xFF000000);
        g.fill(tx + 1, hy + 1, tx + TRACK_W - 1, hy + handle - 1, PANEL);
        g.fill(tx + 1, hy + 1, tx + TRACK_W - 1, hy + 2, LIGHT);
        g.fill(tx + 1, hy + handle - 2, tx + TRACK_W - 1, hy + handle - 1, DARK);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        Component used = Component.translatable("gui.thoroughchests.used", menu.value(TcChestMenu.D_USED), menu.value(TcChestMenu.D_SLOTS));
        g.text(font, used, imageWidth - 8 - font.width(used), titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mx, int my) {
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            int i = hoveredSlot.index;
            String key = i == menu.lockIndex() ? "lock" : i == menu.augmentIndex() ? "augment" : i >= menu.filterIndex() && i < menu.playerIndex() ? "filter" : null;
            if (key != null) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("gui.thoroughchests.slot." + key));
                g.setComponentTooltipForNextFrame(font, lines, mx, my);
                return;
            }
        }
        super.extractTooltip(g, mx, my);
    }
}
