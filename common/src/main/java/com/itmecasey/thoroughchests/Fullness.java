package com.itmecasey.thoroughchests;

/** Comparator signal across a whole inventory, however many slots it has. */
public final class Fullness {
    private Fullness() {
    }

    /**
     * @param sum   the sum over all slots of count divided by that stack's maximum size
     * @param slots the number of slots in the chest
     * @param any   whether at least one slot holds something
     */
    public static int signal(float sum, int slots, boolean any) {
        if (!any || slots <= 0) {
            return 0;
        }
        float fill = Math.min(1.0F, Math.max(0.0F, sum / slots));
        return 1 + (int) Math.floor(fill * 14.0F);
    }
}
