"""Draws the large Join SkyBlock icon: a SkyBlock world cube with a play button over it.

A 64x64 pixel grid scaled 8x to 512x512. Pure standard library: python3 art/icon.py
The PNG goes straight into the mod resources, where fabric.mod.json points at it.
"""
import math
import struct
import zlib
from pathlib import Path

N, SCALE = 64, 8
px = [[(0, 0, 0, 0)] * N for _ in range(N)]


def blend(dst, src):
    """Alpha-composites src over dst, both RGBA tuples with 0-255 channels."""
    sa = src[3] / 255
    da = dst[3] / 255
    oa = sa + da * (1 - sa)
    if oa == 0:
        return (0, 0, 0, 0)
    ch = [round((src[i] * sa + dst[i] * da * (1 - sa)) / oa) for i in range(3)]
    return (*ch, round(oa * 255))


def put(x, y, color):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = blend(px[y][x], color)


def shade(color, factor):
    return tuple(min(255, round(c * factor)) for c in color[:3]) + (255,)


# --- The world texture: 8x8 per face like a Minecraft head, sea with continents -----------

WATER = (0x5A, 0x6C, 0xE8)
WATER_DEEP = (0x3D, 0x47, 0xA8)
LAND = (0x6C, 0xB8, 0x3C)
LAND_DARK = (0x4A, 0x8A, 0x2C)
T = 8


# Drawn by hand, one string per row: "." sea, "w" deep sea, "g" land, "G" forest.
FACES = {
    "top": [
        "gg....ww",
        "g...w...",
        "...gg...",
        "..gGgg..",
        "...gg..g",
        "w.....gg",
        "..g...g.",
        ".ggw....",
    ],
    "left": [
        "..gg....",
        ".gGgg..w",
        "..gg....",
        "......g.",
        "w....ggg",
        "...g.gGg",
        "..ggg.g.",
        "...g....",
    ],
    "right": [
        "....gg..",
        "g..gGg..",
        "gg..g...",
        "g......w",
        "...w....",
        ".gg...g.",
        "gGgg.ggg",
        ".gg...g.",
    ],
}
PALETTE = {".": WATER, "w": WATER_DEEP, "g": LAND, "G": LAND_DARK}


def face_texture(name):
    return [[PALETTE[ch] for ch in row] for row in FACES[name]]


TOP, LEFT, RIGHT = face_texture("top"), face_texture("left"), face_texture("right")

# --- The cube, in 2:1 isometric -----------------------------------------------------------

K = 3.25       # screen pixels per texel along the top edges
H = 3.5        # screen pixels per texel down the vertical edges
TOP_V = (32.0, 5.0)
LEFT_C = (TOP_V[0] - T * K, TOP_V[1] + T * K / 2)
FRONT_C = (TOP_V[0], TOP_V[1] + T * K)


def cube_sample(x, y):
    """Returns (colour, face) for the screen point, or None outside the cube."""
    dx, dy = x - TOP_V[0], y - TOP_V[1]
    u = (dx / K + 2 * dy / K) / 2
    v = (2 * dy / K - dx / K) / 2
    if 0 <= u < T and 0 <= v < T:
        return shade(TOP[int(v)][int(u)], 1.0), "top"
    s = (x - LEFT_C[0]) / K
    t = (y - LEFT_C[1] - s * K / 2) / H
    if 0 <= s < T and 0 <= t < T:
        return shade(LEFT[int(t)][int(s)], 0.8), "left"
    s = (x - FRONT_C[0]) / K
    t = (y - FRONT_C[1] + s * K / 2) / H
    if 0 <= s < T and 0 <= t < T:
        return shade(RIGHT[int(t)][int(s)], 0.62), "right"
    return None


# A soft shadow the cube casts on the ground.
for y in range(N):
    for x in range(N):
        d = ((x + 0.5 - 32) / 24) ** 2 + ((y + 0.5 - 59.5) / 3.2) ** 2
        if d < 1:
            put(x, y, (0, 0, 0, round(70 * (1 - d))))

faces = {}
for y in range(N):
    for x in range(N):
        hit = cube_sample(x + 0.5, y + 0.5)
        if hit:
            put(x, y, hit[0])
            faces[(x, y)] = hit[1]

# A dark rim around the cube so it stands off any background.
for (x, y) in list(faces):
    if any((x + dx, y + dy) not in faces for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
        px[y][x] = shade(px[y][x], 0.55)

# --- The play button -----------------------------------------------------------------------

CX, CY, R = 32, 33, 18
GREEN = (0x3C, 0x9A, 0x2E, 255)
GREEN_LIGHT = (0x6C, 0xCB, 0x4E, 255)
GREEN_DARK = (0x1F, 0x5A, 0x16, 255)


def in_circle(x, y, cx, cy, r):
    return (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r


# Drop shadow, offset down and to the right.
for y in range(N):
    for x in range(N):
        if in_circle(x, y, CX + 2, CY + 3, R):
            put(x, y, (0, 0, 0, 110))

disc = {(x, y) for y in range(N) for x in range(N) if in_circle(x, y, CX, CY, R)}
for (x, y) in disc:
    rim = any((x + dx, y + dy) not in disc for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    if rim:
        px[y][x] = GREEN_DARK
    elif not in_circle(x, y, CX + 1.5, CY + 1.5, R):
        px[y][x] = GREEN_LIGHT     # a thin crescent of light on the upper left
    elif not in_circle(x, y, CX - 1.5, CY - 1.5, R):
        px[y][x] = shade(GREEN, 0.75)   # and its shadow on the lower right
    else:
        px[y][x] = GREEN

# The triangle, nudged right of centre so it looks centred, with its own small shadow.
tri_h, tri_w = 19, 17
left, top = CX - tri_w // 2 + 1, CY - tri_h // 2
tri = set()
for r in range(tri_h):
    half = min(r, tri_h - 1 - r) + 0.5
    length = max(1, round(half * tri_w / (tri_h / 2)))
    for c in range(length):
        tri.add((left + c, top + r))
for (x, y) in tri:
    if (x + 1, y + 1) not in tri:
        put(x + 1, y + 1, (0, 0, 0, 90))
for (x, y) in tri:
    px[y][x] = (0xFF, 0xFF, 0xFF, 255)


def write_png(path: Path):
    size = N * SCALE
    rows = bytearray()
    for y in range(size):
        rows.append(0)
        for x in range(size):
            rows.extend(px[y // SCALE][x // SCALE])

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(rows), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


out = Path(__file__).resolve().parent.parent / "src/main/resources/assets/joinskyblock/icon.png"
write_png(out)
print(out)
