"""Writes the phone PABX's screen texture: a plain vanilla-style grey panel, 176 x 166 on a 256 x 256 sheet
(PabxScreen draws the text boxes, the Save button and the text on it).

    python tools/pabx_gui.py
"""
from pathlib import Path

from PIL import Image

GUI = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/textures/gui"
WIDTH, HEIGHT = 176, 166

BLACK = (0, 0, 0, 255)
LIGHT = (255, 255, 255, 255)
FACE = (198, 198, 198, 255)
SHADOW = (85, 85, 85, 255)


def panel(w, h):
    image = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = image.load()
    for x in range(w):
        for y in range(h):
            px[x, y] = FACE
    # Black outline with the corners cut, as vanilla's container panels.
    for x in range(2, w - 2):
        px[x, 0] = BLACK
        px[x, h - 1] = BLACK
    for y in range(2, h - 2):
        px[0, y] = BLACK
        px[w - 1, y] = BLACK
    for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
        px[x, y] = BLACK
    for x, y in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1),
                 (0, h - 1), (1, h - 1), (0, h - 2), (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)):
        px[x, y] = (0, 0, 0, 0)
    # Light top and left edge, dark bottom and right, two pixels each.
    for i in (1, 2):
        for x in range(2, w - 3):
            px[x, i] = LIGHT
        for y in range(2, h - 3):
            px[i, y] = LIGHT
        for x in range(3, w - 2):
            px[x, h - 1 - i] = SHADOW
        for y in range(3, h - 2):
            px[w - 1 - i, y] = SHADOW
    px[2, 2] = LIGHT
    px[w - 3, h - 3] = SHADOW
    return image


out = GUI / "pabx.png"
panel(WIDTH, HEIGHT).save(out)
print("wrote", out)
