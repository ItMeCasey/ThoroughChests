package com.itmecasey.thoroughchests.neoforge;

import com.itmecasey.thoroughchests.Settings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The NeoForge config file {@code config/thoroughchests-common.toml}, built from the single list of settings in common. Its values are
 * pushed into {@link Settings} when the file loads or reloads. On the first start an old {@code thoroughchests.properties} supplies the
 * defaults; that file is only read, never changed or deleted.
 */
final class ChestConfig {
    static final ModConfigSpec SPEC;
    private static final Map<String, ModConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        Path dir = FMLPaths.CONFIGDIR.get();
        Map<String, String> legacy = Files.exists(dir.resolve("thoroughchests-common.toml")) ? Map.of() : Settings.legacyValues(dir);
        for (Settings.Def d : Settings.defs()) {
            String start = legacy.getOrDefault(d.key(), d.fallback());
            builder.comment(d.comment());
            switch (d.kind()) {
                case INT -> VALUES.put(d.key(), builder.defineInRange(d.key(), clampInt(start, d), (int) d.min(), (int) d.max()));
                case DOUBLE -> VALUES.put(d.key(), builder.defineInRange(d.key(), clampDouble(start, d), d.min(), d.max()));
                case BOOL -> VALUES.put(d.key(), builder.define(d.key(), Boolean.parseBoolean(start)));
            }
        }
        SPEC = builder.build();
    }

    private ChestConfig() {
    }

    private static int clampInt(String text, Settings.Def d) {
        try {
            return (int) Math.max(d.min(), Math.min(d.max(), Math.round(Double.parseDouble(text))));
        } catch (NumberFormatException e) {
            return (int) Math.round(Double.parseDouble(d.fallback()));
        }
    }

    private static double clampDouble(String text, Settings.Def d) {
        try {
            return Math.max(d.min(), Math.min(d.max(), Double.parseDouble(text)));
        } catch (NumberFormatException e) {
            return Double.parseDouble(d.fallback());
        }
    }

    /** Copies the loaded file values into {@link Settings}. */
    static void push() {
        for (Map.Entry<String, ModConfigSpec.ConfigValue<?>> e : VALUES.entrySet()) {
            Settings.set(e.getKey(), e.getValue().get());
        }
    }
}
