package com.itmecasey.thoroughchests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Checks the generated textures and the model files every chest, card and augment needs. */
class AssetTest {
    private static final String BASE = "/assets/thoroughchests/";

    private static BufferedImage image(String path) throws IOException {
        try (InputStream in = AssetTest.class.getResourceAsStream(BASE + path)) {
            assertNotNull(in, "missing " + path);
            return ImageIO.read(in);
        }
    }

    private static int alpha(BufferedImage img, int x, int y) {
        return (img.getRGB(x, y) >>> 24) & 0xFF;
    }

    private static boolean exists(String path) {
        return AssetTest.class.getResource(BASE + path) != null;
    }

    @Test
    void everyChestHasATextureOfTheVanillaSize() throws IOException {
        for (ChestLevel level : ChestLevel.values()) {
            BufferedImage img = image("textures/entity/chest/" + level.id() + ".png");
            assertEquals(64, img.getWidth());
            assertEquals(64, img.getHeight());
            assertTrue(alpha(img, 20, 20) == 255, level + " body must be opaque");
        }
    }

    @Test
    void everyChestHasBlockstateModelsAndItemDefinition() {
        for (ChestLevel level : ChestLevel.values()) {
            String id = level.id() + "_chest";
            assertTrue(exists("blockstates/" + id + ".json"), id);
            assertTrue(exists("models/block/" + id + ".json"), id);
            assertTrue(exists("models/item/" + id + ".json"), id);
            assertTrue(exists("items/" + id + ".json"), id);
        }
    }

    @Test
    void everyAugmentHasAnIconAnOverlayAndModels() throws IOException {
        for (Augment a : Augment.values()) {
            for (int l = 1; l <= a.maxLevel(); l++) {
                String name = a.itemName(l);
                BufferedImage icon = image("textures/item/" + name + ".png");
                assertEquals(16, icon.getWidth(), name);
                assertEquals(16, icon.getHeight(), name);
                assertTrue(exists("models/item/" + name + ".json"), name);
                assertTrue(exists("items/" + name + ".json"), name);
                assertTrue(exists("lang/en_us.json"));
                BufferedImage sheet = image("textures/entity/chest/overlay_" + name + ".png");
                assertEquals(192, sheet.getWidth(), name);
                assertEquals(192, sheet.getHeight(), name);
            }
        }
    }

    @Test
    void overlayArtSitsOnTheTopFaceOfThePlateAndNowhereElse() throws IOException {
        for (Augment a : Augment.values()) {
            for (int l = 1; l <= a.maxLevel(); l++) {
                BufferedImage sheet = image("textures/entity/chest/overlay_" + a.itemName(l) + ".png");
                int inside = 0;
                for (int y = 0; y < 192; y++) {
                    for (int x = 0; x < 192; x++) {
                        boolean front = x >= 24 && x < 36 && y >= 0 && y < 12;
                        if (alpha(sheet, x, y) > 0) {
                            assertTrue(front, a.itemName(l) + " has a pixel outside the front face at " + x + "," + y);
                            inside++;
                        }
                    }
                }
                assertTrue(inside >= 30, a.itemName(l) + " draws too little: " + inside);
            }
        }
    }

    @Test
    void overlayIsCenteredOnItsFace() throws IOException {
        for (Augment a : Augment.values()) {
            BufferedImage sheet = image("textures/entity/chest/overlay_" + a.itemName(1) + ".png");
            int minX = 999, maxX = -1, minY = 999, maxY = -1;
            for (int y = 0; y < 192; y++) {
                for (int x = 0; x < 192; x++) {
                    if (alpha(sheet, x, y) > 0) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            int left = minX - 24;
            int right = 35 - maxX;
            int top = minY;
            int bottom = 11 - maxY;
            assertTrue(Math.abs(left - right) <= 1, a + " is off center horizontally: " + left + " vs " + right);
            assertTrue(Math.abs(top - bottom) <= 1, a + " is off center vertically: " + top + " vs " + bottom);
        }
    }

    @Test
    void levelsAreTheOutlineColor() throws IOException {
        for (Augment a : new Augment[] {Augment.FURNACE, Augment.MAGNET}) {
            int[] seen = new int[4];
            for (int l = 1; l <= 3; l++) {
                BufferedImage icon = image("textures/item/" + a.itemName(l) + ".png");
                int bestCount = 0;
                int color = 0;
                java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        if (alpha(icon, x, y) > 0) {
                            counts.merge(icon.getRGB(x, y), 1, Integer::sum);
                        }
                    }
                }
                for (var e : counts.entrySet()) {
                    if (e.getValue() > bestCount) {
                        bestCount = e.getValue();
                        color = e.getKey();
                    }
                }
                seen[l] = color;
            }
            assertTrue(seen[1] != seen[2] && seen[2] != seen[3] && seen[1] != seen[3], a + " levels must look different");
        }
    }

    @Test
    void cardsHaveIcons() throws IOException {
        for (ChestLevel level : ChestLevel.values()) {
            if (level != ChestLevel.COPPER) {
                assertEquals(16, image("textures/item/" + level.id() + "_upgrade_card.png").getWidth());
            }
        }
    }
}
