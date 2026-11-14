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


def crop_center(img, size):
    w, h = img.size
    x = (w - size) // 2
    y = (h - size) // 2
    return img.crop((x, y, x + size, y + size))


def rimmed_plate(src, x0, y0, w, h, canvas, shift=0):
    px = canvas.load()
    sp = src.load()
    sw, sh = src.size
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            ring = min(x - x0, x0 + w - 1 - x, y - y0, y0 + h - 1 - y)
            c = sp[(x + shift) % sw, (y + shift) % sh][:3]
            if ring == 0:
                c = shade(c, 0.45)
            elif ring == 1:
                c = shade(c, 1.1 if (x - x0 == 1 or y - y0 == 1) else 0.85)
            px[x, y] = (c[0], c[1], c[2], 255)


def card(vanilla, level):
    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rimmed_plate(vanilla.get(MATERIAL[level]), 3, 1, 10, 14, canvas)
    paper = vanilla.get("item/paper")
    pp = paper.load()
    cp = canvas.load()
    colors = [pp[x, y][:3] for x in range(16) for y in range(16) if pp[x, y][3] > 0]
    colors.sort(key=sum)
    light = colors[int(len(colors) * 0.9)]
    for y in range(3, 13):
        for x in range(5, 11):
            cp[x, y] = (light[0], light[1], light[2], 255)
    arrow = shade(vanilla.get(MATERIAL[level]).getpixel((4, 4)), 0.55)
    for (x, y) in [(7, 4), (8, 4), (6, 5), (9, 5), (5, 6), (10, 6), (7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)]:
        cp[x, y] = (arrow[0], arrow[1], arrow[2], 255)
    return canvas


HEARTS = {
    "compact": "block/crafting_table_front",
    "furnace": "block/furnace_front",
    "blast_furnace": "block/blast_furnace_front",
    "magnet": "block/lodestone_side",
    "feeder": "block/hay_block_side",
    "void": "block/obsidian",
}
RIMS = {1: "block/iron_block", 2: "block/gold_block", 3: "block/diamond_block"}


def augment(vanilla, kind, level):
    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rimmed_plate(vanilla.get(RIMS[level]), 2, 2, 12, 11, canvas, shift=level)
    heart = crop_center(vanilla.get(HEARTS[kind]), 6)
    cp = canvas.load()
    hp = heart.load()
    for y in range(6):
        for x in range(6):
            c = hp[x, y]
            cp[5 + x, 4 + y] = (c[0], c[1], c[2], 255)
    gold = vanilla.get("block/gold_block")
    pips = [gold.getpixel((i * 3 % 16, i * 5 % 16))[:3] for i in range(3)]
    # Level pips under the plate: one gold pixel pair per level.
    for i in range(level):
        for dx in range(2):
            cp[4 + i * 3 + dx, 14] = (pips[i][0], pips[i][1], pips[i][2], 255)
            cp[4 + i * 3 + dx, 13] = (pips[i][0], pips[i][1], pips[i][2], 255)
    return canvas


def lock(vanilla):
    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    iron = vanilla.get("block/iron_block")
    gold = vanilla.get("block/gold_block")
    cp = canvas.load()
    ip = iron.load()
    # Shackle
    for (x, y) in [(5, 2), (6, 1), (7, 1), (8, 1), (9, 1), (10, 2), (5, 3), (10, 3), (5, 4), (10, 4), (5, 5), (10, 5)]:
        c = ip[x % 16, y % 16][:3]
        cp[x, y] = (c[0], c[1], c[2], 255)
    # Body
    rimmed_plate(gold, 3, 6, 10, 8, canvas, shift=2)
    k = shade(gold.getpixel((2, 2)), 0.4)
    for (x, y) in [(8, 8), (7, 9), (8, 9), (8, 10), (8, 11)]:
        cp[x, y] = (k[0], k[1], k[2], 255)
    return canvas


def save(img, *parts):
    path = os.path.join(OUT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return path


def main():
    vanilla = Vanilla(sys.argv[1] if len(sys.argv) > 1 else None)
    base = vanilla.get("entity/chest/normal")
    sheet = Image.new("RGBA", (64 * 5 + 16 * 14 + 40, 64), (60, 60, 60, 255))
    sx = 0
    for i, level in enumerate(LEVELS):
        img = frame_chest(base, vanilla.get(FRAME_SOURCE[level]), i + 1)
        save(img, "entity", "chest", f"{level}.png")
        sheet.paste(img, (sx, 0))
        sx += 64
    icons = []
    for level in LEVELS[1:]:
        img = card(vanilla, level)
        save(img, "item", f"{level}_upgrade_card.png")
        icons.append(img)
    for kind, maxlevel in (("compact", 1), ("furnace", 3), ("blast_furnace", 3), ("magnet", 3), ("feeder", 3), ("void", 1)):
        for lv in range(1, maxlevel + 1):
            name = f"{kind}_augment" if maxlevel == 1 else f"{kind}_augment_{lv}"
            img = augment(vanilla, kind, lv)
            save(img, "item", f"{name}.png")
            icons.append(img)
    img = lock(vanilla)
    save(img, "item", "chest_lock.png")
    icons.append(img)
    for n, icon in enumerate(icons[: 15]):
        sheet.paste(icon, (sx + n * 16 // 1 if False else sx + (n % 15) * 16, 0))
    # A preview of everything at 4x for a quick look (not shipped).
    preview = os.path.join(ROOT, "docs", "frames", "textures_preview.png")
    os.makedirs(os.path.dirname(preview), exist_ok=True)
    full = Image.new("RGBA", (64 * 5, 64 + 16 * 2), (60, 60, 60, 255))
    for n, level in enumerate(LEVELS):
        full.paste(Image.open(os.path.join(OUT, "entity", "chest", f"{level}.png")), (n * 64, 0))
    for n, icon in enumerate(icons):
        full.paste(icon, ((n % 20) * 16, 64 + (n // 20) * 16))
    full.resize((full.width * 3, full.height * 3), Image.NEAREST).save(preview)
    print("textures written to", OUT)


if __name__ == "__main__":
    main()
