"""Builds every texture of Thorough Chests from vanilla Minecraft textures, so they can be regenerated at any time.

Usage: python scripts/make_textures.py [path to a Minecraft client jar or a folder with assets/minecraft/textures]

The vanilla textures are read from the jar (the default is the dev jar Gradle leaves in neoforge-26.2/build/moddev/artifacts); nothing
from it is copied into the repository except what is generated here.

Chest textures: the vanilla chest texture (64x64) is kept as it is, and a two pixel frame is laid around every face of the lid and body.
The frame takes its pixels from the vanilla block texture of the level below: Copper gets the vanilla planks look, Iron a copper
frame, Gold an iron frame, Diamond a gold frame, Netherite a diamond frame. The outer ring is the same texture darkened the way the vanilla
chest outline is, the inner ring is lit from the top left. Item icons (upgrade cards, augments, lock) are drawn the same way from vanilla
palettes. The textures are not approved art.
"""
import glob
import io
import os
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "thoroughchests", "textures")
LEVELS = ["copper", "iron", "gold", "diamond", "netherite"]
FRAME_SOURCE = {
    "copper": "block/spruce_planks",
    "iron": "block/copper_block",
    "gold": "block/iron_block",
    "diamond": "block/gold_block",
    "netherite": "block/diamond_block",
}
# Material that stands for a level on cards and augment rims.
MATERIAL = {
    "copper": "block/copper_block", "iron": "block/iron_block", "gold": "block/gold_block",
    "diamond": "block/diamond_block", "netherite": "block/netherite_block",
}
# The faces of the 64x64 chest texture: (x, y, width, height). The lock at the top left is left alone.
FACES = [
    (14, 0, 14, 14), (28, 0, 14, 14),
    (0, 14, 14, 5), (14, 14, 14, 5), (28, 14, 14, 5), (42, 14, 14, 5),
    (14, 19, 14, 14), (28, 19, 14, 14),
    (0, 33, 14, 10), (14, 33, 14, 10), (28, 33, 14, 10), (42, 33, 14, 10),
]


class Vanilla:
    def __init__(self, source=None):
        self.zip = None
        self.folder = None
        if source and os.path.isdir(source):
            self.folder = source
        else:
            path = source
            if not path:
                found = sorted(glob.glob(os.path.join(ROOT, "neoforge-26.2", "build", "moddev", "artifacts", "minecraft-patched-*.jar")))
                found = [f for f in found if not f.endswith("-sources.jar") and not f.endswith("-merged.jar")] or found
                if not found:
                    sys.exit("No Minecraft jar found. Build neoforge-26.2 once or pass a jar path.")
                path = found[0]
            self.zip = zipfile.ZipFile(path)

    def get(self, name):
        rel = f"assets/minecraft/textures/{name}.png"
        if self.folder:
            return Image.open(os.path.join(self.folder, rel)).convert("RGBA")
        return Image.open(io.BytesIO(self.zip.read(rel))).convert("RGBA")


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb[:3])


def frame_chest(base, src, tile_shift):
    out = base.copy()
    px = out.load()
    sp = src.load()
    sw, sh = src.size
    for face, (x0, y0, w, h) in enumerate(FACES):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                ring = min(x - x0, x0 + w - 1 - x, y - y0, y0 + h - 1 - y)
                if ring > 1:
                    continue
                c = sp[(x + tile_shift * (face + 1)) % sw, (y + tile_shift * 2 * (face + 1)) % sh]
                rgb = c[:3]
                if ring == 0:
                    rgb = shade(rgb, 0.5)
                else:
                    lit = (x - x0 == 1 or y - y0 == 1)
                    rgb = shade(rgb, 1.12 if lit else 0.86)
                px[x, y] = (rgb[0], rgb[1], rgb[2], 255)
    return out


def save(img, *parts):
    path = os.path.join(OUT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return path


ITEM_TEXTURES = ["block/furnace_front_on", "block/blast_furnace_front_on", "block/crafting_table_top", "block/iron_block", "block/gold_block",
                 "block/diamond_block", "block/netherite_block", "block/redstone_block", "block/obsidian"]
AUGMENT_KINDS = (("compact", 1), ("furnace", 3), ("blast_furnace", 3), ("magnet", 3), ("feeder", 3), ("void", 1))


def item_name(kind, maxlevel, level):
    return f"{kind}_augment" if maxlevel == 1 else f"{kind}_augment_{level}"


def review_sheet(entries, path):
    """A sheet with every icon at 1x and at 8x, so they can be looked at one by one."""
    cols = 8
    rows = (len(entries) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 140, rows * 150), (72, 72, 80, 255))
    for i, (name, icon) in enumerate(entries):
        x = (i % cols) * 140
        y = (i // cols) * 150
        big = icon.resize((icon.width * 8, icon.height * 8), Image.NEAREST)
        sheet.alpha_composite(big, (x + 6, y + 4))
        sheet.alpha_composite(icon, (x + 6, y + 4 + 16 * 8 + 4))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.save(path)


def main():
    import item_art
    vanilla = Vanilla(sys.argv[1] if len(sys.argv) > 1 else None)
    base = vanilla.get("entity/chest/normal")
    for i, level in enumerate(LEVELS):
        save(frame_chest(base, vanilla.get(FRAME_SOURCE[level]), i + 1), "entity", "chest", f"{level}.png")
    v = {name.split("/")[-1]: vanilla.get(name) for name in ITEM_TEXTURES}
    entries = []
    for tier in LEVELS[1:]:
        icon = item_art.card(v, tier)
        save(icon, "item", f"{tier}_upgrade_card.png")
        entries.append((f"{tier}_upgrade_card", icon))
    for kind, maxlevel in AUGMENT_KINDS:
        for lv in range(1, maxlevel + 1):
            name = item_name(kind, maxlevel, lv)
            icon = item_art.augment_icon(v, kind, lv)
            save(icon, "item", f"{name}.png")
            entries.append((name, icon))
            # The chest overlay: the same 12x12 design on the top face of the 4x4x1 plate model (4x4 chest pixels on the top of the lid, top right: a 64x64 layer with a 192x192 sheet, the top face, which model space calls the bottom region, at 24,0, turned half a turn so it reads upright from the front of the chest: one design pixel is a third of a chest pixel).
            sheet = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
            sheet.alpha_composite(item_art.augment_canvas(v, kind, lv).rotate(180), (24, 0))
            save(sheet, "entity", "chest", f"overlay_{name}.png")
    review_sheet(entries, os.path.join(ROOT, "docs", "frames", "textures_review.png"))
    chests = Image.new("RGBA", (64 * 5, 64), (60, 60, 60, 255))
    for n, level in enumerate(LEVELS):
        chests.paste(Image.open(os.path.join(OUT, "entity", "chest", f"{level}.png")), (n * 64, 0))
    chests.resize((chests.width * 3, chests.height * 3), Image.NEAREST).save(os.path.join(ROOT, "docs", "frames", "textures_chests.png"))
    print("textures written to", OUT)


if __name__ == "__main__":
    main()
