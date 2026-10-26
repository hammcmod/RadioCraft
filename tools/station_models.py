"""Builds the station furniture: the tape player, the commercial repeater rack and the commercial radio
module (a GE MASTR II style cabinet), in the same vintage style as the rest. The microwave horn and the manpack radio
started here too; the horn is drawn in Blockbench now (doc/models/microwave_horn.bbmodel, built by build_models.py)
and the manpack (manpack_radio.json and .png) is edited by hand, so this script leaves both alone.

    python tools/station_models.py [--preview DIR]

Writes under assets/radiocraft (models/block and textures/block). Every model's front is its north face so a plain
`facing` blockstate turns it. The tall ones come as one 16x16x16 model per block cell; the whole thing stacked only
exists in the preview (Minecraft will not load a model past y 32).

- tape_player                    whole, reels at frame 0 (item); tape_player_base (tan steel cabinet, recessed
                                 deck, heads, tape, keys and knobs); tape_player_reels_0..3 (both reels turned 0,
                                 22.5, 45 and 67.5 degrees: cycle them while playing; the four spokes repeat every 90)
- commercial_repeater_0..3       bottom to top: batteries and charger; the repeater radio (lamps) and amplifier;
                                 the duplexer cavities and harness; the controller and roof.
                                 commercial_repeater_1_pwr/_rx/_tx and commercial_repeater_3_beacon are the lit lamps
- commercial_radio_module_0..3   bottom to top: louvred door, door with badge and handle, the control panel (lamps),
                                 top vent and cap. commercial_radio_module_2_pwr/_rx/_tx are the lit lamps
- microwave_radio                a full block: beige chassis with its front panel set 3 back, the screen cut into
                                 it, rack handles, buttons, a knob, jacks and a lamp standing proud of the panel;
                                 microwave_radio_screen is the lit screen overlay
- telephone                      half a block wide and a quarter tall: cream desk phone, brown sloped keypad deck
                                 (22.5 degrees on x), handset in its cradle, line buttons, coiled cord
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from blockmodel import *  # noqa: E402,F403

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/radiocraft"
MODELS = ASSETS / "models/block"
TEXTURES = ASSETS / "textures/block"
CUTOUT = "minecraft:cutout"
FRAMES = (0.0, 22.5, 45.0, -22.5)
PANEL = (38, 30, 26)
PANEL_LIGHT = (58, 48, 42)
CHROME = (190, 194, 196)
CHROME_DARK = (120, 124, 128)
LAMP_OFF = (52, 44, 40)
LIT = {"green": (90, 220, 110), "amber": (240, 180, 60), "red": (240, 70, 60)}
WALNUT = (92, 58, 36)
WALNUT_DARK = (66, 40, 24)
ALU = (176, 178, 174)
ALU_DARK = (140, 142, 140)
TAPE = (58, 40, 30)


def lamp_fill(colour):
    return lambda x, y, w, h: tuple(min(255, c + 40) for c in colour) if 1 <= x <= 2 and 1 <= y <= 2 else None


def lamps(p):
    p.fill("lamp_off", LAMP_OFF, 4, rim((80, 70, 60)))
    for name, colour in LIT.items():
        p.fill(name, colour, 6, lamp_fill(colour))


def meter(cx, cy, r):
    """A round cream meter with a brass bezel and a needle, as a fill shader piece."""
    def fn(x, y):
        d = math.hypot(x - cx, y - cy)
        if d > r:
            return None
        if d > r - 1.5:
            return BRASS
        if abs((x - cx) - (cy - y) * 0.35) < 0.7 and y < cy:
            return (30, 30, 30)
        return (236, 228, 200)
    return fn


def tube_z(b, name, frm, to, mat, overrides=None):
    """A cylinder along z: a box and a copy turned 45 degrees about z, so it reads as round from the front."""
    cx, cy = (frm[0] + to[0]) / 2, (frm[1] + to[1]) / 2
    r = (to[0] - frm[0]) / 2 * 0.83
    return [b.box(name, frm, to, mat, overrides),
            b.box(name + "_turned", [cx - r, cy - r, frm[2] + 0.1], [cx + r, cy + r, to[2] - 0.1], mat, overrides,
                  rotation={"angle": 45.0, "axis": "z", "origin": [cx, cy, frm[2]]})]


# ---------------------------------------------------------------- tape player

TP = {
    "body":   [0, 0, 8, 8],             # off-white tan painted steel
    "deck":   [8, 0, 16, 8],            # the recessed deck plate, a shade darker
    "clear":  [0, 8, 2, 10],            # fully transparent (the reel sprites' edges)
    "head":   [2, 8, 4, 10],
    "tape":   [4, 8, 6, 10],
    "chrome": [6, 8, 8, 10],
    "knob":   [8, 8, 10, 10],
    "key":    [10, 8, 12, 10],
    "reel":   ("1", 0, 0, 16, 16),      # the reel sprite, its own 32px texture
}
TP_TEXTURES = {"0": "radiocraft:block/tape_player", "1": "radiocraft:block/tape_player_reel"}
TAN = (212, 202, 182)
TAN_DARK = (176, 166, 148)
REELS = ((4.75, 9.25), (11.25, 9.25))


def paint_tape_player():
    p = Painter(TP, 32, seed=21)
    p.fill("body", TAN, 2, lambda x, y, w, h: TAN_DARK if x == 0 or y == 0 else None)
    p.fill("deck", TAN_DARK, 2)
    p.fill("clear", (0, 0, 0, 0), 0)
    p.fill("head", CHROME_DARK, 6, edge(CHROME, (90, 92, 96), vertical=False))
    p.fill("tape", TAPE, 4)
    p.fill("chrome", CHROME, 8, edge((230, 232, 234), CHROME_DARK, vertical=False))
    p.fill("knob", (36, 36, 36), 4, lambda x, y, w, h: (200, 200, 196) if x == 1 and y == 0 else None)
    p.fill("key", (232, 226, 206), 3, lambda x, y, w, h: (190, 184, 168) if y == h - 1 else None)
    p.write(TEXTURES / "tape_player.png")

    reel = Painter({"reel": [0, 0, 16, 16]}, 32, seed=22)

    def sprite(x, y, w, h):
        dx, dy = x - 15.5, y - 15.5
        d = math.hypot(dx, dy)
        if d > 15.5:
            return (0, 0, 0, 0)
        if d > 14:
            return CHROME if dx + dy < 0 else CHROME_DARK
        if d < 1.5:
            return (20, 20, 20)
        if d < 5:
            return CHROME_DARK if d > 4 else CHROME
        if abs(dx) < 1.2 or abs(dy) < 1.2:      # four spokes
            return CHROME
        return TAPE if (int(d) % 2) else (70, 50, 38)

    reel.fill("reel", TAPE, 2, sprite)
    reel.write(TEXTURES / "tape_player_reel.png")


def tape_player_base(b):
    e = [b.box("cabinet", [0, 0, 2.5], [16, 16, 16], "body", {"north": "deck"}),
         b.box("top_rail", [0, 13.5, 0], [16, 16, 2.5], "body"),
         b.box("left_rail", [0, 4, 0], [1, 13.5, 2.5], "body"),
         b.box("right_rail", [15, 4, 0], [16, 13.5, 2.5], "body"),
         b.box("controls", [0, 0, 1.25], [16, 4, 2.5], "body"),
         b.box("head_block", [6, 4.25, 1.5], [10, 5.75, 2.25], "head", {"up": "chrome"}),
         b.box("guide_left", [3.5, 4.5, 1.5], [4.25, 6, 2.25], "chrome"),
         b.box("guide_right", [11.75, 4.5, 1.5], [12.5, 6, 2.25], "chrome"),
         b.box("tape_left", [1.5, 5.5, 1.6], [1.8, 9.25, 1.9], "tape"),
         b.box("tape_right", [14.2, 5.5, 1.6], [14.5, 9.25, 1.9], "tape"),
         b.box("tape_run", [1.5, 5.5, 1.6], [14.5, 5.8, 1.9], "tape"),
         b.box("knob_left", [11, 1.25, 0.25], [12.5, 2.75, 1.25], "knob"),
         b.box("knob_right", [13.25, 1.25, 0.25], [14.75, 2.75, 1.25], "knob")]
    for k in range(3):
        x = 2 + 2 * k
        e.append(b.box(f"key_{k}", [x, 1, 0.5], [x + 1.5, 1.75, 1.25], "key"))
    return e


def tape_player_reels(b, angle):
    e = []
    for i, (cx, cy) in enumerate(REELS):
        rot = {"angle": angle, "axis": "z", "origin": [cx, cy, 1.75]}
        e.append(b.box(f"reel_{i}", [cx - 3, cy - 3, 1.5], [cx + 3, cy + 3, 2], "clear", {"north": "reel"},
                       rotation=rot))
    return e


def build_tape_player():
    b = Builder(TP, TP_TEXTURES, 32, stretch=("reel",))
    save(MODELS, "tape_player", b.model(tape_player_base(b) + tape_player_reels(b, 0.0), CUTOUT))
    save(MODELS, "tape_player_base", b.model(tape_player_base(b)))
    for i, a in enumerate(FRAMES):
        save(MODELS, f"tape_player_reels_{i}", b.model(tape_player_reels(b, a), CUTOUT))
    paint_tape_player()


# ---------------------------------------------------------------- commercial repeater (open rack, four blocks)

CR = {
    "steel":      [0, 0, 8, 8],
    "inner":      [8, 0, 16, 8],
    "shelf":      [0, 8, 8, 9],
    "battery":    [8, 8, 12, 11],
    "cavity":     [0, 9, 2, 16],
    "cavity_end": [2, 9, 4, 11],
    "chassis":    [4, 11, 12, 14],
    "panel":      [4, 14, 12, 16],
    "cable":      [12, 8, 14, 11],
    "lamp_off":   [12, 11, 13, 12],
    "green":      [13, 11, 14, 12],
    "red":        [14, 11, 15, 12],
    "amber":      [15, 11, 16, 12],
    "hammertone": [12, 12, 16, 16],
    "feet":       [2, 11, 4, 13],
}
CR_LAMPS = (("pwr", "amber", 9.5), ("rx", "green", 11), ("tx", "red", 12.5))


def paint_commercial_repeater():
    p = Painter(CR, 64, seed=31)
    p.fill("steel", GALV, 5, lambda x, y, w, h: GALV_DARK if x % 16 == 0 or y % 16 == 0 else None)
    p.fill("inner", GALV_DARK, 5)
    p.fill("shelf", GALV, 4, lambda x, y, w, h: (70, 72, 70) if x % 2 == 0 and y % 2 == 0 else None)
    p.fill("battery", (40, 70, 150), 6, lambda x, y, w, h: CHROME if y == 0 and x % 5 == 1 else (30, 50, 110) if y == 0 else None)
    p.fill("cavity", CHROME, 5, lambda x, y, w, h: (230, 232, 234) if x in (1, 2) else CHROME_DARK if x >= w - 2 else None)
    p.fill("cavity_end", CHROME_DARK, 5, rim(CHROME))
    p.fill("chassis", HAMMERTONE, 8, lambda x, y, w, h: meter(8, 6, 5)(x, y) or hammertone(x, y, w, h))
    p.fill("panel", HAMMERTONE, 8, lambda x, y, w, h: (30, 30, 30) if 3 <= y <= 5 and x % 4 in (1, 2) and x < 24 else hammertone(x, y, w, h))
    p.fill("cable", (30, 30, 30), 5, lambda x, y, w, h: (170, 40, 40) if x % 4 == 1 else (40, 60, 160) if x % 4 == 3 else None)
    lamps(p)
    p.fill("hammertone", HAMMERTONE, 10, hammertone)
    p.fill("feet", IRON, 6)
    return p


def rack_shell(b, cell):
    e = [b.box("back", [1, 0, 14], [15, 16, 15], "steel", {"north": "inner"}),
         b.box("side_w", [1, 0, 1], [2, 16, 14], "steel", {"east": "inner"}),
         b.box("side_e", [14, 0, 1], [15, 16, 14], "steel", {"west": "inner"})]
    if cell == 3:
        e[0]["to"][1] = e[1]["to"][1] = e[2]["to"][1] = 14
    return e


def rack_shelf(b, y):
    return b.box(f"shelf_{y}", [2, y, 2], [14, y + 0.5, 14], "shelf")


def commercial_repeater_cell(b, cell):
    e = rack_shell(b, cell)
    if cell == 0:
        e += [b.box("floor", [1, 1, 1], [15, 2, 14], "steel")]
        e += [b.box(f"foot_{i}", [x, 0, z], [x + 2, 1, z + 2], "feet")
              for i, (x, z) in enumerate(((1, 1), (13, 1), (1, 12), (13, 12)))]
        e += [b.box(f"battery_{i}", [x, 2, z], [x + 5, 8, z + 5], "battery")
              for i, (x, z) in enumerate(((2.5, 3), (8.5, 3), (2.5, 8.5), (8.5, 8.5)))]
        e += [b.box("cables", [6, 8, 4], [10, 10, 13], "cable"), rack_shelf(b, 12),
              b.box("charger", [3, 12.5, 4], [13, 15.5, 13], "hammertone", {"north": "panel"})]
    elif cell == 1:
        e += [rack_shelf(b, 0.5), b.box("radio", [2.5, 1, 3], [13.5, 7.5, 14], "hammertone", {"north": "chassis"})]
        e += [b.box(f"bezel_{n}", [x, 5.25, 2.75], [x + 1, 6.25, 3], "lamp_off") for n, _, x in CR_LAMPS]
        e += [rack_shelf(b, 9), b.box("amplifier", [2.5, 9.5, 3], [13.5, 14.5, 14], "hammertone", {"north": "panel"})]
    elif cell == 2:
        e += tube_z(b, "cavity_w", [2.5, 2, 2.5], [7.5, 7, 13.5], "cavity", {"north": "cavity_end", "south": "cavity_end"})
        e += tube_z(b, "cavity_e", [8.5, 2, 2.5], [13.5, 7, 13.5], "cavity", {"north": "cavity_end", "south": "cavity_end"})
        e += [rack_shelf(b, 8), b.box("harness", [3, 8.5, 3], [13, 12.5, 13], "inner", {"north": "cable"}),
              b.box("coax", [12, 12.5, 8], [13, 16, 9], "cable")]
    else:
        e += [rack_shelf(b, 0.5), b.box("controller", [2.5, 1, 3], [13.5, 5.5, 14], "hammertone", {"north": "panel"}),
              b.box("bezel_beacon", [7.5, 3, 2.75], [8.5, 4, 3], "lamp_off"),
              b.box("feedthrough", [5, 6, 4], [11, 10, 13], "inner"),
              b.box("roof", [0, 14, 0], [16, 15.25, 16], "steel"),
              b.box("cap", [0.5, 15.25, 0.5], [15.5, 16, 15.5], "steel")]
    return e


def build_commercial_repeater():
    b = Builder(CR, "radiocraft:block/commercial_repeater", 64)
    for cell in range(4):
        save(MODELS, f"commercial_repeater_{cell}", b.model(commercial_repeater_cell(b, cell)))
    for n, colour, x in CR_LAMPS:
        save(MODELS, f"commercial_repeater_1_{n}", b.model([lamp(b, n, [x, 5.25, 2.74], [x + 1, 6.25, 3], colour)], CUTOUT))
    save(MODELS, "commercial_repeater_3_beacon", b.model([lamp(b, "beacon", [7.5, 3, 2.74], [8.5, 4, 3], "amber")], CUTOUT))
    paint_commercial_repeater().write(TEXTURES / "commercial_repeater.png")


# ---------------------------------------------------------------- commercial radio module (GE MASTR II cabinet)

CM = {
    "skin":     [0, 0, 8, 8],
    "grille":   [8, 0, 16, 4],
    "badge":    [8, 4, 11, 5.5],
    "handle":   [11, 4, 13, 6],
    "plinth":   [8, 6, 16, 8],
    "control":  [0, 8, 14, 14],
    "lamp_off": [14, 8, 15, 9],
    "green":    [15, 8, 16, 9],
    "red":      [14, 9, 15, 10],
    "amber":    [15, 9, 16, 10],
}
CM_LAMPS = (("pwr", "amber", 9.5), ("rx", "green", 11), ("tx", "red", 12.5))
TAN = (182, 176, 160)
TAN_DARK = (150, 144, 130)


def paint_commercial_radio_module():
    p = Painter(CM, 64, seed=41)
    p.fill("skin", TAN, 2, lambda x, y, w, h: TAN_DARK if x == 0 or y == 0 else None)
    p.fill("grille", TAN, 2, lambda x, y, w, h: (60, 58, 54) if y % 4 == 1 and 2 <= x <= w - 3 else TAN_DARK if y % 4 == 2 and 2 <= x <= w - 3 else None)
    p.fill("badge", BRASS, 6, lambda x, y, w, h: BRASS_DARK if (x, y) in ((3, 1), (4, 1), (3, 2), (5, 2), (3, 3), (4, 3), (5, 3), (7, 1), (8, 1), (7, 2), (7, 3), (8, 3)) else rim(BRASS_DARK)(x, y, w, h))
    p.fill("handle", CHROME, 8, edge((230, 232, 234), CHROME_DARK))
    p.fill("plinth", (50, 50, 48), 4)

    def control(x, y, w, h):
        if min(x, y, w - 1 - x, h - 1 - y) == 0:
            return PANEL_LIGHT
        c = meter(12, 12, 8)(x, y)
        if c:
            return c
        if 2 <= y <= 4 and 26 <= x <= 44:                 # brass nameplate
            return BRASS if y == 2 else BRASS_DARK if (x % 3) else (60, 48, 30)
        if 15 <= y <= 19 and 26 <= x <= 36 and x % 5 in (1, 2):   # toggle switches
            return CHROME if y != 17 else (30, 30, 30)
        return None

    p.fill("control", PANEL, 3, control)
    lamps(p)
    return p


def commercial_radio_module_cell(b, cell):
    if cell == 0:
        return [b.box("plinth", [0.5, 0, 0.5], [15.5, 1, 15.5], "plinth"),
                b.box("body", [1, 1, 1], [15, 16, 15], "skin"),
                b.box("grille", [2, 2, 0.75], [14, 14, 1], "skin", {"north": "grille"})]
    if cell == 1:
        return [b.box("body", [1, 0, 1], [15, 16, 15], "skin"),
                b.box("handle", [12.5, 6, 0.5], [13.5, 10, 1], "handle"),
                b.box("badge", [2, 13, 0.75], [5, 14.5, 1], "badge")]
    if cell == 2:
        e = [b.box("body", [1, 0, 1], [15, 16, 15], "skin"),
             b.box("control", [1.5, 3, 0.5], [14.5, 11, 1], "skin", {"north": "control"})]
        e += [b.box(f"bezel_{n}", [x, 8.5, 0.25], [x + 1, 9.5, 0.5], "lamp_off") for n, _, x in CM_LAMPS]
        return e
    return [b.box("body", [1, 0, 1], [15, 15, 15], "skin"),
            b.box("cap", [0.5, 15, 0.5], [15.5, 16, 15.5], "skin"),
            b.box("vent", [3, 9, 0.75], [13, 13, 1], "skin", {"north": "grille"})]


def build_commercial_radio_module():
    b = Builder(CM, "radiocraft:block/commercial_radio_module", 64)
    for cell in range(4):
        save(MODELS, f"commercial_radio_module_{cell}", b.model(commercial_radio_module_cell(b, cell)))
    for n, colour, x in CM_LAMPS:
        save(MODELS, f"commercial_radio_module_2_{n}", b.model([lamp(b, n, [x, 8.5, 0.24], [x + 1, 9.5, 0.5], colour)], CUTOUT))
    paint_commercial_radio_module().write(TEXTURES / "commercial_radio_module.png")


# ---------------------------------------------------------------- microwave radio (a full block, screen on the front)

MR = {
    "beige":    [0, 0, 4, 4],       # painted aluminium
    "glass":    [4, 0, 8, 4],       # the dark screen
    "lid":      [8, 0, 16, 8],      # the top with its screws
    "chrome":   [0, 4, 2, 6],
    "knob":     [2, 4, 4, 6],
    "button":   [4, 4, 6, 6],
    "lamp":     [6, 4, 8, 6],
    "lit":      [0, 6, 4, 10],      # the screen glowing
}
BEIGE = (196, 186, 164)
BEIGE_DARK = (160, 150, 130)
SCREEN = [3, 7, 10, 13]           # the screen opening on the front, in units
FRONT = 3                         # the front panel sits this far back from the block face


def paint_microwave_radio():
    p = Painter(MR, 32, seed=61)
    p.fill("beige", BEIGE, 2, lambda x, y, w, h: BEIGE_DARK if x == 0 or y == 0 else None)
    p.fill("glass", (18, 26, 20), 3)
    p.fill("lid", BEIGE, 2, lambda x, y, w, h: CHROME_DARK if (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)) else BEIGE_DARK if x == 0 or y == 0 else None)
    p.fill("chrome", CHROME, 8, edge((230, 232, 234), CHROME_DARK))
    p.fill("knob", (36, 36, 36), 4, lambda x, y, w, h: (200, 200, 196) if x == 1 and y == 0 else None)
    p.fill("button", (232, 226, 206), 3, lambda x, y, w, h: (190, 184, 168) if y == h - 1 else None)
    p.fill("lamp", (200, 40, 40), 5, rim((150, 30, 30)))
    p.fill("lit", (120, 200, 140), 6, lambda x, y, w, h: (100, 170, 120) if x % 4 == 0 or y % 4 == 0 else None)
    return p


def microwave_radio_body(b):
    z = FRONT
    x0, y0, x1, y1 = SCREEN
    e = [b.box("chassis", [0, 0, z + 1], [16, 16, 16], "beige", {"north": "glass", "up": "lid"}),
         # the front panel, with the screen opening cut through it
         b.box("panel_top", [0, y1, z], [16, 16, z + 1], "beige", {"up": "lid"}),
         b.box("panel_bottom", [0, 0, z], [16, y0, z + 1], "beige"),
         b.box("panel_left", [0, y0, z], [x0, y1, z + 1], "beige"),
         b.box("panel_right", [x1, y0, z], [16, y1, z + 1], "beige"),
         # rack handles either side
         b.box("handle_l", [1, 3, 0.5], [2.5, 13, 1.25], "chrome"),
         b.box("handle_l_top", [1, 11.5, 1.25], [2.5, 13, z], "chrome"),
         b.box("handle_l_bottom", [1, 3, 1.25], [2.5, 4.5, z], "chrome"),
         b.box("handle_r", [13.5, 3, 0.5], [15, 13, 1.25], "chrome"),
         b.box("handle_r_top", [13.5, 11.5, 1.25], [15, 13, z], "chrome"),
         b.box("handle_r_bottom", [13.5, 3, 1.25], [15, 4.5, z], "chrome"),
         # a knob, two jacks and a lamp on the right, three buttons on the left
         b.box("knob", [11, 2, z - 1], [13, 4, z], "knob"),
         b.box("jack_a", [11, 9, z - 0.75], [12, 10, z], "chrome"),
         b.box("jack_b", [12.5, 9, z - 0.75], [13.5, 10, z], "chrome"),
         b.box("lamp", [11.5, 14, z - 0.25], [12.5, 15, z], "lamp")]
    for k in range(3):
        y = 2 + 1.5 * k
        e.append(b.box(f"button_{k}", [3.5, y, z - 0.5], [5, y + 1, z], "button"))
    return e


def microwave_radio_screen(b):
    x0, y0, x1, y1 = SCREEN
    return [lamp(b, "screen", [x0, y0, FRONT + 0.98], [x1, y1, FRONT + 1], "lit")]


def build_microwave_radio():
    b = Builder(MR, "radiocraft:block/microwave_radio", 32)
    save(MODELS, "microwave_radio", b.model(microwave_radio_body(b)))
    save(MODELS, "microwave_radio_screen", b.model(microwave_radio_screen(b), CUTOUT))
    paint_microwave_radio().write(TEXTURES / "microwave_radio.png")


# ---------------------------------------------------------------- telephone (half a block wide, a quarter tall)

TL = {
    "body":       [0, 0, 8, 8],         # cream
    "deck":       [8, 0, 12.75, 6],     # brown with the keypad
    "brown":      [8, 6, 10, 8],
    "button":     [13, 0, 14, 1],
    "button_red": [14, 0, 15, 1],
    "cord":       [13, 1, 15, 2],
}
PHONE_CREAM = (222, 212, 190)
PHONE_BROWN = (74, 56, 46)
DECK_ROT = {"angle": -22.5, "axis": "x", "origin": [7, 3.5, 11.25]}


def paint_telephone():
    p = Painter(TL, 64, seed=71)
    p.fill("body", PHONE_CREAM, 3, lambda x, y, w, h: (200, 190, 168) if y == h - 1 else None)

    def deck(x, y, w, h):
        kx, ky = (x - 4) % 4, (y - 6) % 4
        if 4 <= x <= 14 and 6 <= y <= 20 and kx < 3 and ky < 3:
            return (236, 236, 230) if (kx, ky) == (1, 1) else (48, 48, 48)
        return None

    p.fill("deck", PHONE_BROWN, 4, deck)
    p.fill("brown", PHONE_BROWN, 4)
    p.fill("button", (236, 236, 230), 4, rim((190, 190, 184)))
    p.fill("button_red", (200, 40, 40), 4, rim((150, 30, 30)))
    p.fill("cord", PHONE_CREAM, 4)
    return p


def telephone(b):
    e = [b.box("tray", [4, 0, 4.5], [12, 1.25, 11.5], "body"),
         b.box("cradle", [4.25, 1.25, 4.75], [6.75, 2.25, 11.25], "body"),
         b.box("deck_fill", [7, 1.25, 6], [11.5, 1.5, 11.25], "brown"),
         b.box("deck", [7, 2.75, 6], [11.5, 3.5, 11.25], "brown", {"up": "deck"}, rotation=dict(DECK_ROT)),
         b.box("earpiece", [4.3, 2.25, 5], [6.7, 3.5, 6.75], "body"),
         b.box("mouthpiece", [4.3, 2.25, 9.5], [6.7, 3.5, 11.25], "body"),
         b.box("handle", [4.9, 2.75, 6.75], [6.1, 3.75, 9.5], "body"),
         b.box("cord_0", [5, 0, 3.5], [5.75, 0.75, 4.5], "cord"),
         b.box("cord_1", [4, 0, 2.75], [5, 0.75, 3.5], "cord"),
         b.box("cord_2", [5, 0, 2], [6, 0.75, 2.75], "cord"),
         b.box("cord_3", [4.25, 0.75, 4], [4.75, 2.25, 4.5], "cord")]
    for i in range(6):
        z = 6.25 + 0.75 * i
        e.append(b.box(f"line_{i}", [11.6, 3.5, z], [12.2, 3.9, z + 0.6], "body",
                       {"up": "button_red" if i == 5 else "button"}, rotation=dict(DECK_ROT)))
    return e


def build_telephone():
    b = Builder(TL, "radiocraft:block/telephone", 64)
    save(MODELS, "telephone", b.model(telephone(b)))
    paint_telephone().write(TEXTURES / "telephone.png")


# ---------------------------------------------------------------- preview

def lift(elements, dy):
    for el in elements:
        el["from"][1] += dy
        el["to"][1] += dy
        if "rotation" in el:
            el["rotation"]["origin"][1] += dy
    return elements


def stack(b, cell_fn, lit=()):
    elements = []
    for cell in range(4):
        elements += lift(cell_fn(b, cell), 16 * cell)
    return elements + lit


def build_preview(root):
    tp = Builder(TP, "", 32, stretch=("reel",))
    cr = Builder(CR, "", 64)
    cm = Builder(CM, "", 64)
    cr_lit = lift([lamp(cr, n, [x, 5.25, 2.74], [x + 1, 6.25, 3], c) for n, c, x in CR_LAMPS], 16)
    cr_lit += lift([lamp(cr, "beacon", [7.5, 3, 2.74], [8.5, 4, 3], "amber")], 48)
    cm_lit = lift([lamp(cm, n, [x, 8.5, 0.24], [x + 1, 9.5, 0.5], c) for n, c, x in CM_LAMPS], 32)
    mr = Builder(MR, "", 32)
    rows = [(["tape_player", "tape_player_reel"], tape_player_base(tp) + tape_player_reels(tp, 0.0)),
            (["microwave_radio"], microwave_radio_body(mr) + microwave_radio_screen(mr)),
            (["telephone"], telephone(Builder(TL, "", 64))),
            (["commercial_repeater"], stack(cr, commercial_repeater_cell, cr_lit)),
            (["commercial_radio_module"], stack(cm, commercial_radio_module_cell, cm_lit))]
    write_preview(root, "radiocraft", TEXTURES, rows, 64, spacing=24)


if __name__ == "__main__":
    build_tape_player()
    build_commercial_repeater()
    build_commercial_radio_module()
    build_microwave_radio()
    build_telephone()
    if len(sys.argv) > 2 and sys.argv[1] == "--preview":
        build_preview(sys.argv[2])
