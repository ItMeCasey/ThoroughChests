package com.itmecasey.thoroughchests.augment;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.Compaction;
import com.itmecasey.thoroughchests.FurnaceSim;
import com.itmecasey.thoroughchests.ScriptEvents;
import com.itmecasey.thoroughchests.Settings;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** What each augment does on the server, one pass per tick call. All numbers come from {@link Settings}. */
public final class Augments {
    private Augments() {
    }

    /** Runs the installed augment for this tick. The caller has checked that it is installed. */
    public static void tick(TcChestBlockEntity chest, ServerLevel level) {
        Augment augment = chest.augment();
        int lv = Math.max(1, chest.augmentLevel());
        long t = chest.ticks();
        switch (augment) {
            case COMPACT -> {
                if (t % Math.max(1, Settings.getInt("compact.interval_ticks")) == 0) {
                    compact(chest, level);
                }
            }
            case FURNACE -> cook(chest, level, false, lv);
            case BLAST_FURNACE -> cook(chest, level, true, lv);
            case MAGNET -> {
                if (t % Math.max(1, Settings.getInt("magnet.interval." + lv)) == 0) {
                    magnet(chest, level, Settings.getInt("magnet.radius." + lv));
                }
            }
            case FEEDER -> {
                if (t % Math.max(1, Settings.getInt("feeder.interval." + lv)) == 0) {
                    feed(chest, level, Settings.getInt("feeder.range." + lv));
                }
            }
            case VOID -> {
                if (t % 8 == 0) {
                    voidHoppers(chest, level);
                }
            }
        }
    }

    private static void announce(TcChestBlockEntity chest, ServerLevel level, String action, String item, int count) {
        if (ScriptEvents.hasListeners("augment_action")) {
            BlockPos p = chest.getBlockPos();
            ScriptEvents.fire("augment_action", "dimension", String.valueOf(level.dimension().identifier()), "x", p.getX(), "y", p.getY(), "z", p.getZ(),
                    "augment", chest.augment().id(), "augment_level", chest.augmentLevel(), "action", action, "item", item, "count", count);
        }
    }

    /** What is left when one of the stack's item is used up in a craft or a furnace (a bucket, a bottle), or empty. */
    @SuppressWarnings("deprecation")
    private static ItemStack remainder(ItemStack stack) {
        net.minecraft.world.item.ItemStackTemplate template = stack.getItem().getCraftingRemainder();
        return template == null ? ItemStack.EMPTY : template.create();
    }

    private static String id(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    // ---- Compact ----

    private static void compact(TcChestBlockEntity chest, ServerLevel level) {
        int budget = Settings.getInt("compact.max_crafts");
        List<ItemStack> tried = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize() && budget > 0; i++) {
            ItemStack s = chest.getItem(i);
            if (s.isEmpty() || !remainder(s).isEmpty() || s.getCount() < 4) {
                continue;
            }
            boolean seen = false;
            for (ItemStack t : tried) {
                if (ItemStack.isSameItemSameComponents(t, s)) {
                    seen = true;
                    break;
                }
            }
            if (seen) {
                continue;
            }
            tried.add(s.copyWithCount(1));
            budget -= compactOne(chest, level, s.copyWithCount(1), budget);
        }
    }

    /** Compacts one item type; returns the crafts made. */
    private static int compactOne(TcChestBlockEntity chest, ServerLevel level, ItemStack one, int budget) {
        int have = chest.countOf(one);
        for (int side : new int[] {3, 2}) {
            int per = side * side;
            if (have < per) {
                continue;
            }
            List<ItemStack> grid = new ArrayList<>();
            for (int k = 0; k < per; k++) {
                grid.add(one.copy());
            }
            CraftingInput input = CraftingInput.of(side, side, grid);
            Optional<RecipeHolder<CraftingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (found.isEmpty()) {
                continue;
            }
            ItemStack result = found.get().value().assemble(input);
            if (result.isEmpty() || !Compaction.compacts(per, result.getCount()) || ItemStack.isSameItemSameComponents(result, one)) {
                continue;
            }
            if (!Settings.getBool("compact.any_recipe") && !reversible(level, result, one, per)) {
                continue;
            }
            Compaction.Plan plan = Compaction.plan(have, per, result.getCount(), budget);
            if (plan.crafts() == 0) {
                continue;
            }
            int removed = chest.removeMatching(one, plan.consumed());
            ItemStack out = result.copyWithCount(plan.produced());
            ItemStack rest = chest.insertStack(out, false);
            if (!rest.isEmpty()) {
                // The result did not fit: undo the part that did not fit by returning inputs for the crafts not stored.
                int notStored = (int) Math.ceil((double) rest.getCount() / result.getCount());
                chest.removeMatching(result, plan.produced() - rest.getCount());
                chest.insertStack(one.copyWithCount(removed), false);
                if (notStored >= plan.crafts()) {
                    return 0;
                }
            }
            announce(chest, level, "compact", id(result), plan.produced());
            return plan.crafts();
        }
        return 0;
    }

    /** Whether one result crafts back into at least {@code per} of the input item. */
    private static boolean reversible(ServerLevel level, ItemStack result, ItemStack one, int per) {
        CraftingInput back = CraftingInput.of(1, 1, List.of(result.copyWithCount(1)));
        Optional<RecipeHolder<CraftingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, back, level);
        if (found.isEmpty()) {
            return false;
        }
        ItemStack yield = found.get().value().assemble(back);
        return ItemStack.isSameItemSameComponents(yield, one) && Compaction.reversible(per, yield.getCount());
    }

    // ---- Furnace and Blast Furnace ----

    private static void cook(TcChestBlockEntity chest, ServerLevel level, boolean blast, int lv) {
        FurnaceSim sim = chest.sim;
        String key = blast ? "blast_furnace" : "furnace";
        double speed = Settings.getDouble(key + ".speed." + lv);
        double fuelUse = Settings.getDouble(key + ".fuel_use." + lv);
        RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> type = blast ? RecipeType.BLASTING : RecipeType.SMELTING;
        if (!chest.pendingOutput().isEmpty()) {
            ItemStack rest = chest.insertStack(chest.pendingOutput(), false);
            chest.setPending(rest);
            return;
        }
        if (sim.cooking()) {
            if (sim.tick()) {
                ItemStack result = chest.cookResult();
                ItemStack rest = chest.insertStack(result, false);
                chest.setCook(ItemStack.EMPTY, ItemStack.EMPTY);
                if (!rest.isEmpty()) {
                    chest.setPending(rest);
                }
                announce(chest, level, blast ? "blast" : "smelt", id(result), result.getCount());
            }
            return;
        }
        if (chest.ticks() % 5 != 0) {
            return;
        }
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            Optional<? extends RecipeHolder<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe>> recipe =
                    level.recipeAccess().getRecipeFor(type, new SingleRecipeInput(s), level);
            if (recipe.isEmpty()) {
                continue;
            }
            net.minecraft.world.item.crafting.AbstractCookingRecipe cooking = recipe.get().value();
            ItemStack result = cooking.assemble(new SingleRecipeInput(s));
            if (result.isEmpty()) {
                continue;
            }
            int time = cooking.cookingTime();
            double cost = FurnaceSim.fuelCost(time, fuelUse);
            while (!sim.canAfford(cost)) {
                if (!takeFuel(chest, level, type, sim)) {
                    return;
                }
            }
            sim.start(time, speed, fuelUse);
            ItemStack input = s.copyWithCount(1);
            s.shrink(1);
            if (s.isEmpty()) {
                chest.setItem(i, ItemStack.EMPTY);
            }
            chest.setCook(input, result);
            return;
        }
    }

    /** Burns one fuel item from the chest into the bank. Fuel is an item with a burn time and no recipe for this furnace type. */
    private static boolean takeFuel(TcChestBlockEntity chest, ServerLevel level, RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> type,
            FurnaceSim sim) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            int burn = Fuel.burnTicks(level, chest, s);
            if (burn <= 0 || level.recipeAccess().getRecipeFor(type, new SingleRecipeInput(s), level).isPresent()) {
                continue;
            }
            ItemStack remainder = remainder(s);
            s.shrink(1);
            if (s.isEmpty()) {
                chest.setItem(i, ItemStack.EMPTY);
            }
            if (!remainder.isEmpty()) {
                chest.insertStack(remainder, false);
            }
            sim.addFuel(burn);
            chest.setChanged();
            return true;
        }
        return false;
    }

    // ---- Magnet ----

    private static void magnet(TcChestBlockEntity chest, ServerLevel level, int radius) {
        AABB box = new AABB(chest.getBlockPos()).inflate(radius);
        List<ItemEntity> found = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.hasPickUpDelay());
        int pulled = 0;
        for (ItemEntity e : found) {
            if (pulled >= 32) {
                break;
            }
            ItemStack stack = e.getItem();
            int before = stack.getCount();
            ItemStack rest = chest.insertFromAutomation(stack, false);
            if (rest.isEmpty()) {
                e.discard();
            } else {
                e.setItem(rest);
            }
            if (rest.getCount() != before) {
                pulled++;
                announce(chest, level, "magnet", id(stack), before - rest.getCount());
            }
        }
    }

    // ---- Feeder ----

    private static ItemStack food(TcChestBlockEntity chest, boolean wantAnimal, @org.jetbrains.annotations.Nullable Animal animal) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (wantAnimal) {
                if (animal != null && animal.isFood(s)) {
                    return s;
                }
                continue;
            }
            FoodProperties props = s.get(DataComponents.FOOD);
            if (props == null) {
                continue;
            }
            var consumable = s.get(DataComponents.CONSUMABLE);
            if (consumable != null && !consumable.onConsumeEffects().isEmpty() && !Settings.getBool("feeder.effect_foods")) {
                continue;
            }
            return s;
        }
        return ItemStack.EMPTY;
    }

    private static void consumeOne(TcChestBlockEntity chest, ItemStack food) {
        ItemStack remainder = remainder(food);
        food.shrink(1);
        if (!remainder.isEmpty()) {
            chest.insertStack(remainder, false);
        }
        chest.setChanged();
    }

    /** Feeds one food item to a hungry player; returns whether it did. */
    public static boolean feedPlayer(TcChestBlockEntity chest, ServerLevel level, Player player) {
        if (player.isSpectator() || !player.getFoodData().needsFood()) {
            return false;
        }
        ItemStack food = food(chest, false, null);
        if (food.isEmpty()) {
            return false;
        }
        player.getFoodData().eat(food.get(DataComponents.FOOD));
        announce(chest, level, "feed_player", id(food), 1);
        consumeOne(chest, food);
        return true;
    }

    private static void feed(TcChestBlockEntity chest, ServerLevel level, int range) {
        AABB box = new AABB(chest.getBlockPos()).inflate(range);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e instanceof Player player) {
                feedPlayer(chest, level, player);
            } else if (e instanceof Animal animal) {
                ItemStack food = food(chest, true, animal);
                if (food.isEmpty()) {
                    continue;
                }
                FoodProperties props = food.get(DataComponents.FOOD);
                if (animal.getHealth() < animal.getMaxHealth() && props != null) {
                    animal.heal(Math.max(1, props.nutrition()));
                } else if (animal.isBaby()) {
                    animal.ageUp(net.minecraft.world.entity.AgeableMob.getSpeedUpSecondsWhenFeeding(-animal.getAge()), true);
                } else if (Settings.getBool("feeder.breed") && animal.getAge() == 0 && animal.canFallInLove()) {
                    animal.setInLove(null);
                } else {
                    continue;
                }
                announce(chest, level, "feed_animal", id(food), 1);
                consumeOne(chest, food);
            }
        }
    }

    // ---- Void ----

    /** What the Void augment does with items that did not fit: destroy them. */
    public static ItemStack overflow(TcChestBlockEntity chest, ItemStack rest, boolean simulate) {
        if (!simulate && chest.getLevel() instanceof ServerLevel level) {
            announce(chest, level, "void", id(rest), rest.getCount());
        }
        return ItemStack.EMPTY;
    }

    /** Hoppers pointing into a full chest would wait forever; with Void on, their stuck items are destroyed. */
    private static void voidHoppers(TcChestBlockEntity chest, ServerLevel level) {
        BlockPos pos = chest.getBlockPos();
        for (Direction dir : Direction.values()) {
            BlockPos hp = pos.relative(dir);
            BlockState state = level.getBlockState(hp);
            if (!(state.getBlock() instanceof HopperBlock) || state.getValue(HopperBlock.FACING) != dir.getOpposite()) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(hp);
            if (!(be instanceof HopperBlockEntity hopper)) {
                continue;
            }
            for (int i = 0; i < hopper.getContainerSize(); i++) {
                ItemStack s = hopper.getItem(i);
                if (s.isEmpty() || chest.insertStack(s, true).getCount() != s.getCount()) {
                    continue;
                }
                overflow(chest, s.copy(), false);
                hopper.setItem(i, ItemStack.EMPTY);
                break;
            }
        }
    }

    /** Used by the loader wrappers and tests. */
    public static boolean isOverflowing(TcChestBlockEntity chest, ItemStack stack) {
        return chest.insertStack(stack, true).getCount() == stack.getCount();
    }

    @SuppressWarnings("unused")
    private static Level levelOf(TcChestBlockEntity chest) {
        return chest.getLevel();
    }
}
