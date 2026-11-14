package com.itmecasey.thoroughchests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Every tunable number in the mod: the single list of keys, defaults, ranges and descriptions. Each loader layer builds its config
 * spec (NeoForge {@code ModConfigSpec}, Forge {@code ForgeConfigSpec}, file {@code config/thoroughchests-common.toml}) from
 * {@link #defs()} and pushes the loaded values in with {@link #set}, so common code and tests only ever read from here. This class
 * reads and writes no config file.
 */
public final class Settings {
    /** The kind of value a setting holds. */
    public enum Kind { INT, DOUBLE, BOOL }

    /** One setting: its key, kind, default, allowed range (numbers only) and description. */
    public record Def(String key, Kind kind, String fallback, double min, double max, String comment) {
    }

    private static final Map<String, Def> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, Object fallback, double min, double max, String comment) {
        Kind kind = fallback instanceof Boolean ? Kind.BOOL : fallback instanceof Integer ? Kind.INT : Kind.DOUBLE;
        String text = fallback instanceof String s ? s : String.valueOf(fallback);
        if (fallback instanceof String) {
            kind = Kind.DOUBLE;
        }
        DEFS.put(key, new Def(key, kind, text, min, max, comment));
    }

    private static void def(String key, boolean fallback, String comment) {
        def(key, (Object) fallback, 0, 0, comment);
    }

    static {
        for (ChestLevel level : ChestLevel.values()) {
            def("level." + level.id() + ".slots", level.defaultSlots(), 1, ChestLevel.MAX_SLOTS, "Slots in a " + level.title() + " Chest (1 to " + ChestLevel.MAX_SLOTS + ").");
            def("level." + level.id() + ".blast_resistance", (double) level.defaultResistance(), 0, 3600000.0, "Blast resistance of a " + level.title() + " Chest (3600000 or more is immune).");
        }
        def("compact.interval_ticks", 20, 1, 1200, "Ticks between compaction passes of the Compact augment.");
        def("compact.max_crafts", 16, 1, 256, "Most crafts one compaction pass makes.");
        def("compact.any_recipe", false, "true lets Compact use any uniform recipe that gives fewer items than it uses; false only uses recipes that can be crafted back.");
        String[] speed = {"1.0", "2.0", "4.0"};
        String[] fuel = {"1.0", "0.75", "0.5"};
        int[] radius = {4, 8, 12};
        int[] pull = {10, 6, 3};
        int[] feedInterval = {40, 20, 10};
        for (int l = 1; l <= 3; l++) {
            def("furnace.speed." + l, speed[l - 1], 0.1, 100, "Furnace augment level " + l + ": how many times faster than a furnace it smelts.");
            def("furnace.fuel_use." + l, fuel[l - 1], 0.01, 100, "Furnace augment level " + l + ": fuel burned per item, as a fraction of a furnace.");
            def("blast_furnace.speed." + l, speed[l - 1], 0.1, 100, "Blast Furnace augment level " + l + ": how many times faster than a blast furnace it works.");
            def("blast_furnace.fuel_use." + l, fuel[l - 1], 0.01, 100, "Blast Furnace augment level " + l + ": fuel burned per item, as a fraction of a blast furnace.");
            def("magnet.radius." + l, radius[l - 1], 1, 64, "Magnet augment level " + l + ": radius in blocks.");
            def("magnet.interval." + l, pull[l - 1], 1, 1200, "Magnet augment level " + l + ": ticks between pulls.");
            def("feeder.range." + l, radius[l - 1], 1, 64, "Feeder augment level " + l + ": range in blocks.");
            def("feeder.interval." + l, feedInterval[l - 1], 1, 2400, "Feeder augment level " + l + ": ticks between feedings.");
        }
        def("feeder.breed", true, "true lets the Feeder put adult animals into love mode with their food.");
        def("feeder.effect_foods", false, "true lets the Feeder use foods that give effects (golden apples, suspicious stew).");
        def("gui.default_rows", 6, 1, 12, "Rows of the chest window when the player has no saved size (1 to 12).");
    }

    private Settings() {
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Def e = def(key);
        return VALUES.getOrDefault(key, e.fallback());
    }

    /** Every setting key, sorted. */
    public static List<String> keys() {
        return new ArrayList<>(new TreeSet<>(DEFS.keySet()));
    }

    /** The current value of a setting as text. */
    public static String value(String key) {
        return raw(key);
    }

    /** Overrides one setting (tests and scripts; not saved to the file). */
    public static synchronized void set(String key, Object value) {
        raw(key);
        VALUES.put(key, String.valueOf(value));
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(raw(key).trim());
        } catch (NumberFormatException ex) {
            return Double.parseDouble(DEFS.get(key).fallback());
        }
    }

    public static int getInt(String key) {
        return (int) Math.round(getDouble(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }

    /** Every setting definition, in the order the config files list them. */
    public static List<Def> defs() {
        return new ArrayList<>(DEFS.values());
    }

    public static Def def(String key) {
        Def d = DEFS.get(key);
        if (d == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return d;
    }
}
