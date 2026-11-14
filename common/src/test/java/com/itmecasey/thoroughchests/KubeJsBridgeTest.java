package com.itmecasey.thoroughchests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KubeJsBridgeTest {
    @Test
    void writesTheHelperOnceAndNeverOverwrites(@TempDir Path dir) throws IOException {
        assertTrue(KubeJsBridge.writeHelper(dir));
        Path file = dir.resolve("kubejs/startup_scripts/thoroughchests.js");
        assertTrue(Files.readString(file).contains("global.ThoroughChests"));
        Files.writeString(file, "// mine\n");
        assertTrue(KubeJsBridge.writeHelper(dir));
        assertEquals("// mine\n", Files.readString(file));
    }

    @Test
    void nothingHappensWithoutKubeJs(@TempDir Path dir) {
        assertFalse(KubeJsBridge.present());
        assertFalse(KubeJsBridge.install(dir));
        assertFalse(Files.exists(dir.resolve("kubejs")));
    }
}
