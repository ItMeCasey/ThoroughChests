package com.itmecasey.thoroughchests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MachineryTest {
    @Test
    void furnaceNeedsFuelBeforeItStarts() {
        FurnaceSim sim = new FurnaceSim();
        assertFalse(sim.start(200, 1.0, 1.0));
        sim.addFuel(1600);
        assertTrue(sim.start(200, 1.0, 1.0));
        assertEquals(1400, sim.burnBank(), 1e-6);
    }

    @Test
    void cookTakesRecipeTimeDividedBySpeed() {
        assertEquals(200, FurnaceSim.cookTicks(200, 1.0));
        assertEquals(100, FurnaceSim.cookTicks(200, 2.0));
        assertEquals(50, FurnaceSim.cookTicks(200, 4.0));
        assertEquals(1, FurnaceSim.cookTicks(1, 100.0));
        FurnaceSim sim = new FurnaceSim();
        sim.addFuel(1000);
        sim.start(200, 4.0, 1.0);
        int ticks = 0;
        while (!sim.tick()) {
            ticks++;
            assertTrue(ticks < 1000);
        }
        assertEquals(49, ticks);
        assertFalse(sim.cooking());
    }

    @Test
    void higherLevelsSaveFuel() {
        double l1 = FurnaceSim.fuelCost(200, 1.0);
        double l2 = FurnaceSim.fuelCost(200, 0.75);
        double l3 = FurnaceSim.fuelCost(200, 0.5);
        assertTrue(l1 > l2 && l2 > l3);
        FurnaceSim sim = new FurnaceSim();
        sim.addFuel(1600);
        int items = 0;
        while (sim.start(200, 1.0, 0.5)) {
            while (!sim.tick()) {
                // run the cook to the end
            }
            items++;
        }
        assertEquals(16, items);
    }

    @Test
    void onlyOneItemCooksAtATime() {
        FurnaceSim sim = new FurnaceSim();
        sim.addFuel(10000);
        assertTrue(sim.start(100, 1.0, 1.0));
        assertFalse(sim.start(100, 1.0, 1.0));
    }

    @Test
    void furnaceStateRoundTrips() {
        FurnaceSim a = new FurnaceSim();
        a.addFuel(500);
        a.start(200, 2.0, 1.0);
        a.tick();
        FurnaceSim b = new FurnaceSim();
        b.load(a.burnBank(), a.cookLeft(), a.cooking());
        assertEquals(a.burnBank(), b.burnBank(), 1e-9);
        assertEquals(a.cookLeft(), b.cookLeft());
        assertTrue(b.cooking());
    }

    @Test
    void compactionMath() {
        assertEquals(Compaction.Plan.NONE, Compaction.plan(8, 9, 1, 16));
        Compaction.Plan p = Compaction.plan(100, 9, 1, 16);
        assertEquals(11, p.crafts());
        assertEquals(99, p.consumed());
        assertEquals(11, p.produced());
        assertEquals(2, Compaction.plan(100, 9, 1, 2).crafts());
        assertEquals(Compaction.Plan.NONE, Compaction.plan(100, 9, 1, 0));
        assertEquals(4, Compaction.plan(17, 4, 1, 16).crafts());
    }

    @Test
    void onlyRealCompactionCounts() {
        assertTrue(Compaction.compacts(9, 1));
        assertTrue(Compaction.compacts(4, 1));
        assertFalse(Compaction.compacts(4, 4));
        assertFalse(Compaction.compacts(4, 8));
        assertTrue(Compaction.reversible(9, 9));
        assertFalse(Compaction.reversible(4, 1));
    }

    @Test
    void windowShowsEverythingWithoutAQuery() {
        List<Integer> all = Window.filtered(20, "", i -> "");
        assertEquals(20, all.size());
        assertEquals(5, Window.map(all, 0, 5));
        assertEquals(14, Window.map(all, 1, 5));
        assertEquals(-1, Window.map(all, 2, 5));
    }

    @Test
    void searchMatchesWordsInAnyOrder() {
        String[] names = {"iron ingot minecraft", "gold ingot minecraft", "", "iron pickaxe @minecraft #pickaxes"};
        List<Integer> f = Window.filtered(4, "Iron ingot", i -> names[i]);
        assertEquals(List.of(0), f);
        assertEquals(List.of(0, 3), Window.filtered(4, "iron", i -> names[i]));
        assertEquals(List.of(3), Window.filtered(4, "#pickaxes", i -> names[i]));
        assertEquals(List.of(0, 1, 3), Window.filtered(4, "@minecraft ", i -> names[i].isEmpty() ? "" : names[i] + " @minecraft"));
        assertTrue(Window.filtered(4, "diamond", i -> names[i]).isEmpty());
    }

    @Test
    void scrollStaysInRange() {
        assertEquals(0, Window.maxScroll(54, 6));
        assertEquals(9, Window.maxScroll(135, 6));
        assertEquals(0, Window.maxScroll(0, 6));
        assertEquals(9, Window.clampScroll(50, 135, 6));
        assertEquals(0, Window.clampScroll(-3, 135, 6));
        assertEquals(15, Window.totalRows(135));
        assertEquals(1, Window.totalRows(1));
    }

    @Test
    void rowsFitEveryScreenHeight() {
        for (int h = 150; h <= 1200; h += 7) {
            int rows = GuiFit.rows(h, GuiFit.MAX_ROWS);
            assertTrue(rows >= GuiFit.MIN_ROWS && rows <= GuiFit.MAX_ROWS);
            if (!GuiFit.tooSmall(h)) {
                assertTrue(GuiFit.height(rows) <= h, "rows " + rows + " do not fit height " + h);
            }
            if (rows < GuiFit.MAX_ROWS && !GuiFit.tooSmall(h)) {
                assertTrue(GuiFit.height(rows + 1) > h, "one more row would still fit height " + h);
            }
        }
    }

    @Test
    void commonWindowSizesAndGuiScales() {
        int[][] windows = {{854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440}, {3840, 2160}};
        for (int[] w : windows) {
            for (int scale = 1; scale <= 6; scale++) {
                int h = w[1] / scale;
                int rows = GuiFit.rows(h, 6);
                if (!GuiFit.tooSmall(h)) {
                    assertTrue(GuiFit.height(rows) <= h, w[0] + "x" + w[1] + " at scale " + scale);
                }
            }
        }
        assertEquals(6, GuiFit.rows(1080 / 3, 6));
        assertTrue(GuiFit.tooSmall(480 / 4));
    }

    @Test
    void sideRoomCheck() {
        assertTrue(GuiFit.sideFits(400, 70));
        assertFalse(GuiFit.sideFits(200, 70));
    }
}
