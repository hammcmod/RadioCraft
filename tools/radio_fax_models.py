"""Writes the radio fax machine's block models, one per wood, its detail texture, its GUI panel and the teletype roll
item texture.

    python tools/radio_fax_models.py [--preview DIR --logs DIR]

- radio_fax_<wood>          a desk fax machine, 14 by 12 and 13 tall: a plank-clad case (the vanilla planks, so
                            there is one detail texture, radio_fax.png, and a model per wood: oak, spruce, birch,
                            jungle, acacia, dark_oak, mangrove, cherry, bamboo, crimson and warped), a cream deck with
                            a keypad and a small grey LCD at the front, a sheet lying in the front tray, and a sheet
                            standing in the feeder at the back
- textures/gui/radio_fax.png
                            the screen's panel: a cream fascia with the slot sockets, the LCD bezel and the SEND key
- textures/item/teletype_roll.png
                            a torn-off roll of teletype paper

`--preview DIR` writes DIR/assets/radiocraft/models/block/radiocraft_preview.json with every wood in a row; the plank
textures are read from `--logs DIR` (a folder of <wood>_planks.png pulled out of the client jar).
"""
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models/block"
TEXTURES = ROOT / "textures/block"
GUI = ROOT / "textures/gui"
ITEMS = ROOT / "textures/item"

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"]


def log_textures(wood):
    """The case's side and end textures: the wood's planks all round, as the recipe takes planks (maintainer, 2026-10-30;
    it was the log, bark and end grain)."""
    return f"minecraft:block/{wood}_planks", f"minecraft:block/{wood}_planks"


PAPER = (236, 232, 220)
PAPER_DARK = (210, 204, 188)
LCD = (168, 176, 160)
LCD_DARK = (120, 128, 114)
KEY = (200, 192, 170)
KEY_DARK = (150, 142, 122)
SLOT = (120, 112, 96)
GUIDE_DARK = (54, 50, 46)

# ---------------------------------------------------------------- block model, one per wood

FAX = {
    "wood": ("wood", 0, 0, 16, 16),
    "wood_top": ("wood_top", 0, 0, 16, 16),
    "deck": [0, 0, 14, 8],       # cream fascia
    "paper": [14, 0, 16, 8],     # a sheet
    "lcd": [0, 8, 4, 11],        # the little display
    "keys": [4, 8, 11, 11],      # the keypad
    "guide": [11, 8, 16, 10],    # the feeder's dark slot
    "foot": [11, 10, 16, 11],    # rubber feet
    "tray": [0, 11, 14, 13],     # the output tray's lip
}


def paint_fax():
    p = Painter(FAX, 32, seed=11)
    p.fill("deck", CREAM, 4, edge((232, 226, 202), CREAM_DIRTY, vertical=False))
    p.fill("paper", PAPER, 3, lambda x, y, w, h: PAPER_DARK if y % 3 == 2 and 1 <= x < w - 1 else None)
    p.fill("lcd", LCD, 3, lambda x, y, w, h: LCD_DARK if (y == 2 and x in (1, 2, 4, 5, 6)) or (y == 4 and x in (1, 3, 4)) else None)
    p.fill("keys", KEY, 4, lambda x, y, w, h: KEY_DARK if x % 2 == 1 or y % 2 == 1 else None)
    p.fill("guide", GUIDE_DARK, 3, lambda x, y, w, h: (80, 76, 70) if y == 0 else None)
    p.fill("foot", BAKELITE_DARK, 3)
    p.fill("tray", CREAM_DIRTY, 3, edge((232, 226, 202), (180, 172, 148), vertical=False))
    p.write(TEXTURES / "radio_fax.png")


def fax_elements(b):
    return [
        # The case: planks round the outside and underneath, the fascia on top.
        b.box("case", [1, 0.5, 2], [15, 6, 14], "wood", {"up": "deck", "down": "wood_top"}),
        b.box("foot_nw", [2, 0, 3], [4, 0.5, 5], "foot"),
        b.box("foot_ne", [12, 0, 3], [14, 0.5, 5], "foot"),
        b.box("foot_sw", [2, 0, 11], [4, 0.5, 13], "foot"),
        b.box("foot_se", [12, 0, 11], [14, 0.5, 13], "foot"),
        # The rear deck steps up over the feeder.
        b.box("rear_deck", [1, 6, 8], [15, 8, 14], "wood", {"up": "deck", "north": "deck"}),
        b.box("feed_slot", [3, 8, 11.5], [13, 9, 13], "guide"),
        b.box("paper_in", [4, 9, 12], [12, 13, 12.5], "paper"),
        # The front: a sheet lying in the tray, the keypad and the LCD.
        b.box("tray_lip", [1, 6, 2], [15, 6.5, 2.5], "tray"),
        b.box("paper_out", [3.5, 6, 2.5], [12.5, 6.25, 8], "paper"),
        b.box("keypad", [2, 6, 5], [8.5, 6.75, 7.75], "keys"),
        b.box("lcd", [10, 6, 5], [14, 6.75, 7.75], "lcd"),
    ]


def fax_builder(wood):
    side, top = log_textures(wood)
    return Builder(FAX, {"0": "radiocraft:block/radio_fax", "wood": side, "wood_top": top}, 32)


# ---------------------------------------------------------------- the screen's panel

PANEL_W, PANEL_H = 217, 110
FASCIA = (214, 206, 182)
FASCIA_LIGHT = (230, 224, 204)
FASCIA_DARK = (168, 160, 138)
BEZEL = (58, 56, 52)
BEZEL_LIGHT = (96, 94, 88)
SOCKET = (120, 114, 100)
SOCKET_DARK = (92, 86, 74)
SOCKET_LIGHT = (236, 230, 212)
LCD_FACE = (176, 184, 168)

# Where the menu puts its slots (the item's top-left corner); the socket is drawn a pixel round each.
SLOTS = {"outbox": (20, 30), "paper": (20, 80), "book": (42, 80), "map": (64, 80),
         "tray": [(110, 80), (132, 80), (154, 80), (176, 80)]}
LCD_BOX = (120, 8, 211, 64)      # the bezel's outside
SEND_KEY = (18, 52, 58, 66)


def rect(p, x0, y0, x1, y1, colour, fn=None, jitter=2):
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = colour
            if fn:
                c = fn(x - x0, y - y0, x1 - x0, y1 - y0) or colour
            j = p.rnd.randint(-jitter, jitter)
            p.img[y][x] = tuple(max(0, min(255, v + j)) for v in c) + (255,)


def socket(p, x, y):
    """An 18 by 18 inset for a slot at item corner x, y: dark on the top and left, light on the bottom and right."""
    rect(p, x - 1, y - 1, x + 17, y + 17, SOCKET,
         lambda sx, sy, w, h: SOCKET_DARK if sx == 0 or sy == 0 else SOCKET_LIGHT if sx == w - 1 or sy == h - 1 else None, jitter=1)


def paint_gui():
    p = Painter({}, PANEL_W, seed=12, height=PANEL_H)
    rect(p, 0, 0, PANEL_W, PANEL_H, FASCIA,
         lambda x, y, w, h: FASCIA_LIGHT if x <= 1 or y <= 1 else FASCIA_DARK if x >= w - 2 or y >= h - 2 else None)
    rect(p, 2, 2, PANEL_W - 2, 3, FASCIA_LIGHT, jitter=0)
    # A darker strip along the bottom where the tray is.
    rect(p, 2, 72, PANEL_W - 2, PANEL_H - 2, (204, 196, 172),
         lambda x, y, w, h: FASCIA_DARK if y == 0 else None)
    for name, at in SLOTS.items():
        for x, y in (at if isinstance(at, list) else [at]):
            socket(p, x, y)
    # The LCD: a dark bezel with a light grey face.
    x0, y0, x1, y1 = LCD_BOX
    rect(p, x0, y0, x1, y1, BEZEL, lambda x, y, w, h: BEZEL_LIGHT if x == 0 or y == 0 else (36, 34, 32) if x == w - 1 or y == h - 1 else None, jitter=1)
    rect(p, x0 + 3, y0 + 3, x1 - 3, y1 - 3, LCD_FACE, lambda x, y, w, h: (150, 158, 142) if x == 0 or y == 0 else None, jitter=1)
    # The SEND key: a raised cream key with a dark edge.
    x0, y0, x1, y1 = SEND_KEY
    rect(p, x0, y0, x1, y1, (226, 218, 196),
         lambda x, y, w, h: (246, 240, 224) if x == 0 or y == 0 else (140, 132, 112) if x == w - 1 or y == h - 1 else None, jitter=1)
    p.write(GUI / "radio_fax.png")


# ---------------------------------------------------------------- the teletype roll item

def paint_roll():
    p = Painter({}, 16, seed=13)
    # A roll seen from the side, a torn tail hanging off it, with faint type on the tail.
    for y in range(2, 11):
        half = [3, 4, 5, 5, 5, 5, 5, 4, 3][y - 2]
        rect(p, 7 - half, y, 8 + half, y + 1, PAPER, lambda x, yy, w, h: PAPER_DARK if x == 0 or x == w - 1 else None, jitter=3)
    rect(p, 6, 5, 9, 8, (190, 184, 168), jitter=2)           # the core
    rect(p, 4, 10, 12, 15, PAPER, lambda x, y, w, h: PAPER_DARK if y == h - 1 and x % 2 == 0 else None, jitter=3)
    for y, cols in ((11, (5, 6, 8, 9, 10)), (13, (5, 7, 8, 10))):
        for x in cols:
            p.img[y][x] = (70, 60, 50, 255)
    p.write(ITEMS / "teletype_roll.png")


# ----------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="write a Blockbench preview under this directory")
    ap.add_argument("--logs", help="a folder of vanilla log textures for the preview")
    args = ap.parse_args()

    paint_fax()
    paint_gui()
    paint_roll()
    items = []
    for i, wood in enumerate(WOODS):
        b = fax_builder(wood)
        m = b.model(fax_elements(b))
        m["textures"]["particle"] = log_textures(wood)[0]
        save(MODELS, f"radio_fax_{wood}", m)
        items.append((m["textures"], fax_elements(b), (20 * i, 0, 0)))
    if args.preview:
        write_scene(args.preview, "radiocraft", TEXTURES, items, size=32,
                    sources={"minecraft": Path(args.logs)} if args.logs else None)


if __name__ == "__main__":
    main()
