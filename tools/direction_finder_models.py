"""Writes the direction finder block, its matching module, their screen (GUI) textures, and the handheld finder.

    python tools/direction_finder_models.py [--preview DIR]

Block models (front north, a coax SO-239 low on the back where the coax wire model runs, like the antenna switch):

- direction_finder            a cream CRT cabinet in the H19's manner but taller (14) and deeper, with a 12 x 9.5 screen,
                              a power button and a pilot lamp on the lower panel
- direction_finder_on         overlay (cutout, full bright): the pilot lamp amber and an amber prompt on the screen
- direction_finder_compass    overlay: the lamp amber and an amber compass rose on the screen
- direction_finder_module     the same cabinet with a floppy slot and eject button, nothing else, and one lamp
- direction_finder_module_red / _green
                              overlays: the module's lamp lit

GUI textures (textures/gui, 300 x 200 like h19_gui.png): direction_finder.png is the cabinet round a black screen
with a side panel (power button up, lamp off, DF nameplate); direction_finder_widgets.png holds the button pressed
and the lamp lit, at the same places. The constants CRT, POWER_BUTTON and LAMP below give the screen class its boxes.

Handheld item (models/block/handheld_direction_finder.json, for an item model to use as its parent): a much
simplified loop direction finder, an olive set with a meter and dial on the front, a loop antenna and a whip on top,
and a lamp on the top edge; handheld_direction_finder_lit is the lamp flashing. Display transforms are included.
"""
import argparse
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ROOT / "models/block"
TEXTURES = ROOT / "textures/block"
GUI = ROOT / "textures/gui"

CUTOUT = "minecraft:cutout"
# Files the maintainer has since edited by hand: the script no longer writes these.
HAND_EDITED = {"direction_finder.json", "direction_finder.png", "direction_finder_module.json",
               "handheld_direction_finder.json", "handheld_direction_finder.png"}
AMBER = (255, 176, 40)
AMBER_DIM = (120, 80, 20)
PANEL = (214, 204, 176)         # cream cabinet
PANEL_LIGHT = (232, 224, 200)
PANEL_DARK = (172, 162, 136)
GLASS = (18, 20, 22)
RED_LIT = (255, 70, 60)
GREEN_LIT = (80, 240, 90)

# ---------------------------------------------------------------- the cabinet (shared by block and module)

FRONT_Z = 3                      # the front plate's face
SCREEN = ([2, 3, FRONT_Z - 0.5], [14, 12.5, FRONT_Z + 0.5])   # the screen glass box
LAMP = ([3, 1.25], [4.25, 2.25])                               # x and y of the pilot lamp on the lower panel
BUTTON = ([11.5, 1], [13.5, 2.5])

SIDE = {
    "side": ("1", 0, 0, 12, 14),
    "coax": ("1", 12, 0, 14, 2),
    "coax_end": ("1", 14, 0, 16, 2),
}

DF = dict(SIDE, **{
    "front": [0, 0, 16, 14],
    "lamp_off": [0, 14, 1, 15],
    "lamp_amber": [1, 14, 2, 15],
    "button": [2, 14, 4, 16],
    "glass": ("2", 0, 0, 6, 4.75),         # the screen texture, 128 px: 6 x 4.75 units stretched over the 12 x 9.5 glass
    "prompt": ("2", 6, 0, 12, 4.75),
    "compass": ("2", 0, 4.75, 6, 9.5),
})

DFM = dict(SIDE, **{
    "front": [0, 0, 16, 14],
    "lamp_off": [0, 14, 1, 15],
    "lamp_red": [1, 14, 2, 15],
    "lamp_green": [2, 14, 3, 15],
    "eject": [3, 14, 4, 15],
})


def paint_side():
    p = Painter(SIDE, 32, seed=11)
    p.fill("side", PANEL, 4, lambda x, y, w, h: PANEL_LIGHT if y < 2 else PANEL_DARK if y >= h - 2 else None)
    knurl = lambda x, y, w, h: GALV_LIGHT if (x + y) % 2 == 0 else GALV_DARK
    p.fill("coax", GALV, 4, knurl)
    p.fill("coax_end", GALV_LIGHT, 4, rim(GALV_DARK))
    for dy in (1, 2):
        for dx in (1, 2):
            p.put("coax_end", dx, dy, BLACK)
    p.write(TEXTURES / "direction_finder_side.png")


def paint_front(regions, name, detail):
    """The front plate at 64 px: cream, a darker lower panel, vents, and `detail(p)` for the block's own features."""
    p = Painter({k: v for k, v in regions.items() if len(v) == 4}, 64, seed=12)
    p.fill("front", PANEL, 4, lambda x, y, w, h: PANEL_LIGHT if y < 2 or x < 2 else PANEL_DARK if y >= h - 12 or x >= w - 2 else None)
    for i in range(5):                           # vent slots along the lower panel's right
        for x in range(26, 40):
            p.put("front", x, 46 + i * 2, PANEL_DARK)
    p.fill("lamp_off", AMBER_DIM, 6, rim((60, 50, 40)))
    detail(p)
    p.write(TEXTURES / f"{name}.png")


def df_front_detail(p):
    p.fill("lamp_amber", AMBER, 8, rim((200, 130, 30)))
    p.fill("button", (46, 42, 40), 4, lambda x, y, w, h: (84, 80, 76) if y < 2 or x < 2 else (24, 22, 20) if y >= h - 2 else None)
    x0, y0, x1, y1 = p.px("front")
    for i, ch in enumerate("DF"):                # the nameplate under the screen, left
        p.glyph("front", 10 + i * 4, 51, {"D": ("##.", "#.#", "#.#", "#.#", "##."), "F": ("###", "#..", "##.", "#..", "#..")}[ch], (60, 50, 40))


def module_front_detail(p):
    p.fill("lamp_red", RED_LIT, 8, rim((180, 40, 30)))
    p.fill("lamp_green", GREEN_LIT, 8, rim((40, 160, 50)))
    p.fill("eject", (40, 38, 36), 4, rim((70, 66, 62)))
    x0, y0, x1, y1 = p.px("front")
    for y in range(y0 + 22, y0 + 26):            # the floppy slot: a dark recess with a lighter lip above
        for x in range(x0 + 16, x0 + 48):
            p.img[y][x] = ((20, 18, 16) if y > y0 + 22 else (236, 228, 206)) + (255,)
    for x in range(x0 + 20, x0 + 44, 4):         # the drive's label line
        p.img[y0 + 32][x] = PANEL_DARK + (255,)


def paint_screen_texture():
    """128 px: the dark glass, the glass with an amber prompt, the glass with the compass rose."""
    regions = {k: v for k, v in DF.items() if len(v) == 5 and v[0] == "2"}
    p = Painter(regions, 128, seed=13)
    curve = lambda x, y, w, h: (34, 36, 40) if (x < 3 or y < 3) else (10, 11, 12) if (x >= w - 3 or y >= h - 3) else None
    for r in regions:
        p.fill(r, GLASS, 3, curve)
    # scanlines on the lit screens
    for r in ("prompt", "compass"):
        x0, y0, x1, y1 = p.px(r)
        for y in range(y0 + 3, y1 - 3, 2):
            for x in range(x0 + 3, x1 - 3):
                p.img[y][x] = (24, 24, 22, 255)
    p.glyph("prompt", 6, 6, (">...", "....", "....", "...."), AMBER)
    for x in range(10, 16):                      # the cursor block
        for y in range(6, 12):
            p.put("prompt", x, y, AMBER)
    # the compass rose: a ring with four points, N marked
    x0, y0, x1, y1 = p.px("compass")
    cx, cy, r = (x1 - x0) / 2, (y1 - y0) / 2, min(x1 - x0, y1 - y0) / 2 - 5
    for y in range(y1 - y0):
        for x in range(x1 - x0):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            on_ring = abs(d - r) < 1
            ang = math.atan2(y + 0.5 - cy, x + 0.5 - cx)
            star = any(abs((ang - a + math.pi) % (2 * math.pi) - math.pi) < 0.08 and d < r for a in (0, math.pi / 2, math.pi, -math.pi / 2))
            if on_ring or star or d < 2:
                p.put("compass", x, y, AMBER)
    p.glyph("compass", int(cx) - 1, 2, ("#.#", "###", "###", "#.#", "#.#"), AMBER)   # N
    p.write(TEXTURES / "direction_finder_screen.png")


def cabinet(b, front="front"):
    els = [
        b.box("body", [0, 0, FRONT_Z + 1], [16, 14, 14], "side"),
        b.box("plate", [0.5, 0, FRONT_Z], [15.5, 14, FRONT_Z + 1], "side", {"north": front}),
        b.box("coax", [7, 0, 14], [9, 2, 16], "coax", {"south": "coax_end"}),
    ]
    return els


def df_elements(b):
    els = cabinet(b)
    els.append(b.box("screen", SCREEN[0], SCREEN[1], "glass", only=("north", "up", "down", "east", "west")))
    els.append(b.box("button", [BUTTON[0][0], BUTTON[0][1], FRONT_Z - 0.5], [BUTTON[1][0], BUTTON[1][1], FRONT_Z], "button"))
    els.append(b.box("lamp", [LAMP[0][0], LAMP[0][1], FRONT_Z - 0.25], [LAMP[1][0], LAMP[1][1], FRONT_Z], "lamp_off"))
    return els


def df_overlay(b, screen_region):
    z = FRONT_Z - 0.5 - 0.02
    return [lamp(b, "lamp_lit", [LAMP[0][0], LAMP[0][1], FRONT_Z - 0.27], [LAMP[1][0], LAMP[1][1], FRONT_Z], "lamp_amber"),
            lamp(b, "screen_lit", [SCREEN[0][0], SCREEN[0][1], z], [SCREEN[1][0], SCREEN[1][1], z], screen_region)]


def module_elements(b):
    els = cabinet(b)
    els.append(b.box("slot_lip", [4, 8.5, FRONT_Z - 0.25], [12, 9, FRONT_Z], "side"))
    els.append(b.box("eject", [12.5, 6.5, FRONT_Z - 0.25], [13.5, 7.5, FRONT_Z], "eject"))
    els.append(b.box("lamp", [LAMP[0][0], LAMP[0][1], FRONT_Z - 0.25], [LAMP[1][0], LAMP[1][1], FRONT_Z], "lamp_off"))
    return els


def module_overlay(b, colour):
    return [lamp(b, "lamp_lit", [LAMP[0][0], LAMP[0][1], FRONT_Z - 0.27], [LAMP[1][0], LAMP[1][1], FRONT_Z], f"lamp_{colour}")]


def df_builder():
    return Builder(DF, {"0": "radiocraft:block/direction_finder", "1": "radiocraft:block/direction_finder_side",
                        "2": "radiocraft:block/direction_finder_screen"}, 64, stretch=("glass", "prompt", "compass"))


def module_builder():
    return Builder(DFM, {"0": "radiocraft:block/direction_finder_module", "1": "radiocraft:block/direction_finder_side"}, 64)


# ---------------------------------------------------------------- the GUI, 300 x 200 like the H19's

GUI_W, GUI_H = 300, 200
CRT = (10, 10, 232, 164)          # the glass: x, y, width, height (the screen class draws its amber text here)
POWER_BUTTON = (252, 150, 38, 20)
GUI_LAMP = (264, 128, 14, 14)
NAMEPLATE = (250, 20, 42, 16)


class Canvas(Painter):
    def __init__(self, seed):
        super().__init__({}, GUI_W, seed, height=GUI_H)

    def rect(self, box, colour, jitter=0, fn=None):
        x0, y0, w, h = box
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                c = (fn and fn(x - x0, y - y0, w, h)) or colour
                if c is None:
                    continue
                j = self.rnd.randint(-jitter, jitter) if jitter else 0
                self.img[y][x] = tuple(max(0, min(255, v + j)) for v in c[:3]) + (255,)


def bevel(light, dark, size=1):
    def fn(x, y, w, h):
        if y < size or x < size:
            return light
        if y >= h - size or x >= w - size:
            return dark
        return None
    return fn


FONT = {"D": ("##.", "#.#", "#.#", "#.#", "##."), "F": ("###", "#..", "##.", "#..", "#.."),
        "P": ("###", "#.#", "###", "#..", "#.."), "W": ("#.#", "#.#", "#.#", "###", "#.#"),
        "R": ("###", "#.#", "##.", "#.#", "#.#"), "O": ("###", "#.#", "#.#", "#.#", "###"),
        "E": ("###", "#..", "##.", "#..", "###"), "-": ("...", "...", "###", "...", "..."),
        "1": (".#.", "##.", ".#.", ".#.", "###"), " ": ("...", "...", "...", "...", "...")}


def text(c, x, y, s, colour, scale=1):
    for ch in s:
        for dy, row in enumerate(FONT[ch]):
            for dx, bit in enumerate(row):
                if bit == "#":
                    c.rect((x + dx * scale, y + dy * scale, scale, scale), colour)
        x += 4 * scale


def button_sprite(c, box, down):
    x0, y0, w, h = box
    c.rect(box, (40, 36, 34))
    face = (x0 + 1, y0 + 1 + (2 if down else 0), w - 2, h - 2 - (2 if down else 0))
    c.rect(face, (70, 66, 62) if not down else (56, 52, 48), 3, bevel((100, 96, 90), (30, 28, 26)))
    text(c, face[0] + (face[2] - 19) // 2, face[1] + (face[3] - 5) // 2, "POWER", (200, 190, 170))


def lamp_sprite(c, box, lit):
    x0, y0, w, h = box
    c.rect(box, (60, 50, 40))
    c.rect((x0 + 1, y0 + 1, w - 2, h - 2), AMBER if lit else AMBER_DIM, 8, bevel((255, 210, 120) if lit else (150, 100, 30), (160, 100, 20) if lit else (80, 50, 10)))


def paint_gui():
    c = Canvas(14)
    c.rect((0, 0, GUI_W, GUI_H), PANEL, 4, bevel(PANEL_LIGHT, PANEL_DARK, 3))
    # the CRT: a dark bezel round black glass with a slight curve
    x, y, w, h = CRT
    c.rect((x - 6, y - 6, w + 12, h + 12), (54, 50, 46), 3, bevel((36, 34, 32), (80, 76, 70), 2))
    c.rect(CRT, GLASS, 2, lambda px, py, pw, ph: (34, 36, 40) if (px < 3 or py < 3) else (8, 9, 10) if (px >= pw - 3 or py >= ph - 3) else None)
    # the side panel
    c.rect((246, 10, 46, 180), PANEL_DARK, 3, bevel((150, 140, 116), PANEL_LIGHT, 2))
    c.rect(NAMEPLATE, (46, 42, 40), 3, bevel((80, 76, 72), (24, 22, 20)))
    text(c, NAMEPLATE[0] + 5, NAMEPLATE[1] + 3, "DF-1", AMBER, 2)
    for i in range(8):                                      # vent slots
        c.rect((252, 50 + i * 6, 34, 2), (150, 140, 116))
    lamp_sprite(c, GUI_LAMP, False)
    text(c, 254, 118, "PWR", (60, 50, 40))
    button_sprite(c, POWER_BUTTON, False)
    c.write(GUI / "direction_finder.png")
    w = Canvas(15)
    button_sprite(w, POWER_BUTTON, True)
    lamp_sprite(w, GUI_LAMP, True)
    w.write(GUI / "direction_finder_widgets.png")


# ---------------------------------------------------------------- the handheld loop direction finder

HH = {
    "olive": [0, 0, 8, 8],
    "front": [8, 0, 16, 8],
    "loop": [0, 8, 2, 10],
    "whip": [2, 8, 3, 10],
    "lamp_off": [3, 8, 4, 9],
    "lamp_lit": [4, 8, 5, 9],
    "knob": [5, 8, 6, 9],
    "mount": [0, 10, 4, 12],
}
OLIVE = (96, 104, 66)
OLIVE_LIGHT = (122, 130, 88)
OLIVE_DARK = (66, 72, 44)
LOOP_C, LOOP_Y, LOOP_R = 8, 13.5, 4


def paint_handheld():
    p = Painter(HH, 32, seed=16)
    wrinkle = speckle_fn = lambda x, y, w, h: OLIVE_DARK if (x * 7 + y * 13) % 9 == 0 else OLIVE_LIGHT if (x * 3 + y * 5) % 11 == 0 else None
    p.fill("olive", OLIVE, 4, wrinkle)
    p.fill("front", OLIVE, 4, wrinkle)
    x0, y0, x1, y1 = p.px("front")
    for y in range(y0 + 2, y0 + 8):              # the meter: a cream window with a needle
        for x in range(x0 + 2, x0 + 8):
            c = (230, 222, 196) if not (x - x0 == 4 + (y - y0 - 2) // 3 and y < y0 + 7) else (40, 36, 30)
            p.img[y][x] = c + (255,)
    for y in range(y0 + 10, y0 + 14):            # the tuning dial: a brass arc
        for x in range(x0 + 2, x0 + 8):
            if abs(math.hypot(x - x0 - 5, y - y0 - 13) - 3) < 1:
                p.img[y][x] = BRASS + (255,)
    for x in range(x0 + 10, x0 + 15):            # label lines on the right
        p.img[y0 + 4][x] = (230, 222, 196, 255)
        p.img[y0 + 12][x] = (230, 222, 196, 255)
    p.fill("loop", OLIVE_DARK, 3, rim(OLIVE))
    p.fill("whip", GALV, 4)
    p.fill("lamp_off", (90, 30, 30), 6, rim((50, 20, 20)))
    p.fill("lamp_lit", RED_LIT, 8, rim((200, 50, 40)))
    p.fill("knob", BAKELITE_DARK, 4, lambda x, y, w, h: (120, 112, 100) if x == 1 and y == 0 else None)
    p.fill("mount", OLIVE_DARK, 4, rim(OLIVE))
    p.write(TEXTURES / "handheld_direction_finder.png")


def loop(b):
    """An octagonal ring in the x-y plane: four axis-aligned segments and the same four turned 45 degrees."""
    seg = LOOP_R * math.tan(math.radians(22.5))
    els = []
    boxes = {
        "top": ([LOOP_C - seg, LOOP_Y + LOOP_R - 0.5, 7.75], [LOOP_C + seg, LOOP_Y + LOOP_R, 8.25]),
        "bottom": ([LOOP_C - seg, LOOP_Y - LOOP_R, 7.75], [LOOP_C + seg, LOOP_Y - LOOP_R + 0.5, 8.25]),
        "west": ([LOOP_C - LOOP_R, LOOP_Y - seg, 7.75], [LOOP_C - LOOP_R + 0.5, LOOP_Y + seg, 8.25]),
        "east": ([LOOP_C + LOOP_R - 0.5, LOOP_Y - seg, 7.75], [LOOP_C + LOOP_R, LOOP_Y + seg, 8.25]),
    }
    for name, (frm, to) in boxes.items():
        frm, to = [round(v, 3) for v in frm], [round(v, 3) for v in to]
        els.append(b.box(f"loop_{name}", frm, to, "loop"))
        els.append(b.box(f"loop_{name}_turned", frm, to, "loop",
                         rotation={"angle": 45, "axis": "z", "origin": [LOOP_C, LOOP_Y, 8]}))
    return els


def handheld_elements(b):
    els = [
        b.box("set", [4, 0, 6], [12, 8, 10], "olive", {"north": "front"}),
        b.box("lid", [4, 8, 6], [12, 8.5, 10], "olive"),
        b.box("mount", [7, 8.5, 7], [9, 9.5, 9], "mount"),
        b.box("stem", [7.5, 9.5, 7.75], [8.5, LOOP_Y - LOOP_R + 0.5, 8.25], "loop"),
        b.box("whip", [11.25, 8.5, 7.75], [11.75, 17, 8.25], "whip"),
        b.box("knob_1", [5, 1, 5.5], [6, 2, 6], "knob"),
        b.box("knob_2", [9, 1, 5.5], [10, 2, 6], "knob"),
        b.box("lamp", [5, 8.5, 6.25], [6, 9, 7.25], "lamp_off"),
    ]
    return els + loop(b)


def handheld_lit(b):
    return [b.box("lamp_lit", [4.98, 8.48, 6.23], [6.02, 9.02, 7.27], "lamp_lit", extra=FULL_BRIGHT)]


HANDHELD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"rotation": [0, -30, 0], "translation": [1, 2, 1], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 30, 0], "translation": [1, 2, 1], "scale": [0.6, 0.6, 0.6]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.55, 0.55, 0.55]},
    "ground": {"scale": [0.4, 0.4, 0.4]},
    "fixed": {"rotation": [0, 180, 0], "scale": [0.6, 0.6, 0.6]},
}


def handheld_builder():
    return Builder(HH, "radiocraft:block/handheld_direction_finder", 32)


# ----------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="write a Blockbench preview under this directory")
    args = ap.parse_args()

    def keep(name):
        kept = name in HAND_EDITED
        if kept:
            print(f"kept {name} (edited by hand)")
        return kept

    paint_side()
    if not keep("direction_finder.png"):
        paint_front(DF, "direction_finder", df_front_detail)
    if not keep("direction_finder_module.png"):
        paint_front(DFM, "direction_finder_module", module_front_detail)
    paint_screen_texture()
    paint_gui()
    if not keep("handheld_direction_finder.png"):
        paint_handheld()

    b = df_builder()
    if not keep("direction_finder.json"):
        save(MODELS, "direction_finder", b.model(df_elements(b)))
    save(MODELS, "direction_finder_on", b.model(df_overlay(b, "prompt"), CUTOUT))
    save(MODELS, "direction_finder_compass", b.model(df_overlay(b, "compass"), CUTOUT))
    m = module_builder()
    if not keep("direction_finder_module.json"):
        save(MODELS, "direction_finder_module", m.model(module_elements(m)))
    for colour in ("red", "green"):
        save(MODELS, f"direction_finder_module_{colour}", m.model(module_overlay(m, colour), CUTOUT))
    h = handheld_builder()
    model = h.model(handheld_elements(h))
    model["display"] = HANDHELD_DISPLAY
    if not keep("handheld_direction_finder.json"):
        save(MODELS, "handheld_direction_finder", model)
    lit = h.model(handheld_lit(h), CUTOUT)
    lit["display"] = HANDHELD_DISPLAY
    save(MODELS, "handheld_direction_finder_lit", lit)

    if args.preview:
        items = [(b.model([])["textures"], df_elements(b) + df_overlay(b, "compass"), (0, 0, 0)),
                 (b.model([])["textures"], df_elements(b) + df_overlay(b, "prompt"), (24, 0, 0)),
                 (m.model([])["textures"], module_elements(m) + module_overlay(m, "green"), (48, 0, 0)),
                 (h.model([])["textures"], handheld_elements(h) + handheld_lit(h), (72, 0, 0))]
        write_scene(args.preview, "radiocraft", TEXTURES, items, size=64)


if __name__ == "__main__":
    main()
