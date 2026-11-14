package com.itmecasey.thoroughchests;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/** The six augments. A chest holds one of them at a time. */
public enum Augment {
    COMPACT(1),
    FURNACE(3),
    BLAST_FURNACE(3),
    MAGNET(3),
    FEEDER(3),
    VOID(1);

    private final int maxLevel;

    Augment(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The registry name of the item for a level: compact_augment, furnace_augment_2. */
    public String itemName(int level) {
        return maxLevel == 1 ? id() + "_augment" : id() + "_augment_" + level;
    }

    @Nullable
    public static Augment byId(String id) {
        for (Augment a : values()) {
            if (a.id().equals(id)) {
                return a;
            }
        }
        return null;
    }
}
