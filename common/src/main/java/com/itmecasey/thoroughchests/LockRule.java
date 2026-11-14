package com.itmecasey.thoroughchests;

import org.jetbrains.annotations.Nullable;

/** Who may use a locked chest. */
public final class LockRule {
    private LockRule() {
    }

    /**
     * @param owner       the owner's UUID text, or null when the chest has no lock
     * @param player      the player's UUID text
     * @param isOperator  whether the player is an operator
     * @param opsBypass   the {@code lock.ops_bypass} setting
     */
    public static boolean canAccess(@Nullable String owner, String player, boolean isOperator, boolean opsBypass) {
        if (owner == null || owner.isEmpty() || owner.equals(player)) {
            return true;
        }
        return opsBypass && isOperator;
    }
}
