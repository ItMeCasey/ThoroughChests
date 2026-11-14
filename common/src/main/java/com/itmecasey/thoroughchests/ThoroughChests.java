package com.itmecasey.thoroughchests;

import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer. The layers call {@link #init} from their mod constructor. */
public final class ThoroughChests {
    public static final String MODID = "thoroughchests";

    private ThoroughChests() {
    }

    public static void init(Supplier<Path> configDir) {
        Settings.loadFile(configDir.get().resolve(MODID + ".properties"));
    }
}
