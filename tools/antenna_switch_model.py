"""Writes the antenna switch block model: a small coaxial antenna switch (after a Workman CX-3) with one input and
three outputs, a connector low on each side, and a pointer knob on top that the mod turns toward the live output.

    python tools/antenna_switch_model.py [--preview DIR]

- antenna_switch.json          the body: a 10x6x10 aluminium box, a cream label plate on top with the output numbers,
                               an octagonal black knob hub, and a 2x2x2 SO-239 on each side at ground level, one
                               pixel inside the block face (x 7..9, y 0..2) where the coax wire model runs. The
                               input is the north side (a brass connector); outputs 1, 2, 3 are east, south, west.
- antenna_switch_pointer.json  the knob's lever, pointing north; rotate it with the blockstate (y = 90, 180, 270) to
                               point at the output in use.
"""
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import (BAKELITE_DARK, BLACK, BRASS, BRASS_DARK, BRASS_LIGHT, CREAM, GALV, GALV_DARK, GALV_LIGHT,  # noqa: E402
                        Builder, Painter, edge, rim, save, write_scene)

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models/block"
TEXTURES = ROOT / "textures/block"
TEXTURE = "antenna_switch"
SIZE = 32

# regions in UV units on a 32-pixel texture
REGIONS = {
    "side": [0, 0, 10, 6],
    "top": [0, 6, 10, 16],
    "connector": [10, 0, 12, 2],      # nickel knurl
    "connector_end": [12, 0, 14, 2],
    "input": [14, 0, 16, 2],          # brass knurl
    "input_end": [10, 2, 12, 4],
    "bottom": [12, 2, 14, 4],
    "knob": [10, 4, 14, 8],
    "pointer_top": [14, 4, 16, 9],
}

DIGITS = {"1": (".#.", "##.", ".#.", ".#.", "###"), "2": ("###", "..#", "###", "#..", "###"),
          "3": ("###", "..#", "###", "..#", "###")}
INK = (40, 36, 32)


def paint():
    p = Painter(REGIONS, SIZE, seed=7)
    # the aluminium body: a lighter top edge, a darker lip at the bottom
    p.fill("side", GALV, 5, lambda x, y, w, h: GALV_LIGHT if y == 0 else GALV_DARK if y >= h - 2 else None)
    p.fill("bottom", GALV_DARK, 4)
    # the top: an aluminium rim round a cream label plate
    p.fill("top", GALV, 5, rim(GALV_LIGHT))
    x0, y0, x1, y1 = p.px("top")
    plate = (x0 + 2, y0 + 2, x1 - 2, y1 - 2)
    for y in range(plate[1], plate[3]):
        for x in range(plate[0], plate[2]):
            c = CREAM if min(x - plate[0], y - plate[1], plate[2] - 1 - x, plate[3] - 1 - y) else (190, 182, 160)
            p.img[y][x] = c + (255,)
    # output numbers: 1 east, 2 south, 3 west (seen from above with north at the top), and the input arrow north
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    p.glyph("top", x1 - x0 - 6, cy - y0 - 2, DIGITS["1"], INK)
    p.glyph("top", cx - x0 - 1, y1 - y0 - 8, DIGITS["2"], INK)
    p.glyph("top", 3, cy - y0 - 2, DIGITS["3"], INK)
    p.glyph("top", cx - x0 - 2, 3, (".#.", "###", ".#.", ".#.", ".#."), INK)   # the input arrow
    # the connectors: knurled rings, a dark socket in the end face
    knurl = lambda light, dark: (lambda x, y, w, h: light if (x + y) % 2 == 0 else dark)
    p.fill("connector", GALV, 4, knurl(GALV_LIGHT, GALV_DARK))
    p.fill("input", BRASS, 4, knurl(BRASS_LIGHT, BRASS_DARK))
    p.fill("connector_end", GALV_LIGHT, 4, rim(GALV_DARK))
    p.fill("input_end", BRASS_LIGHT, 4, rim(BRASS_DARK))
    for region in ("connector_end", "input_end"):
        for dy in (1, 2):
            for dx in (1, 2):
                p.put(region, dx, dy, BLACK)
    # the knob: black bakelite with a highlight ring, the pointer with a cream index line along it
    p.fill("knob", BAKELITE_DARK, 4, rim((70, 66, 60)))
    p.fill("pointer_top", BAKELITE_DARK, 4, edge((70, 66, 60), (20, 18, 16)))
    x0, y0, x1, y1 = p.px("pointer_top")
    for y in range(y0 + 1, y1 - 1):
        for x in range(x0 + 1, x1 - 1):
            p.img[y][x] = CREAM + (255,)
    p.write(TEXTURES / f"{TEXTURE}.png")


def body(b):
    els = [b.box("body", [3, 0, 3], [13, 6, 13], "side", {"up": "top", "down": "bottom"})]
    # SO-239 connectors at ground level, one pixel inside each block face
    els.append(b.box("input", [7, 0, 1], [9, 2, 3], "input", {"north": "input_end", "down": "bottom"}))
    els.append(b.box("out_1", [13, 0, 7], [15, 2, 9], "connector", {"east": "connector_end", "down": "bottom"}))
    els.append(b.box("out_2", [7, 0, 13], [9, 2, 15], "connector", {"south": "connector_end", "down": "bottom"}))
    els.append(b.box("out_3", [1, 0, 7], [3, 2, 9], "connector", {"west": "connector_end", "down": "bottom"}))
    # the knob hub: two squares at 45 degrees make an octagon
    els.append(b.box("hub", [6, 6, 6], [10, 7, 10], "knob"))
    els.append(b.box("hub_turned", [6, 6, 6], [10, 7, 10], "knob", rotation={"angle": 45, "axis": "y", "origin": [8, 6, 8]}))
    return els


def pointer(b):
    return [b.box("pointer", [7, 7, 3], [9, 8.5, 8], "knob", {"up": "pointer_top"})]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="write a Blockbench preview under this directory")
    args = ap.parse_args()
    paint()
    b = Builder(REGIONS, f"radiocraft:block/{TEXTURE}", SIZE, stretch=("pointer_top",))
    save(MODELS, "antenna_switch", b.model(body(b)))
    save(MODELS, "antenna_switch_pointer", b.model(pointer(b)))
    if args.preview:
        write_scene(args.preview, "radiocraft", TEXTURES, [([TEXTURE], body(b) + pointer(b), (0, 0, 0))], size=SIZE)


if __name__ == "__main__":
    main()
