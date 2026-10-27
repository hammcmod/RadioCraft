"""Draws the tape reel item's three layers, 16x16, for assets/radiocraft/textures/item:

- tape_reel_tape.png    the wound tape, brown, drawn as it is (layer0)
- tape_reel_spool.png   the plastic reel: rim, spokes and hub, in greys for TapeItem to tint (layer1, 12 colours)
- tape_reel_label.png   the paper label on the hub, near white for TapeItem to tint (layer2, 5 colours)

    pip install pillow
    python tools/tape_textures.py
"""
import math
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/textures/item"
C = 7.5  # centre of a 16 pixel sprite


def layer(paint):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - C, y - C)
            a = math.atan2(y - C, x - C)
            colour = paint(x, y, r, a)
            if colour:
                img.putpixel((x, y), colour)
    return img


def tape(x, y, r, a):
    if 2.6 <= r <= 6.2:
        shade = 70 + int(8 * math.sin(r * 5))  # the turns of tape
        return (shade, 46, 30, 255)
    return None


def spool(x, y, r, a):
    if 6.2 < r <= 7.6:  # rim
        light = 215 if y < C else 160
        return (light, light, light, 255)
    spoke = min((a * 3 / (2 * math.pi)) % 1, 1 - (a * 3 / (2 * math.pi)) % 1) < 0.07
    if 2.6 < r <= 6.2 and spoke:
        return (190, 190, 190, 255)
    if r <= 1.0:  # the spindle hole
        return (40, 40, 40, 255)
    return None


def label(x, y, r, a):
    if 1.0 < r <= 2.6:
        return (238, 236, 228, 255) if (x + y) % 5 else (210, 208, 200, 255)
    return None


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    layer(tape).save(OUT / "tape_reel_tape.png")
    layer(spool).save(OUT / "tape_reel_spool.png")
    layer(label).save(OUT / "tape_reel_label.png")
    print("wrote", OUT)
