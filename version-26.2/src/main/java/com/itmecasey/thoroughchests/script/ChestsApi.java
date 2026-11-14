package com.itmecasey.thoroughchests.script;

import com.itmecasey.thoroughchests.Augment;
import com.itmecasey.thoroughchests.ChestLevel;
import com.itmecasey.thoroughchests.ScriptEvents;
import com.itmecasey.thoroughchests.Settings;
import com.itmecasey.thoroughchests.block.TcChestBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The ThoroughChests global of ModJS server scripts: events, read access to chests, settings and item rules.
 * Everything uses strings, numbers and plain lists and maps so a script engine can call it.
 */
public final class ChestsApi {
    /** The events {@link #on} accepts. */
    public List<String> events() {
        return ScriptEvents.TYPES;
    }

    /** Listens to an event (see events()); the function gets the event with get(key), string(key) and number(key). */
    public void on(String event, Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on(event, listener);
    }

    /** Refuses items: when the function returns false for an item id, no chest takes that item. */
    public void addItemRule(Predicate<String> allow) {
        ScriptEvents.addInsertFilter(allow);
    }

    /** The chest levels, from copper to netherite. */
    public List<String> levels() {
        List<String> out = new ArrayList<>();
        for (ChestLevel l : ChestLevel.values()) {
            out.add(l.id());
        }
        return out;
    }

    /** The augments. */
    public List<String> augments() {
        List<String> out = new ArrayList<>();
        for (Augment a : Augment.values()) {
            out.add(a.id());
        }
        return out;
    }

    /** Slots a chest of the level has under the current settings. */
    public int slots(String level) {
        ChestLevel l = ChestLevel.byId(level);
        if (l == null) {
            throw new IllegalArgumentException("Unknown chest level " + level + ". Levels: " + levels());
        }
        return l.slots();
    }

    /** What a chest at a position holds and does, or null when there is no chest (or the server is not running yet). */
    public Map<String, Object> chest(String dimension, int x, int y, int z) {
        ServerLevel level = ScriptHost.level(dimension);
        if (level == null || !(level.getBlockEntity(new BlockPos(x, y, z)) instanceof TcChestBlockEntity chest)) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("level", chest.level().id());
        out.put("slots", chest.getContainerSize());
        out.put("used", chest.usedSlots());
        out.put("augment", chest.augment() == null ? "" : chest.augment().id());
        out.put("augmentLevel", chest.augmentLevel());
        out.put("locked", chest.hasLock());
        out.put("owner", chest.ownerName());
        out.put("open", chest.openerCount());
        out.put("comparator", chest.comparatorSignal());
        return out;
    }

    /** How many of an item (such as minecraft:iron_ingot) a chest at a position holds, or -1 when there is no chest. */
    public int count(String dimension, int x, int y, int z, String itemId) {
        ServerLevel level = ScriptHost.level(dimension);
        if (level == null || !(level.getBlockEntity(new BlockPos(x, y, z)) instanceof TcChestBlockEntity chest)) {
            return -1;
        }
        int n = 0;
        for (ItemStack s : chest.rawItems()) {
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(itemId)) {
                n += s.getCount();
            }
        }
        return n;
    }

    public List<String> settingKeys() {
        return Settings.keys();
    }

    /** A setting as text, for example magnet.radius.1. */
    public String setting(String key) {
        return Settings.value(key);
    }

    /** Changes a setting until the game closes (the config file is not touched). Unknown keys throw. */
    public void setSetting(String key, String value) {
        Settings.value(key);
        Settings.set(key, value);
    }
}
