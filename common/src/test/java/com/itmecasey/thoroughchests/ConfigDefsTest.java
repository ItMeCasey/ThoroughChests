package com.itmecasey.thoroughchests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The definitions the loader config specs are built from. */
class ConfigDefsTest {
    @Test
    void everyDefaultIsInsideItsRangeAndHasADescription() {
        Set<String> keys = new HashSet<>();
        for (Settings.Def d : Settings.defs()) {
            assertTrue(keys.add(d.key()), "duplicate " + d.key());
            assertFalse(d.comment().isBlank(), d.key());
            if (d.kind() != Settings.Kind.BOOL) {
                double v = Double.parseDouble(d.fallback());
                assertTrue(d.min() <= v && v <= d.max(), d.key() + " default " + v + " outside " + d.min() + ".." + d.max());
                assertTrue(d.min() < d.max(), d.key());
            }
        }
        assertTrue(keys.size() > 30);
    }

    @Test
    void keysAreValidTomlPaths() {
        for (Settings.Def d : Settings.defs()) {
            assertTrue(d.key().matches("[a-z0-9_]+([.][a-z0-9_]+)+"), d.key());
        }
    }
}
