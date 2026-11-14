package com.itmecasey.thoroughchests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * KubeJS support without a compile-time dependency on any KubeJS version: scripts reach the API through KubeJS's own
 * {@code Java.loadClass}. When KubeJS is installed, a small startup script is written once to
 * {@code kubejs/startup_scripts/thoroughchests.js}; it puts the API in {@code global.ThoroughChests} so every script type can use it.
 * An existing file is never overwritten. KubeJS is optional; without it nothing happens.
 */
public final class KubeJsBridge {
    private static final Logger LOG = Logger.getLogger("thoroughchests");

    static final String SCRIPT = "// Written by Thorough Chests. An existing file is never overwritten.\n"
            + "// Use global.ThoroughChests from any script: global.ThoroughChests.on('chest_upgraded', e => console.log(e.string('to')))\n"
            + "global.ThoroughChests = Java.loadClass('com.itmecasey.thoroughchests.script.Bridges').API\n";

    private KubeJsBridge() {
    }

    /** True when a KubeJS class is visible. */
    public static boolean present() {
        for (String name : new String[] {"dev.latvian.mods.kubejs.KubeJS", "dev.latvian.mods.kubejs.KubeJSPlugin"}) {
            try {
                Class.forName(name, false, KubeJsBridge.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                // try the next name
            }
        }
        return false;
    }

    /** Writes the helper script into the game folder's kubejs when KubeJS is installed; returns whether KubeJS was found. */
    public static boolean install(Path gameDir) {
        return present() && writeHelper(gameDir);
    }

    /** Writes the helper script if it is missing. Returns whether the file exists afterwards. */
    static boolean writeHelper(Path gameDir) {
        Path file = gameDir.resolve("kubejs").resolve("startup_scripts").resolve("thoroughchests.js");
        try {
            if (Files.exists(file)) {
                return Files.isRegularFile(file);
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, SCRIPT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            LOG.info("ThoroughChests: wrote " + file + " so KubeJS scripts get the ThoroughChests global");
        } catch (FileAlreadyExistsException e) {
            return Files.isRegularFile(file);
        } catch (IOException e) {
            LOG.warning("ThoroughChests: could not write the KubeJS helper script: " + e);
            return false;
        }
        return true;
    }
}
