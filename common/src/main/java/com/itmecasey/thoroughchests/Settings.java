package com.itmecasey.thoroughchests;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Every tunable number in the mod. The file {@code config/thoroughchests.properties} holds one {@code key=value} line per
 * setting; a missing key uses its default, and the file is rewritten with every key and a comment after loading.
 */
public final class Settings {
    private record Entry(String fallback, String comment) {
    }

    private static final Map<String, Entry> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, Object fallback, String comment) {
        DEFS.put(key, new Entry(String.valueOf(fallback), comment));
    }

    static {
        for (ChestLevel level : ChestLevel.values()) {
            def("level." + level.id() + ".slots", level.defaultSlots(), "Slots in a " + level.title() + " Chest (1 to " + ChestLevel.MAX_SLOTS + ").");
            def("level." + level.id() + ".blast_resistance", level.defaultResistance(), "Blast resistance of a " + level.title() + " Chest (3600000 or more is immune).");
        }
        def("compact.interval_ticks", 20, "Ticks between compaction passes of the Compact augment.");
        def("compact.max_crafts", 16, "Most crafts one compaction pass makes.");
        def("compact.any_recipe", false, "true lets Compact use any uniform recipe that gives fewer items than it uses; false only uses recipes that can be crafted back.");
        String[] speed = {"1.0", "2.0", "4.0"};
        String[] fuel = {"1.0", "0.75", "0.5"};
        int[] radius = {4, 8, 12};
        int[] pull = {10, 6, 3};
        int[] feedInterval = {40, 20, 10};
        for (int l = 1; l <= 3; l++) {
            def("furnace.speed." + l, speed[l - 1], "Furnace augment level " + l + ": how many times faster than a furnace it smelts.");
            def("furnace.fuel_use." + l, fuel[l - 1], "Furnace augment level " + l + ": fuel burned per item, as a fraction of a furnace.");
            def("blast_furnace.speed." + l, speed[l - 1], "Blast Furnace augment level " + l + ": how many times faster than a blast furnace it works.");
            def("blast_furnace.fuel_use." + l, fuel[l - 1], "Blast Furnace augment level " + l + ": fuel burned per item, as a fraction of a blast furnace.");
            def("magnet.radius." + l, radius[l - 1], "Magnet augment level " + l + ": radius in blocks.");
            def("magnet.interval." + l, pull[l - 1], "Magnet augment level " + l + ": ticks between pulls.");
            def("feeder.range." + l, radius[l - 1], "Feeder augment level " + l + ": range in blocks.");
            def("feeder.interval." + l, feedInterval[l - 1], "Feeder augment level " + l + ": ticks between feedings.");
        }
        def("feeder.breed", true, "true lets the Feeder put adult animals into love mode with their food.");
        def("feeder.effect_foods", false, "true lets the Feeder use foods that give effects (golden apples, suspicious stew).");
        def("void.enabled_by_default", false, "Whether a newly installed Void augment starts switched on.");
        def("lock.ops_bypass", true, "Operators can open, break and unlock locked chests.");
        def("lock.block_automation", false, "true keeps hoppers and pipes out of locked chests.");
        def("gui.default_rows", 6, "Rows of the chest window when the player has no saved size (1 to 12).");
    }

    private Settings() {
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Entry e = DEFS.get(key);
        if (e == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
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

    /** Reads overrides from a properties text (also used by tests). */
    public static synchronized void load(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        VALUES.clear();
        for (String key : p.stringPropertyNames()) {
            if (DEFS.containsKey(key)) {
                VALUES.put(key, p.getProperty(key));
            }
        }
    }

    /** Loads the file if there is one, then rewrites it with every key so players can see what they can change. */
    public static synchronized void loadFile(Path file) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    load(r);
                }
            }
            write(file);
        } catch (IOException | RuntimeException ex) {
            System.err.println("[ThoroughChests] Could not read " + file + ", using defaults: " + ex);
            VALUES.clear();
        }
    }

    private static void write(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("# Thorough Chests settings. Times are in game ticks (20 per second); distances are in blocks.\n");
            w.write("# Edit a value and restart the game or server. Delete a line to restore its default.\n");
            for (Map.Entry<String, Entry> e : DEFS.entrySet()) {
                w.write("\n# " + e.getValue().comment() + "\n");
                w.write(e.getKey() + "=" + VALUES.getOrDefault(e.getKey(), e.getValue().fallback()) + "\n");
            }
        }
    }
}
