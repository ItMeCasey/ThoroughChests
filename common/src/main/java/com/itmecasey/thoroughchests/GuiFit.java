package com.itmecasey.thoroughchests;

/** Picks how many chest rows fit a screen, so the screen works at any window size and GUI scale. */
public final class GuiFit {
    public static final int MIN_ROWS = 1;
    public static final int MAX_ROWS = 12;
    /** Pixels of the title and search strip above the chest rows. */
    public static final int HEAD = 36;
    /** Pixels of one slot row. */
    public static final int ROW = 18;
    /** Pixels of a strip between the chest rows and the player inventory (none: the screen has only the rows and the inventory). */
    public static final int TOOLS = 0;
    /** Pixels of the player inventory block (label, three rows, gap, hotbar, padding). */
    public static final int PLAYER = 90;
    /** Pixels of the strip below the player inventory. */
    public static final int FOOT = 7;

    private GuiFit() {
    }

    /** Height of a screen with the given number of rows. */
    public static int height(int rows) {
        return HEAD + rows * ROW + TOOLS + PLAYER + FOOT;
    }

    /**
     * @param scaledHeight screen height in GUI pixels
     * @param wantedRows   rows the player or the chest wants
     */
    public static int rows(int scaledHeight, int wantedRows) {
        int rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, wantedRows));
        while (rows > MIN_ROWS && height(rows) > scaledHeight) {
            rows--;
        }
        return rows;
    }

    /** Whether the side panel fits to the right of the main panel (176 wide) at this width. */
    public static boolean sideFits(int scaledWidth, int sideWidth) {
        return scaledWidth >= 176 + sideWidth + 8;
    }

    /** True when even the smallest layout does not fit, so the screen scrolls its whole content. */
    public static boolean tooSmall(int scaledHeight) {
        return height(MIN_ROWS) > scaledHeight;
    }
}
