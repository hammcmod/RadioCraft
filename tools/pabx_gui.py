"""Writes the phone PABX's screen textures from the telephone's: the same keypad, with the labels on EXT, CALL and
HANG painted out (the screen writes MODE, SET and BACK there).

    python tools/pabx_gui.py
"""
from collections import Counter
from pathlib import Path

from PIL import Image

GUI = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft/textures/gui"
BUTTONS = [(142, 54, 26, 22), (24, 206, 62, 22), (94, 206, 62, 22)]  # EXT, CALL, HANG, as in TelephoneScreen


def blank(image):
    px = image.load()
    for x0, y0, w, h in BUTTONS:
        inner = [(x, y) for x in range(x0 + 3, x0 + w - 3) for y in range(y0 + 3, y0 + h - 3)]
        face = Counter(px[x, y] for x, y in inner).most_common(1)[0][0]
        for x, y in inner:
            px[x, y] = face
    return image


for name in ("telephone", "telephone_widgets"):
    out = GUI / (name.replace("telephone", "pabx") + ".png")
    blank(Image.open(GUI / f"{name}.png").convert("RGBA")).save(out)
    print("wrote", out)
