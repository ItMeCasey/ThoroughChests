package com.itmecasey.thoroughchests;

/** The arithmetic of the Compact augment. The recipe lookup itself is Minecraft code. */
public final class Compaction {
    private Compaction() {
    }

    /** What one compaction pass of one item type does. */
    public record Plan(int crafts, int consumed, int produced) {
        public static final Plan NONE = new Plan(0, 0, 0);
    }

    /**
     * @param have       items of that type in the chest
     * @param perCraft   items one craft uses (4 for a 2x2 grid, 9 for a 3x3 grid)
     * @param result     items one craft gives
     * @param maxCrafts  most crafts this pass may make
     */
    public static Plan plan(int have, int perCraft, int result, int maxCrafts) {
        if (perCraft <= 0 || result <= 0 || have < perCraft || maxCrafts <= 0) {
            return Plan.NONE;
        }
        int crafts = Math.min(have / perCraft, maxCrafts);
        return new Plan(crafts, crafts * perCraft, crafts * result);
    }

    /** Whether a recipe with these numbers really compacts: it gives fewer items than it uses. */
    public static boolean compacts(int perCraft, int result) {
        return result < perCraft;
    }

    /** Whether the reverse recipe (one result back into items) returns at least as many items as one craft used. */
    public static boolean reversible(int perCraft, int backYield) {
        return backYield >= perCraft;
    }
}
