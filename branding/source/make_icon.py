"""Draws the Thorough Chests icon as 32x32 pixel art and writes it at several sizes.

Run from the repository root: python branding/source/make_icon.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg, branding/icon/thoroughchests_logo.png and a copy for the jar.

The picture: an open chest on the studio's night tile, a gold frame, a diamond glint and a few items rising out of it.
"""
import math
import shutil
from pathlib import Path

from PIL import Image

N = 32
HERE = Path(__file__).resolve().parent
OUT = HERE.parent / "icon"
JAR = HERE.parent.parent / "common" / "src" / "main" / "resources"

BG = (24, 22, 38)
BG_EDGE = (46, 42, 70)
BG_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
WOOD = (143, 100, 48)
WOOD_HI = (176, 128, 64)
WOOD_LO = (96, 64, 28)
DARK = (28, 20, 14)
INNER = (54, 36, 20)
IRON = (200, 200, 208)
IRON_LO = (120, 120, 132)
DIAMOND = (92, 226, 232)
DIAMOND_LO = (40, 150, 160)
EMERALD = (80, 220, 120)
REDSTONE = (230, 60, 50)

px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


def rect(x0, y0, x1, y1, c):
    for yy in range(y0, y1 + 1):
        for xx in range(x0, x1 + 1):
            put(xx, yy, c)


R = 4
for y in range(N):
    for x in range(N):
        cx = min(max(x, R - 0.5), N - R - 0.5)
        cy = min(max(y, R - 0.5), N - R - 0.5)
        if math.hypot(x - cx, y - cy) <= R:
            px[y][x] = BG
for y in range(N):
    for x in range(N):
        if px[y][x] is None:
            continue
        edge = any(not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            px[y][x] = BG_TOP if y < N // 2 else BG_EDGE

# Chest body
rect(5, 17, 26, 27, DARK)
rect(6, 18, 25, 26, WOOD)
rect(6, 18, 25, 18, WOOD_HI)
rect(6, 24, 25, 26, WOOD_LO)
for x in range(8, 24, 4):
    rect(x, 19, x, 23, WOOD_LO)
# Gold frame
rect(5, 17, 26, 17, GOLD)
rect(5, 27, 26, 27, GOLD_LO)
rect(5, 17, 5, 27, GOLD)
rect(26, 17, 26, 27, GOLD_LO)
rect(5, 17, 6, 18, GOLD_HI)
# Lock
rect(14, 19, 17, 23, IRON_LO)
rect(15, 19, 16, 22, IRON)
put(15, 21, DARK)
put(16, 21, DARK)
# Open lid, tilted back
rect(6, 8, 25, 13, DARK)
rect(7, 9, 24, 12, INNER)
rect(7, 9, 24, 9, WOOD_LO)
rect(5, 14, 26, 16, GOLD)
rect(5, 14, 26, 14, GOLD_HI)
rect(5, 16, 26, 16, GOLD_LO)
rect(6, 7, 25, 7, GOLD_LO)
rect(6, 7, 7, 7, GOLD)
# Things rising out of the chest
rect(9, 10, 11, 12, DIAMOND)
put(9, 10, (200, 250, 252))
rect(10, 12, 11, 12, DIAMOND_LO)
rect(14, 9, 16, 11, EMERALD)
put(14, 9, (190, 255, 200))
rect(19, 10, 21, 12, REDSTONE)
put(19, 10, (255, 140, 120))
# Sparkle
put(24, 4, GOLD_HI)
put(23, 5, GOLD)
put(25, 5, GOLD)
put(24, 6, GOLD)

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
for y in range(N):
    for x in range(N):
        if px[y][x] is not None:
            img.putpixel((x, y), px[y][x] + (255,))

OUT.mkdir(parents=True, exist_ok=True)
for size in (32, 64, 128, 256, 512, 1024):
    img.resize((size, size), Image.NEAREST).save(OUT / f"icon_{size}.png", optimize=True)
img.resize((512, 512), Image.NEAREST).save(OUT / "thoroughchests_logo.png", optimize=True)
JAR.mkdir(parents=True, exist_ok=True)
shutil.copyfile(OUT / "thoroughchests_logo.png", JAR / "thoroughchests_logo.png")

rects = []
for y in range(N):
    for x in range(N):
        c = px[y][x]
        if c is not None:
            rects.append(f'<rect x="{x}" y="{y}" width="1.02" height="1.02" fill="#{c[0]:02x}{c[1]:02x}{c[2]:02x}"/>')
(OUT / "icon.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n",
    encoding="utf-8",
)
print("wrote", OUT)
