"""Pixel designs for the augment, upgrade card and lock icons of Thorough Chests.

Every design is built the way vanilla items are: a dark outline, three tones of one hue (light on the top left, dark on the bottom
right) and palettes sampled from vanilla textures. The furnace and blast furnace faces are cut from the vanilla block textures; the
magnet, the food shape, the crafting grid and the void swirl are drawn here.

An augment design is a 12x12 canvas: the symbol with its outline in the top ten rows, level pips in the last row. The item icon puts
that canvas in the middle of a 16x16 sprite and the chest overlay uses the same canvas as it is. Cards are 16x16.
"""
import math

from PIL import Image

OUTLINE = (30, 22, 26)


def px(img):
    return img.load()


def sample(src, x, y):
    return src.getpixel((x % src.width, y % src.height))[:3]


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb[:3])


def tones(src):
    """Dark, mid, light and highlight tones of a vanilla texture, by brightness."""
    colors = sorted({src.getpixel((x, y))[:3] for x in range(src.width) for y in range(src.height) if src.getpixel((x, y))[3] > 0}, key=sum)
    n = len(colors)
    return colors[int(n * 0.1)], colors[int(n * 0.45)], colors[int(n * 0.75)], colors[min(n - 1, int(n * 0.97))]


def from_rows(rows, palette):
    h = len(rows)
    w = len(rows[0])
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    p = img.load()
    for y, row in enumerate(rows):
        assert len(row) == w, f"row {y} has {len(row)} columns, expected {w}"
        for x, ch in enumerate(row):
            if ch != ".":
                c = palette[ch]
                p[x, y] = (c[0], c[1], c[2], 255)
    return img


def outline(img, color=OUTLINE):
    """Adds a one pixel outline around a shape (the canvas grows by two pixels each way)."""
    out = Image.new("RGBA", (img.width + 2, img.height + 2), (0, 0, 0, 0))
    src = px(img)
    dst = px(out)
    for y in range(img.height):
        for x in range(img.width):
            if src[x, y][3] > 0:
                dst[x + 1, y + 1] = src[x, y]
    for y in range(out.height):
        for x in range(out.width):
            if dst[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < out.width and 0 <= ny < out.height and src_alpha(img, nx - 1, ny - 1):
                        dst[x, y] = color + (255,)
                        break
    return out


def src_alpha(img, x, y):
    return 0 <= x < img.width and 0 <= y < img.height and img.getpixel((x, y))[3] > 0


def crop(src, x, y, w, h):
    return src.crop((x, y, x + w, y + h)).convert("RGBA")


def round_corners(img, depth=1):
    p = px(img)
    w, h = img.size
    for i in range(depth):
        for (cx, cy) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
            p[cx, cy] = (0, 0, 0, 0)
    return img


# ---- symbols (10 columns by 8 rows inside the outline, or drawn whole) ----

def furnace(v, ol=OUTLINE):
    face = crop(v["furnace_front_on"], 3, 6, 10, 9)
    return outline(round_corners(face), ol)


def blast_furnace(v, ol=OUTLINE):
    face = crop(v["blast_furnace_front_on"], 3, 3, 10, 9)
    p = px(face)
    iron = tones(v["iron_block"])
    for (x, y) in ((0, 0), (9, 0), (0, 8), (9, 8)):
        p[x, y] = iron[2] + (255,)
    return outline(face, ol)


def compact(v, ol=OUTLINE):
    wood = tones(v["crafting_table_top"])
    iron = tones(v["iron_block"])
    canvas = Image.new("RGBA", (10, 9), (0, 0, 0, 0))
    p = px(canvas)
    for y in range(9):
        for x in range(10):
            p[x, y] = shade(wood[0], 0.8) + (255,)
    # 3x3 grid of light planks with dark gaps; the middle cell is the compressed result
    for gy in range(3):
        for gx in range(3):
            x0 = 1 + gx * 3
            y0 = 0 + gy * 3
            cell = wood[2] if (gx, gy) != (1, 1) else iron[2]
            for y in range(y0, y0 + 2):
                for x in range(x0, x0 + 2):
                    if x < 10 and y < 9:
                        p[x, y] = (cell if (x, y) != (x0, y0) else wood[3] if (gx, gy) != (1, 1) else iron[3]) + (255,)
    for (x, y) in ((0, 0), (9, 0), (0, 8), (9, 8)):
        p[x, y] = iron[1] + (255,)
    return outline(canvas, ol)


def magnet(v, ol=OUTLINE):
    red = tones(v["redstone_block"])
    iron = tones(v["iron_block"])
    pal = {"O": ol, "R": red[1], "L": red[3], "D": red[0], "S": iron[3], "W": iron[2], "G": iron[0]}
    # Left half only; the right half is its mirror image (light edge becomes dark), so both arms are identical.
    left = [
        "...OOO",
        "..OLRR",
        ".OLRRR",
        ".OLRRO",
        ".OLRO.",
        ".OLRO.",
        ".OSSO.",
        ".OWWO.",
        ".OGGO.",
        "..OO..",
    ]
    swap = str.maketrans("L", "D")
    rows = [r + r[::-1].translate(swap) for r in left]
    return from_rows(rows, pal)


def feeder(v, ol=OUTLINE):
    red = tones(v["redstone_block"])
    pal = {"O": ol, "R": (205, 38, 38), "L": (245, 120, 110), "D": (140, 20, 28), "G": (70, 160, 50), "B": (110, 70, 30)}
    rows = [
        ".....OB.O...",
        "....OBOGGO..",
        "..OOOOBOGO..",
        ".ORRROOOROO.",
        "ORLLRRRRRRDO",
        "ORLRRRRRRRDO",
        "ORRRRRRRRRDO",
        "ORRRRRRRRDDO",
        ".ORRRRRRDDO.",
        "..OORRRDDOO.",
        "....OOOOO...",
    ]
    return from_rows([r[:12].ljust(12, ".") for r in rows], pal)


def void(v, ol=OUTLINE):
    obs = tones(v["obsidian"])
    size = 10
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    p = px(img)
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            if r > 4.6:
                continue
            theta = math.atan2(dy, dx)
            arm = ((theta / (2 * math.pi)) * 2 + r / 4.2) % 1.0
            if r < 0.9:
                col = (226, 190, 255)
            elif arm < 0.22:
                col = (176, 96, 240)
            elif arm < 0.34:
                col = (112, 48, 190)
            else:
                col = shade(obs[0], 0.7)
            p[x, y] = col + (255,)
    return outline(img, ol)


TIER_OUTLINE = {1: (196, 198, 208), 2: (244, 192, 52), 3: (72, 224, 218)}


SYMBOLS = {"compact": compact, "furnace": furnace, "blast_furnace": blast_furnace, "magnet": magnet, "feeder": feeder, "void": void}


def augment_canvas(v, kind, level):
    """The 12x12 design: the symbol, outlined in the color of its level (iron, gold, diamond)."""
    sym = SYMBOLS[kind](v, TIER_OUTLINE[level])
    canvas = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    canvas.alpha_composite(sym, ((12 - sym.width) // 2, (12 - sym.height) // 2))
    return canvas


def augment_icon(v, kind, level):
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.alpha_composite(augment_canvas(v, kind, level), (2, 2))
    return icon


# ---- cards ----

def card(v, tier):
    """The upgrade card: one silhouette for all four, in the color of the metal it upgrades to."""
    src = {"iron": v["iron_block"], "gold": v["gold_block"], "diamond": v["diamond_block"], "netherite": v["netherite_block"]}[tier]
    dark, mid, light, high = tones(src)
    arrow_color = {"iron": (236, 236, 244), "gold": (255, 232, 120), "diamond": (150, 255, 250), "netherite": (206, 160, 140)}[tier]
    arrow_shadow = shade(arrow_color, 0.55)
    face = shade(dark, 0.55)
    pal = {"O": OUTLINE, "D": dark, "M": mid, "L": light, "H": high, "F": face, "A": arrow_color, "S": arrow_shadow}
    rows = [
        "..OOOOOOOOOO..",
        ".OHLLLLLLLLMO.",
        "OHLMMMMMMMMMDO",
        "OLMFFFFFFFFMDO",
        "OLMFFFAAFFFMDO",
        "OLMFFAAAAFFMDO",
        "OLMFAAAAAAFMDO",
        "OLMFFFAAFFFMDO",
        "OLMFFFAAFFFMDO",
        "OLMFFFAAFFFMDO",
        "OLMFFFSSFFFMDO",
        "OMMMMMMMMMMDDO",
        ".ODDDDDDDDDDO.",
        "..OOOOOOOOOO..",
    ]
    img = from_rows(rows, pal)
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.alpha_composite(img, (1, 1))
    return icon


def lock(v):
    gold = tones(v["gold_block"])
    iron = tones(v["iron_block"])
    pal = {"O": OUTLINE, "I": iron[2], "i": iron[0], "G": gold[1], "L": gold[3], "D": gold[0], "K": (40, 28, 20)}
    rows = [
        "....OOOOOO....",
        "...OIIIIIIO...",
        "..OIOOOOOOIO..",
        "..OIO....OiO..",
        "..OIO....OiO..",
        "..OIO....OiO..",
        ".OOOOOOOOOOOO.",
        ".OLLLLLLLLGDO.",
        ".OLGGGKKGGGDO.",
        ".OLGGGKKGGGDO.",
        ".OLGGGGKGGGDO.",
        ".OGGGGGGGGGDO.",
        ".ODDDDDDDDDDO.",
        "..OOOOOOOOOO..",
    ]
    img = from_rows(rows, pal)
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.alpha_composite(img, (1, 1))
    return icon
