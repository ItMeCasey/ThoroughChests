package com.itmecasey.thoroughchests.dev;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.Dev;
import com.itmecasey.thoroughchests.ModContent;
import com.itmecasey.thoroughchests.RedstoneMode;
import com.itmecasey.thoroughchests.ScriptEvents;
import com.itmecasey.thoroughchests.Settings;
import com.itmecasey.thoroughchests.augment.Augments;
import com.itmecasey.thoroughchests.block.TcChestBlock;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import com.itmecasey.thoroughchests.menu.TcChestMenu;
import com.itmecasey.thoroughchests.script.ChestsApi;
import com.itmecasey.thoroughchests.script.ScriptBridge;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check of the whole mod on a real server: every chest level, slot counts, comparator, saving and loading, upgrade cards, locks,
 * explosions, hopper access, each augment and level, filters, redstone mode, the menu (scroll, search, clicks) and the
 * scripting API without ModJS. Runs once at server start when {@code THOROUGHCHESTS_SELFTEST} is set and prints {@code SELFTEST PASS/FAIL}
 * lines ending in {@code SELFTEST DONE failures=N}. It ticks block entities by hand, so it needs no running game loop.
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("ThoroughChests");
    private static ServerLevel level;
    private static ServerPlayer owner;
    private static ServerPlayer stranger;
    private static int failures;

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("THOROUGHCHESTS_SELFTEST") != null && Dev.ENABLED;
    }

    public static int run(MinecraftServer server) {
        level = server.overworld();
        failures = 0;
        owner = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "selftest_owner"), ClientInformation.createDefault());
        stranger = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "selftest_other"), ClientInformation.createDefault());
        BlockPos spawn = level.getRespawnData().pos();
        int x = ((spawn.getX() >> 4) << 4) + 4;
        int z = ((spawn.getZ() >> 4) << 4) + 4;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 25;
        try {
            Settings.reset();
            levelsAndSlots(new BlockPos(x, y, z));
            comparator(new BlockPos(x, y, z + 4));
            persistence(new BlockPos(x, y, z + 8));
            upgrades(new BlockPos(x, y, z + 12));
            locks(new BlockPos(x, y, z + 16));
            installRules(new BlockPos(x, y, z + 20));
            hoppers(new BlockPos(x, y + 6, z));
            compact(new BlockPos(x, y + 6, z + 4));
            furnaces(new BlockPos(x, y + 6, z + 8));
            magnet(new BlockPos(x, y + 12, z));
            feeder(new BlockPos(x, y + 18, z));
            feederRange(new BlockPos(x, y + 24, z));
            voidAugment(new BlockPos(x, y + 30, z));
            menus(new BlockPos(x, y + 36, z));
            scripts(new BlockPos(x, y + 42, z));
            explosions(new BlockPos(x, y + 48, z));
        } catch (RuntimeException e) {
            LOGGER.error("SELFTEST error", e);
            verdict("selftest_ran_clean", false, e.toString());
        } finally {
            Settings.reset();
            ScriptEvents.clear();
        }
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    // ---- helpers ----

    private static void verdict(String name, boolean ok, String detail) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {} ({})", name, detail);
        } else {
            failures++;
            LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        }
    }

    private static void clear(BlockPos from, BlockPos to) {
        for (BlockPos p : BlockPos.betweenClosed(from, to)) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void area(BlockPos p) {
        clear(p.offset(-2, -1, -2), p.offset(14, 3, 3));
    }

    private static TcChestBlockEntity chest(BlockPos pos, ChestLevel lv) {
        level.setBlock(pos, ModContent.chest(lv).defaultBlockState(), 3);
        return (TcChestBlockEntity) level.getBlockEntity(pos);
    }

    private static void tick(TcChestBlockEntity be, int n) {
        for (int i = 0; i < n; i++) {
            TcChestBlockEntity.serverTick(level, be.getBlockPos(), level.getBlockState(be.getBlockPos()), be);
        }
    }

    private static void augment(TcChestBlockEntity be, Augment a, int lv) {
        be.augmentSlot.setItem(0, new ItemStack(ModContent.augmentItem(a, lv)));
        be.augmentChanged(false);
        be.setAugmentEnabled(true);
    }

    private static void fill(TcChestBlockEntity be, net.minecraft.world.item.Item item) {
        for (int i = 0; i < be.getContainerSize(); i++) {
            be.setItem(i, new ItemStack(item, 64));
        }
    }

    private static int count(TcChestBlockEntity be, net.minecraft.world.item.Item item) {
        return be.countOf(new ItemStack(item));
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int n) {
        return new ItemStack(item, n);
    }

    // ---- tests ----

    private static void levelsAndSlots(BlockPos p) {
        area(p);
        int[] expected = {36, 54, 72, 108, 135};
        float last = -1;
        for (ChestLevel lv : ChestLevel.values()) {
            TcChestBlockEntity be = chest(p.offset(lv.ordinal() * 3, 0, 0), lv);
            verdict("slots_" + lv.id(), be.getContainerSize() == expected[lv.ordinal()], "size=" + be.getContainerSize());
            float resistance = ModContent.chest(lv).getExplosionResistance();
            verdict("resistance_rises_" + lv.id(), resistance > last, resistance + " > " + last);
            last = resistance;
            verdict("open_counter_starts_closed_" + lv.id(), be.openerCount() == 0, "");
        }
        verdict("netherite_is_immune_by_setting", ModContent.chest(ChestLevel.NETHERITE).getExplosionResistance() >= ChestLevel.IMMUNE, "");
        Settings.set("level.gold.slots", 90);
        TcChestBlockEntity gold = chest(p.offset(0, 0, 2), ChestLevel.GOLD);
        verdict("slot_count_follows_config", gold.getContainerSize() == 90, "size=" + gold.getContainerSize());
        Settings.reset();
        TcChestBlockEntity c = chest(p.offset(0, 0, 4), ChestLevel.COPPER);
        verdict("insert_merges_then_fills", c.insertStack(stack(Items.DIRT, 100), false).isEmpty() && c.getItem(0).getCount() == 64 && c.getItem(1).getCount() == 36, "");
        verdict("insert_into_full_chest_returns_the_rest", fillAndTry(c) == 5, "");
        verdict("lid_closed_without_openers", c.getOpenNess(0.0F) == 0.0F, "");
        clear(p.offset(-1, -1, -1), p.offset(14, 3, 5));
    }

    private static int fillAndTry(TcChestBlockEntity c) {
        fill(c, Items.DIRT);
        return c.insertStack(stack(Items.STONE, 5), false).getCount();
    }

    private static void comparator(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.NETHERITE);
        verdict("comparator_empty_is_zero", be.comparatorSignal() == 0, "");
        be.setItem(134, stack(Items.DIRT, 1));
        verdict("comparator_counts_the_last_slot", be.comparatorSignal() == 1, "signal=" + be.comparatorSignal());
        for (int i = 0; i < 67; i++) {
            be.setItem(i, stack(Items.DIRT, 64));
        }
        int half = be.comparatorSignal();
        verdict("comparator_half_full_is_about_half", half >= 7 && half <= 9, "signal=" + half);
        fill(be, Items.DIRT);
        verdict("comparator_full_is_fifteen", be.comparatorSignal() == 15, "signal=" + be.comparatorSignal());
        verdict("block_reports_the_signal", level.getBlockState(p).getAnalogOutputSignal(level, p, Direction.NORTH) == 15, "");
    }

    private static void persistence(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.NETHERITE);
        be.setItem(0, stack(Items.DIAMOND, 7));
        be.setItem(77, stack(Items.IRON_INGOT, 33));
        be.setItem(134, stack(Items.GOLD_INGOT, 2));
        augment(be, Augment.FURNACE, 2);
        be.setRedstone(RedstoneMode.WHEN_POWERED);
        be.setFilterWhitelist(true);
        be.filterSlots.setItem(3, stack(Items.COAL, 1));
        be.lockInstalled(owner);
        be.lockSlot.setItem(0, stack(ModContent.LOCK, 1));
        be.sim.addFuel(500);
        CompoundTag saved = be.saveWithoutMetadata(level.registryAccess());
        Settings.set("level.netherite.slots", 36);
        TcChestBlockEntity copy = new TcChestBlockEntity(p, level.getBlockState(p));
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        verdict("save_keeps_every_slot", copy.getItem(0).getCount() == 7 && copy.getItem(77).getCount() == 33 && copy.getItem(134).is(Items.GOLD_INGOT), "");
        verdict("load_never_shrinks_below_stored_items", copy.getContainerSize() >= 135, "size=" + copy.getContainerSize());
        verdict("save_keeps_augment", copy.augment() == Augment.FURNACE && copy.augmentLevel() == 2, "");
        verdict("save_keeps_settings", copy.redstone() == RedstoneMode.WHEN_POWERED && copy.filterWhitelist() && copy.filterSlots.getItem(3).is(Items.COAL), "");
        verdict("save_keeps_owner", copy.hasLock() && "selftest_owner".equals(copy.ownerName()) && owner.getStringUUID().equals(copy.ownerId()), "");
        verdict("save_keeps_furnace_bank", Math.abs(copy.sim.burnBank() - 500) < 1e-6, "bank=" + copy.sim.burnBank());
        Settings.reset();
        // The block entity also survives being rebuilt from the world's own chunk data path.
        BlockEntityRoundTrip.check(be, level);
    }

    /** Small holder so the chunk-style round trip has its own name in the log. */
    private static final class BlockEntityRoundTrip {
        static void check(TcChestBlockEntity be, ServerLevel level) {
            CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
            var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), tag, level.registryAccess());
            boolean ok = loaded instanceof TcChestBlockEntity c && c.getItem(77).getCount() == 33 && c.augment() == Augment.FURNACE;
            verdict("world_save_and_load_round_trip", ok, String.valueOf(loaded));
        }
    }

    private static InteractionResult useCard(BlockPos pos, ChestLevel card, ServerPlayer who) {
        ItemStack stack = new ItemStack(ModContent.card(card));
        who.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return stack.getItem().useOn(new UseOnContext(level, who, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static void upgrades(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.COPPER);
        be.setItem(35, stack(Items.DIRT, 9));
        be.setItem(0, stack(Items.EMERALD, 3));
        augment(be, Augment.MAGNET, 1);
        be.lockInstalled(owner);
        be.lockSlot.setItem(0, stack(ModContent.LOCK, 1));
        be.filterSlots.setItem(0, stack(Items.IRON_INGOT, 1));
        InteractionResult wrong = useCard(p, ChestLevel.GOLD, owner);
        verdict("card_cannot_skip_a_level", wrong == InteractionResult.FAIL && level.getBlockState(p).getBlock() == ModContent.chest(ChestLevel.COPPER), "result=" + wrong);
        verdict("a_stranger_cannot_upgrade_a_locked_chest", useCard(p, ChestLevel.IRON, stranger) == InteractionResult.FAIL
                && level.getBlockState(p).getBlock() == ModContent.chest(ChestLevel.COPPER), "");
        ChestLevel[] order = ChestLevel.values();
        for (int i = 1; i < order.length; i++) {
            InteractionResult r = useCard(p, order[i], owner);
            TcChestBlockEntity now = (TcChestBlockEntity) level.getBlockEntity(p);
            boolean ok = r == InteractionResult.SUCCESS && level.getBlockState(p).getBlock() == ModContent.chest(order[i]) && now != null
                    && now.getContainerSize() == order[i].slots() && now.getItem(35).getCount() == 9 && now.getItem(0).is(Items.EMERALD)
                    && now.augment() == Augment.MAGNET && now.hasLock() && now.filterSlots.getItem(0).is(Items.IRON_INGOT);
            verdict("card_upgrades_to_" + order[i].id() + "_keeping_everything", ok, "result=" + r + " size=" + (now == null ? -1 : now.getContainerSize())
                    + " dirt=" + (now == null ? "" : now.getItem(35)) + " emerald=" + (now == null ? "" : now.getItem(0)) + " aug=" + (now == null ? "" : now.augment())
                    + " lock=" + (now != null && now.hasLock()) + " filter=" + (now == null ? "" : now.filterSlots.getItem(0)));
        }
        verdict("netherite_card_does_nothing_on_netherite", useCard(p, ChestLevel.NETHERITE, owner) == InteractionResult.FAIL, "");
        BlockPos q = p.offset(4, 0, 0);
        chest(q, ChestLevel.IRON).setItem(0, stack(Items.DIRT, 1));
        verdict("card_is_consumed_by_survival_players_only", useCard(q, ChestLevel.GOLD, owner) == InteractionResult.SUCCESS, "");
        ItemStack held = owner.getItemInHand(InteractionHand.MAIN_HAND);
        verdict("card_stack_shrinks", held.isEmpty(), "left=" + held.getCount());
    }

    private static void locks(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.GOLD);
        verdict("unlocked_chest_is_open_to_everyone", be.mayAccess(stranger) && be.mayAccess(owner), "");
        ItemStack lock = stack(ModContent.LOCK, 2);
        verdict("lock_installs", be.installLock(lock, owner) && be.hasLock() && lock.getCount() == 1, "");
        verdict("owner_name_is_stored", "selftest_owner".equals(be.ownerName()), be.ownerName());
        verdict("owner_may_access", be.mayAccess(owner), "");
        verdict("stranger_may_not_access", !be.mayAccess(stranger), "");
        verdict("stranger_cannot_mine_it", level.getBlockState(p).getDestroyProgress(stranger, level, p) == 0.0F, "");
        verdict("owner_can_mine_it", level.getBlockState(p).getDestroyProgress(owner, level, p) > 0.0F, "");
        verdict("second_lock_is_refused", !be.installLock(stack(ModContent.LOCK, 1), owner), "");
        Settings.set("lock.block_automation", true);
        verdict("automation_can_be_blocked_for_locked_chests", be.insertFromAutomation(stack(Items.DIRT, 5), false).getCount() == 5, "");
        Settings.reset();
        verdict("automation_works_by_default", be.insertFromAutomation(stack(Items.DIRT, 5), false).isEmpty(), "");
        be.lockRemoved();
        verdict("lock_removal_opens_the_chest", !be.hasLock() && be.mayAccess(stranger), "");
    }

    private static void installRules(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.DIAMOND);
        owner.getAbilities().instabuild = false;
        ItemStack l1 = new ItemStack(ModContent.augmentItem(Augment.FURNACE, 1), 3);
        ItemStack l3 = new ItemStack(ModContent.augmentItem(Augment.FURNACE, 3), 1);
        ItemStack l2 = new ItemStack(ModContent.augmentItem(Augment.FURNACE, 2), 1);
        verdict("level_three_needs_level_two", !be.installFrom(l3, owner) && be.augment() == null, "");
        verdict("level_two_needs_level_one", !be.installFrom(l2, owner) && be.augment() == null, "");
        verdict("level_one_installs", be.installFrom(l1, owner) && be.augment() == Augment.FURNACE && be.augmentLevel() == 1 && l1.getCount() == 2, "");
        verdict("level_one_twice_is_refused", !be.installFrom(l1, owner), "");
        ItemStack blast = new ItemStack(ModContent.augmentItem(Augment.BLAST_FURNACE, 1), 1);
        verdict("furnace_and_blast_furnace_never_mix", !be.installFrom(blast, owner), "");
        verdict("level_three_still_needs_two", !be.installFrom(l3, owner), "");
        verdict("level_two_upgrades_and_consumes_the_old_one", be.installFrom(l2, owner) && be.augmentLevel() == 2 && be.augmentSlot.getItem(0).getCount() == 1, "");
        verdict("level_three_upgrades", be.installFrom(l3, owner) && be.augmentLevel() == 3, "");
        for (Augment a : Augment.values()) {
            verdict("every_augment_has_an_item_" + a.id(), ModContent.augmentItem(a, 1) != null && ModContent.augmentItem(a, a.maxLevel()) != null, "");
        }
        verdict("compact_and_void_have_one_level", Augment.COMPACT.maxLevel() == 1 && Augment.VOID.maxLevel() == 1 && Augment.FURNACE.maxLevel() == 3, "");
    }

    private static void hoppers(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.DIAMOND);
        verdict("hoppers_see_the_chest_as_a_container", HopperBlockEntity.getContainerAt(level, p) == be, "");
        for (int i = 0; i < 100; i++) {
            be.setItem(i, stack(Items.DIRT, 64));
        }
        ItemStack rest = HopperBlockEntity.addItem(null, be, stack(Items.DIAMOND, 5), null);
        verdict("hopper_insert_reaches_slots_past_one_hundred", rest.isEmpty() && be.getItem(100).is(Items.DIAMOND) && be.getContainerSize() == 108, "slot100=" + be.getItem(100));
        be.setItem(107, stack(Items.EMERALD, 4));
        var out = be.removeItem(107, 2);
        verdict("hopper_extract_reads_the_last_slot", out.getCount() == 2 && be.getItem(107).getCount() == 2, "");
    }

    private static void compact(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.GOLD);
        be.setItem(0, stack(Items.IRON_INGOT, 18));
        be.setItem(1, stack(Items.OAK_PLANKS, 8));
        be.setItem(2, stack(Items.GOLD_INGOT, 5));
        tick(be, 25);
        verdict("compact_off_without_augment", count(be, Items.IRON_BLOCK) == 0, "");
        augment(be, Augment.COMPACT, 1);
        tick(be, 25);
        verdict("compact_turns_ingots_into_blocks", count(be, Items.IRON_BLOCK) == 2 && count(be, Items.IRON_INGOT) == 0, "blocks=" + count(be, Items.IRON_BLOCK));
        verdict("compact_leaves_leftovers", count(be, Items.GOLD_INGOT) == 5, "");
        verdict("compact_skips_recipes_that_cannot_be_crafted_back", count(be, Items.OAK_PLANKS) == 8, "planks=" + count(be, Items.OAK_PLANKS));
        Settings.set("compact.any_recipe", true);
        tick(be, 25);
        verdict("any_recipe_setting_compacts_planks", count(be, Items.OAK_PLANKS) == 0 && count(be, Items.CRAFTING_TABLE) == 2, "tables=" + count(be, Items.CRAFTING_TABLE));
        Settings.reset();
        be.setItem(5, stack(Items.IRON_INGOT, 9));
        be.filterSlots.setItem(0, stack(Items.IRON_INGOT, 1));
        be.setFilterWhitelist(false);
        tick(be, 25);
        verdict("compact_blacklist_protects_items", count(be, Items.IRON_INGOT) == 9, "");
        be.setFilterWhitelist(true);
        tick(be, 25);
        verdict("compact_whitelist_allows_listed_items", count(be, Items.IRON_INGOT) == 0 && count(be, Items.IRON_BLOCK) == 3, "");
        be.setItem(6, stack(Items.IRON_INGOT, 9));
        be.setAugmentEnabled(false);
        tick(be, 25);
        verdict("compact_toggle_off_stops_it", count(be, Items.IRON_INGOT) == 9, "");
        be.setAugmentEnabled(true);
        be.setRedstone(RedstoneMode.WHEN_POWERED);
        tick(be, 25);
        verdict("redstone_mode_powered_waits_without_power", count(be, Items.IRON_INGOT) == 9, "");
        be.setRedstone(RedstoneMode.WHEN_UNPOWERED);
        tick(be, 25);
        verdict("redstone_mode_unpowered_works_without_power", count(be, Items.IRON_INGOT) == 0, "");
        TcChestBlockEntity full = chest(p.offset(4, 0, 0), ChestLevel.COPPER);
        augment(full, Augment.COMPACT, 1);
        for (int i = 0; i < full.getContainerSize(); i++) {
            full.setItem(i, stack(Items.COBBLESTONE, 1));
        }
        full.setItem(0, stack(Items.COAL, 9));
        tick(full, 25);
        verdict("compact_never_loses_items_in_a_full_chest", count(full, Items.COAL) + count(full, Items.COAL_BLOCK) * 9 == 9, "coal=" + count(full, Items.COAL) + " blocks=" + count(full, Items.COAL_BLOCK));
    }

    private static void furnaces(BlockPos p) {
        area(p);
        TcChestBlockEntity f1 = chest(p, ChestLevel.GOLD);
        f1.setItem(0, stack(Items.SAND, 2));
        f1.setItem(1, stack(Items.COAL, 1));
        augment(f1, Augment.FURNACE, 1);
        tick(f1, 150);
        verdict("furnace_level_1_is_not_done_early", count(f1, Items.GLASS) == 0, "stone=" + count(f1, Items.GLASS));
        tick(f1, 80);
        verdict("furnace_level_1_smelts_with_coal", count(f1, Items.GLASS) == 1, "stone=" + count(f1, Items.GLASS));
        tick(f1, 220);
        verdict("furnace_smelts_every_item_and_burns_fuel", count(f1, Items.GLASS) == 2 && count(f1, Items.COAL) == 0 && f1.sim.burnBank() > 1000, "bank=" + f1.sim.burnBank());
        TcChestBlockEntity f3 = chest(p.offset(4, 0, 0), ChestLevel.GOLD);
        f3.setItem(0, stack(Items.SAND, 2));
        f3.setItem(1, stack(Items.COAL, 1));
        augment(f3, Augment.FURNACE, 3);
        tick(f3, 70);
        verdict("furnace_level_3_is_four_times_faster", count(f3, Items.GLASS) == 1, "stone=" + count(f3, Items.GLASS));
        TcChestBlockEntity nofuel = chest(p.offset(8, 0, 0), ChestLevel.GOLD);
        nofuel.setItem(0, stack(Items.SAND, 2));
        augment(nofuel, Augment.FURNACE, 3);
        tick(nofuel, 300);
        verdict("furnace_needs_fuel", count(nofuel, Items.GLASS) == 0 && count(nofuel, Items.SAND) == 2, "");
        TcChestBlockEntity blast = chest(p.offset(12, 0, 0), ChestLevel.GOLD);
        blast.setItem(0, stack(Items.RAW_IRON, 1));
        blast.setItem(1, stack(Items.COAL, 1));
        augment(blast, Augment.BLAST_FURNACE, 3);
        tick(blast, 70);
        verdict("blast_furnace_smelts_ore_at_blast_speed", count(blast, Items.IRON_INGOT) == 1, "ingots=" + count(blast, Items.IRON_INGOT));
        TcChestBlockEntity plain = chest(p.offset(12, 0, 3), ChestLevel.GOLD);
        plain.setItem(0, stack(Items.RAW_IRON, 1));
        plain.setItem(1, stack(Items.COAL, 1));
        augment(plain, Augment.FURNACE, 1);
        tick(plain, 70);
        verdict("furnace_is_slower_than_blast_furnace_for_ore", count(plain, Items.IRON_INGOT) == 0, "");
        TcChestBlockEntity off = chest(p.offset(8, 0, 3), ChestLevel.GOLD);
        off.setItem(0, stack(Items.SAND, 1));
        off.setItem(1, stack(Items.COAL, 1));
        augment(off, Augment.FURNACE, 3);
        off.setRedstone(RedstoneMode.WHEN_POWERED);
        tick(off, 100);
        verdict("furnace_waits_for_redstone", count(off, Items.GLASS) == 0, "");
        off.setRedstone(RedstoneMode.IGNORE);
        tick(off, 70);
        verdict("furnace_runs_when_redstone_allows", count(off, Items.GLASS) == 1, "");
        Settings.set("furnace.speed.1", 10.0);
        TcChestBlockEntity fast = chest(p.offset(4, 0, 3), ChestLevel.GOLD);
        fast.setItem(0, stack(Items.SAND, 1));
        fast.setItem(1, stack(Items.COAL, 1));
        augment(fast, Augment.FURNACE, 1);
        tick(fast, 40);
        verdict("furnace_speed_is_configurable", count(fast, Items.GLASS) == 1, "");
        Settings.reset();
        augment(fast, Augment.COMPACT, 1);
        verdict("removing_a_furnace_resets_its_state", !fast.sim.cooking() && fast.cookInput().isEmpty(), "");
    }

    private static ItemEntity drop(BlockPos at, net.minecraft.world.item.Item item, int n) {
        ItemEntity e = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, stack(item, n));
        e.setNoPickUpDelay();
        e.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(e);
        return e;
    }

    private static void killEntities(BlockPos p) {
        for (net.minecraft.world.entity.Entity e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(p).inflate(60),
                e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            e.discard();
        }
    }

    private static void magnet(BlockPos p) {
        area(p);
        killEntities(p);
        TcChestBlockEntity be = chest(p, ChestLevel.IRON);
        augment(be, Augment.MAGNET, 1);
        ItemEntity near = drop(p.offset(3, 0, 0), Items.DIRT, 5);
        ItemEntity far = drop(p.offset(6, 0, 0), Items.STONE, 3);
        tick(be, 12);
        verdict("magnet_pulls_items_inside_its_radius", !near.isAlive() && count(be, Items.DIRT) == 5, "dirt=" + count(be, Items.DIRT));
        verdict("magnet_level_1_leaves_distant_items", far.isAlive() && count(be, Items.STONE) == 0, "");
        augment(be, Augment.MAGNET, 2);
        tick(be, 12);
        verdict("magnet_level_2_reaches_further", !far.isAlive() && count(be, Items.STONE) == 3, "stone=" + count(be, Items.STONE));
        be.filterSlots.setItem(0, stack(Items.IRON_INGOT, 1));
        be.setFilterWhitelist(true);
        ItemEntity filtered = drop(p.offset(2, 0, 0), Items.GRAVEL, 4);
        tick(be, 12);
        verdict("magnet_whitelist_ignores_other_items", filtered.isAlive() && count(be, Items.GRAVEL) == 0, "");
        be.setFilterWhitelist(false);
        tick(be, 12);
        verdict("magnet_blacklist_takes_everything_else", !filtered.isAlive() && count(be, Items.GRAVEL) == 4, "");
        be.setAugmentEnabled(false);
        ItemEntity idle = drop(p.offset(1, 0, 0), Items.SAND, 2);
        tick(be, 12);
        verdict("magnet_toggle_off_stops_it", idle.isAlive(), "");
        be.setAugmentEnabled(true);
        augment(be, Augment.MAGNET, 1);
        ItemEntity wide = drop(p.offset(5, 0, 0), Items.CLAY_BALL, 2);
        tick(be, 12);
        verdict("magnet_radius_default_is_four", wide.isAlive(), "");
        Settings.set("magnet.radius.1", 7);
        tick(be, 12);
        verdict("magnet_radius_is_configurable", !wide.isAlive() && count(be, Items.CLAY_BALL) == 2, "clay=" + count(be, Items.CLAY_BALL));
        Settings.reset();
        ItemEntity cold = drop(p.offset(1, 1, 0), Items.SNOWBALL, 1);
        cold.discard();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(p).inflate(20))) {
            e.discard();
        }
    }

    private static Cow cow(BlockPos at) {
        Cow c = net.minecraft.world.entity.EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
        c.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(c);
        return c;
    }

    private static void feeder(BlockPos p) {
        area(p);
        killEntities(p);
        level.setBlock(p.offset(-1, -1, -1), Blocks.STONE.defaultBlockState(), 2);
        TcChestBlockEntity be = chest(p, ChestLevel.GOLD);
        augment(be, Augment.FEEDER, 1);
        be.setItem(0, stack(Items.WHEAT, 4));
        be.setItem(1, stack(Items.BREAD, 4));
        be.setItem(2, stack(Items.GOLDEN_APPLE, 1));
        Cow cow = cow(p.offset(2, 0, 0));
        tick(be, 45);
        verdict("feeder_feeds_animals_in_range", cow.isInLove() && count(be, Items.WHEAT) == 3, "inLove=" + cow.isInLove() + " wheat=" + count(be, Items.WHEAT));
        owner.getFoodData().setFoodLevel(8);
        boolean fed = Augments.feedPlayer(be, level, owner);
        verdict("feeder_feeds_hungry_players", fed && owner.getFoodData().getFoodLevel() > 8 && count(be, Items.BREAD) == 3, "food=" + owner.getFoodData().getFoodLevel());
        owner.getFoodData().setFoodLevel(20);
        verdict("feeder_skips_full_players", !Augments.feedPlayer(be, level, owner), "");
        owner.getFoodData().setFoodLevel(8);
        TcChestBlockEntity apples = chest(p.offset(4, 0, 4), ChestLevel.GOLD);
        augment(apples, Augment.FEEDER, 1);
        apples.setItem(0, stack(Items.GOLDEN_APPLE, 2));
        verdict("feeder_skips_effect_foods_by_default", !Augments.feedPlayer(apples, level, owner) && count(apples, Items.GOLDEN_APPLE) == 2, "");
        Settings.set("feeder.effect_foods", true);
        verdict("effect_foods_can_be_enabled", Augments.feedPlayer(apples, level, owner), "");
        Settings.reset();
        be.filterSlots.setItem(0, stack(Items.BREAD, 1));
        be.setFilterWhitelist(false);
        owner.getFoodData().setFoodLevel(8);
        verdict("feeder_blacklist_skips_listed_food", !Augments.feedPlayer(be, level, owner) || count(be, Items.BREAD) == 3, "");
        cow.discard();
    }

    private static void feederRange(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.GOLD);
        augment(be, Augment.FEEDER, 1);
        be.setItem(0, stack(Items.WHEAT, 8));
        Cow far = cow(p.offset(6, 0, 0));
        tick(be, 45);
        verdict("feeder_level_1_ignores_animals_beyond_four_blocks", !far.isInLove(), "");
        augment(be, Augment.FEEDER, 2);
        tick(be, 25);
        verdict("feeder_level_2_reaches_eight_blocks", far.isInLove(), "");
        far.discard();
    }

    private static void voidAugment(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.COPPER);
        fill(be, Items.DIRT);
        verdict("full_chest_rejects_without_void", be.insertFromAutomation(stack(Items.STONE, 5), false).getCount() == 5, "");
        be.augmentSlot.setItem(0, new ItemStack(ModContent.augmentItem(Augment.VOID, 1)));
        be.augmentChanged(false);
        verdict("void_is_off_by_default", !be.augmentEnabled() && be.insertFromAutomation(stack(Items.STONE, 5), true).getCount() == 5, "");
        be.setAugmentEnabled(true);
        verdict("void_destroys_overflow", be.insertFromAutomation(stack(Items.STONE, 5), false).isEmpty() && count(be, Items.STONE) == 0, "");
        verdict("void_keeps_what_fits", be.insertFromAutomation(stack(Items.DIRT, 5), false).isEmpty() == true, "");
        be.removeItem(0, 64);
        verdict("void_fills_free_space_first", be.insertFromAutomation(stack(Items.STONE, 70), false).isEmpty() && count(be, Items.STONE) == 64, "stone=" + count(be, Items.STONE));
        be.filterSlots.setItem(0, stack(Items.DIAMOND, 1));
        be.setFilterWhitelist(true);
        fill(be, Items.DIRT);
        verdict("void_whitelist_only_destroys_listed_items", be.insertFromAutomation(stack(Items.STONE, 5), false).getCount() == 5
                && be.insertFromAutomation(stack(Items.DIAMOND, 5), false).isEmpty(), "");
        be.filterSlots.setItem(0, ItemStack.EMPTY);
        // Link
        BlockPos q = p.offset(4, 0, 0);
        TcChestBlockEntity target = chest(q, ChestLevel.COPPER);
        ItemStack voidItem = new ItemStack(ModContent.augmentItem(Augment.VOID, 1));
        CompoundTag tag = new CompoundTag();
        tag.putString("link_dim", String.valueOf(level.dimension().identifier()));
        tag.putInt("link_x", q.getX());
        tag.putInt("link_y", q.getY());
        tag.putInt("link_z", q.getZ());
        voidItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        be.augmentSlot.setItem(0, voidItem);
        be.setAugmentEnabled(true);
        verdict("void_link_passes_overflow_on", be.insertFromAutomation(stack(Items.STONE, 5), false).isEmpty() && count(target, Items.STONE) == 5, "linked=" + count(target, Items.STONE));
        fill(target, Items.GRAVEL);
        verdict("void_link_full_keeps_the_items", be.insertFromAutomation(stack(Items.SAND, 3), false).getCount() == 3, "");
        // Hopper above a full chest
        be.augmentSlot.setItem(0, new ItemStack(ModContent.augmentItem(Augment.VOID, 1)));
        be.augmentChanged(false);
        be.setAugmentEnabled(true);
        BlockPos hp = p.above();
        level.setBlock(hp, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), 3);
        HopperBlockEntity hopper = (HopperBlockEntity) level.getBlockEntity(hp);
        hopper.setItem(0, stack(Items.COBBLESTONE, 4));
        tick(be, 16);
        verdict("void_clears_hoppers_stuck_on_a_full_chest", hopper.getItem(0).isEmpty(), "left=" + hopper.getItem(0));
    }

    private static void menus(BlockPos p) {
        area(p);
        TcChestBlockEntity be = chest(p, ChestLevel.NETHERITE);
        for (int i = 0; i < 135; i++) {
            be.setItem(i, i % 5 == 0 ? stack(Items.DIAMOND, 1 + i % 60) : i % 7 == 0 ? stack(Items.IRON_INGOT, 3) : ItemStack.EMPTY);
        }
        be.setItem(130, stack(Items.EMERALD, 2));
        TcChestMenu menu = new TcChestMenu(ModContent.menu(6), 6, 1, owner.getInventory(), be);
        verdict("window_has_rows_times_nine_slots", menu.windowSlots() == 54 && menu.rows() == 6, "");
        verdict("window_shows_the_first_slots", menu.slots.get(0).getItem() == be.getItem(0) && menu.slots.get(5).getItem() == be.getItem(5), "");
        menu.clickMenuButton(owner, TcChestMenu.BTN_SCROLL + 3);
        menu.broadcastChanges();
        verdict("scrolling_moves_the_window", menu.scroll() == 3 && menu.slots.get(0).getItem() == be.getItem(27), "scroll=" + menu.scroll());
        menu.clickMenuButton(owner, TcChestMenu.BTN_SCROLL + 500);
        verdict("scroll_is_clamped_to_the_end", menu.scroll() == menu.maxScroll() && menu.maxScroll() == 15 - 6, "scroll=" + menu.scroll());
        menu.clickMenuButton(owner, TcChestMenu.BTN_SCROLL);
        for (char c : "emer".toCharArray()) {
            menu.clickMenuButton(owner, TcChestMenu.BTN_CHAR + c);
        }
        menu.broadcastChanges();
        verdict("search_filters_by_name", menu.filteredCount() == 1 && menu.slots.get(0).getItem().is(Items.EMERALD), "count=" + menu.filteredCount());
        verdict("search_hides_other_slots", !menu.slots.get(1).isActive(), "");
        menu.clicked(0, 0, ContainerInput.PICKUP, owner);
        verdict("clicking_a_searched_slot_takes_the_right_item", menu.getCarried().is(Items.EMERALD) && menu.getCarried().getCount() == 2 && be.getItem(130).isEmpty(), "carried=" + menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
        menu.clickMenuButton(owner, TcChestMenu.BTN_CLEAR_QUERY);
        for (char c : "@minecraft".toCharArray()) {
            menu.clickMenuButton(owner, TcChestMenu.BTN_CHAR + c);
        }
        menu.broadcastChanges();
        verdict("search_by_mod", menu.filteredCount() > 20, "count=" + menu.filteredCount());
        menu.clickMenuButton(owner, TcChestMenu.BTN_CLEAR_QUERY);
        menu.clickMenuButton(owner, TcChestMenu.BTN_CHAR + '#');
        for (char c : "gems".toCharArray()) {
            menu.clickMenuButton(owner, TcChestMenu.BTN_CHAR + c);
        }
        menu.broadcastChanges();
        verdict("search_by_tag", menu.filteredCount() > 0 && menu.slots.get(0).getItem().is(Items.DIAMOND), "count=" + menu.filteredCount());
        menu.clickMenuButton(owner, TcChestMenu.BTN_CLEAR_QUERY);
        menu.broadcastChanges();
        // Shift-click from the chest into the player inventory
        int before = owner.getInventory().countItem(Items.DIAMOND);
        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, owner);
        verdict("shift_click_moves_chest_items_to_the_player", owner.getInventory().countItem(Items.DIAMOND) > before, "");
        // Shift-click from the player inventory into the chest
        owner.getInventory().setItem(9, stack(Items.COPPER_INGOT, 40));
        int slot = menu.playerIndex();
        menu.clicked(slot, 0, ContainerInput.QUICK_MOVE, owner);
        verdict("shift_click_moves_player_items_into_any_chest_slot", count(be, Items.COPPER_INGOT) == 40 && owner.getInventory().getItem(9).isEmpty(), "");
        // Augment slot through the menu
        owner.getInventory().setItem(12, new ItemStack(ModContent.augmentItem(Augment.MAGNET, 1), 1));
        menu.clicked(menu.playerIndex() + 3, 0, ContainerInput.QUICK_MOVE, owner);
        verdict("shift_click_installs_an_augment", be.augment() == Augment.MAGNET && be.augmentLevel() == 1, "");
        menu.setCarried(new ItemStack(ModContent.augmentItem(Augment.MAGNET, 2), 1));
        menu.clicked(menu.augmentIndex(), 0, ContainerInput.PICKUP, owner);
        verdict("clicking_a_higher_level_upgrades_and_consumes_the_old_one", be.augmentLevel() == 2 && menu.getCarried().isEmpty(), "carried=" + menu.getCarried());
        menu.setCarried(new ItemStack(ModContent.augmentItem(Augment.FEEDER, 1), 1));
        menu.clicked(menu.augmentIndex(), 0, ContainerInput.PICKUP, owner);
        verdict("a_second_augment_kind_is_refused", be.augment() == Augment.MAGNET && !menu.getCarried().isEmpty(), "");
        menu.setCarried(ItemStack.EMPTY);
        // Toggles
        menu.clickMenuButton(owner, TcChestMenu.BTN_TOGGLE);
        menu.clickMenuButton(owner, TcChestMenu.BTN_REDSTONE);
        menu.clickMenuButton(owner, TcChestMenu.BTN_FILTER_MODE);
        verdict("buttons_toggle_the_chest_settings", !be.augmentEnabled() && be.redstone() == RedstoneMode.WHEN_POWERED && be.filterWhitelist(), "");
        // Filter ghost slots
        menu.setCarried(stack(Items.DIRT, 30));
        menu.clicked(menu.filterIndex() + 2, 0, ContainerInput.PICKUP, owner);
        verdict("filter_slots_copy_without_taking", be.filterSlots.getItem(2).is(Items.DIRT) && menu.getCarried().getCount() == 30, "");
        menu.clicked(menu.filterIndex() + 2, 1, ContainerInput.PICKUP, owner);
        verdict("filter_slots_clear_on_right_click", be.filterSlots.getItem(2).isEmpty(), "");
        menu.setCarried(ItemStack.EMPTY);
        // Lock slot
        menu.setCarried(stack(ModContent.LOCK, 1));
        menu.clicked(menu.lockIndex(), 0, ContainerInput.PICKUP, owner);
        verdict("placing_a_lock_in_the_slot_sets_the_owner", be.hasLock() && "selftest_owner".equals(be.ownerName()), "");
        menu.setCarried(ItemStack.EMPTY);
        owner.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        stranger.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        verdict("menu_closes_for_strangers_of_locked_chests", !menu.stillValid(stranger) && menu.stillValid(owner), "");
        menu.clicked(menu.lockIndex(), 0, ContainerInput.PICKUP, owner);
        verdict("taking_the_lock_out_unlocks_the_chest", !be.hasLock() && menu.getCarried().getItem() == ModContent.LOCK, "");
        menu.setCarried(ItemStack.EMPTY);
        verdict("menu_counts_as_an_opener", be.openerCount() == 1, "openers=" + be.openerCount());
        menu.removed(owner);
        verdict("closing_the_menu_closes_the_lid", be.openerCount() == 0, "openers=" + be.openerCount());
        // Row choices
        verdict("rows_follow_the_chest_size", TcChestMenu.rowsFor(owner, chest(p.offset(6, 0, 0), ChestLevel.COPPER)) == 4, "");
        TcChestMenu.rememberRows(owner, 3);
        verdict("rows_are_remembered_per_player", TcChestMenu.rowsFor(owner, be) == 3, "");
        TcChestMenu.rememberRows(owner, 6);
        TcChestMenu small = new TcChestMenu(ModContent.menu(1), 1, 2, owner.getInventory(), be);
        small.clickMenuButton(owner, TcChestMenu.BTN_SCROLL + 14);
        verdict("a_one_row_window_scrolls_the_whole_chest", small.slots.get(0).getItem() == be.getItem(14 * 9), "");
        small.removed(owner);
    }

    private static void scripts(BlockPos p) {
        area(p);
        verdict("mod_runs_without_modjs", !ScriptBridge.install(), "");
        ChestsApi api = new ChestsApi();
        TcChestBlockEntity be = chest(p, ChestLevel.DIAMOND);
        be.setItem(3, stack(Items.IRON_INGOT, 12));
        augment(be, Augment.MAGNET, 2);
        var seen = new java.util.ArrayList<String>();
        api.on("augment_installed", e -> seen.add(e.string("augment") + e.number("augment_level")));
        api.on("chest_upgraded", e -> seen.add(e.string("from") + ">" + e.string("to")));
        api.on("chest_locked", e -> seen.add("locked " + e.string("owner")));
        api.on("chest_opened", e -> seen.add("opened"));
        augment(be, Augment.COMPACT, 1);
        be.augmentSlot.setItem(0, ItemStack.EMPTY);
        be.augmentChanged(false);
        be.lockInstalled(owner);
        be.lockRemoved();
        TcChestBlock.upgrade(level, p, ChestLevel.NETHERITE);
        verdict("script_events_fire", seen.contains("compact1.0") && seen.contains("diamond>netherite") && seen.contains("locked selftest_owner"), seen.toString());
        verdict("api_lists_levels_and_augments", api.levels().size() == 5 && api.augments().size() == 6 && api.slots("gold") == 72, "");
        verdict("api_slot_lookup_rejects_unknown_levels", throwsOn(() -> api.slots("wood")), "");
        var info = api.chest(String.valueOf(level.dimension().identifier()), p.getX(), p.getY(), p.getZ());
        verdict("api_reads_a_chest", info != null && "netherite".equals(info.get("level")) && ((Integer) info.get("slots")) == 135 && ((Integer) info.get("used")) == 1, String.valueOf(info));
        verdict("api_counts_items", api.count(String.valueOf(level.dimension().identifier()), p.getX(), p.getY(), p.getZ(), "minecraft:iron_ingot") == 12, "");
        verdict("api_returns_null_without_a_chest", api.chest(String.valueOf(level.dimension().identifier()), p.getX() + 5, p.getY(), p.getZ()) == null, "");
        ((ChestsApi) api).addItemRule(id -> !id.equals("minecraft:bedrock"));
        TcChestBlockEntity ruled = (TcChestBlockEntity) level.getBlockEntity(p);
        verdict("item_rules_refuse_items", ruled.insertStack(stack(Items.BEDROCK, 1), false).getCount() == 1 && ruled.insertStack(stack(Items.DIRT, 1), false).isEmpty(), "");
        api.setSetting("magnet.radius.3", "30");
        verdict("api_changes_settings", Settings.getInt("magnet.radius.3") == 30 && api.setting("magnet.radius.3").equals("30"), "");
        verdict("api_rejects_unknown_settings", throwsOn(() -> api.setSetting("nope", "1")), "");
        ScriptEvents.clear();
        Settings.reset();
    }

    private static boolean throwsOn(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void explosions(BlockPos p) {
        clear(p.offset(-2, -2, -2), p.offset(14, 3, 3));
        for (ChestLevel lv : ChestLevel.values()) {
            BlockPos at = p.offset(lv.ordinal() * 4, 0, 0);
            level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 2);
            chest(at, lv);
        }
        for (ChestLevel lv : ChestLevel.values()) {
            BlockPos at = p.offset(lv.ordinal() * 4, 0, 0);
            level.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12.0F, Level.ExplosionInteraction.TNT);
        }
        boolean copper = level.getBlockState(p).isAir();
        boolean iron = level.getBlockState(p.offset(4, 0, 0)).isAir();
        boolean gold = level.getBlockState(p.offset(8, 0, 0)).isAir();
        boolean diamond = level.getBlockState(p.offset(12, 0, 0)).getBlock() == ModContent.chest(ChestLevel.DIAMOND);
        boolean netherite = level.getBlockState(p.offset(16, 0, 0)).getBlock() == ModContent.chest(ChestLevel.NETHERITE);
        verdict("explosion_destroys_copper_iron_and_gold_chests", copper && iron && gold, "copper=" + copper + " iron=" + iron + " gold=" + gold);
        verdict("diamond_chest_survives_a_big_explosion", diamond, "");
        verdict("netherite_chest_is_immune_to_explosions", netherite, "");
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(p).inflate(30))) {
            e.discard();
        }
    }
}
