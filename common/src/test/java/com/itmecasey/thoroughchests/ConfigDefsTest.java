package com.itmecasey.thoroughchests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        assertTrue(keys.size() > 40);
    }

    @Test
    void keysAreValidTomlPaths() {
        for (Settings.Def d : Settings.defs()) {
            assertTrue(d.key().matches("[a-z0-9_]+([.][a-z0-9_]+)+"), d.key());
        }
    }

    @Test
    void legacyFileIsReadAndLeftUntouched(@TempDir Path dir) throws IOException {
        Path old = dir.resolve("thoroughchests.properties");
        String text = "level.gold.slots=90\nnot.a.key=1\nmagnet.radius.1=6\n";
        Files.writeString(old, text);
        Map<String, String> values = Settings.legacyValues(dir);
        assertEquals(Map.of("level.gold.slots", "90", "magnet.radius.1", "6"), values);
        assertEquals(text, Files.readString(old));
    }

    @Test
    void missingLegacyFileGivesNothing(@TempDir Path dir) {
        assertTrue(Settings.legacyValues(dir).isEmpty());
        assertFalse(Files.exists(dir.resolve("thoroughchests.properties")));
    }
}
