package com.itmecasey.thoroughchests;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/** The five chest levels. Slot counts and blast resistance come from the settings, so a pack can change them. */
public enum ChestLevel {
    COPPER(36, 6.0F),
    IRON(54, 12.0F),
    GOLD(72, 30.0F),
    DIAMOND(108, 1200.0F),
    NETHERITE(135, 3600000.0F);

    public static final int MAX_SLOTS = 512;
    /** Blast resistance at or above this counts as immune to explosions. */
    public static final float IMMUNE = 3600000.0F;

    private final int defaultSlots;
    private final float defaultResistance;

    ChestLevel(int defaultSlots, float defaultResistance) {
        this.defaultSlots = defaultSlots;
        this.defaultResistance = defaultResistance;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String title() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }

    public int defaultSlots() {
        return defaultSlots;
    }

    public float defaultResistance() {
        return defaultResistance;
    }

    /** Configured slot count, kept between 1 and {@link #MAX_SLOTS}. */
    public int slots() {
        return Math.max(1, Math.min(MAX_SLOTS, Settings.getInt("level." + id() + ".slots")));
    }

    public float blastResistance() {
        return (float) Settings.getDouble("level." + id() + ".blast_resistance");
    }

    public boolean explosionImmune() {
        return blastResistance() >= IMMUNE;
    }

    /** The next level up, or null for netherite. */
    @Nullable
    public ChestLevel next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    @Nullable
    public ChestLevel previous() {
        return ordinal() > 0 ? values()[ordinal() - 1] : null;
    }

    /** Whether a card for this level may upgrade a chest of the given level: exactly one step. */
    public boolean upgradesFrom(ChestLevel current) {
        return current.next() == this;
    }

    @Nullable
    public static ChestLevel byId(String id) {
        for (ChestLevel l : values()) {
            if (l.id().equals(id)) {
                return l;
            }
        }
        return null;
    }

    /** The container size a chest of this level needs when it holds stacks up to the given highest index. */
    public int sizeFor(int highestUsedIndex) {
        return Math.max(slots(), highestUsedIndex + 1);
    }
}
