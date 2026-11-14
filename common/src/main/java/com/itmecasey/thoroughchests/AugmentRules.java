package com.itmecasey.thoroughchests;

import org.jetbrains.annotations.Nullable;

/** The install rule for augments: one per chest, level 1 into an empty slot, level n only on top of level n-1 of the same augment. */
public final class AugmentRules {
    /** An installed or candidate augment: which one and which level. */
    public record Installed(Augment augment, int level) {
        public Installed {
            if (level < 1 || level > augment.maxLevel()) {
                throw new IllegalArgumentException("Bad level " + level + " for " + augment);
            }
        }
    }

    private AugmentRules() {
    }

    /** Whether the candidate may go into the slot that currently holds {@code current} (null for empty). */
    public static boolean canInstall(@Nullable Installed current, Installed candidate) {
        if (candidate.level() == 1) {
            return current == null;
        }
        return current != null && current.augment() == candidate.augment() && current.level() == candidate.level() - 1;
    }

    /** Whether installing the candidate consumes the installed item (an upgrade) rather than filling an empty slot. */
    public static boolean isUpgrade(@Nullable Installed current, Installed candidate) {
        return current != null && canInstall(current, candidate);
    }
}
