#!/usr/bin/env python3
"""
Generates assets/all-addons/icon.png without any image library.

The icon is a 128x128 meteor: a purple gradient sky, a fading streak and a bright head, drawn with
4x supersampling so the edges stay smooth. Run `python3 tools/make_icon.py` to regenerate it.
"""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

SIZE = 128
SUPERSAMPLE = 4
OUTPUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/all-addons/icon.png"

# Palette
SKY_TOP_LEFT = (18, 13, 34)
SKY_BOTTOM_RIGHT = (46, 26, 74)
STREAK_HEAD = (255, 226, 255)
STREAK_TAIL = (150, 96, 255)
HEAD_CORE = (255, 255, 255)
HEAD_GLOW = (196, 140, 255)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[float, float, float]:
    t = max(0.0, min(1.0, t))
    return (lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t))


def segment_distance(px: float, py: float, x1: float, y1: float, x2: float, y2: float) -> tuple[float, float]:
    """Distance from a point to a segment, plus the normalised position along it."""
    dx, dy = x2 - x1, y2 - y1
    length_sq = dx * dx + dy * dy

    if length_sq == 0:
        return math.hypot(px - x1, py - y1), 0.0

    t = ((px - x1) * dx + (py - y1) * dy) / length_sq
    t = max(0.0, min(1.0, t))

    cx, cy = x1 + t * dx, y1 + t * dy
    return math.hypot(px - cx, py - cy), t


def colour_at(x: float, y: float) -> tuple[float, float, float]:
    # Background gradient, diagonal.
    t = (x + y) / (2 * SIZE)
    colour = list(mix(SKY_TOP_LEFT, SKY_BOTTOM_RIGHT, t))

    # Faint stars: a fixed hash so the output is stable between runs.
    h = (int(x) * 73856093) ^ (int(y) * 19349663)
    if h % 977 == 0:
        colour = [min(255.0, c + 55) for c in colour]

    # Meteor streak: from the top right corner down to the head.
    head_x, head_y = 54.0, 78.0
    tail_x, tail_y = 118.0, 10.0

    distance, along = segment_distance(x, y, head_x, head_y, tail_x, tail_y)

    # The streak is thin at the tail and thick at the head.
    width = lerp(1.2, 7.5, 1.0 - along)
    if distance < width:
        intensity = (1.0 - distance / width) * lerp(0.15, 1.0, 1.0 - along)
        colour = list(mix(tuple(colour), STREAK_TAIL, intensity))

    # Head: a small glowing ball.
    head_distance = math.hypot(x - head_x, y - head_y)
    if head_distance < 20:
        glow = max(0.0, 1.0 - head_distance / 20) ** 1.6
        colour = list(mix(tuple(colour), HEAD_GLOW, glow * 0.85))

    if head_distance < 6.5:
        colour = list(mix(tuple(colour), HEAD_CORE, 1.0 - head_distance / 6.5))

    return colour[0], colour[1], colour[2]


def render() -> list[list[tuple[int, int, int]]]:
    rows = []

    for py in range(SIZE):
        row = []
        for px in range(SIZE):
            r = g = b = 0.0
            for sy in range(SUPERSAMPLE):
                for sx in range(SUPERSAMPLE):
                    x = px + (sx + 0.5) / SUPERSAMPLE
                    y = py + (sy + 0.5) / SUPERSAMPLE
                    cr, cg, cb = colour_at(x, y)
                    r += cr
                    g += cg
                    b += cb

            samples = SUPERSAMPLE * SUPERSAMPLE
            row.append((int(r / samples), int(g / samples), int(b / samples)))

        rows.append(row)

    return rows


def write_png(path: Path, rows: list[list[tuple[int, int, int]]]) -> None:
    raw = bytearray()

    for row in rows:
        raw.append(0)  # filter type 0
        for r, g, b in row:
            raw += bytes((r, g, b, 255))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


if __name__ == "__main__":
    write_png(OUTPUT, render())
    print(f"wrote {OUTPUT} ({OUTPUT.stat().st_size} bytes)")
