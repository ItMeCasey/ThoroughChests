package com.itmecasey.thoroughchests.dev;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.Dev;
import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import com.itmecasey.thoroughchests.menu.TcChestMenu;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only camera tour: opens a fresh superflat world, builds the chests, and saves screenshots of every level, every installed
 * augment and the chest screen into {@code run/screenshots} as {@code thoroughchests_<name>.png}, then quits. Does nothing unless the
 * environment variable {@code THOROUGHCHESTS_SHOTS} is set. It never touches the real mouse or keyboard: it calls the game's own
 * handlers (screen scrolling and character input, container clicks) from inside the game.
 */
public final class ShotTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("ThoroughChests");
    private static final boolean ENABLED = System.getenv("THOROUGHCHESTS_SHOTS") != null;
    private static final String WORLD = "thoroughchests_shots";

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    private record Step(int delay, Runnable action) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    private static int ground;
    private static boolean pauseOnLostFocus;

    private ShotTour() {
    }

    public static void tick() {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
        try {
            switch (stage) {
                case WAITING_FOR_MENU -> waitForMenu(minecraft);
                case LOADING_WORLD -> waitForWorld(minecraft);
                case TOURING -> runSteps();
                default -> {
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("THOROUGHCHESTS-SHOTS failed", e);
            finish();
        }
    }

    private static void finish() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
        LOGGER.info("THOROUGHCHESTS-SHOTS DONE");
        stage = Stage.DONE;
        minecraft.stop();
    }

    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.getOverlay() != null || minecraft.level != null || minecraft.screen == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                LOGGER.warn("THOROUGHCHESTS-SHOTS could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        LOGGER.info("THOROUGHCHESTS-SHOTS creating world");
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                WorldPresets::createFlatWorldDimensions, new TitleScreen());
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
                || minecraft.getSingleplayerServer() == null || ++idle < 40) {
            return;
        }
        ground = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        LOGGER.info("THOROUGHCHESTS-SHOTS in the world, ground at y={}", ground);
        plan();
        stage = Stage.TOURING;
    }

    private static void runSteps() {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    // ---- The tour ----

    private static TcChestBlockEntity chestAt(MinecraftServer server, BlockPos pos) {
        return (TcChestBlockEntity) server.overworld().getBlockEntity(pos);
    }

    private static void setLid(BlockPos pos, boolean open) {
        onServer(server -> {
            ServerLevel level = server.overworld();
            level.blockEvent(pos, level.getBlockState(pos).getBlock(), 1, open ? 1 : 0);
        });
    }

    private static void install(BlockPos pos, Augment a, int lv) {
        onServer(server -> {
            TcChestBlockEntity be = chestAt(server, pos);
            be.augmentSlot.setItem(0, new ItemStack(ModContent.augmentItem(a, lv)));
            be.augmentChanged(false);
        });
    }

    private static void plan() {
        int y = ground;
        then(0, () -> {
            server("gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule spawn_monsters false",
                    "difficulty peaceful", "time set noon", "weather clear", "kill @e[type=!player,type=!item]", "clear @p");
            player().getAbilities().flying = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        // A floor and the five levels in a row, front to the north (the camera stands north of them).
        then(4, () -> {
            server("fill -4 " + (y - 1) + " -8 24 " + (y - 1) + " 6 minecraft:smooth_stone");
            for (ChestLevel lv : ChestLevel.values()) {
                server(String.format(Locale.ROOT, "setblock %d %d 0 thoroughchests:%s_chest[facing=north]", lv.ordinal() * 3, y, lv.id()));
            }
        });
        then(10, () -> onServer(server -> {
            for (ChestLevel lv : ChestLevel.values()) {
                TcChestBlockEntity be = chestAt(server, new BlockPos(lv.ordinal() * 3, y, 0));
                for (int i = 0; i < be.getContainerSize(); i += 2) {
                    be.setItem(i, new ItemStack(i % 3 == 0 ? Items.DIAMOND : i % 3 == 1 ? Items.IRON_INGOT : Items.GOLD_INGOT, 1 + i % 60));
                }
            }
        }));
        then(30, () -> moveTo(new Vec3(6.0, y + 2.6, -6.0), new Vec3(6.0, y + 0.6, 0.5)));
        shoot("levels_closed", 30);
        for (ChestLevel lv : ChestLevel.values()) {
            int x = lv.ordinal() * 3;
            then(2, () -> setLid(new BlockPos(x, y, 0), true));
            then(2, () -> moveTo(new Vec3(x + 0.5, y + 1.6, -2.6), new Vec3(x + 0.5, y + 0.5, 0.5)));
            shoot("level_" + lv.id() + "_open", 25);
            then(2, () -> setLid(new BlockPos(x, y, 0), false));
            shoot("level_" + lv.id() + "_closed", 25);
        }
        // Every augment on a chest of its own, closed, close up; one of each kind also open.
        then(2, () -> server("fill -4 " + y + " -8 24 " + (y + 4) + " 6 minecraft:air"));
        then(6, () -> {
            int n = 0;
            for (Augment a : Augment.values()) {
                for (int lv = 1; lv <= a.maxLevel(); lv++) {
                    server(String.format(Locale.ROOT, "setblock %d %d 12 thoroughchests:gold_chest[facing=north]", n * 2, y));
                    n++;
                }
            }
        });
        then(10, () -> {
            int n = 0;
            for (Augment a : Augment.values()) {
                for (int lv = 1; lv <= a.maxLevel(); lv++) {
                    install(new BlockPos(n * 2, y, 12), a, lv);
                    n++;
                }
            }
        });
        int i = 0;
        for (Augment a : Augment.values()) {
            for (int lv = 1; lv <= a.maxLevel(); lv++) {
                int x = i * 2;
                String name = a.itemName(lv);
                then(8, () -> moveTo(new Vec3(x + 0.5, y + 1.0, 9.6), new Vec3(x + 0.5, y + 0.55, 12.5)));
                shoot("overlay_" + name, 14);
                if (lv == a.maxLevel()) {
                    then(2, () -> setLid(new BlockPos(x, y, 12), true));
                    shoot("overlay_" + name + "_lid_open", 25);
                    then(2, () -> setLid(new BlockPos(x, y, 12), false));
                }
                i++;
            }
        }
        // Overview of all overlays.
        then(2, () -> moveTo(new Vec3(14.0, y + 2.0, 6.0), new Vec3(14.0, y + 0.6, 12.5)));
        shoot("overlays_overview", 30);

        // The screen.
        BlockPos big = new BlockPos(0, y, 20);
        then(2, () -> server(String.format(Locale.ROOT, "setblock 0 %d 20 thoroughchests:netherite_chest[facing=north]", y)));
        then(10, () -> onServer(server -> {
            TcChestBlockEntity be = chestAt(server, big);
            net.minecraft.world.item.Item[] pool = {Items.DIAMOND, Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD, Items.COAL, Items.REDSTONE, Items.OAK_LOG,
                    Items.COBBLESTONE, Items.APPLE, Items.BREAD, Items.COPPER_INGOT, Items.LAPIS_LAZULI, Items.QUARTZ, Items.GLASS, Items.SAND};
            for (int n = 0; n < be.getContainerSize(); n++) {
                if (n % 4 != 3) {
                    be.setItem(n, new ItemStack(pool[n % pool.length], 1 + (n * 7) % 64));
                }
            }
        }));
        then(10, () -> moveTo(new Vec3(0.5, y + 1.0, 18.0), new Vec3(0.5, y + 0.5, 20.5)));
        then(5, () -> open(big));
        shoot("gui_netherite", 40);
        then(2, () -> scroll(-4));
        shoot("gui_scrolled", 25);
        then(2, () -> typeIntoScreen("dia"));
        shoot("gui_search_dia", 30);
        then(2, () -> close());
        for (Augment a : Augment.values()) {
            then(4, () -> install(big, a, a.maxLevel()));
            then(6, () -> open(big));
            shoot("gui_augment_" + a.id(), 30);
            then(2, () -> close());
        }
        then(4, () -> onServer(server -> {
            TcChestBlockEntity be = chestAt(server, big);
            be.lockInstalled(server.getPlayerList().getPlayers().get(0));
            be.lockSlot.setItem(0, new ItemStack(ModContent.LOCK));
        }));
        then(6, () -> open(big));
        shoot("gui_locked", 30);
        then(2, () -> close());
        // Fitting: the same chest at other GUI scales.
        then(6, () -> open(big));
        for (int sc : new int[] {1, 2, 4, 5}) {
            then(10, () -> {
                Minecraft minecraft = Minecraft.getInstance();
                var window = minecraft.getWindow();
                window.setGuiScale(window.calculateScale(sc, minecraft.isEnforceUnicode()));
                if (minecraft.screen != null) {
                    minecraft.screen.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
                }
            });
            shoot("gui_scale_" + sc, 60);
        }
        then(2, () -> close());
        then(20, ShotTour::finish);
    }

    private static void scroll(int rows) {
        Minecraft minecraft = Minecraft.getInstance();
        var screen = minecraft.screen;
        if (screen != null) {
            for (int i = 0; i < Math.abs(rows); i++) {
                screen.mouseScrolled(screen.width / 2.0, screen.height / 2.0 - 30, 0.0, rows > 0 ? 1.0 : -1.0);
            }
        }
    }

    private static void open(BlockPos pos) {
        onServer(server -> {
            ServerPlayer p = server.getPlayerList().getPlayers().get(0);
            ServerLevel level = server.overworld();
            var result = level.getBlockState(pos).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
            LOGGER.info("THOROUGHCHESTS-SHOTS open {} result={} menu={}", pos, result, p.containerMenu.getClass().getSimpleName());
        });
    }

    private static void close() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            minecraft.screen.onClose();
        }
    }

    private static void typeIntoScreen(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            for (char c : text.toCharArray()) {
                minecraft.screen.charTyped(new CharacterEvent(c));
            }
        }
    }

    static void moveTo(Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        server(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
        LocalPlayer player = player();
        player.snapTo(eye.x, eye.y - 1.62, eye.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    static void shoot(String name, int delay) {
        then(delay, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.getChat().clearMessages(false);
            minecraft.getToastManager().clear();
        });
        then(2, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LOGGER.info("THOROUGHCHESTS-SHOTS {}", name);
            Screenshot.grab(minecraft.gameDirectory, "thoroughchests_" + name + ".png", minecraft.getMainRenderTarget(), 1,
                    message -> LOGGER.info("THOROUGHCHESTS-SHOTS saved {}", name));
        });
    }

    static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    static void onServer(java.util.function.Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> action.accept(server));
        }
    }

    static void server(String... commands) {
        onServer(server -> {
            for (String command : commands) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        });
    }

    static {
        if (Dev.ENABLED && ENABLED) {
            LOGGER.info("THOROUGHCHESTS-SHOTS enabled");
        }
    }
}
