"""Draws the Join SkyBlock icon on a 16x16 pixel grid and writes it as a 256x256 PNG.

Pure standard library, so it runs anywhere: python3 art/icon.py
The PNG goes straight into the mod resources, where fabric.mod.json points at it.
"""
import struct
import zlib
from pathlib import Path

N, SCALE = 16, 16
C = {
    "outline": (0x1E, 0x1E, 0x1E, 255),
    "face": (0x6F, 0x6F, 0x6F, 255),
    "light": (0xA5, 0xA5, 0xA5, 255),
    "shadow": (0x4A, 0x4A, 0x4A, 255),
    "play": (0x5D, 0xBB, 0x3F, 255),
    "grass": (0x5D, 0xBB, 0x3F, 255),
    "grass_top": (0x7B, 0xD3, 0x5A, 255),
    "dirt": (0x8B, 0x5A, 0x2B, 255),
    "stone": (0x7A, 0x7A, 0x7A, 255),
    "stone_dark": (0x5E, 0x5E, 0x5E, 255),
    "trunk": (0x6B, 0x4A, 0x2B, 255),
    "leaves": (0x2F, 0x8F, 0x3A, 255),
    "leaves_light": (0x46, 0xB0, 0x4F, 255),
}
px = [[(0, 0, 0, 0)] * N for _ in range(N)]


def rect(x, y, w, h, color):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px[yy][xx] = C[color]


# The button, a vanilla-style one with a one-pixel bevel. The face inside is 12x8.
rect(0, 2, 16, 12, "outline")
rect(1, 3, 14, 10, "face")
rect(1, 3, 14, 1, "light")
rect(1, 3, 1, 10, "light")
rect(1, 12, 14, 1, "shadow")
rect(14, 4, 1, 9, "shadow")

# The play arrow, 3 wide and 5 tall, its tip level with the island's grass.
rect(3, 6, 1, 5, "play")
rect(4, 7, 1, 3, "play")
rect(5, 8, 1, 1, "play")

# The island, five blocks wide, with its tree. Crown, trunk and island share the centre column,
# and the six rows from the treetop to the stone sit centred in the button's face.
rect(10, 5, 1, 1, "leaves_light")
rect(9, 6, 3, 1, "leaves")
rect(10, 7, 1, 1, "trunk")
rect(8, 8, 5, 1, "grass_top")
rect(8, 9, 5, 1, "dirt")
rect(9, 10, 3, 1, "stone")


def write_png(path: Path):
    size = N * SCALE
    rows = b""
    for y in range(size):
        row = b"".join(bytes(px[y // SCALE][x // SCALE]) for x in range(size))
        rows += b"\x00" + row

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(rows, 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


out = Path(__file__).resolve().parent.parent / "src/main/resources/assets/joinskyblock/icon.png"
write_png(out)
print(out)
