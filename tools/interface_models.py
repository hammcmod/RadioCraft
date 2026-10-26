"""Builds the Radio Interface (a modem box) and the Digital Transfer Interface (a file and SSTV terminal with a paper
feed) in the 1960s style of the weather instruments: grey-green hammertone cases, bakelite panels, brass bezels.

    python tools/interface_models.py [--preview DIR]

Writes under assets/radiocraft (models/block and textures/block), keeping each block's footprint inside its
VoxelShape, front on the north face so the existing `facing` blockstates turn it:

- radio_interface            the body, lamps dark (what the blockstate shows today)
- radio_interface_<lamp>     PWR, DCD, TX, RX and RPT: one full-bright cutout model per lit lamp face, to be added to
                             the blockstate when the block gets lamp properties (the parrot controller way)
- transfer_interface         the body, lamp dark
- transfer_interface_busy    the lit BUSY lamp, the same way

With `--preview DIR` it also writes DIR/assets/radiocraft/models/block/radiocraft_preview.json with both in a row,
every lamp lit, for a fresh Blockbench.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ASSETS / "models/block"
TEXTURES = ASSETS / "textures/block"
SIZE = 32
CUTOUT = "minecraft:cutout"
PANEL = (38, 30, 26)         # bakelite
PANEL_LIGHT = (58, 48, 42)
CHROME = (190, 194, 196)
CHROME_DARK = (120, 124, 128)
RUBBER = (30, 30, 30)
LAMP_OFF = (52, 44, 40)
LIT = {"green": (90, 220, 110), "amber": (240, 180, 60), "red": (240, 70, 60), "blue": (110, 170, 240)}


def lamp_fill(colour):
    return lambda x, y, w, h: tuple(min(255, c + 40) for c in colour) if x == 1 and y == 1 else None


# ---------------------------------------------------------------- radio interface

RI = {
    "case":     [0, 0, 8, 6],       # hammertone
    "front":    [0, 6, 15, 11],     # bakelite panel: nameplate, lamp labels, switch label
    "lamp_off": [8, 0, 9.5, 1.5],
    "green":    [9.5, 0, 11, 1.5],
    "amber":    [11, 0, 12.5, 1.5],
    "red":      [12.5, 0, 14, 1.5],
    "blue":     [14, 0, 15.5, 1.5],
    "bezel":    [8, 2, 10, 4],
    "chrome":   [10, 2, 12, 4],
    "foot":     [12, 2, 14, 4],
    "jack":     [14, 2, 16, 4],
    "terminal": [8, 4, 16, 6],
}
LAMPS = (("pwr", "green", 2), ("dcd", "amber", 4.25), ("tx", "red", 6.5), ("rx", "green", 8.75), ("rpt", "blue", 11))


def paint_radio_interface():
    p = Painter(RI, SIZE, seed=11)
    p.fill("case", HAMMERTONE, 10, hammertone)

    def front(x, y, w, h):
        if min(x, y, w - 1 - x, h - 1 - y) == 0:
            return PANEL_LIGHT
        if 1 <= y <= 2 and 2 <= x <= 9:          # brass nameplate top-left
            return BRASS if y == 1 else BRASS_DARK
        if y == 8 and x % 4 in (0, 1) and 4 <= x <= 25:  # engraved lamp labels under the lamps
            return (200, 196, 180)
        return None

    p.fill("front", PANEL, 3, front)
    p.fill("lamp_off", LAMP_OFF, 4, rim((80, 70, 60)))
    for name, colour in LIT.items():
        p.fill(name, colour, 6, lamp_fill(colour))
    p.fill("bezel", BRASS, 8, rim(BRASS_DARK))
    p.fill("chrome", CHROME, 8, edge((230, 232, 234), CHROME_DARK))
    p.fill("foot", RUBBER, 4)
    p.fill("jack", CHROME_DARK, 6, rim(CHROME))
    p.fill("terminal", CREAM, 4, lambda x, y, w, h: CHROME_DARK if x % 3 == 1 and y in (1, 2) else None)
    return p


def radio_interface_body(b):
    e = [b.box("case", [0.5, 0.75, 4.5], [15.5, 5.75, 15.5], "case"),
         b.box("panel", [0.5, 0.75, 4], [15.5, 5.75, 4.5], "case", {"north": "front"}),
         b.box("switch_plate", [12.5, 1.25, 3.75], [14.5, 2.25, 4], "chrome"),
         b.box("switch_lever", [13.25, 1.5, 3], [13.75, 2, 3.75], "chrome"),
         b.box("jack", [7, 2.5, 15.5], [8.5, 4, 16], "jack"),
         b.box("terminal_strip", [2, 1.5, 15.5], [6, 3.5, 16], "terminal")]
    for i, (x, z) in enumerate(((1, 5), (13.5, 5), (1, 14), (13.5, 14))):
        e.append(b.box(f"foot_{i}", [x, 0, z], [x + 1.5, 0.75, z + 1.5], "foot"))
    for name, _, x in LAMPS:
        e.append(b.box(f"bezel_{name}", [x, 2.5, 3.75], [x + 1.5, 4, 4], "bezel", {"north": "lamp_off"}))
    return e


def radio_interface_lamp(b, name):
    colour, x = next((c, x) for n, c, x in LAMPS if n == name)
    return [lamp(b, name, [x, 2.5, 3.74], [x + 1.5, 4, 4], colour)]


def build_radio_interface():
    b = Builder(RI, "radiocraft:block/radio_interface", SIZE)
    save(MODELS, "radio_interface", b.model(radio_interface_body(b)))
    for name, _, _ in LAMPS:
        save(MODELS, f"radio_interface_{name}", b.model(radio_interface_lamp(b, name), CUTOUT))
    paint_radio_interface().write(TEXTURES / "radio_interface.png")


# ---------------------------------------------------------------- digital transfer interface

TI = {
    "case":     [0, 0, 8, 6],       # hammertone
    "front":    [0, 6, 14, 10],     # bakelite panel with a round meter and the paper slot
    "paper":    [8, 0, 12, 6],      # cream with grey text lines
    "roll_end": [12, 0, 14, 2],
    "roller":   [12, 2, 14, 4],
    "chrome":   [14, 0, 16, 2],
    "key":      [14, 2, 16, 4],
    "lamp_off": [12, 4, 14, 6],
    "amber":    [14, 4, 16, 6],
    "foot":     [8, 10, 10, 12],
    "bezel":    [10, 10, 12, 12],
}


def paint_transfer_interface():
    p = Painter(TI, SIZE, seed=12)
    p.fill("case", HAMMERTONE, 10, hammertone)

    def front(x, y, w, h):
        if min(x, y, w - 1 - x, h - 1 - y) == 0:
            return PANEL_LIGHT
        r = ((x - 7) ** 2 + (y - 4) ** 2) ** 0.5     # a round meter on the left
        if r < 3.2:
            return BRASS if r > 2.3 else (236, 228, 200) if not (x == 7 and y <= 4) else (30, 30, 30)
        if y == 2 and 13 <= x <= 24:                 # the paper slot
            return (20, 18, 16)
        if y in (5, 6) and 13 <= x <= 24 and x % 2 == 1:  # a row of key caps
            return (70, 60, 54)
        return None

    p.fill("front", PANEL, 3, front)
    p.fill("paper", (236, 230, 210), 3,
           lambda x, y, w, h: (120, 116, 110) if y % 3 == 1 and 1 <= x <= w - 3 and (x + y) % 5 else None)
    p.fill("roll_end", (236, 230, 210), 3, lambda x, y, w, h: (190, 184, 170) if (x + y) % 2 else None)
    p.fill("roller", BAKELITE, 4)
    p.fill("chrome", CHROME, 8, edge((230, 232, 234), CHROME_DARK, vertical=False))
    p.fill("key", PANEL_LIGHT, 4, lambda x, y, w, h: (210, 206, 196) if (x, y) in ((1, 1), (2, 1)) else None)
    p.fill("lamp_off", LAMP_OFF, 4, rim((80, 70, 60)))
    p.fill("amber", LIT["amber"], 6, lamp_fill(LIT["amber"]))
    p.fill("foot", RUBBER, 4)
    p.fill("bezel", BRASS, 8, rim(BRASS_DARK))
    return p


def transfer_interface_body(b):
    e = [b.box("case", [1, 0.5, 2.5], [15, 5.5, 14], "case"),
         b.box("panel", [1, 0.5, 2], [15, 5.5, 2.5], "case", {"north": "front"}),
         b.box("key_bed", [2.5, 5.5, 3], [13.5, 6, 6], "case"),
         b.box("tear_bar", [2.75, 7.5, 5.75], [13.25, 8, 6.25], "chrome"),
         b.box("sheet", [3.25, 7.9, 6], [12.75, 8, 10.25], "paper"),
         b.box("roll", [2.5, 5.5, 10.25], [13.5, 8, 12.75], "paper", {"east": "roll_end", "west": "roll_end"}),
         b.box("roll_turned", [2.75, 5.6, 10.35], [13.25, 7.9, 12.65], "paper", {"east": "roll_end", "west": "roll_end"},
               rotation={"angle": 45.0, "axis": "x", "origin": [8, 6.75, 11.5]}),
         b.box("spindle", [2, 6.25, 11], [14, 7.25, 12], "roller"),
         b.box("bezel_busy", [12.5, 3.5, 1.75], [13.5, 4.5, 2], "bezel", {"north": "lamp_off"})]
    for i, (x, z) in enumerate(((1.5, 3), (13, 3), (1.5, 12.5), (13, 12.5))):
        e.append(b.box(f"foot_{i}", [x, 0, z], [x + 1.5, 0.5, z + 1.5], "foot"))
    for row, z in enumerate((3.25, 4.75)):
        for k in range(8):
            x = 3 + k * 1.25 + row * 0.5
            e.append(b.box(f"key_{row}_{k}", [x, 6, z], [x + 1, 6.5, z + 1], "key"))
    return e


def transfer_interface_lamp(b):
    return [lamp(b, "busy", [12.5, 3.5, 1.74], [13.5, 4.5, 2], "amber")]


def build_transfer_interface():
    b = Builder(TI, "radiocraft:block/transfer_interface", SIZE)
    save(MODELS, "transfer_interface", b.model(transfer_interface_body(b)))
    save(MODELS, "transfer_interface_busy", b.model(transfer_interface_lamp(b), CUTOUT))
    paint_transfer_interface().write(TEXTURES / "transfer_interface.png")


def build_preview(root):
    ri = Builder(RI, "", SIZE)
    ti = Builder(TI, "", SIZE)
    rows = [(["radio_interface"], radio_interface_body(ri) + [el for n, _, _ in LAMPS for el in radio_interface_lamp(ri, n)]),
            (["transfer_interface"], transfer_interface_body(ti) + transfer_interface_lamp(ti))]
    write_preview(root, "radiocraft", TEXTURES, rows, SIZE)


if __name__ == "__main__":
    build_radio_interface()
    build_transfer_interface()
    if len(sys.argv) > 2 and sys.argv[1] == "--preview":
        build_preview(sys.argv[2])
