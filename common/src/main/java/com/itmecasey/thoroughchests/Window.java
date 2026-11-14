package com.itmecasey.thoroughchests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

/** Which container slot a window slot shows, with search and scroll. */
public final class Window {
    public static final int COLS = 9;

    private Window() {
    }

    /** Indices of the container slots whose search text matches the query; an empty query matches every slot, empty ones included. */
    public static List<Integer> filtered(int size, String query, IntFunction<String> searchText) {
        List<Integer> out = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < size; i++) {
            if (q.isEmpty() || matches(searchText.apply(i), q)) {
                out.add(i);
            }
        }
        return out;
    }

    /** True when the slot's text holds every word of the query (words are separated by spaces). */
    public static boolean matches(String text, String lowerQuery) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.toLowerCase(Locale.ROOT);
        for (String word : lowerQuery.split("\\s+")) {
            if (!word.isEmpty() && !t.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /** Rows of the filtered list. */
    public static int totalRows(int filteredCount) {
        return (filteredCount + COLS - 1) / COLS;
    }

    /** Largest scroll value (in rows) for a list and a number of visible rows. */
    public static int maxScroll(int filteredCount, int visibleRows) {
        return Math.max(0, totalRows(filteredCount) - visibleRows);
    }

    public static int clampScroll(int scroll, int filteredCount, int visibleRows) {
        return Math.max(0, Math.min(scroll, maxScroll(filteredCount, visibleRows)));
    }

    /** The container slot shown by window slot {@code windowSlot}, or -1 when the window slot has nothing behind it. */
    public static int map(List<Integer> filtered, int scroll, int windowSlot) {
        int index = scroll * COLS + windowSlot;
        return index >= 0 && index < filtered.size() ? filtered.get(index) : -1;
    }
}
