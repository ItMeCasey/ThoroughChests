package com.itmecasey.thoroughchests.script;

import com.itmecasey.thoroughchests.KubeJsBridge;
import java.nio.file.Path;

/** The scripting entry points: the one API object both ModJS and KubeJS scripts use, and the installers for each. */
public final class Bridges {
    public static final ChestsApi API = new ChestsApi();

    private Bridges() {
    }

    /** Called once from each loader's mod constructor. Returns a short text of what was hooked up, for the log. */
    public static String install(Path gameDir) {
        boolean modjs = ScriptBridge.install();
        boolean kubejs = KubeJsBridge.install(gameDir);
        return (modjs ? "ModJS " : "") + (kubejs ? "KubeJS" : "");
    }
}
