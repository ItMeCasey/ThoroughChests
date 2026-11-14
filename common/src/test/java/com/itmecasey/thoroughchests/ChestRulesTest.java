package com.itmecasey.thoroughchests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ChestRulesTest {
    @AfterEach
    void reset() {
        Settings.reset();
        ScriptEvents.clear();
    }

    @Test
    void defaultSlotCountsMatchTheSpec() {
        assertEquals(36, ChestLevel.COPPER.slots());
        assertEquals(54, ChestLevel.IRON.slots());
        assertEquals(72, ChestLevel.GOLD.slots());
        assertEquals(108, ChestLevel.DIAMOND.slots());
        assertEquals(135, ChestLevel.NETHERITE.slots());
    }

    @Test
    void slotCountsComeFromSettingsAndStayInRange() {
        Settings.set("level.gold.slots", 90);
        Settings.set("level.iron.slots", 0);
        Settings.set("level.copper.slots", 100000);
        assertEquals(90, ChestLevel.GOLD.slots());
        assertEquals(1, ChestLevel.IRON.slots());
        assertEquals(ChestLevel.MAX_SLOTS, ChestLevel.COPPER.slots());
    }

    @Test
    void sizeNeverDropsStoredItems() {
        assertEquals(36, ChestLevel.COPPER.sizeFor(10));
        assertEquals(51, ChestLevel.COPPER.sizeFor(50));
    }

    @Test
    void blastResistanceRisesWithEveryLevel() {
        float last = -1;
        for (ChestLevel l : ChestLevel.values()) {
            assertTrue(l.blastResistance() > last, l + " must resist more than the level below");
            last = l.blastResistance();
        }
    }

    @Test
    void onlyNetheriteIsImmuneByDefault() {
        for (ChestLevel l : ChestLevel.values()) {
            assertEquals(l == ChestLevel.NETHERITE, l.explosionImmune(), l.name());
        }
        Settings.set("level.diamond.blast_resistance", 3600000);
        assertTrue(ChestLevel.DIAMOND.explosionImmune());
    }

    @Test
    void aCardMovesAChestExactlyOneLevel() {
        for (ChestLevel card : ChestLevel.values()) {
            for (ChestLevel chest : ChestLevel.values()) {
                assertEquals(chest.ordinal() + 1 == card.ordinal(), card.upgradesFrom(chest), card + " on " + chest);
            }
        }
        assertNull(ChestLevel.NETHERITE.next());
        assertNull(ChestLevel.COPPER.previous());
        assertEquals(ChestLevel.GOLD, ChestLevel.IRON.next());
    }

    @Test
    void levelsHaveIds() {
        assertEquals(ChestLevel.DIAMOND, ChestLevel.byId("diamond"));
        assertNull(ChestLevel.byId("wood"));
        assertEquals("Netherite", ChestLevel.NETHERITE.title());
    }

    @Test
    void augmentItemNames() {
        assertEquals("compact_augment", Augment.COMPACT.itemName(1));
        assertEquals("furnace_augment_2", Augment.FURNACE.itemName(2));
        assertEquals("void_augment", Augment.VOID.itemName(1));
        assertEquals(3, Augment.MAGNET.maxLevel());
        assertEquals(Augment.BLAST_FURNACE, Augment.byId("blast_furnace"));
    }

    @Test
    void levelOneNeedsAnEmptySlot() {
        AugmentRules.Installed one = new AugmentRules.Installed(Augment.MAGNET, 1);
        assertTrue(AugmentRules.canInstall(null, one));
        assertFalse(AugmentRules.canInstall(new AugmentRules.Installed(Augment.FEEDER, 1), one));
        assertFalse(AugmentRules.canInstall(one, one));
    }

    @Test
    void higherLevelsNeedTheLevelBelowOfTheSameAugment() {
        for (Augment a : Augment.values()) {
            for (int have = 1; have <= a.maxLevel(); have++) {
                for (int want = 2; want <= a.maxLevel(); want++) {
                    boolean ok = AugmentRules.canInstall(new AugmentRules.Installed(a, have), new AugmentRules.Installed(a, want));
                    assertEquals(want == have + 1, ok, a + " " + have + " to " + want);
                }
            }
            if (a.maxLevel() > 1) {
                assertFalse(AugmentRules.canInstall(null, new AugmentRules.Installed(a, 2)));
                assertFalse(AugmentRules.canInstall(null, new AugmentRules.Installed(a, 3)));
            }
        }
    }

    @Test
    void furnaceAndBlastFurnaceNeverMix() {
        AugmentRules.Installed furnace = new AugmentRules.Installed(Augment.FURNACE, 1);
        AugmentRules.Installed blast = new AugmentRules.Installed(Augment.BLAST_FURNACE, 1);
        assertFalse(AugmentRules.canInstall(furnace, blast));
        assertFalse(AugmentRules.canInstall(blast, furnace));
        assertFalse(AugmentRules.canInstall(furnace, new AugmentRules.Installed(Augment.BLAST_FURNACE, 2)));
    }

    @Test
    void upgradeConsumesTheOldAugment() {
        AugmentRules.Installed one = new AugmentRules.Installed(Augment.FURNACE, 1);
        AugmentRules.Installed two = new AugmentRules.Installed(Augment.FURNACE, 2);
        assertTrue(AugmentRules.isUpgrade(one, two));
        assertFalse(AugmentRules.isUpgrade(null, one));
    }

    @Test
    void badLevelsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AugmentRules.Installed(Augment.COMPACT, 2));
        assertThrows(IllegalArgumentException.class, () -> new AugmentRules.Installed(Augment.FURNACE, 0));
    }

    @Test
    void comparatorSignalSpansTheWholeInventory() {
        assertEquals(0, Fullness.signal(0, 135, false));
        assertEquals(1, Fullness.signal(0.01F, 135, true));
        assertEquals(8, Fullness.signal(67.5F, 135, true));
        assertEquals(15, Fullness.signal(135, 135, true));
        assertEquals(15, Fullness.signal(500, 135, true));
        int last = 0;
        for (int stacks = 0; stacks <= 135; stacks++) {
            int s = Fullness.signal(stacks, 135, stacks > 0);
            assertTrue(s >= last);
            last = s;
        }
    }

    @Test
    void locksOpenForTheOwnerAndOperators() {
        assertTrue(LockRule.canAccess(null, "a", false, true));
        assertTrue(LockRule.canAccess("a", "a", false, true));
        assertFalse(LockRule.canAccess("a", "b", false, true));
        assertTrue(LockRule.canAccess("a", "b", true, true));
        assertFalse(LockRule.canAccess("a", "b", true, false));
    }

    @Test
    void unknownSettingsThrow() {
        assertThrows(IllegalArgumentException.class, () -> Settings.value("nope"));
        assertThrows(IllegalArgumentException.class, () -> Settings.set("nope", 1));
    }

    @Test
    void everyAugmentNumberIsConfigurable() {
        for (int l = 1; l <= 3; l++) {
            assertTrue(Settings.getDouble("furnace.speed." + l) > 0);
            assertTrue(Settings.getDouble("blast_furnace.fuel_use." + l) > 0);
            assertTrue(Settings.getInt("magnet.radius." + l) > 0);
            assertTrue(Settings.getInt("magnet.interval." + l) > 0);
            assertTrue(Settings.getInt("feeder.range." + l) > 0);
            assertTrue(Settings.getInt("feeder.interval." + l) > 0);
        }
        Settings.set("magnet.radius.2", 20);
        assertEquals(20, Settings.getInt("magnet.radius.2"));
    }

    @Test
    void scriptEventsReachListenersAndSurviveErrors() {
        int[] seen = {0};
        ScriptEvents.on("chest_upgraded", e -> seen[0] += (int) e.number("x"));
        ScriptEvents.on("chest_upgraded", e -> {
            throw new IllegalStateException("script bug");
        });
        ScriptEvents.fire("chest_upgraded", "x", 5, "to", "gold");
        assertEquals(5, seen[0]);
        assertThrows(IllegalArgumentException.class, () -> ScriptEvents.on("nope", e -> { }));
    }

    @Test
    void itemRulesFromScripts() {
        assertTrue(ScriptEvents.allowInsert("minecraft:dirt"));
        ScriptEvents.addInsertFilter(id -> !id.equals("minecraft:dirt"));
        assertFalse(ScriptEvents.allowInsert("minecraft:dirt"));
        assertTrue(ScriptEvents.allowInsert("minecraft:stone"));
    }
}
