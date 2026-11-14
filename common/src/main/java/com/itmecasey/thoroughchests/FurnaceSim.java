package com.itmecasey.thoroughchests;

/**
 * The timing of a Furnace or Blast Furnace augment, without Minecraft. One item at a time: {@link #start} takes the fuel it needs from
 * the burn bank, {@link #tick} counts the cook down. Speed shortens the cook time; the fuel factor shrinks the fuel an item costs.
 */
public final class FurnaceSim {
    private double burnBank;
    private int cookLeft;
    private boolean cooking;

    /** Burn ticks an item of this recipe costs: recipe time times the level's fuel factor. */
    public static double fuelCost(int recipeTime, double fuelUse) {
        return recipeTime * fuelUse;
    }

    /** Ticks the cook takes: recipe time divided by the level's speed, at least one. */
    public static int cookTicks(int recipeTime, double speed) {
        return Math.max(1, (int) Math.ceil(recipeTime / Math.max(0.01, speed)));
    }

    public double burnBank() {
        return burnBank;
    }

    public boolean cooking() {
        return cooking;
    }

    public int cookLeft() {
        return cookLeft;
    }

    /** Adds the burn time of one fuel item to the bank. */
    public void addFuel(double burnTicks) {
        burnBank += burnTicks;
    }

    /** Whether the bank pays for an item. */
    public boolean canAfford(double cost) {
        return burnBank + 1.0E-9 >= cost;
    }

    /** Starts cooking an item if none is cooking and the bank pays for it. */
    public boolean start(int recipeTime, double speed, double fuelUse) {
        double cost = fuelCost(recipeTime, fuelUse);
        if (cooking || !canAfford(cost)) {
            return false;
        }
        burnBank -= cost;
        cookLeft = cookTicks(recipeTime, speed);
        cooking = true;
        return true;
    }

    /** Advances one tick; true on the tick the item finishes. */
    public boolean tick() {
        if (!cooking) {
            return false;
        }
        if (--cookLeft <= 0) {
            cooking = false;
            cookLeft = 0;
            return true;
        }
        return false;
    }

    public void load(double bank, int left, boolean isCooking) {
        burnBank = bank;
        cookLeft = left;
        cooking = isCooking;
    }
}
