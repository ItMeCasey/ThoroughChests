package com.itmecasey.thoroughchests;

/** When a chest's augment works relative to redstone power. */
public enum RedstoneMode {
    IGNORE,
    WHEN_POWERED,
    WHEN_UNPOWERED;

    public boolean active(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case WHEN_POWERED -> powered;
            case WHEN_UNPOWERED -> !powered;
        };
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RedstoneMode byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : IGNORE;
    }
}
